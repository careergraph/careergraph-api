package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.interview.InterviewStatus;
import com.hcmute.careergraph.enums.interview.InterviewType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interviews")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = { "application", "company", "job", "candidate", "participants", "recordings",
        "feedbacks", "timeProposals" })
@EqualsAndHashCode(callSuper = true, exclude = { "application", "company", "job", "candidate", "participants",
        "recordings", "feedbacks", "timeProposals" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class Interview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private InterviewType type;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "meeting_link", length = 50)
    private String meetingLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_status", nullable = false, length = 30)
    private InterviewStatus interviewStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "rescheduled_from_id")
    private String rescheduledFromId;

    @Column(name = "hidden_from_candidate", nullable = false)
    @lombok.Builder.Default
    private boolean hiddenFromCandidate = false;

    @Column(name = "reminder_24h_sent", nullable = false)
    @lombok.Builder.Default
    private boolean reminder24hSent = false;

    @Column(name = "reminder_1h_sent", nullable = false)
    @lombok.Builder.Default
    private boolean reminder1hSent = false;

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    @lombok.Builder.Default
    private List<InterviewParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL)
    @lombok.Builder.Default
    private List<InterviewRecording> recordings = new ArrayList<>();

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL)
    @lombok.Builder.Default
    private List<InterviewFeedback> feedbacks = new ArrayList<>();

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdDate ASC")
    @lombok.Builder.Default
    private List<InterviewTimeProposal> timeProposals = new ArrayList<>();

    public void addParticipant(InterviewParticipant participant) {
        if (participant != null) {
            participant.setInterview(this);
            participants.add(participant);
        }
    }
}
