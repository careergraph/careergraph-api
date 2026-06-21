package com.hcmute.careergraph.services.impl;

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
import com.hcmute.careergraph.services.CompanyAccessPolicyService;
import com.hcmute.careergraph.services.CompanyRecruitmentStageService;
import com.hcmute.careergraph.services.MailService;
import com.hcmute.careergraph.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
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
    @Mock
    private CompanyAccessPolicyService companyAccessPolicyService;

    private ApplicationServiceImpl applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationServiceImpl(
                applicationRepository,
                candidateRepository,
                accountRepository,
                jobRepository,
                mailService,
                notificationService,
                applicationEventPublisher,
                companyRecruitmentStageService,
                companyAccessPolicyService);
    }

    @Test
    void createApplication_shouldRejectBlockedCompanyJob() {
        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");

        Company company = new Company();
        company.setId("company-1");

        Job job = new Job();
        job.setId("job-1");
        job.setStatus(Status.ACTIVE);
        job.setExpiryDate(LocalDate.now().plusDays(5).toString());
        job.setCompany(company);

        ApplicationRequest request = new ApplicationRequest();
        request.setCandidateId("candidate-1");
        request.setJobId("job-1");
        request.setResumeUrl("resume.pdf");

        when(candidateRepository.findById("candidate-1")).thenReturn(Optional.of(candidate));
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        doThrow(new BadRequestException("Cong viec nay hien khong kha dung."))
                .when(companyAccessPolicyService)
                .assertJobAcceptingCandidateApplications(job);

        assertThatThrownBy(() -> applicationService.createApplication(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cong viec nay hien khong kha dung.");
    }
}
