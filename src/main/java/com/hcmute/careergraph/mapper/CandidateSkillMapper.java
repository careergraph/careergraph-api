package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.CandidateSkillDto;
import com.hcmute.careergraph.persistence.models.CandidateSkill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CandidateSkillMapper {

    @Mapping(target = "candidateId", source = "candidate.id")
    @Mapping(target = "skillId", source = "skill.id")
    CandidateSkillDto toDto(CandidateSkill candidateSkill);
}
