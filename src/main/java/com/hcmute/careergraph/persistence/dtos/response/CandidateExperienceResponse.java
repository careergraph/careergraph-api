package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;

@Builder
public record CandidateExperienceResponse(
        String startDate,
        String endDate,
        Integer salary,
        String jobTitle,
        Boolean isCurrent,
        String description,
        String candidateId,
        String companyId
) {
}
