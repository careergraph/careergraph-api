package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.CandidateExperienceDto;
import com.hcmute.careergraph.persistence.models.CandidateExperience;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CandidateExperienceMapper {

    @Mapping(target = "candidateId", source = "candidate.id")
    @Mapping(target = "companyId", source = "company.id")
    CandidateExperienceDto toDto(CandidateExperience candidateExperience);
}
