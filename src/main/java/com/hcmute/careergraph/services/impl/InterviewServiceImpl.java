package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.common.ErrorType;
import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.enums.interview.*;
import com.hcmute.careergraph.exception.AppException;
import com.hcmute.careergraph.persistence.dtos.request.InterviewFeedbackRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewRecordingRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewRescheduleRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewTimeProposalRequest;
import com.hcmute.careergraph.persistence.models.*;
import com.hcmute.careergraph.repositories.*;
import com.hcmute.careergraph.services.InterviewRoomService;
import com.hcmute.careergraph.services.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewParticipantRepository participantRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final InterviewRecordingRepository recordingRepository;
    private final InterviewTimeProposalRepository timeProposalRepository;
    private final ApplicationRepository applicationRepository;
    private final AccountRepository accountRepository;
    private final InterviewRoomService roomService;

    private static final List<InterviewStatus> ACTIVE_STATUSES = List.of(InterviewStatus.SCHEDULED,
            InterviewStatus.CONFIRMED,
            InterviewStatus.PENDING_RESCHEDULE,
            InterviewStatus.IN_PROGRESS);
        private static final Set<ApplicationStage> SCHEDULABLE_STAGES = Set.of(
            ApplicationStage.HR_CONTACTED,
            ApplicationStage.SCHEDULED,
            ApplicationStage.INTERVIEW,
            ApplicationStage.INTERVIEW_SCHEDULED);

    @Override
    public Interview createInterview(InterviewRequest request, String companyId) {
        log.info("Creating interview for application: {}", request.getApplicationId());

        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new AppException(ErrorType.BAD_REQUEST, "Không tìm thấy hồ sơ ứng viên"));

        if (!application.getJob().getCompany().getId().equals(companyId)) {
            throw new AppException(ErrorType.BAD_REQUEST, "Hồ sơ ứng viên không thuộc công ty của bạn");
        }

        LocalDate date = LocalDate.parse(request.getDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        LocalTime startTime = LocalTime.parse(request.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalDateTime scheduledAt = LocalDateTime.of(date, startTime);

        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorType.BAD_REQUEST, "Không thể lên lịch phỏng vấn trong quá khứ");
        }

        if (request.getDurationMinutes() == null || request.getDurationMinutes() < 15) {
            throw new AppException(ErrorType.BAD_REQUEST, "Thời lượng phỏng vấn tối thiểu là 15 phút");
        }

        validateApplicationStageForScheduling(application);

        LocalDateTime endAt = scheduledAt.plusMinutes(request.getDurationMinutes());

        InterviewType type = InterviewType.valueOf(request.getType().toUpperCase());

        List<Interview> existingActiveInterviews = interviewRepository.findActiveByApplicationAndJob(
                application.getId(), application.getJob().getId(), ACTIVE_STATUSES);

        if (!existingActiveInterviews.isEmpty()) {
            Interview currentActive = existingActiveInterviews.get(0);
            boolean sameDay = currentActive.getScheduledAt() != null
                    && currentActive.getScheduledAt().toLocalDate().isEqual(date);

            boolean hasInProgressInterview = existingActiveInterviews.stream()
                .anyMatch(i -> i.getInterviewStatus() == InterviewStatus.IN_PROGRESS);
            if (hasInProgressInterview) {
            throw new AppException(
                ErrorType.BAD_REQUEST,
                "Không thể tạo lịch mới khi ứng viên đang có buổi phỏng vấn diễn ra");
            }

            if (!request.isConfirmOverwrite()) {
                String marker = sameDay ? "ACTIVE_INTERVIEW_SAME_DAY" : "ACTIVE_INTERVIEW_EXISTS";
                throw new AppException(
                        ErrorType.BAD_REQUEST,
                        String.format(
                                "%s|Ứng viên đã có lịch phỏng vấn %s vào %s. Vui lòng xác nhận ghi đè để cập nhật lịch mới.",
                                marker,
                                currentActive.getInterviewStatus(),
                                currentActive.getScheduledAt()));
            }

            for (Interview active : existingActiveInterviews) {
                active.setInterviewStatus(InterviewStatus.CANCELLED);
                active.setCancellationReason("Overwritten by HR scheduling");
                active.setHiddenFromCandidate(false);
                interviewRepository.save(active);
            }
        }

        // Check candidate conflicts
        List<Interview> conflicts = interviewRepository.findOverlappingByCandidate(
                application.getCandidate().getId(), scheduledAt, endAt, ACTIVE_STATUSES);
        if (!conflicts.isEmpty()) {
            throw new AppException(ErrorType.BAD_REQUEST, "Ứng viên đang có lịch phỏng vấn trùng thời gian");
        }

        // Check interviewer conflicts
        if (request.getInterviewerIds() != null) {
            for (String interviewerId : request.getInterviewerIds()) {
                List<Interview> interviewerConflicts = interviewRepository.findOverlappingByParticipant(
                        interviewerId, scheduledAt, endAt, ACTIVE_STATUSES);
                if (!interviewerConflicts.isEmpty()) {
                    throw new AppException(ErrorType.BAD_REQUEST,
                            "Người phỏng vấn " + interviewerId + " đang có lịch trùng thời gian");
                }
            }
        }

        if (type == InterviewType.OFFLINE && !StringUtils.hasText(request.getLocation())) {
            throw new AppException(ErrorType.BAD_REQUEST, "Phỏng vấn offline bắt buộc phải có địa điểm");
        }

        // Daily Room Model: reuse room by job + date for ONLINE interviews
        String meetingLink = null;
        if (type == InterviewType.ONLINE) {
            String jobId = application.getJob().getId();
            InterviewRoom room = roomService.findOrCreateRoom(jobId, date, null);
            meetingLink = room.getRoomCode();

            // Add candidate slot to room
            roomService.addParticipantSlot(
                    room.getId(),
                    application.getId(),
                    application.getCandidate().getId(),
                    scheduledAt,
                    endAt);
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

        if (application.getCurrentStage() != ApplicationStage.INTERVIEW) {
            ApplicationStage previousStage = application.getCurrentStage();
            application.setCurrentStage(ApplicationStage.INTERVIEW);
            application.setStageChangedAt(LocalDateTime.now());
            application.setCurrentStageNote("Đã lên lịch phỏng vấn.");
            application.addStageHistory(ApplicationStageHistory.builder()
                .fromStage(previousStage)
                .toStage(ApplicationStage.INTERVIEW)
                .note("Đã lên lịch phỏng vấn.")
                .changedBy("Hệ thống")
                .changedAt(LocalDateTime.now())
                .build());
            applicationRepository.save(application);
        }

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
                .orElseThrow(() -> new AppException(ErrorType.BAD_REQUEST, "Interview not found with id: " + id));
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
            return interviewRepository.findByCandidateIdAndInterviewStatusInAndHiddenFromCandidateFalse(candidateId,
                    statuses);
        }
        return interviewRepository.findByCandidateIdAndInterviewStatusInAndHiddenFromCandidateFalse(
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
            throw new AppException(ErrorType.BAD_REQUEST, "This interview does not belong to you");
        }
        if (interview.getInterviewStatus() != InterviewStatus.SCHEDULED) {
            throw new AppException(ErrorType.BAD_REQUEST, "Interview can only be confirmed when in SCHEDULED status");
        }
        interview.setInterviewStatus(InterviewStatus.CONFIRMED);
        return interviewRepository.save(interview);
    }

    @Override
    public Interview declineInterview(String id, String candidateId, String reason) {
        Interview interview = getInterviewById(id);
        if (!interview.getCandidate().getId().equals(candidateId)) {
            throw new AppException(ErrorType.BAD_REQUEST, "This interview does not belong to you");
        }
        if (interview.getInterviewStatus() != InterviewStatus.SCHEDULED
                && interview.getInterviewStatus() != InterviewStatus.CONFIRMED) {
            throw new AppException(ErrorType.BAD_REQUEST, "Interview cannot be declined in current status");
        }
        interview.setInterviewStatus(InterviewStatus.CANCELLED);
        interview.setCancellationReason(reason);
        return interviewRepository.save(interview);
    }

    @Override
    public Interview cancelInterview(String id, String companyId, String reason) {
        Interview interview = getInterviewById(id);
        if (!interview.getCompany().getId().equals(companyId)) {
            throw new AppException(ErrorType.BAD_REQUEST, "Interview does not belong to your company");
        }
        if (interview.getInterviewStatus() == InterviewStatus.COMPLETED
                || interview.getInterviewStatus() == InterviewStatus.CANCELLED) {
            throw new AppException(ErrorType.BAD_REQUEST, "Interview cannot be cancelled in current status");
        }
        boolean cancelledBeforeCandidateConfirmed = interview.getInterviewStatus() == InterviewStatus.SCHEDULED;
        interview.setInterviewStatus(InterviewStatus.CANCELLED);
        interview.setCancellationReason(reason);
        interview.setHiddenFromCandidate(cancelledBeforeCandidateConfirmed);
        return interviewRepository.save(interview);
    }

    @Override
    public Interview rescheduleInterview(String id, InterviewRescheduleRequest request, String companyId) {
        Interview original = getInterviewById(id);
        if (!original.getCompany().getId().equals(companyId)) {
            throw new AppException(ErrorType.BAD_REQUEST, "Lịch phỏng vấn không thuộc công ty của bạn");
        }

        // Cancel the original
        boolean cancelledBeforeCandidateConfirmed = original.getInterviewStatus() == InterviewStatus.SCHEDULED;
        original.setInterviewStatus(InterviewStatus.CANCELLED);
        original.setCancellationReason("Rescheduled");
        original.setHiddenFromCandidate(cancelledBeforeCandidateConfirmed);
        interviewRepository.save(original);

        // Create new interview
        LocalDate date = LocalDate.parse(request.getNewDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        LocalTime startTime = LocalTime.parse(request.getNewStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalDateTime scheduledAt = LocalDateTime.of(date, startTime);
        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorType.BAD_REQUEST, "Không thể dời lịch phỏng vấn vào thời điểm trong quá khứ");
        }
        LocalDateTime endAt = scheduledAt.plusMinutes(request.getDurationMinutes());

        String meetingLink = null;
        if (original.getType() == InterviewType.ONLINE) {
            InterviewRoom room = roomService.findOrCreateRoom(original.getJob().getId(), date, null);
            meetingLink = room.getRoomCode();
            roomService.addParticipantSlot(
                    room.getId(),
                    original.getApplication().getId(),
                    original.getCandidate().getId(),
                    scheduledAt,
                    endAt);
        }

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
            throw new AppException(ErrorType.BAD_REQUEST, "Interview does not belong to your company");
        }
        if (interview.getInterviewStatus() != InterviewStatus.CONFIRMED
                && interview.getInterviewStatus() != InterviewStatus.IN_PROGRESS) {
            throw new AppException(ErrorType.BAD_REQUEST,
                    "Interview can only be completed from CONFIRMED or IN_PROGRESS status");
        }

        validateCandidateJoinedForRoomInterview(interview,
                "Cannot complete interview because candidate has not joined this interview room yet");

        interview.setInterviewStatus(InterviewStatus.COMPLETED);
        return interviewRepository.save(interview);
    }

    @Override
    public InterviewFeedback addFeedback(String interviewId, InterviewFeedbackRequest request,
            String reviewerAccountId) {
        Interview interview = getInterviewById(interviewId);

        validateCandidateJoinedForRoomInterview(interview,
                "Cannot add feedback because candidate has not joined this interview room yet");

        if (interview.getInterviewStatus() != InterviewStatus.CONFIRMED
                && interview.getInterviewStatus() != InterviewStatus.IN_PROGRESS
                && interview.getInterviewStatus() != InterviewStatus.COMPLETED) {
            throw new AppException(ErrorType.BAD_REQUEST,
                    "Feedback can only be added to CONFIRMED, IN_PROGRESS, or COMPLETED interviews");
        }

        if (feedbackRepository.existsByInterviewIdAndReviewerId(interviewId, reviewerAccountId)) {
            throw new AppException(ErrorType.BAD_REQUEST, "You have already submitted feedback for this interview");
        }

        Account reviewer = accountRepository.findById(reviewerAccountId)
                .orElseThrow(() -> new AppException(ErrorType.BAD_REQUEST, "Reviewer account not found"));

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

        InterviewFeedback saved = feedbackRepository.save(feedback);

        // Auto-update application stage based on recommendation
        updateApplicationStageFromFeedback(interview, recommendation);

        return saved;
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
                .filter(app -> app.getCurrentStage() != null && SCHEDULABLE_STAGES.contains(app.getCurrentStage()))
                .filter(app -> !scheduledAppIds.contains(app.getId()))
                .toList();
    }

    @Override
    public List<InterviewTimeProposal> proposeAlternativeTimes(
            String interviewId, InterviewTimeProposalRequest request, String candidateId) {
        Interview interview = getInterviewById(interviewId);
        if (!interview.getCandidate().getId().equals(candidateId)) {
            throw new AppException(ErrorType.BAD_REQUEST, "This interview does not belong to you");
        }
        if (interview.getInterviewStatus() != InterviewStatus.SCHEDULED
                && interview.getInterviewStatus() != InterviewStatus.PENDING_RESCHEDULE) {
            throw new AppException(ErrorType.BAD_REQUEST, "Can only propose alternatives for SCHEDULED interviews");
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
            throw new AppException(ErrorType.BAD_REQUEST, "Interview does not belong to your company");
        }

        InterviewTimeProposal proposal = timeProposalRepository.findById(proposalId)
                .orElseThrow(() -> new AppException(ErrorType.BAD_REQUEST, "Proposal not found"));
        if (!proposal.getInterview().getId().equals(interviewId)) {
            throw new AppException(ErrorType.BAD_REQUEST, "Proposal does not belong to this interview");
        }
        if (proposal.getProposalStatus() != ProposalStatus.PENDING) {
            throw new AppException(ErrorType.BAD_REQUEST, "Proposal is not in PENDING status");
        }

        // Reject all other pending proposals
        List<InterviewTimeProposal> pendingProposals = timeProposalRepository
                .findByInterviewIdAndProposalStatus(interviewId, ProposalStatus.PENDING);
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

        // Daily Room Model: reuse room by job + date
        String meetingLink = null;
        if (interview.getType() == InterviewType.ONLINE) {
            InterviewRoom room = roomService.findOrCreateRoom(
                    interview.getJob().getId(), proposal.getProposedDate(), null);
            meetingLink = room.getRoomCode();

            roomService.addParticipantSlot(
                    room.getId(),
                    interview.getApplication().getId(),
                    interview.getCandidate().getId(),
                    scheduledAt,
                    endAt);
        }

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
            throw new AppException(ErrorType.BAD_REQUEST, "Interview does not belong to your company");
        }

        InterviewTimeProposal proposal = timeProposalRepository.findById(proposalId)
                .orElseThrow(() -> new AppException(ErrorType.BAD_REQUEST, "Proposal not found"));
        if (!proposal.getInterview().getId().equals(interviewId)) {
            throw new AppException(ErrorType.BAD_REQUEST, "Proposal does not belong to this interview");
        }

        proposal.setProposalStatus(ProposalStatus.REJECTED);
        timeProposalRepository.save(proposal);

        // If no more pending proposals, revert interview back to SCHEDULED
        List<InterviewTimeProposal> remaining = timeProposalRepository.findByInterviewIdAndProposalStatus(interviewId,
                ProposalStatus.PENDING);
        if (remaining.isEmpty()) {
            interview.setInterviewStatus(InterviewStatus.SCHEDULED);
            interviewRepository.save(interview);
        }

        log.info("HR rejected proposal {} for interview {}", proposalId, interviewId);
    }

    private Account findAccountByCandidate(Candidate candidate) {
        if (candidate == null)
            return null;
        return accountRepository.findByCandidateId(candidate.getId()).orElse(null);
    }

    private List<InterviewStatus> resolveStatusFilter(String filter) {
        return switch (filter.toUpperCase()) {
            case "UPCOMING" ->
                List.of(InterviewStatus.SCHEDULED, InterviewStatus.CONFIRMED, InterviewStatus.PENDING_RESCHEDULE);
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
        List<Interview> interviews = getInterviewsByRoomCode(roomCode);

        return interviews.stream()
            .filter(i -> ACTIVE_STATUSES.contains(i.getInterviewStatus()))
            .min(Comparator.comparing(Interview::getScheduledAt,
                Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(interviews.get(0));
    }

    @Transactional(readOnly = true)
    public List<Interview> getInterviewsByRoomCode(String roomCode) {
        List<Interview> interviews = interviewRepository.findByMeetingLinkOrderByScheduledAtAsc(roomCode);
        if (interviews.isEmpty()) {
            throw new AppException(ErrorType.BAD_REQUEST, "Interview room not found");
        }

        validateRoomAccessWindow(interviews);
        return interviews;
    }

    private void validateRoomAccessWindow(List<Interview> interviews) {
        LocalDateTime now = LocalDateTime.now();

        LocalDate roomDate = interviews.stream()
                .map(Interview::getScheduledAt)
                .filter(java.util.Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .min(LocalDate::compareTo)
                .orElse(now.toLocalDate());

        if (roomDate.isBefore(now.toLocalDate())) {
            throw new AppException(ErrorType.BAD_REQUEST,
                    "Phòng phỏng vấn của ngày trước đã hết hiệu lực truy cập");
        }

        LocalDateTime latestEnd = interviews.stream()
                .map(Interview::getEndAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        boolean noActiveSession = interviews.stream()
                .noneMatch(i -> ACTIVE_STATUSES.contains(i.getInterviewStatus()));

        if (latestEnd != null && now.isAfter(latestEnd) && noActiveSession) {
            throw new AppException(ErrorType.BAD_REQUEST,
                    "Phòng phỏng vấn đã kết thúc do quá thời gian cho phép");
        }
    }

    @Override
    public InterviewRecording saveRecording(String interviewId, InterviewRecordingRequest request, String recordedBy) {
        Interview interview = getInterviewById(interviewId);

        validateCandidateJoinedForRoomInterview(interview,
                "Cannot save recording because candidate has not joined this interview room yet");

        InterviewRecording recording = InterviewRecording.builder()
                .interview(interview)
                .fileKey(request.getFileKey())
                .fileSize(request.getFileSize())
                .durationSeconds(request.getDurationSeconds())
                .mimeType(request.getMimeType() != null ? request.getMimeType() : "video/webm")
                .recordingStatus(RecordingStatus.AVAILABLE)
                .recordedBy(recordedBy)
                .build();

        InterviewRecording saved = recordingRepository.save(recording);
        log.info("Recording saved for interview {} by {}", interviewId, recordedBy);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewRecording> getRecordings(String interviewId) {
        return recordingRepository.findByInterviewId(interviewId);
    }

    @Override
    public Interview startInterview(String id, String companyId) {
        Interview interview = getInterviewById(id);
        if (!interview.getCompany().getId().equals(companyId)) {
            throw new AppException(ErrorType.BAD_REQUEST, "Interview does not belong to your company");
        }
        if (interview.getInterviewStatus() != InterviewStatus.SCHEDULED
                && interview.getInterviewStatus() != InterviewStatus.CONFIRMED) {
            throw new AppException(ErrorType.BAD_REQUEST,
                    "Interview can only be started from SCHEDULED or CONFIRMED status");
        }
        interview.setInterviewStatus(InterviewStatus.IN_PROGRESS);
        return interviewRepository.save(interview);
    }

    private void validateCandidateJoinedForRoomInterview(Interview interview, String errorMessage) {
        if (interview == null || interview.getType() != InterviewType.ONLINE) {
            return;
        }

        if (!StringUtils.hasText(interview.getMeetingLink()) || interview.getApplication() == null) {
            return;
        }

        RoomParticipant participant = roomParticipantRepository
                .findByRoomCodeAndApplicationId(interview.getMeetingLink(), interview.getApplication().getId())
                .orElseThrow(() -> new AppException(ErrorType.BAD_REQUEST,
                        "Room participant slot not found for this interview"));

        if (participant.getJoinedAt() == null) {
            throw new AppException(ErrorType.BAD_REQUEST, errorMessage);
        }
    }

    private void updateApplicationStageFromFeedback(Interview interview, FeedbackRecommendation recommendation) {
        try {
            Application application = interview.getApplication();
            if (application == null)
                return;

            com.hcmute.careergraph.enums.application.ApplicationStage newStage = switch (recommendation) {
                case NEXT_ROUND -> com.hcmute.careergraph.enums.application.ApplicationStage.INTERVIEW;
                case EXTEND_OFFER -> com.hcmute.careergraph.enums.application.ApplicationStage.OFFER_EXTENDED;
                case REJECT -> com.hcmute.careergraph.enums.application.ApplicationStage.REJECTED;
                case HOLD -> com.hcmute.careergraph.enums.application.ApplicationStage.INTERVIEW;
            };

            if (newStage != null && newStage != application.getCurrentStage()) {
                application.setCurrentStage(newStage);
                application.setStageChangedAt(LocalDateTime.now());
                applicationRepository.save(application);
                log.info("Application {} stage updated to {} based on feedback recommendation {}",
                        application.getId(), newStage, recommendation);
            }
        } catch (Exception e) {
            log.warn("Failed to auto-update application stage from feedback: {}", e.getMessage());
        }
    }

    private void validateApplicationStageForScheduling(Application application) {
        ApplicationStage currentStage = application.getCurrentStage();
        if (currentStage == null) {
            throw new AppException(ErrorType.BAD_REQUEST, "Hồ sơ ứng viên chưa có trạng thái hợp lệ để lên lịch");
        }

        if (!SCHEDULABLE_STAGES.contains(currentStage)) {
            throw new AppException(
                    ErrorType.BAD_REQUEST,
                    String.format("Không thể lên lịch phỏng vấn khi hồ sơ đang ở trạng thái %s", currentStage));
        }
    }
}
