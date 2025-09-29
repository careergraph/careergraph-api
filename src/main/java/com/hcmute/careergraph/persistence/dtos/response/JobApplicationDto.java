package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.Status;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobApplicationDto {

    private String id;

    private String jobId;

    private String candidateId;

    private String coverLetter;

    private String resumeUrl;

    private Status status;

    private String rejectionReason;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastStatusChangeAt;
    
    // Additional fields for response
    private JobDto job;

    private CandidateDto candidate;
}
