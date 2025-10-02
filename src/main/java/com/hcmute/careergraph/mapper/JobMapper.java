package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.JobDto;
import com.hcmute.careergraph.persistence.models.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "jobId", source = "id")
    @Mapping(target = "requiredSkills", ignore = true)
    @Mapping(target = "applications", ignore = true)
    @Mapping(target = "company", ignore = true)
    JobDto toDto(Job job);
}
