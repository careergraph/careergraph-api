package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.interview.AdmitStatus;
import com.hcmute.careergraph.enums.interview.RoomStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InterviewRoomResponse {

    private String id;
    private String jobId;
    private String jobTitle;
    private LocalDate interviewDate;
    private String roomCode;
    private RoomStatus roomStatus;
    private String hostId;
    private String hostName;
    private LocalDateTime openAt;
    private LocalDateTime closedAt;
    private Integer maxDuration;
    private List<RoomParticipantResponse> participants;
    private LocalDateTime createdDate;

    @Data
    @Builder
    public static class RoomParticipantResponse {
        private String id;
        private String applicationId;
        private String candidateId;
        private String candidateName;
        private String candidateEmail;
        private String candidateAvatar;
        private LocalDateTime slotStart;
        private LocalDateTime slotEnd;
        private AdmitStatus admitStatus;
        private Integer knockCount;
        private LocalDateTime joinedAt;
        private LocalDateTime leftAt;
        private String hrNote;
    }
}
