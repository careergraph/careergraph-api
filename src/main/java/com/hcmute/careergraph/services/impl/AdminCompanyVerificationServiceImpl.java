package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.company.CompanyOperationalStatus;
import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import com.hcmute.careergraph.exception.NotFoundException;
import com.hcmute.careergraph.persistence.dtos.request.CompanyVerificationRequests;
import com.hcmute.careergraph.persistence.dtos.response.CompanyVerificationResponses;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.CompanyVerificationRequest;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.repositories.CompanyVerificationRequestRepository;
import com.hcmute.careergraph.services.AdminCompanyVerificationService;
import com.hcmute.careergraph.services.CompanyAccessPolicyService;
import com.hcmute.careergraph.services.JobService;
import com.hcmute.careergraph.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCompanyVerificationServiceImpl implements AdminCompanyVerificationService {

    private final CompanyVerificationRequestRepository verificationRequestRepository;
    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;
    private final CompanyVerificationMapperSupport mapperSupport;
    private final CompanyAccessPolicyService companyAccessPolicyService;
    private final NotificationService notificationService;
    private final JobService jobService;

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyVerificationResponses.VerificationRequestSummaryResponse> getVerificationRequests(
            CompanyVerificationStatus status,
            String query,
            Pageable pageable) {
        companyAccessPolicyService.assertCurrentAccountIsAdmin();
        return verificationRequestRepository.searchForAdmin(
                        status != null ? status.name() : null,
                        blankToNull(query),
                        pageable)
                .map(mapperSupport::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyVerificationResponses.AdminCompanyListItemResponse> getCompanies(
            CompanyVerificationStatus verificationStatus,
            CompanyOperationalStatus operationalStatus,
            String query,
            Pageable pageable) {
        companyAccessPolicyService.assertCurrentAccountIsAdmin();
        return companyRepository.searchCompaniesForAdmin(
                        verificationStatus != null ? verificationStatus.name() : null,
                        operationalStatus != null ? operationalStatus.name() : null,
                        blankToNull(query),
                        pageable)
                .map(this::mapAdminCompanyListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyVerificationResponses.AdminDashboardSummaryResponse getDashboardSummary() {
        companyAccessPolicyService.assertCurrentAccountIsAdmin();
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse> latestPendingRequests =
                verificationRequestRepository.searchForAdmin(
                                CompanyVerificationStatus.PENDING_REVIEW.name(),
                                null,
                                PageRequest.of(0, 5))
                        .getContent()
                        .stream()
                        .map(mapperSupport::toSummaryResponse)
                        .toList();

        return CompanyVerificationResponses.AdminDashboardSummaryResponse.builder()
                .pendingVerification(verificationRequestRepository.countByVerificationStatus(CompanyVerificationStatus.PENDING_REVIEW))
                .reviewedToday(verificationRequestRepository.countByReviewedAtBetween(from, to))
                .companiesMonitored(companyRepository.count())
                .policyIncidents(companyRepository.countByOperationalStatusIn(
                        EnumSet.of(CompanyOperationalStatus.BLOCKED, CompanyOperationalStatus.SUSPENDED)))
                .latestPendingRequests(latestPendingRequests)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyVerificationResponses.VerificationRequestDetailResponse getCompanyDetail(String companyId) {
        companyAccessPolicyService.assertCurrentAccountIsAdmin();
        Company company = findCompany(companyId);
        return mapperSupport.toDetailResponse(latestOrSyntheticRequest(company));
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyVerificationResponses.VerificationRequestDetailResponse getVerificationRequestDetail(String requestId) {
        companyAccessPolicyService.assertCurrentAccountIsAdmin();
        return mapperSupport.toDetailResponse(findVerificationRequest(requestId));
    }

    @Override
    public CompanyVerificationResponses.VerificationRequestDetailResponse approveRequest(
            String requestId,
            String adminAccountId,
            CompanyVerificationRequests.AdminVerificationDecisionRequest request) {
        CompanyVerificationRequest verificationRequest = markVerificationDecision(
                requestId,
                adminAccountId,
                CompanyVerificationStatus.APPROVED,
                request.note());
        notificationService.onCompanyVerificationApproved(verificationRequest.getCompany(), verificationRequest);
        return mapperSupport.toDetailResponse(verificationRequest);
    }

    @Override
    public CompanyVerificationResponses.VerificationRequestDetailResponse rejectRequest(
            String requestId,
            String adminAccountId,
            CompanyVerificationRequests.AdminVerificationDecisionRequest request) {
        CompanyVerificationRequest verificationRequest = markVerificationDecision(
                requestId,
                adminAccountId,
                CompanyVerificationStatus.REJECTED,
                request.note());
        notificationService.onCompanyVerificationRejected(verificationRequest.getCompany(), verificationRequest, request.note());
        return mapperSupport.toDetailResponse(verificationRequest);
    }

    @Override
    public CompanyVerificationResponses.VerificationRequestDetailResponse requestAdditionalInfo(
            String requestId,
            String adminAccountId,
            CompanyVerificationRequests.AdminVerificationDecisionRequest request) {
        CompanyVerificationRequest verificationRequest = markVerificationDecision(
                requestId,
                adminAccountId,
                CompanyVerificationStatus.NEEDS_ADDITIONAL_INFO,
                request.note());
        notificationService.onCompanyVerificationNeedsInfo(verificationRequest.getCompany(), verificationRequest, request.note());
        return mapperSupport.toDetailResponse(verificationRequest);
    }

    @Override
    public CompanyVerificationResponses.VerificationRequestDetailResponse blockCompany(
            String companyId,
            String adminAccountId,
            CompanyVerificationRequests.AdminCompanyBlockRequest request) {
        companyAccessPolicyService.assertCurrentAccountIsAdmin();
        Company company = findCompany(companyId);
        Account adminAccount = findAccount(adminAccountId);

        company.setOperationalStatus(CompanyOperationalStatus.BLOCKED);
        company.setBlockReason(request.reason().trim());
        company.setBlockedAt(LocalDateTime.now());
        company.setBlockedByAccount(adminAccount);
        company.setUnblockedAt(null);
        company.setUnblockedByAccount(null);
        Company savedCompany = companyRepository.save(company);
        jobService.syncCompanyJobsSearchDocuments(companyId);

        notificationService.onCompanyBlocked(savedCompany, request.reason());
        return mapperSupport.toDetailResponse(latestOrSyntheticRequest(savedCompany));
    }

    @Override
    public CompanyVerificationResponses.VerificationRequestDetailResponse unblockCompany(
            String companyId,
            String adminAccountId,
            CompanyVerificationRequests.AdminVerificationDecisionRequest request) {
        companyAccessPolicyService.assertCurrentAccountIsAdmin();
        Company company = findCompany(companyId);
        Account adminAccount = findAccount(adminAccountId);

        company.setOperationalStatus(CompanyOperationalStatus.ACTIVE);
        company.setUnblockedAt(LocalDateTime.now());
        company.setUnblockedByAccount(adminAccount);
        company.setBlockReason(blankToNull(request.note()));
        Company savedCompany = companyRepository.save(company);
        jobService.syncCompanyJobsSearchDocuments(companyId);

        notificationService.onCompanyUnblocked(savedCompany, request.note());
        return mapperSupport.toDetailResponse(latestOrSyntheticRequest(savedCompany));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse> getCompanyVerificationHistory(String companyId) {
        companyAccessPolicyService.assertCurrentAccountIsAdmin();
        findCompany(companyId);
        return verificationRequestRepository.findByCompanyIdOrderByCreatedDateDesc(companyId)
                .stream()
                .map(mapperSupport::toSummaryResponse)
                .toList();
    }

    private CompanyVerificationRequest markVerificationDecision(
            String requestId,
            String adminAccountId,
            CompanyVerificationStatus status,
            String note) {
        companyAccessPolicyService.assertCurrentAccountIsAdmin();
        CompanyVerificationRequest verificationRequest = findVerificationRequest(requestId);
        Account adminAccount = findAccount(adminAccountId);

        verificationRequest.setVerificationStatus(status);
        verificationRequest.setAdminNote(note.trim());
        verificationRequest.setReviewedAt(LocalDateTime.now());
        verificationRequest.setReviewedByAccount(adminAccount);
        CompanyVerificationRequest savedRequest = verificationRequestRepository.save(verificationRequest);

        Company company = savedRequest.getCompany();
        company.setVerificationStatus(status);
        company.setVerificationReviewedAt(savedRequest.getReviewedAt());
        company.setVerificationReviewedByAccount(adminAccount);
        company.setVerificationAdminNote(note.trim());
        company.setVerificationSubmittedAt(savedRequest.getSubmittedAt());
        if (status == CompanyVerificationStatus.APPROVED) {
            company.setTaxCode(savedRequest.getTaxCode());
            company.setName(savedRequest.getCompanyName());
            company.setLegalRepresentativeName(savedRequest.getLegalRepresentativeName());
            company.setVerificationBusinessEmail(savedRequest.getBusinessEmail());
            company.setVerificationWebsite(savedRequest.getWebsite());
        }
        companyRepository.save(company);
        jobService.syncCompanyJobsSearchDocuments(company.getId());
        return savedRequest;
    }

    private CompanyVerificationRequest latestOrSyntheticRequest(Company company) {
        return verificationRequestRepository.findTopByCompanyIdOrderBySubmittedAtDescCreatedDateDesc(company.getId())
                .orElseGet(() -> {
                    CompanyVerificationRequest request = new CompanyVerificationRequest();
                    request.setId("company-" + company.getId());
                    request.setCompany(company);
                    request.setCompanyName(company.getName());
                    request.setTaxCode(company.getTaxCode());
                    request.setLegalRepresentativeName(company.getLegalRepresentativeName());
                    request.setBusinessEmail(company.getVerificationBusinessEmail());
                    request.setWebsite(company.getVerificationWebsite());
                    request.setVerificationStatus(company.getVerificationStatus());
                    request.setSubmittedAt(company.getVerificationSubmittedAt());
                    request.setReviewedAt(company.getVerificationReviewedAt());
                    request.setAdminNote(company.getVerificationAdminNote());
                    return request;
                });
    }

    private CompanyVerificationRequest findVerificationRequest(String requestId) {
        return verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Verification request not found"));
    }

    private Company findCompany(String companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
    }

    private Account findAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CompanyVerificationResponses.AdminCompanyListItemResponse mapAdminCompanyListItem(Object[] row) {
        return CompanyVerificationResponses.AdminCompanyListItemResponse.builder()
                .companyId(asString(row[0]))
                .companyName(asString(row[1]))
                .taxCode(asString(row[2]))
                .hrEmail(asString(row[3]))
                .verificationStatus(asEnum(row[4], CompanyVerificationStatus.class))
                .operationalStatus(asEnum(row[5], CompanyOperationalStatus.class))
                .submittedAt(asLocalDateTime(row[6]))
                .totalRequests(asLong(row[7]))
                .build();
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private <T extends Enum<T>> T asEnum(Object value, Class<T> enumType) {
        return value != null ? Enum.valueOf(enumType, value.toString()) : null;
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }
}
