package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.company.CompanyOperationalStatus;
import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CompanyAccessPolicyServiceImplTest {

    @Mock
    private SecurityUtils securityUtils;

    private CompanyAccessPolicyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CompanyAccessPolicyServiceImpl(securityUtils);
        ReflectionTestUtils.setField(service, "supportEmail", "support@careergraph.com");
    }

    @Test
    void isJobPubliclyAvailable_shouldReturnTrueForEligibleJob() {
        Job job = createJob();
        job.setExpiryDate(LocalDate.now().plusDays(2).toString());

        assertThat(service.isJobPubliclyAvailable(job)).isTrue();
    }

    @Test
    void isJobPubliclyAvailable_shouldReturnFalseForExpiredJob() {
        Job job = createJob();
        job.setExpiryDate(LocalDate.now().minusDays(1).toString());

        assertThat(service.isJobPubliclyAvailable(job)).isFalse();
        assertThat(service.isJobExpired(job)).isTrue();
    }

    @Test
    void isJobPubliclyAvailable_shouldReturnFalseForCompanyWithoutApprovedVerification() {
        Job job = createJob();
        job.getCompany().setVerificationStatus(CompanyVerificationStatus.PENDING_REVIEW);

        assertThat(service.isJobPubliclyAvailable(job)).isFalse();
    }

    @Test
    void assertJobAcceptingCandidateApplications_shouldRejectNonPublicJob() {
        Job job = createJob();
        job.getCompany().setOperationalStatus(CompanyOperationalStatus.BLOCKED);

        assertThatThrownBy(() -> service.assertJobAcceptingCandidateApplications(job))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Công việc này hiện không khả dụng.");
    }

    @Test
    void assertCompanyCanSearchCandidates_shouldRejectUnapprovedCompany() {
        Company company = new Company();
        company.setVerificationStatus(CompanyVerificationStatus.PENDING_REVIEW);
        company.setOperationalStatus(CompanyOperationalStatus.ACTIVE);

        assertThatThrownBy(() -> service.assertCompanyCanSearchCandidates(company))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Vui lòng hoàn tất xác thực doanh nghiệp để sử dụng tính năng tìm kiếm ứng viên.");
    }

    private Job createJob() {
        Company company = new Company();
        company.setVerificationStatus(CompanyVerificationStatus.APPROVED);
        company.setOperationalStatus(CompanyOperationalStatus.ACTIVE);

        Job job = new Job();
        job.setId("job-1");
        job.setCompany(company);
        job.setStatus(Status.ACTIVE);
        return job;
    }
}
