package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.interview.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interview_rooms",
        uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "interview_date"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"job", "host", "participants"})
@EqualsAndHashCode(callSuper = true, exclude = {"job", "host", "participants"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewRoom extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "interview_date", nullable = false)
    private LocalDate interviewDate;

    @Column(name = "room_code", unique = true, nullable = false, length = 20)
    private String roomCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_status", nullable = false, length = 20)
    private RoomStatus roomStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private Account host;

    @Column(name = "open_at")
    private LocalDateTime openAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "max_duration", nullable = false)
    @lombok.Builder.Default
    private Integer maxDuration = 480;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("slotStart ASC")
    @lombok.Builder.Default
    private List<RoomParticipant> participants = new ArrayList<>();
}
