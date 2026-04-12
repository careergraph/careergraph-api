package com.hcmute.careergraph.persistence.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewUpdateRequest {

    private String location;

    private String notes;

    private Integer durationMinutes;
}
