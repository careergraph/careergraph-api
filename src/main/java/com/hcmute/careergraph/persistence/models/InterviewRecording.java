package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.interview.RecordingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_recordings")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"interview"})
@EqualsAndHashCode(callSuper = true, exclude = {"interview"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewRecording extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Column(name = "file_key", length = 500)
    private String fileKey;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "recording_status", nullable = false, length = 20)
    private RecordingStatus recordingStatus;

    @Column(name = "recorded_by")
    private String recordedBy;

    @Column(name = "thumbnail_key", length = 500)
    private String thumbnailKey;

    @Column(name = "transcript_key", length = 500)
    private String transcriptKey;

    @Column(name = "analysis_summary", columnDefinition = "TEXT")
    private String analysisSummary;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;
}
