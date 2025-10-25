package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;

import java.util.Set;

@Builder
public record SkillDto(
        String skillId,
        String name,
        String category,
        String description,
        Set<CandidateSkillDto> candidateSkills,
        Set<JobSkillDto> jobSkills
) {
    public SkillDto {
        candidateSkills = candidateSkills != null ? candidateSkills : Set.of();
        jobSkills = jobSkills != null ? jobSkills : Set.of();
    }
}
