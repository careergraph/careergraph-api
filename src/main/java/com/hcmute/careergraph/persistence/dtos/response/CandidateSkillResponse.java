package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;

@Builder
public record CandidateSkillResponse(
        String proficiencyLevel,
        Integer yearsOfExperience,
        Boolean isVerified,
        String endorsedBy,
        Long endorsementDate,
        Integer endorsementCount,
        String candidateId,
        String skillId
) {
}
