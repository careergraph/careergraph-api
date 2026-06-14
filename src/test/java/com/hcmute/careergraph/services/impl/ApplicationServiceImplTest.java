package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationRequest;
import com.hcmute.careergraph.persistence.dtos.request.ApplicationStageUpdateRequest;
import com.hcmute.careergraph.persistence.models.Application;
import com.hcmute.careergraph.persistence.models.Account;
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
import org.mockito.ArgumentCaptor;
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
        when(applicationRepository.findFirstByCandidateIdAndJobIdOrderByCreatedDateDesc(
                candidate.getId(), job.getId())).thenReturn(Optional.empty());
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.createApplication(request);

        assertEquals(job.getId(), result.getJob().getId());
        assertEquals(candidate.getId(), result.getCandidate().getId());
        assertEquals(ApplicationStage.APPLIED, result.getCurrentStage());
        assertEquals(1, result.getStageHistory().size());
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void promoteToHrContactedOnHrMessage_shouldMoveAppliedApplicationToHrContacted() {
        Company company = Company.builder()
                .id("company-1")
                .build();
        Job job = Job.builder()
                .id("job-1")
                .company(company)
                .build();
        Application application = Application.builder()
                .id("app-1")
                .job(job)
                .currentStage(ApplicationStage.APPLIED)
                .build();
        Account hrAccount = Account.builder()
                .id("acc-hr")
                .company(company)
                .build();

        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(companyRecruitmentStageService.isStageActiveForCompany(company.getId(), ApplicationStage.HR_CONTACTED))
                .thenReturn(true);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        applicationService.promoteToHrContactedOnHrMessage(application.getId(), hrAccount);

        ArgumentCaptor<Application> savedCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(savedCaptor.capture());
        assertEquals(ApplicationStage.HR_CONTACTED, savedCaptor.getValue().getCurrentStage());
    }

    @Test
    void promoteToHrContactedOnHrMessage_shouldSkipWhenAlreadyContacted() {
        Application application = Application.builder()
                .id("app-1")
                .currentStage(ApplicationStage.HR_CONTACTED)
                .build();

        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

        applicationService.promoteToHrContactedOnHrMessage(application.getId(), null);

        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void isApplicationReapplyBlocked_shouldReturnFalseWhenRejected() {
        Application application = Application.builder()
                .id("app-1")
                .currentStage(ApplicationStage.REJECTED)
                .build();

        when(applicationRepository.findFirstByCandidateIdAndJobIdOrderByCreatedDateDesc("candidate-1", "job-1"))
                .thenReturn(Optional.of(application));

        assertEquals(false, applicationService.isApplicationReapplyBlocked("job-1", "candidate-1"));
    }

    @Test
    void isApplicationReapplyBlocked_shouldReturnTrueWhenScreening() {
        Application application = Application.builder()
                .id("app-1")
                .currentStage(ApplicationStage.SCREENING)
                .build();

        when(applicationRepository.findFirstByCandidateIdAndJobIdOrderByCreatedDateDesc("candidate-1", "job-1"))
                .thenReturn(Optional.of(application));

        assertEquals(true, applicationService.isApplicationReapplyBlocked("job-1", "candidate-1"));
    }

    @Test
    void createApplication_shouldReapplyWhenPreviouslyRejected() {
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
        Application existing = Application.builder()
                .id("app-1")
                .candidate(candidate)
                .job(job)
                .currentStage(ApplicationStage.REJECTED)
                .build();

        ApplicationRequest request = ApplicationRequest.builder()
                .candidateId(candidate.getId())
                .jobId(job.getId())
                .resumeUrl("/resumes/new-cv.pdf")
                .coverLetter("Reapplying")
                .build();

        when(candidateRepository.findById(candidate.getId())).thenReturn(Optional.of(candidate));
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(applicationRepository.findFirstByCandidateIdAndJobIdOrderByCreatedDateDesc(
                candidate.getId(), job.getId())).thenReturn(Optional.of(existing));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.createApplication(request);

        assertEquals(ApplicationStage.APPLIED, result.getCurrentStage());
        assertEquals("/resumes/new-cv.pdf", result.getResumeUrl());
        verify(applicationRepository).save(existing);
    }

    @Test
    void createApplication_shouldRejectWhenAlreadyInPipeline() {
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
        Application existing = Application.builder()
                .id("app-1")
                .currentStage(ApplicationStage.HR_CONTACTED)
                .build();

        ApplicationRequest request = ApplicationRequest.builder()
                .candidateId(candidate.getId())
                .jobId(job.getId())
                .resumeUrl("/resumes/cv.pdf")
                .build();

        when(candidateRepository.findById(candidate.getId())).thenReturn(Optional.of(candidate));
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(applicationRepository.findFirstByCandidateIdAndJobIdOrderByCreatedDateDesc(
                candidate.getId(), job.getId())).thenReturn(Optional.of(existing));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> applicationService.createApplication(request));

        assertEquals("You have already applied for this job", exception.getMessage());
        verify(applicationRepository, never()).save(any(Application.class));
    }
}
