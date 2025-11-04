package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ApplicationRequest(
        String coverLetter,
        String resumeUrl,
        Integer rating,
        String notes,
        String appliedDate,
        @NotBlank(message = "Candidate ID is required")
        String candidateId,
        @NotBlank(message = "Job ID is required")
        String jobId
) { }