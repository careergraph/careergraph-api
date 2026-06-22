package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.company.CompanyOperationalStatus;
import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.exception.ForbiddenException;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.services.CompanyAccessPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class CompanyAccessPolicyServiceImpl implements CompanyAccessPolicyService {

    private static final String CREATE_JOB_VERIFICATION_MESSAGE =
            "Vui lòng xác thực thông tin công ty để có thể đăng tải công việc.";
    private static final String SEARCH_CANDIDATE_VERIFICATION_MESSAGE =
            "Vui lòng hoàn tất xác thực doanh nghiệp để sử dụng tính năng tìm kiếm ứng viên.";
    private static final String JOB_UNAVAILABLE_MESSAGE = "Công việc này hiện không khả dụng.";

    private final SecurityUtils securityUtils;

    @Value("${support.email:support@careergraph.com}")
    private String supportEmail;

    @Override
    public void assertCurrentAccountIsAdmin() {
        if (!securityUtils.isAdmin()) {
            throw new ForbiddenException("You do not have permission to access this resource");
        }
    }

    @Override
    public void assertCompanyCanManageJobs(Company company) {
        if (company == null) {
            throw new BadRequestException("Company not found");
        }
        if (company.getVerificationStatus() != CompanyVerificationStatus.APPROVED) {
            throw new BadRequestException(CREATE_JOB_VERIFICATION_MESSAGE);
        }
        if (company.getOperationalStatus() != CompanyOperationalStatus.ACTIVE) {
            throw new ForbiddenException(
                    "Công ty/tài khoản của bạn đang bị khóa. Vui lòng liên hệ " + supportEmail + " để giải trình.");
        }
    }

    @Override
    public void assertCompanyCanSearchCandidates(Company company) {
        if (company == null) {
            throw new BadRequestException("Company not found");
        }
        if (company.getVerificationStatus() != CompanyVerificationStatus.APPROVED) {
            throw new BadRequestException(SEARCH_CANDIDATE_VERIFICATION_MESSAGE);
        }
        if (company.getOperationalStatus() != CompanyOperationalStatus.ACTIVE) {
            throw new ForbiddenException(
                    "Công ty/tài khoản của bạn đang bị khóa. Vui lòng liên hệ " + supportEmail + " để giải trình.");
        }
    }

    @Override
    public void assertJobAcceptingCandidateApplications(Job job) {
        if (!isJobPubliclyAvailable(job)) {
            throw new BadRequestException(JOB_UNAVAILABLE_MESSAGE);
        }
    }

    @Override
    public boolean isJobPubliclyAvailable(Job job) {
        if (job == null || job.getCompany() == null) {
            return false;
        }
        if (job.getStatus() != Status.ACTIVE) {
            return false;
        }
        if (isJobExpired(job)) {
            return false;
        }
        Company company = job.getCompany();
        return company.getVerificationStatus() == CompanyVerificationStatus.APPROVED
                && company.getOperationalStatus() == CompanyOperationalStatus.ACTIVE;
    }

    @Override
    public boolean isJobExpired(Job job) {
        if (job == null || !StringUtils.hasText(job.getExpiryDate())) {
            return false;
        }
        try {
            return LocalDate.now().isAfter(LocalDate.parse(job.getExpiryDate().trim()));
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }
}
