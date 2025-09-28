package com.hcmute.careergraph.persistence.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobSkillDto {

    private String proficiencyLevel;

    private Integer yearsOfExperience;

    private Boolean isRequired;

    private String jobId;

    private String skillId;
}
