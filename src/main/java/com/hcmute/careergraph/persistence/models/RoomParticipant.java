package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.interview.AdmitStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_participants")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"room", "application", "candidate"})
@EqualsAndHashCode(callSuper = true, exclude = {"room", "application", "candidate"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private InterviewRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "slot_start", nullable = false)
    private LocalDateTime slotStart;

    @Column(name = "slot_end", nullable = false)
    private LocalDateTime slotEnd;

    @Column(name = "join_token", columnDefinition = "TEXT")
    private String joinToken;

    @Column(name = "session_token", length = 255)
    private String sessionToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "admit_status", nullable = false, length = 20)
    @lombok.Builder.Default
    private AdmitStatus admitStatus = AdmitStatus.PENDING;

    @Column(name = "knock_count", nullable = false)
    @lombok.Builder.Default
    private Integer knockCount = 0;

    @Column(name = "last_knock_at")
    private LocalDateTime lastKnockAt;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "hr_note", columnDefinition = "TEXT")
    private String hrNote;
}
