package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.interview.*;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.persistence.dtos.request.InterviewFeedbackRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewRescheduleRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewTimeProposalRequest;
import com.hcmute.careergraph.persistence.models.*;
import com.hcmute.careergraph.repositories.*;
import com.hcmute.careergraph.services.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewParticipantRepository participantRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final InterviewTimeProposalRepository timeProposalRepository;
    private final ApplicationRepository applicationRepository;
    private final AccountRepository accountRepository;
    private final CandidateRepository candidateRepository;

    private static final List<InterviewStatus> ACTIVE_STATUSES =
            List.of(InterviewStatus.SCHEDULED, InterviewStatus.CONFIRMED);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public Interview createInterview(InterviewRequest request, String companyId) {
        log.info("Creating interview for application: {}", request.getApplicationId());

        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new BadRequestException("Application not found"));

        if (!application.getJob().getCompany().getId().equals(companyId)) {
            throw new BadRequestException("Application does not belong to your company");
        }

        LocalDate date = LocalDate.parse(request.getDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        LocalTime startTime = LocalTime.parse(request.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalDateTime scheduledAt = LocalDateTime.of(date, startTime);
        LocalDateTime endAt = scheduledAt.plusMinutes(request.getDurationMinutes());

        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot schedule interview in the past");
        }

        InterviewType type = InterviewType.valueOf(request.getType().toUpperCase());

        // Check candidate conflicts
        List<Interview> conflicts = interviewRepository.findOverlappingByCandidate(
                application.getCandidate().getId(), scheduledAt, endAt, ACTIVE_STATUSES);
        if (!conflicts.isEmpty()) {
            throw new BadRequestException("Candidate has a scheduling conflict");
        }

        // Check interviewer conflicts
        if (request.getInterviewerIds() != null) {
            for (String interviewerId : request.getInterviewerIds()) {
                List<Interview> interviewerConflicts = interviewRepository.findOverlappingByParticipant(
                        interviewerId, scheduledAt, endAt, ACTIVE_STATUSES);
                if (!interviewerConflicts.isEmpty()) {
                    throw new BadRequestException("Interviewer " + interviewerId + " has a scheduling conflict");
                }
            }
        }

        String meetingLink = type == InterviewType.ONLINE ? generateMeetingLink() : null;

        if (type == InterviewType.ONLINE && meetingLink == null) {
            throw new BadRequestException("Online interview requires a meeting link");
        }
        if (type == InterviewType.OFFLINE && !StringUtils.hasText(request.getLocation())) {
            throw new BadRequestException("Offline interview requires a location");
        }

        Interview interview = Interview.builder()
                .application(application)
                .company(application.getJob().getCompany())
                .job(application.getJob())
                .candidate(application.getCandidate())
                .scheduledAt(scheduledAt)
                .endAt(endAt)
                .durationMinutes(request.getDurationMinutes())
                .type(type)
                .location(request.getLocation())
                .meetingLink(meetingLink)
                .interviewStatus(InterviewStatus.SCHEDULED)
                .notes(request.getNotes())
                .build();

        Interview saved = interviewRepository.save(interview);

        // Add candidate as participant
        Account candidateAccount = findAccountByCandidate(application.getCandidate());
        if (candidateAccount != null) {
            InterviewParticipant candidateParticipant = InterviewParticipant.builder()
                    .interview(saved)
                    .account(candidateAccount)
                    .role(ParticipantRole.CANDIDATE)
                    .build();
            participantRepository.save(candidateParticipant);
        }

        // Add interviewers as participants
        if (request.getInterviewerIds() != null) {
            for (String interviewerId : request.getInterviewerIds()) {
                Account interviewerAccount = accountRepository.findById(interviewerId).orElse(null);
                if (interviewerAccount != null) {
                    InterviewParticipant participant = InterviewParticipant.builder()
                            .interview(saved)
                            .account(interviewerAccount)
                            .role(ParticipantRole.INTERVIEWER)
                            .build();
                    participantRepository.save(participant);
                }
            }
        }

        log.info("Interview created with id: {}", saved.getId());
        return interviewRepository.findById(saved.getId()).orElse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Interview getInterviewById(String id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Interview not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Interview> getInterviewsByCompany(String companyId, String status, Pageable pageable) {
        if (StringUtils.hasText(status)) {
            InterviewStatus interviewStatus = InterviewStatus.valueOf(status.toUpperCase());
            return interviewRepository.findByCompanyIdAndInterviewStatusIn(
                    companyId, List.of(interviewStatus), pageable);
        }
        return interviewRepository.findByCompanyId(companyId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Interview> getInterviewsByCandidate(String candidateId, String statusFilter) {
        if (StringUtils.hasText(statusFilter)) {
            List<InterviewStatus> statuses = resolveStatusFilter(statusFilter);
            return interviewRepository.findByCandidateIdAndInterviewStatusIn(candidateId, statuses);
        }
        return interviewRepository.findByCandidateIdAndInterviewStatusIn(
                candidateId, List.of(InterviewStatus.values()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Interview> getUpcomingByCandidateId(String candidateId, int limit) {
        List<Interview> all = interviewRepository.findUpcomingByCandidate(candidateId, ACTIVE_STATUSES);
        return all.size() > limit ? all.subList(0, limit) : all;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Interview> getCalendarEvents(String companyId, LocalDateTime start, LocalDateTime end) {
        return interviewRepository.findByCompanyIdAndScheduledAtBetween(companyId, start, end);
    }

    @Override
    public Interview confirmInterview(String id, String candidateId) {
        Interview interview = getInterviewById(id);
        if (!interview.getCandidate().getId().equals(candidateId)) {
            throw new BadRequestException("This interview does not belong to you");
        }
        if (interview.getInterviewStatus() != InterviewStatus.SCHEDULED) {
            throw new BadRequestException("Interview can only be confirmed when in SCHEDULED status");
        }
        interview.setInterviewStatus(InterviewStatus.CONFIRMED);
        return interviewRepository.save(interview);
    }

    @Override
    public Interview declineInterview(String id, String candidateId, String reason) {
        Interview interview = getInterviewById(id);
        if (!interview.getCandidate().getId().equals(candidateId)) {
            throw new BadRequestException("This interview does not belong to you");
        }
        if (interview.getInterviewStatus() != InterviewStatus.SCHEDULED
                && interview.getInterviewStatus() != InterviewStatus.CONFIRMED) {
            throw new BadRequestException("Interview cannot be declined in current status");
        }
        interview.setInterviewStatus(InterviewStatus.CANCELLED);
        interview.setCancellationReason(reason);
        return interviewRepository.save(interview);
    }

    @Override
    public Interview cancelInterview(String id, String companyId, String reason) {
        Interview interview = getInterviewById(id);
        if (!interview.getCompany().getId().equals(companyId)) {
            throw new BadRequestException("Interview does not belong to your company");
        }
        if (interview.getInterviewStatus() == InterviewStatus.COMPLETED
                || interview.getInterviewStatus() == InterviewStatus.CANCELLED) {
            throw new BadRequestException("Interview cannot be cancelled in current status");
        }
        interview.setInterviewStatus(InterviewStatus.CANCELLED);
        interview.setCancellationReason(reason);
        return interviewRepository.save(interview);
    }

    @Override
    public Interview rescheduleInterview(String id, InterviewRescheduleRequest request, String companyId) {
        Interview original = getInterviewById(id);
        if (!original.getCompany().getId().equals(companyId)) {
            throw new BadRequestException("Interview does not belong to your company");
        }

        // Cancel the original
        original.setInterviewStatus(InterviewStatus.CANCELLED);
        original.setCancellationReason("Rescheduled");
        interviewRepository.save(original);

        // Create new interview
        LocalDate date = LocalDate.parse(request.getNewDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        LocalTime startTime = LocalTime.parse(request.getNewStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalDateTime scheduledAt = LocalDateTime.of(date, startTime);
        LocalDateTime endAt = scheduledAt.plusMinutes(request.getDurationMinutes());

        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot schedule interview in the past");
        }

        String meetingLink = original.getType() == InterviewType.ONLINE ? generateMeetingLink() : null;

        Interview rescheduled = Interview.builder()
                .application(original.getApplication())
                .company(original.getCompany())
                .job(original.getJob())
                .candidate(original.getCandidate())
                .scheduledAt(scheduledAt)
                .endAt(endAt)
                .durationMinutes(request.getDurationMinutes())
                .type(original.getType())
                .location(original.getLocation())
                .meetingLink(meetingLink)
                .interviewStatus(InterviewStatus.SCHEDULED)
                .notes(request.getNotes())
                .rescheduledFromId(original.getId())
                .build();

        Interview saved = interviewRepository.save(rescheduled);

        // Copy participants from original
        List<InterviewParticipant> originalParticipants = participantRepository.findByInterviewId(original.getId());
        for (InterviewParticipant op : originalParticipants) {
            InterviewParticipant newP = InterviewParticipant.builder()
                    .interview(saved)
                    .account(op.getAccount())
                    .role(op.getRole())
                    .build();
            participantRepository.save(newP);
        }

        log.info("Interview {} rescheduled to new interview {}", id, saved.getId());
        return interviewRepository.findById(saved.getId()).orElse(saved);
    }

    @Override
    public Interview completeInterview(String id, String companyId) {
        Interview interview = getInterviewById(id);
        if (!interview.getCompany().getId().equals(companyId)) {
            throw new BadRequestException("Interview does not belong to your company");
        }
        if (interview.getInterviewStatus() != InterviewStatus.CONFIRMED
                && interview.getInterviewStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BadRequestException("Interview can only be completed from CONFIRMED or IN_PROGRESS status");
        }
        interview.setInterviewStatus(InterviewStatus.COMPLETED);
        return interviewRepository.save(interview);
    }

    @Override
    public InterviewFeedback addFeedback(String interviewId, InterviewFeedbackRequest request, String reviewerAccountId) {
        Interview interview = getInterviewById(interviewId);
        if (interview.getInterviewStatus() != InterviewStatus.COMPLETED) {
            throw new BadRequestException("Feedback can only be added to completed interviews");
        }

        if (feedbackRepository.existsByInterviewIdAndReviewerId(interviewId, reviewerAccountId)) {
            throw new BadRequestException("You have already submitted feedback for this interview");
        }

        Account reviewer = accountRepository.findById(reviewerAccountId)
                .orElseThrow(() -> new BadRequestException("Reviewer account not found"));

        FeedbackRecommendation recommendation = FeedbackRecommendation.valueOf(
                request.getRecommendation().toUpperCase());

        InterviewFeedback feedback = InterviewFeedback.builder()
                .interview(interview)
                .reviewer(reviewer)
                .overallRating(request.getOverallRating())
                .technicalScore(request.getTechnicalScore())
                .communicationScore(request.getCommunicationScore())
                .cultureFitScore(request.getCultureFitScore())
                .problemSolvingScore(request.getProblemSolvingScore())
                .strengths(request.getStrengths())
                .weaknesses(request.getWeaknesses())
                .notes(request.getNotes())
                .recommendation(recommendation)
                .build();

        return feedbackRepository.save(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewFeedback> getFeedback(String interviewId) {
        return feedbackRepository.findByInterviewId(interviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Interview> getInterviewsByApplication(String applicationId) {
        return interviewRepository.findByApplicationId(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Application> getUnscheduledApplicationsByJob(String jobId, String companyId) {
        List<Application> allApps = applicationRepository.findByCompanyIdAndJobId(companyId, jobId);
        List<String> scheduledAppIds = interviewRepository.findScheduledApplicationIdsByJobId(jobId, ACTIVE_STATUSES);
        return allApps.stream()
                .filter(app -> !scheduledAppIds.contains(app.getId()))
                .toList();
    }

    @Override
    public List<InterviewTimeProposal> proposeAlternativeTimes(
            String interviewId, InterviewTimeProposalRequest request, String candidateId) {
        Interview interview = getInterviewById(interviewId);
        if (!interview.getCandidate().getId().equals(candidateId)) {
            throw new BadRequestException("This interview does not belong to you");
        }
        if (interview.getInterviewStatus() != InterviewStatus.SCHEDULED
                && interview.getInterviewStatus() != InterviewStatus.PENDING_RESCHEDULE) {
            throw new BadRequestException("Can only propose alternatives for SCHEDULED interviews");
        }

        List<InterviewTimeProposal> proposals = new ArrayList<>();
        for (InterviewTimeProposalRequest.TimeSlot slot : request.getProposedSlots()) {
            LocalDate date = LocalDate.parse(slot.getDate(), DateTimeFormatter.ISO_LOCAL_DATE);
            LocalTime time = LocalTime.parse(slot.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
            int dur = slot.getDurationMinutes() != null ? slot.getDurationMinutes() : interview.getDurationMinutes();

            InterviewTimeProposal proposal = InterviewTimeProposal.builder()
                    .interview(interview)
                    .proposedDate(date)
                    .proposedStartTime(time)
                    .proposedDurationMinutes(dur)
                    .notes(request.getNotes())
                    .proposalStatus(ProposalStatus.PENDING)
                    .build();
            proposals.add(timeProposalRepository.save(proposal));
        }

        interview.setInterviewStatus(InterviewStatus.PENDING_RESCHEDULE);
        interviewRepository.save(interview);

        log.info("Candidate proposed {} alternative time(s) for interview {}", proposals.size(), interviewId);
        return proposals;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewTimeProposal> getProposals(String interviewId) {
        return timeProposalRepository.findByInterviewIdOrderByCreatedDateAsc(interviewId);
    }

    @Override
    public Interview acceptProposal(String interviewId, String proposalId, String companyId) {
        Interview interview = getInterviewById(interviewId);
        if (!interview.getCompany().getId().equals(companyId)) {
            throw new BadRequestException("Interview does not belong to your company");
        }

        InterviewTimeProposal proposal = timeProposalRepository.findById(proposalId)
                .orElseThrow(() -> new BadRequestException("Proposal not found"));
        if (!proposal.getInterview().getId().equals(interviewId)) {
            throw new BadRequestException("Proposal does not belong to this interview");
        }
        if (proposal.getProposalStatus() != ProposalStatus.PENDING) {
            throw new BadRequestException("Proposal is not in PENDING status");
        }

        // Reject all other pending proposals
        List<InterviewTimeProposal> pendingProposals =
                timeProposalRepository.findByInterviewIdAndProposalStatus(interviewId, ProposalStatus.PENDING);
        for (InterviewTimeProposal p : pendingProposals) {
            if (!p.getId().equals(proposalId)) {
                p.setProposalStatus(ProposalStatus.REJECTED);
                timeProposalRepository.save(p);
            }
        }

        // Accept this proposal
        proposal.setProposalStatus(ProposalStatus.ACCEPTED);
        timeProposalRepository.save(proposal);

        // Cancel the original interview
        interview.setInterviewStatus(InterviewStatus.CANCELLED);
        interview.setCancellationReason("Rescheduled per candidate proposal");
        interviewRepository.save(interview);

        // Create new interview with proposed times
        LocalDateTime scheduledAt = LocalDateTime.of(proposal.getProposedDate(), proposal.getProposedStartTime());
        LocalDateTime endAt = scheduledAt.plusMinutes(proposal.getProposedDurationMinutes());

        String meetingLink = interview.getType() == InterviewType.ONLINE ? generateMeetingLink() : null;

        Interview rescheduled = Interview.builder()
                .application(interview.getApplication())
                .company(interview.getCompany())
                .job(interview.getJob())
                .candidate(interview.getCandidate())
                .scheduledAt(scheduledAt)
                .endAt(endAt)
                .durationMinutes(proposal.getProposedDurationMinutes())
                .type(interview.getType())
                .location(interview.getLocation())
                .meetingLink(meetingLink)
                .interviewStatus(InterviewStatus.CONFIRMED)
                .notes(proposal.getNotes())
                .rescheduledFromId(interview.getId())
                .build();

        Interview saved = interviewRepository.save(rescheduled);

        // Copy participants from original
        List<InterviewParticipant> originalParticipants = participantRepository.findByInterviewId(interview.getId());
        for (InterviewParticipant op : originalParticipants) {
            InterviewParticipant newP = InterviewParticipant.builder()
                    .interview(saved)
                    .account(op.getAccount())
                    .role(op.getRole())
                    .build();
            participantRepository.save(newP);
        }

        log.info("HR accepted proposal {} — rescheduled interview {} to {}", proposalId, interviewId, saved.getId());
        return interviewRepository.findById(saved.getId()).orElse(saved);
    }

    @Override
    public void rejectProposal(String interviewId, String proposalId, String companyId) {
        Interview interview = getInterviewById(interviewId);
        if (!interview.getCompany().getId().equals(companyId)) {
            throw new BadRequestException("Interview does not belong to your company");
        }

        InterviewTimeProposal proposal = timeProposalRepository.findById(proposalId)
                .orElseThrow(() -> new BadRequestException("Proposal not found"));
        if (!proposal.getInterview().getId().equals(interviewId)) {
            throw new BadRequestException("Proposal does not belong to this interview");
        }

        proposal.setProposalStatus(ProposalStatus.REJECTED);
        timeProposalRepository.save(proposal);

        // If no more pending proposals, revert interview back to SCHEDULED
        List<InterviewTimeProposal> remaining =
                timeProposalRepository.findByInterviewIdAndProposalStatus(interviewId, ProposalStatus.PENDING);
        if (remaining.isEmpty()) {
            interview.setInterviewStatus(InterviewStatus.SCHEDULED);
            interviewRepository.save(interview);
        }

        log.info("HR rejected proposal {} for interview {}", proposalId, interviewId);
    }

    private String generateMeetingLink() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private Account findAccountByCandidate(Candidate candidate) {
        if (candidate == null) return null;
        return accountRepository.findByCandidateId(candidate.getId()).orElse(null);
    }

    private List<InterviewStatus> resolveStatusFilter(String filter) {
        return switch (filter.toUpperCase()) {
            case "UPCOMING" -> List.of(InterviewStatus.SCHEDULED, InterviewStatus.CONFIRMED, InterviewStatus.PENDING_RESCHEDULE);
            case "PAST" -> List.of(InterviewStatus.COMPLETED, InterviewStatus.NO_SHOW);
            case "CANCELLED" -> List.of(InterviewStatus.CANCELLED);
            default -> {
                try {
                    yield List.of(InterviewStatus.valueOf(filter.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    yield List.of(InterviewStatus.values());
                }
            }
        };
    }

    @Override
    public Interview getInterviewByRoomCode(String roomCode) {
        return interviewRepository.findByMeetingLink(roomCode)
                .orElseThrow(() -> new BadRequestException("Interview room not found"));
    }
}
