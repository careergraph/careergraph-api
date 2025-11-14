package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApplicationResponse {

    private String applicationId;
    private String coverLetter;
    private String resumeUrl;
    private Integer rating;
    private String notes;
    private String appliedDate;
    private ApplicationStage currentStage;
    private LocalDateTime stageChangedAt;
    private String stageNote;
    private List<ApplicationStageHistoryResponse> stageHistory;
    private CandidateResponse candidate;
    private JobResponse job;

}
