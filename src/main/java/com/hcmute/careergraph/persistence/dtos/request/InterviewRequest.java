package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRequest {

    @NotBlank(message = "Application ID is required")
    private String applicationId;

    @NotBlank(message = "Date is required")
    private String date;

    @NotBlank(message = "Start time is required")
    private String startTime;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    private Integer durationMinutes;

    @NotBlank(message = "Interview type is required")
    private String type;

    private String location;

    private List<String> interviewerIds;

    private String notes;

    @Builder.Default
    private boolean confirmOverwrite = false;

    @Builder.Default
    private boolean notifyCandidate = true;
}
