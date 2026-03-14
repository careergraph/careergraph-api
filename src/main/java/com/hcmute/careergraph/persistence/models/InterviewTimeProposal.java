package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.interview.ProposalStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "interview_time_proposals")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"interview"})
@EqualsAndHashCode(callSuper = true, exclude = {"interview"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewTimeProposal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Column(name = "proposed_date", nullable = false)
    private LocalDate proposedDate;

    @Column(name = "proposed_start_time", nullable = false)
    private LocalTime proposedStartTime;

    @Column(name = "proposed_duration_minutes", nullable = false)
    private Integer proposedDurationMinutes;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposal_status", nullable = false, length = 20)
    @lombok.Builder.Default
    private ProposalStatus proposalStatus = ProposalStatus.PENDING;
}
