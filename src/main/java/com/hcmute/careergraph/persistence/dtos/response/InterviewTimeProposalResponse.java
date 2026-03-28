package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.interview.ProposalStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class InterviewTimeProposalResponse {

    private String id;
    private String interviewId;
    private LocalDate proposedDate;
    private LocalTime proposedStartTime;
    private Integer proposedDurationMinutes;
    private String notes;
    private ProposalStatus proposalStatus;
    private LocalDateTime createdDate;
}
