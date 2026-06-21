package com.hcmute.careergraph.persistence.dtos.request;

import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

public final class CompanyVerificationRequests {

    private CompanyVerificationRequests() {
    }

    @Builder
    public record VerificationDocumentRequest(
            @NotBlank String documentUrl,
            String documentType,
            String originalFileName,
            String mimeType
    ) {
    }

    @Builder
    public record SubmitVerificationRequest(
            @NotBlank @Size(max = 50) String taxCode,
            @NotBlank @Size(max = 255) String companyName,
            @NotBlank @Size(max = 255) String legalRepresentativeName,
            @NotBlank @Email @Size(max = 255) String businessEmail,
            @Size(max = 500) String website,
            @Valid @NotEmpty List<VerificationDocumentRequest> documents
    ) {
    }

    @Builder
    public record AdminVerificationDecisionRequest(
            @NotBlank String note
    ) {
    }

    @Builder
    public record AdminCompanyBlockRequest(
            @NotBlank String reason
    ) {
    }

    @Builder
    public record AdminVerificationListRequest(
            CompanyVerificationStatus status,
            String query
    ) {
    }
}
