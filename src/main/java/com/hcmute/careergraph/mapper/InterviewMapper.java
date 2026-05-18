package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.InterviewFeedbackResponse;
import com.hcmute.careergraph.persistence.dtos.response.InterviewRecordingResponse;
import com.hcmute.careergraph.persistence.dtos.response.InterviewResponse;
import com.hcmute.careergraph.persistence.dtos.response.InterviewTimeProposalResponse;
import com.hcmute.careergraph.persistence.models.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InterviewMapper {

        public InterviewResponse toResponse(Interview interview, boolean includeDetails) {
                if (interview == null)
                        return null;

                String candidateName = Optional.ofNullable(interview.getCandidate())
                                .map(c -> {
                                        String first = c.getFirstName() != null ? c.getFirstName() : "";
                                        String last = c.getLastName() != null ? c.getLastName() : "";
                                        String name = (first + " " + last).trim();
                                        return name.isEmpty() ? c.getTagname() : name;
                                })
                                .orElse(null);

                String candidateAvatar = Optional.ofNullable(interview.getCandidate())
                                .map(Candidate::getAvatar)
                                .orElse(null);

                String jobTitle = Optional.ofNullable(interview.getJob())
                                .map(Job::getTitle)
                                .orElse(null);

                String companyName = Optional.ofNullable(interview.getCompany())
                                .map(Company::getName)
                                .orElse(null);

                InterviewResponse.InterviewResponseBuilder builder = InterviewResponse.builder()
                                .id(interview.getId())
                                .applicationId(interview.getApplication() != null ? interview.getApplication().getId()
                                                : null)
                                .candidateId(interview.getCandidate() != null ? interview.getCandidate().getId() : null)
                                .candidateName(candidateName)
                                .candidateAvatar(candidateAvatar)
                                .jobId(interview.getJob() != null ? interview.getJob().getId() : null)
                                .jobTitle(jobTitle)
                                .companyId(interview.getCompany() != null ? interview.getCompany().getId() : null)
                                .companyName(companyName)
                                .scheduledAt(interview.getScheduledAt())
                                .endAt(interview.getEndAt())
                                .durationMinutes(interview.getDurationMinutes())
                                .type(interview.getType())
                                .interviewStatus(interview.getInterviewStatus())
                                .meetingLink(interview.getMeetingLink())
                                .location(interview.getLocation())
                                .notes(interview.getNotes())
                                .rescheduledFromId(interview.getRescheduledFromId())
                                .cancellationReason(interview.getCancellationReason())
                                .roundNumber(resolveRoundNumber(interview.getNotes()))
                                .interviewers(mapParticipants(interview.getParticipants()))
                                .createdDate(interview.getCreatedDate())
                                .lastModifiedDate(interview.getLastModifiedDate());

                if (includeDetails) {
                        builder.feedback(mapFeedbacks(interview.getFeedbacks()));
                        builder.recordings(mapRecordings(interview.getRecordings()));
                }

                return builder.build();
        }

        public List<InterviewResponse> toResponseList(List<Interview> interviews, boolean includeDetails) {
                if (interviews == null || interviews.isEmpty())
                        return Collections.emptyList();
                return interviews.stream()
                                .map(i -> toResponse(i, includeDetails))
                                .toList();
        }

        public InterviewFeedbackResponse toFeedbackResponse(InterviewFeedback feedback) {
                if (feedback == null)
                        return null;

                String reviewerName = Optional.ofNullable(feedback.getReviewer())
                                .map(Account::getEmail)
                                .orElse(null);

                return InterviewFeedbackResponse.builder()
                                .id(feedback.getId())
                                .interviewId(feedback.getInterview() != null ? feedback.getInterview().getId() : null)
                                .reviewerId(feedback.getReviewer() != null ? feedback.getReviewer().getId() : null)
                                .reviewerName(reviewerName)
                                .overallRating(feedback.getOverallRating())
                                .technicalScore(feedback.getTechnicalScore())
                                .communicationScore(feedback.getCommunicationScore())
                                .cultureFitScore(feedback.getCultureFitScore())
                                .problemSolvingScore(feedback.getProblemSolvingScore())
                                .strengths(feedback.getStrengths())
                                .weaknesses(feedback.getWeaknesses())
                                .notes(feedback.getNotes())
                                .recommendation(feedback.getRecommendation())
                                .createdDate(feedback.getCreatedDate())
                                .build();
        }

        private List<InterviewResponse.ParticipantResponse> mapParticipants(List<InterviewParticipant> participants) {
                if (participants == null || participants.isEmpty())
                        return Collections.emptyList();
                return participants.stream()
                                .filter(Objects::nonNull)
                                .map(p -> InterviewResponse.ParticipantResponse.builder()
                                                .id(p.getId())
                                                .accountId(p.getAccount() != null ? p.getAccount().getId() : null)
                                                .name(p.getAccount() != null ? p.getAccount().getEmail() : null)
                                                .role(p.getRole() != null ? p.getRole().name() : null)
                                                .joinedAt(p.getJoinedAt())
                                                .leftAt(p.getLeftAt())
                                                .build())
                                .toList();
        }

        private List<InterviewFeedbackResponse> mapFeedbacks(List<InterviewFeedback> feedbacks) {
                if (feedbacks == null || feedbacks.isEmpty())
                        return Collections.emptyList();
                return feedbacks.stream()
                                .filter(Objects::nonNull)
                                .map(this::toFeedbackResponse)
                                .toList();
        }

        private List<InterviewRecordingResponse> mapRecordings(List<InterviewRecording> recordings) {
                if (recordings == null || recordings.isEmpty())
                        return Collections.emptyList();
                return recordings.stream()
                                .filter(Objects::nonNull)
                                .map(this::toRecordingResponse)
                                .toList();
        }

        public InterviewTimeProposalResponse toProposalResponse(InterviewTimeProposal proposal) {
                if (proposal == null)
                        return null;
                return InterviewTimeProposalResponse.builder()
                                .id(proposal.getId())
                                .interviewId(proposal.getInterview() != null ? proposal.getInterview().getId() : null)
                                .proposedDate(proposal.getProposedDate())
                                .proposedStartTime(proposal.getProposedStartTime())
                                .proposedDurationMinutes(proposal.getProposedDurationMinutes())
                                .notes(proposal.getNotes())
                                .proposalStatus(proposal.getProposalStatus())
                                .createdDate(proposal.getCreatedDate())
                                .build();
        }

        public List<InterviewTimeProposalResponse> toProposalResponseList(List<InterviewTimeProposal> proposals) {
                if (proposals == null || proposals.isEmpty())
                        return Collections.emptyList();
                return proposals.stream()
                                .filter(Objects::nonNull)
                                .map(this::toProposalResponse)
                                .toList();
        }

        public InterviewRecordingResponse toRecordingResponse(InterviewRecording recording) {
                if (recording == null)
                        return null;
                return InterviewRecordingResponse.builder()
                                .id(recording.getId())
                                .interviewId(recording.getInterview() != null ? recording.getInterview().getId() : null)
                                .fileKey(recording.getFileKey())
                                .fileSize(recording.getFileSize())
                                .durationSeconds(recording.getDurationSeconds())
                                .mimeType(recording.getMimeType())
                                .recordingStatus(recording.getRecordingStatus())
                                .recordedBy(recording.getRecordedBy())
                                .thumbnailKey(recording.getThumbnailKey())
                                .transcriptKey(recording.getTranscriptKey())
                                .analysisSummary(recording.getAnalysisSummary())
                                .analyzedAt(recording.getAnalyzedAt())
                                .createdDate(recording.getCreatedDate())
                                .build();
        }

        private Integer resolveRoundNumber(String notes) {
                if (notes == null) {
                        return 1;
                }
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                                .compile("\\[ROUND:(\\d+)]")
                                .matcher(notes);
                if (!matcher.find()) {
                        return 1;
                }
                try {
                        int parsed = Integer.parseInt(matcher.group(1));
                        return parsed > 0 ? parsed : 1;
                } catch (NumberFormatException ex) {
                        return 1;
                }
        }
}
