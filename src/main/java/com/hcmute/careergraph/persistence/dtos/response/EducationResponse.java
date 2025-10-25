package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;

import java.util.Set;

@Builder
public record EducationDto(
        String educationId,
        String tagname,
        String avatar,
        String cover,
        String startDate,
        String endDate,
        String description,
        Boolean isCurrentlyStudying,
        Set<CandidateEducationDto> candidateEducations
) {
    public EducationDto {
        candidateEducations = candidateEducations != null ? candidateEducations : Set.of();
    }
}
