package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import com.hcmute.careergraph.persistence.dtos.request.CompanyVerificationRequests;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.CompanyVerificationRequest;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.repositories.CompanyVerificationRequestRepository;
import com.hcmute.careergraph.services.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyVerificationServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CompanyVerificationRequestRepository verificationRequestRepository;
    @Mock
    private CompanyVerificationMapperSupport mapperSupport;
    @Mock
    private JobService jobService;

    private CompanyVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CompanyVerificationServiceImpl(
                companyRepository,
                accountRepository,
                verificationRequestRepository,
                mapperSupport,
                jobService);
    }

    @Test
    void updateVerification_shouldCreateNewRequestInsteadOfOverwritingHistory() {
        Company company = new Company();
        company.setId("company-1");
        company.setVerificationStatus(CompanyVerificationStatus.REJECTED);

        CompanyVerificationRequest existingRequest = new CompanyVerificationRequest();
        existingRequest.setId("request-1");
        existingRequest.setCompany(company);
        existingRequest.setVerificationStatus(CompanyVerificationStatus.REJECTED);
        existingRequest.setCompanyName("Old Company");
        existingRequest.setTaxCode("1111111111");

        Account account = new Account();
        account.setId("account-1");

        CompanyVerificationRequests.SubmitVerificationRequest payload =
                new CompanyVerificationRequests.SubmitVerificationRequest(
                        "2222222222",
                        "New Company",
                        "Legal Rep",
                        "hr@company.com",
                        "https://company.com",
                        List.of(new CompanyVerificationRequests.VerificationDocumentRequest(
                                "https://files.example/doc.png",
                                "BUSINESS_LICENSE",
                                "doc.png",
                                "image/png")));

        when(companyRepository.findById("company-1")).thenReturn(Optional.of(company));
        when(verificationRequestRepository.findById("request-1")).thenReturn(Optional.of(existingRequest));
        when(accountRepository.findById("account-1")).thenReturn(Optional.of(account));
        when(verificationRequestRepository.save(any(CompanyVerificationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateVerification("company-1", "request-1", "account-1", payload);

        ArgumentCaptor<CompanyVerificationRequest> captor = ArgumentCaptor.forClass(CompanyVerificationRequest.class);
        verify(verificationRequestRepository).save(captor.capture());

        CompanyVerificationRequest savedRequest = captor.getValue();
        assertThat(savedRequest).isNotSameAs(existingRequest);
        assertThat(savedRequest.getId()).isNull();
        assertThat(savedRequest.getVerificationStatus()).isEqualTo(CompanyVerificationStatus.PENDING_REVIEW);
        assertThat(savedRequest.getCompanyName()).isEqualTo("New Company");
        assertThat(existingRequest.getCompanyName()).isEqualTo("Old Company");
        assertThat(savedRequest.getDocuments()).hasSize(1);
        verify(jobService).syncCompanyJobsSearchDocuments("company-1");
    }
}
