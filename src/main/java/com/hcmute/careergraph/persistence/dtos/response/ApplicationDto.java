package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDto {

    private String applicationId;

    private String coverLetter;

    private String resumeUrl;

    private Integer rating;

    private String notes;

    private String appliedDate;

    private Status status;

    private CandidateDto candidate;

    private JobDto job;
}
