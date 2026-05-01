package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import lombok.Builder;

@Builder
public record CompanyRecruitmentStageResponse(
        ApplicationStage stage,
        String label,
        Integer displayOrder,
        boolean active,
        boolean required
) {
}
