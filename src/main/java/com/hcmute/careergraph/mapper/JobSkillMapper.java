package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.JobSkillDto;
import com.hcmute.careergraph.persistence.models.JobSkill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobSkillMapper {

    @Mapping(target = "jobId", source = "job.id")
    @Mapping(target = "skillId", source = "skill.id")
    JobSkillDto toDto(JobSkill jobSkill);
}
