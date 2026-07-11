package com.hcmute.careergraph.persistence.dtos.projection;

import com.hcmute.careergraph.enums.application.ApplicationStage;

public interface AppliedJobStageProjection {
    ApplicationStage getStatus();
}
