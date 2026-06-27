package com.hcmute.careergraph.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.helper.CvKeywordsHeuristicExtractor;
import com.hcmute.careergraph.persistence.dtos.response.CandidateSearchProfile;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.File;
import com.hcmute.careergraph.repositories.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateSearchTextBuilderTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private CvKeywordsHeuristicExtractor heuristicExtractor;

    private CandidateSearchTextBuilder candidateSearchTextBuilder;

    @BeforeEach
    void setUp() {
        candidateSearchTextBuilder = new CandidateSearchTextBuilder(
                fileRepository,
                heuristicExtractor,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(candidateSearchTextBuilder, "cvKeywordsStrategy", "HYBRID");
        ReflectionTestUtils.setField(candidateSearchTextBuilder, "maxKeywordsChars", 300);
    }

    @Test
    void buildProfile_shouldPreferExplicitJobCriteriaOverCvContent() {
        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");
        candidate.setDesiredPosition("Backend Developer");
        candidate.setIndustries(List.of("Software"));
        candidate.setLocations(List.of("Ho Chi Minh"));
        candidate.setWorkTypes(List.of("Full time"));

        File resume = new File();
        resume.setCvKeywordsJson("{\"searchKeywords\":\"Java Spring Docker\"}");

        when(fileRepository.findByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(
                "candidate-1",
                Status.ACTIVE,
                List.of(FileType.RESUME, FileType.CV)
        )).thenReturn(List.of(resume));

        CandidateSearchProfile profile = candidateSearchTextBuilder.buildProfile(candidate);

        assertThat(profile.getIntentText()).contains("Backend Developer", "Software", "Ho Chi Minh", "Full time");
        assertThat(profile.getEmbeddingText()).isEqualTo(profile.getIntentText());
        assertThat(profile.isHasIntent()).isTrue();
        verify(heuristicExtractor, never()).extract(anyString(), anyInt());
    }

    @Test
    void buildProfile_shouldAggregateCvKeywordsFromAllActiveResumesWhenNoIntentExists() {
        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");

        File latestResume = new File();
        latestResume.setCvKeywordsJson("{\"searchKeywords\":\"Java Spring Boot\"}");

        File olderResume = new File();
        olderResume.setResumeExtractedText("React TypeScript Next.js frontend");

        when(fileRepository.findByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(
                "candidate-1",
                Status.ACTIVE,
                List.of(FileType.RESUME, FileType.CV)
        )).thenReturn(List.of(latestResume, olderResume));
        when(heuristicExtractor.extract("React TypeScript Next.js frontend", 300))
                .thenReturn("React TypeScript Next.js");

        CandidateSearchProfile profile = candidateSearchTextBuilder.buildProfile(candidate);

        assertThat(profile.getIntentText()).isEmpty();
        assertThat(profile.isHasIntent()).isFalse();
        assertThat(profile.getCvKeywords()).contains("Java Spring Boot", "React TypeScript Next.js");
        assertThat(profile.getEmbeddingText()).isEqualTo(profile.getCvKeywords());
    }
}
