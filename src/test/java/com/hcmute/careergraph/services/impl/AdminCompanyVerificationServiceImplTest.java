package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import com.hcmute.careergraph.enums.common.Role;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.persistence.dtos.request.CompanyVerificationRequests;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.CompanyVerificationRequest;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.repositories.CompanyVerificationRequestRepository;
import com.hcmute.careergraph.services.CompanyAccessPolicyService;
import com.hcmute.careergraph.services.JobService;
import com.hcmute.careergraph.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCompanyVerificationServiceImplTest {

    @Mock
    private CompanyVerificationRequestRepository verificationRequestRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CompanyVerificationMapperSupport mapperSupport;
    @Mock
    private CompanyAccessPolicyService companyAccessPolicyService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private JobService jobService;

    private AdminCompanyVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminCompanyVerificationServiceImpl(
                verificationRequestRepository,
                companyRepository,
                accountRepository,
                mapperSupport,
                companyAccessPolicyService,
                notificationService,
                jobService);
    }

    @Test
    void approveRequest_shouldUpdateCompanyAndNotify() {
        Company company = new Company();
        company.setId("company-1");

        CompanyVerificationRequest verificationRequest = new CompanyVerificationRequest();
        verificationRequest.setId("request-1");
        verificationRequest.setCompany(company);
        verificationRequest.setVerificationStatus(CompanyVerificationStatus.PENDING_REVIEW);
        verificationRequest.setTaxCode("123");
        verificationRequest.setCompanyName("CareerGraph");
        verificationRequest.setLegalRepresentativeName("Leader");
        verificationRequest.setBusinessEmail("hr@careergraph.com");
        verificationRequest.setWebsite("https://careergraph.com");

        Account admin = new Account();
        admin.setId("admin-1");
        admin.setRole(Role.ADMIN);

        CompanyVerificationRequests.AdminVerificationDecisionRequest request =
                new CompanyVerificationRequests.AdminVerificationDecisionRequest("approved");

        when(verificationRequestRepository.findById("request-1")).thenReturn(Optional.of(verificationRequest));
        when(accountRepository.findById("admin-1")).thenReturn(Optional.of(admin));
        when(verificationRequestRepository.save(any(CompanyVerificationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.approveRequest("request-1", "admin-1", request);

        assertThat(company.getName()).isEqualTo("CareerGraph");
        assertThat(company.getVerificationAdminNote()).isEqualTo("approved");
        verify(notificationService).onCompanyVerificationApproved(company, verificationRequest);
        verify(jobService).syncCompanyJobsSearchDocuments("company-1");
    }

    @Test
    void rejectRequest_shouldFailWhenRequestAlreadyApproved() {
        Company company = new Company();
        company.setId("company-1");

        CompanyVerificationRequest verificationRequest = new CompanyVerificationRequest();
        verificationRequest.setId("request-1");
        verificationRequest.setCompany(company);
        verificationRequest.setVerificationStatus(CompanyVerificationStatus.APPROVED);

        Account admin = new Account();
        admin.setId("admin-1");
        admin.setRole(Role.ADMIN);

        CompanyVerificationRequests.AdminVerificationDecisionRequest request =
                new CompanyVerificationRequests.AdminVerificationDecisionRequest("rejected");

        when(verificationRequestRepository.findById("request-1")).thenReturn(Optional.of(verificationRequest));
        when(accountRepository.findById("admin-1")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.rejectRequest("request-1", "admin-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only pending verification requests can be updated");

        verify(verificationRequestRepository, never()).save(any(CompanyVerificationRequest.class));
        verify(companyRepository, never()).save(any(Company.class));
        verify(notificationService, never()).onCompanyVerificationRejected(any(), any(), any());
        verify(jobService, never()).syncCompanyJobsSearchDocuments(any());
    }

    @Test
    void requestAdditionalInfo_shouldUpdateCompanyAndNotify() {
        Company company = new Company();
        company.setId("company-1");

        CompanyVerificationRequest verificationRequest = new CompanyVerificationRequest();
        verificationRequest.setId("request-1");
        verificationRequest.setCompany(company);
        verificationRequest.setVerificationStatus(CompanyVerificationStatus.PENDING_REVIEW);

        Account admin = new Account();
        admin.setId("admin-1");
        admin.setRole(Role.ADMIN);

        CompanyVerificationRequests.AdminVerificationDecisionRequest request =
                new CompanyVerificationRequests.AdminVerificationDecisionRequest("  please update tax document  ");

        when(verificationRequestRepository.findById("request-1")).thenReturn(Optional.of(verificationRequest));
        when(accountRepository.findById("admin-1")).thenReturn(Optional.of(admin));
        when(verificationRequestRepository.save(any(CompanyVerificationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.requestAdditionalInfo("request-1", "admin-1", request);

        assertThat(verificationRequest.getVerificationStatus())
                .isEqualTo(CompanyVerificationStatus.NEEDS_ADDITIONAL_INFO);
        assertThat(verificationRequest.getAdminNote()).isEqualTo("please update tax document");
        assertThat(company.getVerificationStatus()).isEqualTo(CompanyVerificationStatus.NEEDS_ADDITIONAL_INFO);
        assertThat(company.getVerificationAdminNote()).isEqualTo("please update tax document");
        verify(notificationService).onCompanyVerificationNeedsInfo(company, verificationRequest, "  please update tax document  ");
        verify(jobService).syncCompanyJobsSearchDocuments("company-1");
    }
}
