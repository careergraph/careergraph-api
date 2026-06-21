package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.persistence.dtos.response.CompanyVerificationResponses;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.CompanyVerificationDocument;
import com.hcmute.careergraph.persistence.models.CompanyVerificationRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class CompanyVerificationMapperSupport {

    public CompanyVerificationResponses.CompanyVerificationStatusResponse toStatusResponse(
            Company company,
            CompanyVerificationRequest latestRequest) {
        return CompanyVerificationResponses.CompanyVerificationStatusResponse.builder()
                .companyId(company.getId())
                .verificationStatus(company.getVerificationStatus())
                .operationalStatus(company.getOperationalStatus())
                .adminNote(company.getVerificationAdminNote())
                .blockReason(company.getBlockReason())
                .submittedAt(company.getVerificationSubmittedAt())
                .reviewedAt(company.getVerificationReviewedAt())
                .latestRequest(latestRequest != null ? toDetailResponse(latestRequest) : null)
                .build();
    }

    public CompanyVerificationResponses.VerificationRequestSummaryResponse toSummaryResponse(
            CompanyVerificationRequest request) {
        return CompanyVerificationResponses.VerificationRequestSummaryResponse.builder()
                .requestId(request.getId())
                .companyId(request.getCompany().getId())
                .companyName(firstNonBlank(request.getCompanyName(), request.getCompany().getName()))
                .taxCode(request.getTaxCode())
                .hrEmail(emailOf(request.getSubmittedByAccount(), request.getCompany().getAccount()))
                .verificationStatus(request.getVerificationStatus())
                .submittedAt(request.getSubmittedAt())
                .reviewedAt(request.getReviewedAt())
                .adminNote(firstNonBlank(request.getAdminNote(), request.getCompany().getVerificationAdminNote()))
                .documents(request.getDocuments() == null ? List.of() : request.getDocuments().stream()
                        .sorted(Comparator.comparing(CompanyVerificationDocument::getCreatedDate,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::toDocumentResponse)
                        .toList())
                .build();
    }

    public CompanyVerificationResponses.VerificationRequestDetailResponse toDetailResponse(
            CompanyVerificationRequest request) {
        Company company = request.getCompany();
        return CompanyVerificationResponses.VerificationRequestDetailResponse.builder()
                .requestId(request.getId())
                .companyId(company.getId())
                .companyName(firstNonBlank(request.getCompanyName(), company.getName()))
                .hrEmail(emailOf(request.getSubmittedByAccount(), company.getAccount()))
                .taxCode(request.getTaxCode())
                .legalRepresentativeName(request.getLegalRepresentativeName())
                .businessEmail(request.getBusinessEmail())
                .website(request.getWebsite())
                .verificationStatus(request.getVerificationStatus())
                .operationalStatus(company.getOperationalStatus())
                .adminNote(firstNonBlank(request.getAdminNote(), company.getVerificationAdminNote()))
                .blockReason(company.getBlockReason())
                .submittedAt(request.getSubmittedAt())
                .reviewedAt(request.getReviewedAt())
                .documents(request.getDocuments() == null ? List.of() : request.getDocuments().stream()
                        .sorted(Comparator.comparing(CompanyVerificationDocument::getCreatedDate,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::toDocumentResponse)
                        .toList())
                .build();
    }

    private CompanyVerificationResponses.VerificationDocumentResponse toDocumentResponse(
            CompanyVerificationDocument document) {
        return CompanyVerificationResponses.VerificationDocumentResponse.builder()
                .id(document.getId())
                .documentUrl(document.getDocumentUrl())
                .documentType(document.getDocumentType())
                .originalFileName(document.getOriginalFileName())
                .mimeType(document.getMimeType())
                .build();
    }

    private String emailOf(Account preferred, Account fallback) {
        if (preferred != null && preferred.getEmail() != null) {
            return preferred.getEmail();
        }
        return fallback != null ? fallback.getEmail() : null;
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }
}
