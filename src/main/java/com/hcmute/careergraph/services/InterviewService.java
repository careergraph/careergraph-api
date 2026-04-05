package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.request.InterviewFeedbackRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewRecordingRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewRescheduleRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewTimeProposalRequest;
import com.hcmute.careergraph.persistence.models.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewService {

    Interview createInterview(InterviewRequest request, String companyId);

    Interview getInterviewById(String id);

    Page<Interview> getInterviewsByCompany(String companyId, String status, Pageable pageable);

    List<Interview> getInterviewsByCandidate(String candidateId, String statusFilter);

    List<Interview> getUpcomingByCandidateId(String candidateId, int limit);

    List<Interview> getCalendarEvents(String companyId, LocalDateTime start, LocalDateTime end);

    Interview confirmInterview(String id, String candidateId);

    Interview declineInterview(String id, String candidateId, String reason);

    Interview cancelInterview(String id, String companyId, String reason);

    Interview rescheduleInterview(String id, InterviewRescheduleRequest request, String companyId);

    Interview completeInterview(String id, String companyId);

    InterviewFeedback addFeedback(String interviewId, InterviewFeedbackRequest request, String reviewerAccountId);

    List<InterviewFeedback> getFeedback(String interviewId);

    List<Interview> getInterviewsByApplication(String applicationId);

    List<Application> getUnscheduledApplicationsByJob(String jobId, String companyId);

    List<InterviewTimeProposal> proposeAlternativeTimes(String interviewId, InterviewTimeProposalRequest request,
            String candidateId);

    List<InterviewTimeProposal> getProposals(String interviewId);

    Interview acceptProposal(String interviewId, String proposalId, String companyId);

    void rejectProposal(String interviewId, String proposalId, String companyId);

    Interview getInterviewByRoomCode(String roomCode);

    List<Interview> getInterviewsByRoomCode(String roomCode);

    InterviewRecording saveRecording(String interviewId, InterviewRecordingRequest request, String recordedBy);

    List<InterviewRecording> getRecordings(String interviewId);

    Interview startInterview(String id, String companyId);
}
