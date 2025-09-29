package com.hcmute.careergraph.persistence.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillDto {

    private String skillId;

    private String name;

    private String category;

    private String description;

    private Set<CandidateSkillDto> candidateSkills;

    private Set<JobSkillDto> jobSkills;
}
