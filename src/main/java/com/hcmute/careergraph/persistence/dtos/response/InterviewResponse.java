package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.interview.InterviewStatus;
import com.hcmute.careergraph.enums.interview.InterviewType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InterviewResponse {

    private String id;
    private String applicationId;
    private String candidateId;
    private String candidateName;
    private String candidateAvatar;
    private String jobId;
    private String jobTitle;
    private String companyId;
    private String companyName;
    private LocalDateTime scheduledAt;
    private LocalDateTime endAt;
    private Integer durationMinutes;
    private InterviewType type;
    private InterviewStatus interviewStatus;
    private String meetingLink;
    private String location;
    private String notes;
    private String rescheduledFromId;
    private String cancellationReason;
    private Integer roundNumber;
    private List<ParticipantResponse> interviewers;
    private List<InterviewFeedbackResponse> feedback;
    private List<InterviewRecordingResponse> recordings;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;

    @Data
    @Builder
    public static class ParticipantResponse {
        private String id;
        private String accountId;
        private String name;
        private String role;
        private LocalDateTime joinedAt;
        private LocalDateTime leftAt;
    }
}
