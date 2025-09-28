package com.hcmute.careergraph.persistence.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidateEducationDto {

    private String startDate;

    private String endDate;

    private String degreeTitle;

    private Boolean isCurrent;

    private String description;

    private String candidateId;

    private String educationId;
}
