package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import com.hcmute.careergraph.enums.company.CompanyOperationalStatus;
import com.hcmute.careergraph.persistence.dtos.request.CompanyVerificationRequests;
import com.hcmute.careergraph.persistence.dtos.response.CompanyVerificationResponses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminCompanyVerificationService {

    Page<CompanyVerificationResponses.VerificationRequestSummaryResponse> getVerificationRequests(
            CompanyVerificationStatus status,
            String query,
            Pageable pageable);

    Page<CompanyVerificationResponses.AdminCompanyListItemResponse> getCompanies(
            CompanyVerificationStatus verificationStatus,
            CompanyOperationalStatus operationalStatus,
            String query,
            Pageable pageable);

    CompanyVerificationResponses.AdminDashboardSummaryResponse getDashboardSummary();

    CompanyVerificationResponses.VerificationRequestDetailResponse getCompanyDetail(String companyId);

    CompanyVerificationResponses.VerificationRequestDetailResponse getVerificationRequestDetail(String requestId);

    CompanyVerificationResponses.VerificationRequestDetailResponse approveRequest(
            String requestId,
            String adminAccountId,
            CompanyVerificationRequests.AdminVerificationDecisionRequest request);

    CompanyVerificationResponses.VerificationRequestDetailResponse rejectRequest(
            String requestId,
            String adminAccountId,
            CompanyVerificationRequests.AdminVerificationDecisionRequest request);

    CompanyVerificationResponses.VerificationRequestDetailResponse requestAdditionalInfo(
            String requestId,
            String adminAccountId,
            CompanyVerificationRequests.AdminVerificationDecisionRequest request);

    CompanyVerificationResponses.VerificationRequestDetailResponse blockCompany(
            String companyId,
            String adminAccountId,
            CompanyVerificationRequests.AdminCompanyBlockRequest request);

    CompanyVerificationResponses.VerificationRequestDetailResponse unblockCompany(
            String companyId,
            String adminAccountId,
            CompanyVerificationRequests.AdminVerificationDecisionRequest request);

    java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse> getCompanyVerificationHistory(String companyId);
}
