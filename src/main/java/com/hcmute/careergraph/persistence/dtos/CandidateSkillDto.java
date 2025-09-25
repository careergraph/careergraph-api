package com.hcmute.careergraph.persistence.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSkillDto {

    private String proficiencyLevel;

    private Integer yearsOfExperience;

    private Boolean isVerified;

    private String endorsedBy;

    private Long endorsementDate;

    private Integer endorsementCount;

    private String candidateId;

    private String skillId;
}
