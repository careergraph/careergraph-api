package com.hcmute.careergraph.persistence.dtos.request;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ApplicationStageUpdateRequest(
        @NotNull(message = "Stage is required")
        ApplicationStage stage,
        String note,
        String changedBy
) {
}
