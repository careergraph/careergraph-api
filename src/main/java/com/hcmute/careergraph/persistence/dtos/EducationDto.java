package com.hcmute.careergraph.persistence.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EducationDto {

    private String educationId;

    private String tagname;

    private String avatar;

    private String cover;

    private String startDate;

    private String endDate;

    private String description;

    private Boolean isCurrentlyStudying;

    private Set<CandidateEducationDto> candidateEducations;

    // private Set<ConnectionDto> educationConnections;
}
