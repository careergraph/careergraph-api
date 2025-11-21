package com.hcmute.careergraph.persistence.dtos.projection;

import com.hcmute.careergraph.enums.application.ApplicationStage;

public interface AppliedJobsProjection {
    String getJobName();
    String getCompanyName();
    String getJobId();
    String getAppliedAt();
    String getDeadline();
    String getLinkResume();
    ApplicationStage getStatus();
}
