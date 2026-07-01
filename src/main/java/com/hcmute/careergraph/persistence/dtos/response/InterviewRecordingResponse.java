package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.interview.RecordingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InterviewRecordingResponse {

    private String id;
    private String interviewId;
    private String roomParticipantId;
    private String applicationId;
    private String candidateId;
    private String candidateName;
    private String fileKey;
    private Long fileSize;
    private Integer durationSeconds;
    private String mimeType;
    private RecordingStatus recordingStatus;
    private String recordedBy;
    private String thumbnailKey;
    private String transcriptKey;
    private String analysisSummary;
    private LocalDateTime analyzedAt;
    private LocalDateTime createdDate;
}
