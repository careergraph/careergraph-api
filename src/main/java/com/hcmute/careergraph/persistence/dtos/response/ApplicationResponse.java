package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.persistence.dtos.record.JobResponse;
import lombok.Builder;

@Builder
public record ApplicationResponse(
        String applicationId,
        String coverLetter,
        String resumeUrl,
        Integer rating,
        String notes,
        String appliedDate,
        Status status,
        CandidateResponse candidate,
        JobResponse job
) {
}
