package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.EducationDto;
import com.hcmute.careergraph.persistence.models.Education;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EducationMapper {

    @Mapping(target = "educationId", source = "id")
    @Mapping(target = "candidateEducations", ignore = true)
    EducationDto toDto(Education education);
}
