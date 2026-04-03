package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.mapper.InterviewMapper;
import com.hcmute.careergraph.persistence.dtos.request.InterviewFeedbackRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewRecordingRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewRescheduleRequest;
import com.hcmute.careergraph.persistence.dtos.request.InterviewTimeProposalRequest;
import com.hcmute.careergraph.persistence.dtos.response.InterviewFeedbackResponse;
import com.hcmute.careergraph.persistence.dtos.response.InterviewRecordingResponse;
import com.hcmute.careergraph.persistence.dtos.response.InterviewResponse;
import com.hcmute.careergraph.persistence.dtos.response.InterviewTimeProposalResponse;
import com.hcmute.careergraph.persistence.models.Interview;
import com.hcmute.careergraph.persistence.models.InterviewFeedback;
import com.hcmute.careergraph.persistence.models.InterviewRecording;
import com.hcmute.careergraph.persistence.models.InterviewTimeProposal;
import com.hcmute.careergraph.services.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final InterviewMapper interviewMapper;
    private final SecurityUtils securityUtils;

    // ==================== HR Endpoints ====================

    @PostMapping
    public RestResponse<InterviewResponse> createInterview(
            @Valid @RequestBody InterviewRequest request,
            Authentication authentication) {

        String companyId = securityUtils.extractCompanyId(authentication);
        if (!StringUtils.hasText(companyId)) {
            throw new BadRequestException("Company ID is required");
        }

        Interview interview = interviewService.createInterview(request, companyId);
        return RestResponse.<InterviewResponse>builder()
                .status(HttpStatus.CREATED)
                .message("Interview scheduled successfully")
                .data(interviewMapper.toResponse(interview, false))
                .build();
    }

    @GetMapping("/{id}")
    public RestResponse<InterviewResponse> getInterviewById(@PathVariable String id) {
        Interview interview = interviewService.getInterviewById(id);
        return RestResponse.<InterviewResponse>builder()
                .status(HttpStatus.OK)
                .message("Interview retrieved successfully")
                .data(interviewMapper.toResponse(interview, true))
                .build();
    }

    @GetMapping("/room/{roomCode}")
    public RestResponse<InterviewResponse> getInterviewByRoomCode(@PathVariable String roomCode) {
        Interview interview = interviewService.getInterviewByRoomCode(roomCode);
        return RestResponse.<InterviewResponse>builder()
                .status(HttpStatus.OK)
                .message("Interview room info retrieved successfully")
                .data(interviewMapper.toResponse(interview, false))
                .build();
    }

    @GetMapping
    public RestResponse<Page<InterviewResponse>> getInterviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        String companyId = securityUtils.extractCompanyId(authentication);
        if (!StringUtils.hasText(companyId)) {
            throw new BadRequestException("Company ID is required");
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt"));
        Page<Interview> interviews = interviewService.getInterviewsByCompany(companyId, status, pageable);
        Page<InterviewResponse> responsePage = interviews.map(i -> interviewMapper.toResponse(i, false));

        return RestResponse.<Page<InterviewResponse>>builder()
                .status(HttpStatus.OK)
                .message("Interviews retrieved successfully")
                .data(responsePage)
                .build();
    }

    @GetMapping("/calendar")
    public RestResponse<List<InterviewResponse>> getCalendarEvents(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Authentication authentication) {

        String companyId = securityUtils.extractCompanyId(authentication);
        if (!StringUtils.hasText(companyId)) {
            throw new BadRequestException("Company ID is required");
        }

        LocalDateTime now = LocalDateTime.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        YearMonth ym = YearMonth.of(y, m);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        List<Interview> events = interviewService.getCalendarEvents(companyId, start, end);
        return RestResponse.<List<InterviewResponse>>builder()
                .status(HttpStatus.OK)
                .message("Calendar events retrieved successfully")
                .data(interviewMapper.toResponseList(events, false))
                .build();
    }

    @PostMapping("/{id}/cancel")
    public RestResponse<InterviewResponse> cancelInterview(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {

        String companyId = securityUtils.extractCompanyId(authentication);
        if (!StringUtils.hasText(companyId)) {
            throw new BadRequestException("Company ID is required");
        }

        String reason = body != null ? body.get("reason") : null;
        Interview interview = interviewService.cancelInterview(id, companyId, reason);
        return RestResponse.<InterviewResponse>builder()
                .status(HttpStatus.OK)
                .message("Interview cancelled")
                .data(interviewMapper.toResponse(interview, false))
                .build();
    }

    @PostMapping("/{id}/reschedule")
    public RestResponse<InterviewResponse> rescheduleInterview(
            @PathVariable String id,
            @Valid @RequestBody InterviewRescheduleRequest request,
            Authentication authentication) {

        String companyId = securityUtils.extractCompanyId(authentication);
        if (!StringUtils.hasText(companyId)) {
            throw new BadRequestException("Company ID is required");
        }

        Interview interview = interviewService.rescheduleInterview(id, request, companyId);
        return RestResponse.<InterviewResponse>builder()
                .status(HttpStatus.CREATED)
                .message("Interview rescheduled")
                .data(interviewMapper.toResponse(interview, false))
                .build();
    }

    @PostMapping("/{id}/complete")
    public RestResponse<InterviewResponse> completeInterview(
            @PathVariable String id,
            Authentication authentication) {

        String companyId = securityUtils.extractCompanyId(authentication);
        if (!StringUtils.hasText(companyId)) {
            throw new BadRequestException("Company ID is required");
        }

        Interview interview = interviewService.completeInterview(id, companyId);
        return RestResponse.<InterviewResponse>builder()
                .status(HttpStatus.OK)
                .message("Interview completed")
                .data(interviewMapper.toResponse(interview, false))
                .build();
    }

    @PostMapping("/{id}/start")
    public RestResponse<InterviewResponse> startInterview(
            @PathVariable String id,
            Authentication authentication) {

        String companyId = securityUtils.extractCompanyId(authentication);
        if (!StringUtils.hasText(companyId)) {
            throw new BadRequestException("Company ID is required");
        }

        Interview interview = interviewService.startInterview(id, companyId);
        return RestResponse.<InterviewResponse>builder()
                .status(HttpStatus.OK)
                .message("Interview started")
                .data(interviewMapper.toResponse(interview, false))
                .build();
    }

    // ==================== Recording Endpoints ====================

    @PostMapping("/{id}/recordings")
    public RestResponse<InterviewRecordingResponse> saveRecording(
            @PathVariable String id,
            @Valid @RequestBody InterviewRecordingRequest request,
            Authentication authentication) {

        String accountId = securityUtils.getCurrentAccount()
                .map(a -> a.getId())
                .orElseThrow(() -> new BadRequestException("Account not found"));

        InterviewRecording recording = interviewService.saveRecording(id, request, accountId);
        return RestResponse.<InterviewRecordingResponse>builder()
                .status(HttpStatus.CREATED)
                .message("Recording saved successfully")
                .data(interviewMapper.toRecordingResponse(recording))
                .build();
    }

    @GetMapping("/{id}/recordings")
    public RestResponse<List<InterviewRecordingResponse>> getRecordings(@PathVariable String id) {
        List<InterviewRecording> recordings = interviewService.getRecordings(id);
        return RestResponse.<List<InterviewRecordingResponse>>builder()
                .status(HttpStatus.OK)
                .message("Recordings retrieved successfully")
                .data(recordings.stream().map(interviewMapper::toRecordingResponse).toList())
                .build();
    }

    @PostMapping("/{id}/feedback")
    public RestResponse<InterviewFeedbackResponse> addFeedback(
            @PathVariable String id,
            @Valid @RequestBody InterviewFeedbackRequest request,
            Authentication authentication) {

        String accountId = securityUtils.getCurrentAccount()
                .map(a -> a.getId())
                .orElseThrow(() -> new BadRequestException("Account not found"));

        InterviewFeedback feedback = interviewService.addFeedback(id, request, accountId);
        return RestResponse.<InterviewFeedbackResponse>builder()
                .status(HttpStatus.CREATED)
                .message("Feedback submitted successfully")
                .data(interviewMapper.toFeedbackResponse(feedback))
                .build();
    }

    @GetMapping("/{id}/feedback")
    public RestResponse<List<InterviewFeedbackResponse>> getFeedback(@PathVariable String id) {
        List<InterviewFeedback> feedbacks = interviewService.getFeedback(id);
        List<InterviewFeedbackResponse> responses = feedbacks.stream()
                .map(interviewMapper::toFeedbackResponse)
                .toList();
        return RestResponse.<List<InterviewFeedbackResponse>>builder()
                .status(HttpStatus.OK)
                .message("Feedback retrieved successfully")
                .data(responses)
                .build();
    }

    // ==================== Candidate Endpoints ====================

    @GetMapping("/me")
    public RestResponse<List<InterviewResponse>> getMyInterviews(
            @RequestParam(required = false) String status,
            Authentication authentication) {

        String candidateId = securityUtils.extractCandidateId(authentication);
        if (!StringUtils.hasText(candidateId)) {
            throw new BadRequestException("Candidate ID is required");
        }

        List<Interview> interviews = interviewService.getInterviewsByCandidate(candidateId, status);
        return RestResponse.<List<InterviewResponse>>builder()
                .status(HttpStatus.OK)
                .message("My interviews retrieved successfully")
                .data(interviewMapper.toResponseList(interviews, false))
                .build();
    }

    @GetMapping("/me/upcoming")
    public RestResponse<List<InterviewResponse>> getUpcomingInterviews(Authentication authentication) {

        String candidateId = securityUtils.extractCandidateId(authentication);
        if (!StringUtils.hasText(candidateId)) {
            throw new BadRequestException("Candidate ID is required");
        }

        List<Interview> interviews = interviewService.getUpcomingByCandidateId(candidateId, 5);
        return RestResponse.<List<InterviewResponse>>builder()
                .status(HttpStatus.OK)
                .message("Upcoming interviews retrieved successfully")
                .data(interviewMapper.toResponseList(interviews, false))
                .build();
    }

    @PostMapping("/{id}/confirm")
    public RestResponse<InterviewResponse> confirmInterview(
            @PathVariable String id,
            Authentication authentication) {

        String candidateId = securityUtils.extractCandidateId(authentication);
        if (!StringUtils.hasText(candidateId)) {
            throw new BadRequestException("Candidate ID is required");
        }

        Interview interview = interviewService.confirmInterview(id, candidateId);
        return RestResponse.<InterviewResponse>builder()
                .status(HttpStatus.OK)
                .message("Interview confirmed")
                .data(interviewMapper.toResponse(interview, false))
                .build();
    }

    @PostMapping("/{id}/decline")
    public RestResponse<InterviewResponse> declineInterview(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {

        String candidateId = securityUtils.extractCandidateId(authentication);
        if (!StringUtils.hasText(candidateId)) {
            throw new BadRequestException("Candidate ID is required");
        }

        String reason = body != null ? body.get("reason") : null;
        Interview interview = interviewService.declineInterview(id, candidateId, reason);
        return RestResponse.<InterviewResponse>builder()
                .status(HttpStatus.OK)
                .message("Interview declined")
                .data(interviewMapper.toResponse(interview, false))
                .build();
    }

    // ==================== Time Proposal Endpoints ====================

    @PostMapping("/{id}/propose")
    public RestResponse<List<InterviewTimeProposalResponse>> proposeAlternativeTimes(
            @PathVariable String id,
            @Valid @RequestBody InterviewTimeProposalRequest request,
            Authentication authentication) {

        String candidateId = securityUtils.extractCandidateId(authentication);
        if (!StringUtils.hasText(candidateId)) {
            throw new BadRequestException("Candidate ID is required");
        }

        List<InterviewTimeProposal> proposals = interviewService.proposeAlternativeTimes(id, request, candidateId);
        return RestResponse.<List<InterviewTimeProposalResponse>>builder()
                .status(HttpStatus.CREATED)
                .message("Time proposals submitted successfully")
                .data(interviewMapper.toProposalResponseList(proposals))
                .build();
    }

    @GetMapping("/{id}/proposals")
    public RestResponse<List<InterviewTimeProposalResponse>> getProposals(@PathVariable String id) {
        List<InterviewTimeProposal> proposals = interviewService.getProposals(id);
        return RestResponse.<List<InterviewTimeProposalResponse>>builder()
                .status(HttpStatus.OK)
                .message("Proposals retrieved successfully")
                .data(interviewMapper.toProposalResponseList(proposals))
                .build();
    }

    @PostMapping("/{id}/proposals/{proposalId}/accept")
    public RestResponse<InterviewResponse> acceptProposal(
            @PathVariable String id,
            @PathVariable String proposalId,
            Authentication authentication) {

        String companyId = securityUtils.extractCompanyId(authentication);
        if (!StringUtils.hasText(companyId)) {
            throw new BadRequestException("Company ID is required");
        }

        Interview interview = interviewService.acceptProposal(id, proposalId, companyId);
        return RestResponse.<InterviewResponse>builder()
                .status(HttpStatus.OK)
                .message("Proposal accepted and interview rescheduled")
                .data(interviewMapper.toResponse(interview, false))
                .build();
    }

    @PostMapping("/{id}/proposals/{proposalId}/reject")
    public RestResponse<Void> rejectProposal(
            @PathVariable String id,
            @PathVariable String proposalId,
            Authentication authentication) {

        String companyId = securityUtils.extractCompanyId(authentication);
        if (!StringUtils.hasText(companyId)) {
            throw new BadRequestException("Company ID is required");
        }

        interviewService.rejectProposal(id, proposalId, companyId);
        return RestResponse.<Void>builder()
                .status(HttpStatus.OK)
                .message("Proposal rejected")
                .build();
    }

    @GetMapping("/application/{applicationId}")
    public RestResponse<List<InterviewResponse>> getInterviewsByApplication(
            @PathVariable String applicationId) {
        List<Interview> interviews = interviewService.getInterviewsByApplication(applicationId);
        return RestResponse.<List<InterviewResponse>>builder()
                .status(HttpStatus.OK)
                .message("Interviews by application retrieved successfully")
                .data(interviewMapper.toResponseList(interviews, false))
                .build();
    }

    @GetMapping("/job/{jobId}/unscheduled")
    public RestResponse<List<Map<String, Object>>> getUnscheduledApplications(
            @PathVariable String jobId,
            Authentication authentication) {

        String companyId = securityUtils.extractCompanyId(authentication);
        if (!StringUtils.hasText(companyId)) {
            throw new BadRequestException("Company ID is required");
        }

        var applications = interviewService.getUnscheduledApplicationsByJob(jobId, companyId);
        List<Map<String, Object>> result = applications.stream().map(app -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("applicationId", app.getId());
            map.put("candidateId", app.getCandidate() != null ? app.getCandidate().getId() : null);
            String firstName = app.getCandidate() != null ? app.getCandidate().getFirstName() : "";
            String lastName = app.getCandidate() != null ? app.getCandidate().getLastName() : "";
            String name = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            map.put("candidateName", name.isEmpty() ? "Unknown" : name);
            map.put("jobTitle", app.getJob() != null ? app.getJob().getTitle() : null);
            map.put("currentStage", app.getCurrentStage() != null ? app.getCurrentStage().name() : null);
            map.put("appliedDate", app.getAppliedDate());
            return map;
        }).toList();

        return RestResponse.<List<Map<String, Object>>>builder()
                .status(HttpStatus.OK)
                .message("Unscheduled applications retrieved")
                .data(result)
                .build();
    }
}
