package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewTimeProposalRequest {

    @NotEmpty(message = "At least one time slot is required")
    @Valid
    private List<TimeSlot> proposedSlots;

    private String notes;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlot {
        @jakarta.validation.constraints.NotBlank(message = "Date is required")
        private String date;

        @jakarta.validation.constraints.NotBlank(message = "Start time is required")
        private String startTime;

        private Integer durationMinutes;
    }
}
