package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ApplicationRequest {
        private String coverLetter;

        private String resumeUrl;

        private String notes;

        private String appliedDate;

        private String candidateId;

        @NotBlank(message = "Job ID is required")
        private String jobId;
}