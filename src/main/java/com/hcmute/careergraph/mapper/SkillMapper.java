package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.SkillDto;
import com.hcmute.careergraph.persistence.models.Skill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SkillMapper {

    @Mapping(target = "skillId", source = "id")
    @Mapping(target = "candidateSkills", ignore = true)
    @Mapping(target = "jobSkills", ignore = true)
    SkillDto toDto(Skill skill);
}
