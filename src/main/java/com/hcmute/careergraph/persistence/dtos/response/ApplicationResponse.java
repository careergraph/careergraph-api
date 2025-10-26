package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ApplicationResponse(
        String applicationId,
        String coverLetter,
        String resumeUrl,
        Integer rating,
        String notes,
        String appliedDate,
        ApplicationStage currentStage,
        LocalDateTime stageChangedAt,
        String stageNote,
        List<ApplicationStageHistoryResponse> stageHistory,
        CandidateResponse candidate,
        JobResponse job
) {
}
