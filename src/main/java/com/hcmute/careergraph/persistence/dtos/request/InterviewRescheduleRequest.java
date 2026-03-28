package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRescheduleRequest {

    @NotBlank(message = "New date is required")
    private String newDate;

    @NotBlank(message = "New start time is required")
    private String newStartTime;

    @NotNull(message = "Duration is required")
    private Integer durationMinutes;

    private List<String> interviewerIds;

    private String notes;
}
