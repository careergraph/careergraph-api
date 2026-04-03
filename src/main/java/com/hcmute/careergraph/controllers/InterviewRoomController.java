package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.dtos.response.InterviewRoomResponse;
import com.hcmute.careergraph.persistence.models.InterviewRoom;
import com.hcmute.careergraph.persistence.models.RoomParticipant;
import com.hcmute.careergraph.services.InterviewRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("rooms")
@RequiredArgsConstructor
public class InterviewRoomController {

    private final InterviewRoomService roomService;
    private final SecurityUtils securityUtils;

    @GetMapping("/{roomCode}")
    public RestResponse<InterviewRoomResponse> getRoomByCode(@PathVariable String roomCode) {
        InterviewRoom room = roomService.getRoomByCode(roomCode);
        return RestResponse.<InterviewRoomResponse>builder()
                .status(HttpStatus.OK)
                .message("Room retrieved successfully")
                .data(toResponse(room))
                .build();
    }

    @PostMapping("/{roomCode}/open")
    public RestResponse<InterviewRoomResponse> openRoom(
            @PathVariable String roomCode,
            Authentication authentication) {

        String accountId = securityUtils.getCurrentAccount()
                .map(a -> a.getId())
                .orElseThrow(() -> new BadRequestException("Account not found"));

        InterviewRoom room = roomService.openRoom(roomCode, accountId);
        return RestResponse.<InterviewRoomResponse>builder()
                .status(HttpStatus.OK)
                .message("Room opened successfully")
                .data(toResponse(room))
                .build();
    }

    @PostMapping("/{roomCode}/close")
    public RestResponse<InterviewRoomResponse> closeRoom(
            @PathVariable String roomCode,
            Authentication authentication) {

        String accountId = securityUtils.getCurrentAccount()
                .map(a -> a.getId())
                .orElseThrow(() -> new BadRequestException("Account not found"));

        InterviewRoom room = roomService.closeRoom(roomCode, accountId);
        return RestResponse.<InterviewRoomResponse>builder()
                .status(HttpStatus.OK)
                .message("Room closing initiated")
                .data(toResponse(room))
                .build();
    }

    @GetMapping("/{roomCode}/participants")
    public RestResponse<List<InterviewRoomResponse.RoomParticipantResponse>> getParticipants(
            @PathVariable String roomCode) {

        InterviewRoom room = roomService.getRoomByCode(roomCode);
        List<RoomParticipant> participants = roomService.getRoomParticipants(room.getId());
        return RestResponse.<List<InterviewRoomResponse.RoomParticipantResponse>>builder()
                .status(HttpStatus.OK)
                .message("Participants retrieved successfully")
                .data(participants.stream().map(this::toParticipantResponse).toList())
                .build();
    }

    @PostMapping("/{roomCode}/participants/{candidateId}/admit")
    public RestResponse<InterviewRoomResponse.RoomParticipantResponse> admitParticipant(
            @PathVariable String roomCode,
            @PathVariable String candidateId) {

        InterviewRoom room = roomService.getRoomByCode(roomCode);
        RoomParticipant participant = roomService.admitParticipant(room.getId(), candidateId);
        return RestResponse.<InterviewRoomResponse.RoomParticipantResponse>builder()
                .status(HttpStatus.OK)
                .message("Participant admitted")
                .data(toParticipantResponse(participant))
                .build();
    }

    @PostMapping("/{roomCode}/participants/{candidateId}/remove")
    public RestResponse<InterviewRoomResponse.RoomParticipantResponse> removeParticipant(
            @PathVariable String roomCode,
            @PathVariable String candidateId) {

        InterviewRoom room = roomService.getRoomByCode(roomCode);
        RoomParticipant participant = roomService.removeParticipant(room.getId(), candidateId);
        return RestResponse.<InterviewRoomResponse.RoomParticipantResponse>builder()
                .status(HttpStatus.OK)
                .message("Participant removed")
                .data(toParticipantResponse(participant))
                .build();
    }

    @PostMapping("/{roomCode}/participants/{candidateId}/complete")
    public RestResponse<InterviewRoomResponse.RoomParticipantResponse> completeParticipant(
            @PathVariable String roomCode,
            @PathVariable String candidateId) {

        InterviewRoom room = roomService.getRoomByCode(roomCode);
        RoomParticipant participant = roomService.completeParticipant(room.getId(), candidateId);
        return RestResponse.<InterviewRoomResponse.RoomParticipantResponse>builder()
                .status(HttpStatus.OK)
                .message("Participant completed")
                .data(toParticipantResponse(participant))
                .build();
    }

    // ── Mapping helpers ─────────────────────────────────

    private InterviewRoomResponse toResponse(InterviewRoom room) {
        List<InterviewRoomResponse.RoomParticipantResponse> participants = Optional.ofNullable(room.getParticipants())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toParticipantResponse)
                .toList();

        return InterviewRoomResponse.builder()
                .id(room.getId())
                .jobId(room.getJob() != null ? room.getJob().getId() : null)
                .jobTitle(room.getJob() != null ? room.getJob().getTitle() : null)
                .interviewDate(room.getInterviewDate())
                .roomCode(room.getRoomCode())
                .roomStatus(room.getRoomStatus())
                .hostId(room.getHost() != null ? room.getHost().getId() : null)
                .hostName(room.getHost() != null ? room.getHost().getEmail() : null)
                .openAt(room.getOpenAt())
                .closedAt(room.getClosedAt())
                .maxDuration(room.getMaxDuration())
                .participants(participants)
                .createdDate(room.getCreatedDate())
                .build();
    }

    private InterviewRoomResponse.RoomParticipantResponse toParticipantResponse(RoomParticipant p) {
        String candidateName = Optional.ofNullable(p.getCandidate())
                .map(c -> {
                    String first = c.getFirstName() != null ? c.getFirstName() : "";
                    String last = c.getLastName() != null ? c.getLastName() : "";
                    return (first + " " + last).trim();
                })
                .orElse(null);

        String candidateAvatar = Optional.ofNullable(p.getCandidate())
                .map(c -> c.getAvatar())
                .orElse(null);

        return InterviewRoomResponse.RoomParticipantResponse.builder()
                .id(p.getId())
                .applicationId(p.getApplication() != null ? p.getApplication().getId() : null)
                .candidateId(p.getCandidate() != null ? p.getCandidate().getId() : null)
                .candidateName(candidateName)
                .candidateAvatar(candidateAvatar)
                .slotStart(p.getSlotStart())
                .slotEnd(p.getSlotEnd())
                .admitStatus(p.getAdmitStatus())
                .knockCount(p.getKnockCount())
                .joinedAt(p.getJoinedAt())
                .leftAt(p.getLeftAt())
                .hrNote(p.getHrNote())
                .build();
    }
}
