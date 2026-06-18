package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.enums.interview.InterviewStatus;
import com.hcmute.careergraph.enums.interview.InterviewType;
import com.hcmute.careergraph.persistence.dtos.request.InterviewFeedbackRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewRequest;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.persistence.models.Application;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.Interview;
import com.hcmute.careergraph.persistence.models.InterviewFeedback;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.persistence.models.RoomParticipant;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.repositories.ApplicationRepository;
import com.hcmute.careergraph.repositories.InterviewFeedbackRepository;
import com.hcmute.careergraph.repositories.InterviewParticipantRepository;
import com.hcmute.careergraph.repositories.InterviewRecordingRepository;
import com.hcmute.careergraph.repositories.InterviewRepository;
import com.hcmute.careergraph.repositories.InterviewTimeProposalRepository;
import com.hcmute.careergraph.repositories.RoomParticipantRepository;
import com.hcmute.careergraph.services.CompanyRecruitmentStageService;
import com.hcmute.careergraph.services.InterviewRoomService;
import com.hcmute.careergraph.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private InterviewParticipantRepository participantRepository;
    @Mock
    private RoomParticipantRepository roomParticipantRepository;
    @Mock
    private InterviewFeedbackRepository feedbackRepository;
    @Mock
    private InterviewRecordingRepository recordingRepository;
    @Mock
    private InterviewTimeProposalRepository timeProposalRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private InterviewRoomService roomService;
    @Mock
    private CompanyRecruitmentStageService companyRecruitmentStageService;
    @Mock
    private NotificationService notificationService;

    private InterviewServiceImpl interviewService;

    @BeforeEach
    void setUp() {
        interviewService = new InterviewServiceImpl(
                interviewRepository,
                participantRepository,
                roomParticipantRepository,
                feedbackRepository,
                recordingRepository,
                timeProposalRepository,
                applicationRepository,
                accountRepository,
                roomService,
                companyRecruitmentStageService,
                notificationService);
    }

    @Test
    void completeInterview_allowsOnlineInterviewBeforeScheduledStartWhenCandidateJoinedRoom() {
        Interview interview = createInterview(
                "interview-1",
                "company-1",
                LocalDateTime.now().plusMinutes(10),
                InterviewType.ONLINE,
                InterviewStatus.CONFIRMED);
        interview.setMeetingLink("room-1");

        RoomParticipant roomParticipant = new RoomParticipant();
        roomParticipant.setJoinedAt(LocalDateTime.now());

        when(interviewRepository.findById("interview-1")).thenReturn(Optional.of(interview));
        when(roomParticipantRepository.findByRoomCodeAndApplicationId("room-1", "application-1"))
                .thenReturn(Optional.of(roomParticipant));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Interview saved = interviewService.completeInterview("interview-1", "company-1");

        assertThat(saved.getInterviewStatus()).isEqualTo(InterviewStatus.COMPLETED);
        verify(interviewRepository).save(interview);
    }

    @Test
    void addFeedback_allowsOnlineInterviewBeforeScheduledStartWhenCandidateJoinedRoom() {
        Interview interview = createInterview(
                "interview-2",
                "company-1",
                LocalDateTime.now().plusMinutes(12),
                InterviewType.ONLINE,
                InterviewStatus.IN_PROGRESS);
        interview.setMeetingLink("room-2");

        RoomParticipant roomParticipant = new RoomParticipant();
        roomParticipant.setJoinedAt(LocalDateTime.now());

        Account reviewer = new Account();
        reviewer.setId("reviewer-1");

        InterviewFeedbackRequest request = new InterviewFeedbackRequest();
        request.setOverallRating(4);
        request.setRecommendation("HOLD");
        request.setNotes("Joined early and completed the interview.");

        when(interviewRepository.findById("interview-2")).thenReturn(Optional.of(interview));
        when(roomParticipantRepository.findByRoomCodeAndApplicationId("room-2", "application-1"))
                .thenReturn(Optional.of(roomParticipant));
        when(feedbackRepository.existsByInterviewIdAndReviewerId("interview-2", "reviewer-1")).thenReturn(false);
        when(accountRepository.findById("reviewer-1")).thenReturn(Optional.of(reviewer));
        when(feedbackRepository.save(any(InterviewFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewFeedback saved = interviewService.addFeedback("interview-2", request, "reviewer-1");

        assertThat(saved.getReviewer()).isSameAs(reviewer);
        assertThat(saved.getInterview()).isSameAs(interview);
        assertThat(saved.getNotes()).isEqualTo("Joined early and completed the interview.");
    }

    @Test
    void createInterview_allowsCandidateOverlapWithoutAnyCandidateConflictCheck() {
        Application application = createApplication("application-1", "company-1", "candidate-1", "job-1");
        InterviewRequest request = createInterviewRequest("application-1", "2026-06-20", "09:00");

        when(applicationRepository.findById("application-1")).thenReturn(Optional.of(application));
        when(interviewRepository.findByApplicationId("application-1")).thenReturn(List.of());
        when(interviewRepository.findActiveByApplicationAndJob(eq("application-1"), eq("job-1"), any())).thenReturn(List.of());
        when(accountRepository.findByCandidateId("candidate-1")).thenReturn(Optional.empty());
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> {
            Interview saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("created-interview-1");
            }
            return saved;
        });
        when(interviewRepository.findById("created-interview-1")).thenReturn(Optional.empty());

        Interview saved = interviewService.createInterview(request, "company-1");

        assertThat(saved).isNotNull();
        assertThat(saved.getCandidate().getId()).isEqualTo("candidate-1");
    }

    private InterviewRequest createInterviewRequest(String applicationId, String date, String startTime) {
        InterviewRequest request = new InterviewRequest();
        request.setApplicationId(applicationId);
        request.setDate(date);
        request.setStartTime(startTime);
        request.setDurationMinutes(60);
        request.setType("OFFLINE");
        request.setLocation("Meeting room A");
        request.setNotifyCandidate(false);
        request.setConfirmOverwrite(false);
        request.setRoundNumber(1);
        return request;
    }

    private Application createApplication(String applicationId, String companyId, String candidateId, String jobId) {
        Company company = new Company();
        company.setId(companyId);
        company.setName("CareerGraph");

        Candidate candidate = new Candidate();
        candidate.setId(candidateId);
        candidate.setFirstName("Nguyen");
        candidate.setLastName("Tester");

        Job job = new Job();
        job.setId(jobId);
        job.setTitle("Backend Engineer");
        job.setCompany(company);

        Application application = new Application();
        application.setId(applicationId);
        application.setCandidate(candidate);
        application.setJob(job);
        application.setCurrentStage(ApplicationStage.INTERVIEW);
        return application;
    }

    private Interview createInterview(
            String interviewId,
            String companyId,
            LocalDateTime scheduledAt,
            InterviewType type,
            InterviewStatus status) {
        Company company = new Company();
        company.setId(companyId);
        company.setName("CareerGraph");

        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");
        candidate.setFirstName("Nguyen");
        candidate.setLastName("Tester");

        Job job = new Job();
        job.setId("job-1");
        job.setTitle("Backend Engineer");
        job.setCompany(company);

        Application application = new Application();
        application.setId("application-1");
        application.setCandidate(candidate);
        application.setJob(job);
        application.setCurrentStage(ApplicationStage.INTERVIEW);

        Interview interview = new Interview();
        interview.setId(interviewId);
        interview.setApplication(application);
        interview.setCompany(company);
        interview.setJob(job);
        interview.setCandidate(candidate);
        interview.setScheduledAt(scheduledAt);
        interview.setEndAt(scheduledAt.plusMinutes(60));
        interview.setDurationMinutes(60);
        interview.setType(type);
        interview.setInterviewStatus(status);
        interview.setNotes("[ROUND:1]");
        return interview;
    }
}
