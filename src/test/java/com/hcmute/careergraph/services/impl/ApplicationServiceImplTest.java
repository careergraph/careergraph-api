package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationRequest;
import com.hcmute.careergraph.persistence.models.Application;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.repositories.ApplicationRepository;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.CompanyRecruitmentStageService;
import com.hcmute.careergraph.services.MailService;
import com.hcmute.careergraph.services.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private MailService mailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private CompanyRecruitmentStageService companyRecruitmentStageService;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void createApplication_shouldRejectExpiredJob() {
        Candidate candidate = Candidate.builder()
                .id("candidate-1")
                .build();
        Company company = Company.builder()
                .id("company-1")
                .build();
        Job job = Job.builder()
                .id("job-1")
                .status(Status.ACTIVE)
                .expiryDate(LocalDate.now().minusDays(1).toString())
                .company(company)
                .build();

        ApplicationRequest request = ApplicationRequest.builder()
                .candidateId(candidate.getId())
                .jobId(job.getId())
                .resumeUrl("/resumes/cv.pdf")
                .build();

        when(candidateRepository.findById(candidate.getId())).thenReturn(Optional.of(candidate));
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> applicationService.createApplication(request));

        assertEquals("Job application deadline has passed", exception.getMessage());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void createApplication_shouldPersistWhenJobIsStillOpen() {
        Candidate candidate = Candidate.builder()
                .id("candidate-1")
                .build();
        Company company = Company.builder()
                .id("company-1")
                .build();
        Job job = Job.builder()
                .id("job-1")
                .status(Status.ACTIVE)
                .expiryDate(LocalDate.now().plusDays(1).toString())
                .company(company)
                .build();

        ApplicationRequest request = ApplicationRequest.builder()
                .candidateId(candidate.getId())
                .jobId(job.getId())
                .resumeUrl("/resumes/cv.pdf")
                .coverLetter("I am interested in this role.")
                .notes("Test notes")
                .appliedDate("2026-06-13T10:00:00")
                .build();

        when(candidateRepository.findById(candidate.getId())).thenReturn(Optional.of(candidate));
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.createApplication(request);

        assertEquals(job.getId(), result.getJob().getId());
        assertEquals(candidate.getId(), result.getCandidate().getId());
        assertEquals(ApplicationStage.APPLIED, result.getCurrentStage());
        assertEquals(1, result.getStageHistory().size());
        verify(applicationRepository).save(any(Application.class));
    }
}
