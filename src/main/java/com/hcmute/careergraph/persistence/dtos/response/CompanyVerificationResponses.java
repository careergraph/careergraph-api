package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.company.CompanyOperationalStatus;
import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public final class CompanyVerificationResponses {

    private CompanyVerificationResponses() {
    }

    @Builder
    public record VerificationDocumentResponse(
            String id,
            String documentUrl,
            String documentType,
            String originalFileName,
            String mimeType
    ) {
    }

    @Builder
    public record VerificationRequestSummaryResponse(
            String requestId,
            String companyId,
            String companyName,
            String taxCode,
            String hrEmail,
            CompanyVerificationStatus verificationStatus,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt,
            String adminNote,
            List<VerificationDocumentResponse> documents
    ) {
    }

    @Builder
    public record AdminCompanyListItemResponse(
            String companyId,
            String companyName,
            String taxCode,
            String hrEmail,
            CompanyVerificationStatus verificationStatus,
            CompanyOperationalStatus operationalStatus,
            LocalDateTime submittedAt,
            long totalRequests
    ) {
    }

    @Builder
    public record VerificationRequestDetailResponse(
            String requestId,
            String companyId,
            String companyName,
            String hrEmail,
            String taxCode,
            String legalRepresentativeName,
            String businessEmail,
            String website,
            CompanyVerificationStatus verificationStatus,
            CompanyOperationalStatus operationalStatus,
            String adminNote,
            String blockReason,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt,
            List<VerificationDocumentResponse> documents
    ) {
    }

    @Builder
    public record AdminDashboardSummaryResponse(
            long pendingVerification,
            long reviewedToday,
            long companiesMonitored,
            long policyIncidents,
            List<VerificationRequestSummaryResponse> latestPendingRequests
    ) {
    }

    @Builder
    public record CompanyVerificationStatusResponse(
            String companyId,
            CompanyVerificationStatus verificationStatus,
            CompanyOperationalStatus operationalStatus,
            String adminNote,
            String blockReason,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt,
            VerificationRequestDetailResponse latestRequest
    ) {
    }
}
