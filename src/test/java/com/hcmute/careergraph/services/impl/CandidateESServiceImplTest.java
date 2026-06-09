package com.hcmute.careergraph.services.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.persistence.documents.CandidateES;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.File;
import com.hcmute.careergraph.repositories.CandidateESRepository;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.FileRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.EmbedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateESServiceImplTest {

  @Mock
  private CandidateESRepository candidateESRepository;
  @Mock
  private CandidateRepository candidateRepository;
  @Mock
  private JobRepository jobRepository;
  @Mock
  private FileRepository fileRepository;
  @Mock
  private ElasticsearchClient client;
  @Mock
  private EmbedService embedService;

  private CandidateESServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new CandidateESServiceImpl(
        candidateESRepository,
        candidateRepository,
        jobRepository,
        fileRepository,
        client,
        embedService,
        new ObjectMapper());
  }

  @Test
  void indexCandidate_shouldDeleteDocumentWhenCandidateIsNotOpenToWork() {
    Candidate candidate = Candidate.builder()
        .id("candidate-1")
        .isOpenToWork(false)
        .desiredPosition("Backend Developer")
        .build();

    CandidateES result = service.indexCandidate(candidate);

    assertNull(result);
    verify(candidateESRepository).deleteById("candidate-1");
    verify(candidateESRepository, never()).save(any());
    verify(embedService, never()).embed(anyString());
  }

  @Test
  void indexCandidate_shouldUseOnlySharedCvForHrSearch() {
    Candidate candidate = Candidate.builder()
        .id("candidate-2")
        .isOpenToWork(true)
        .build();
    File sharedCv = new File();
    sharedCv.setId("cv-1");
    sharedCv.setFileType(FileType.CV);
    sharedCv.setStatus(Status.ACTIVE);
    sharedCv.setShareToFindJob(true);
    sharedCv.setResumeExtractedText("Backend engineer Spring Boot PostgreSQL Elasticsearch");
    sharedCv.setCvKeywordsJson("{\"searchKeywords\":\"backend engineer spring boot elasticsearch\"}");
    sharedCv.setResumeContentHash("hash-1");

    when(fileRepository.findFirstByOwnerIdAndStatusAndFileTypeInAndShareToFindJobTrueOrderByCreatedDateDesc(
        eq("candidate-2"), eq(Status.ACTIVE), anyList()))
        .thenReturn(Optional.of(sharedCv));
    when(embedService.embed(anyString())).thenReturn(new float[3072]);
    when(candidateESRepository.save(any(CandidateES.class))).thenAnswer(inv -> inv.getArgument(0));

    CandidateES result = service.indexCandidate(candidate);

    assertNotNull(result);
    ArgumentCaptor<CandidateES> captor = ArgumentCaptor.forClass(CandidateES.class);
    verify(candidateESRepository).save(captor.capture());
    CandidateES saved = captor.getValue();
    assertEquals("cv-1", saved.getResumeFileId());
    assertEquals("backend engineer spring boot elasticsearch", saved.getCvKeywords());
    assertTrue(saved.getResumeText().contains("Spring Boot"));
    verify(embedService).embed(contains("backend engineer spring boot elasticsearch"));
  }

  @Test
  void syncAllCandidates_shouldDeleteUnsearchableCandidatesAndCountIndexedOnesOnly() {
    Candidate hidden = Candidate.builder()
        .id("hidden")
        .isOpenToWork(false)
        .desiredPosition("Backend Developer")
        .build();
    Candidate searchable = Candidate.builder()
        .id("searchable")
        .isOpenToWork(true)
        .desiredPosition("Frontend Developer")
        .build();

    when(candidateRepository.findAllIds()).thenReturn(List.of("hidden", "searchable"));
    when(candidateRepository.findByIdWithCollections("hidden")).thenReturn(Optional.of(hidden));
    when(candidateRepository.findByIdWithCollections("searchable")).thenReturn(Optional.of(searchable));
    when(fileRepository.findFirstByOwnerIdAndStatusAndFileTypeInAndShareToFindJobTrueOrderByCreatedDateDesc(
        eq("searchable"), eq(Status.ACTIVE), anyList()))
        .thenReturn(Optional.empty());
    when(embedService.embed(anyString())).thenReturn(new float[3072]);
    when(candidateESRepository.save(any(CandidateES.class))).thenAnswer(inv -> inv.getArgument(0));

    int synced = service.syncAllCandidates();

    assertEquals(1, synced);
    verify(candidateESRepository).deleteById("hidden");
    verify(candidateESRepository).save(any(CandidateES.class));
  }
}
