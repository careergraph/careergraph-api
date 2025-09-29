package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.CandidateDto;
import com.hcmute.careergraph.persistence.models.Candidate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CandidateMapper {

    @Mapping(target = "candidateId", source = "id")
    @Mapping(target = "contacts", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "connections", ignore = true)
    @Mapping(target = "educations", ignore = true)
    @Mapping(target = "experiences", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "applications", ignore = true)
    CandidateDto toDto(Candidate candidate);
}
