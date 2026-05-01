package com.hcmute.careergraph.persistence.dtos.request;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

public final class CompanyStageRequests {

    private CompanyStageRequests() {
    }

    @Builder
    public record UpdateRecruitmentStagesRequest(
            @NotNull List<StageConfig> stages
    ) {
    }

    @Builder
    public record StageConfig(
            @NotNull ApplicationStage stage,
            Boolean active,
            Integer displayOrder
    ) {
    }
}
