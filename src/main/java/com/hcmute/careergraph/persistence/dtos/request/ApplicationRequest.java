package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRequest {

    private String coverLetter;

    private String resumeUrl;

    private Integer rating;

    private String notes;

    private String appliedDate;

    @NotBlank(message = "Candidate ID is required")
    private String candidateId;

    @NotBlank(message = "Job ID is required")
    private String jobId;
}