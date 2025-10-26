package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ApplicationStageHistoryResponse(
        ApplicationStage fromStage,
        ApplicationStage toStage,
        String note,
        String changedBy,
        LocalDateTime changedAt
) {
}
