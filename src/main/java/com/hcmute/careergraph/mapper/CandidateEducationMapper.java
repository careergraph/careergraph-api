package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.CandidateEducationDto;
import com.hcmute.careergraph.persistence.models.CandidateEducation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CandidateEducationMapper {

    @Mapping(target = "candidateId", source = "candidate.id")
    @Mapping(target = "educationId", source = "education.id")
    CandidateEducationDto toDto(CandidateEducation candidateEducation);
}
