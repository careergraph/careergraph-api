package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;

@Builder
public record CandidateEducationResponse(
        String startDate,
        String endDate,
        String degreeTitle,
        Boolean isCurrent,
        String description,
        String candidateId,
        String educationId
) {
}
