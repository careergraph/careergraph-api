package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.models.InterviewRoom;
import com.hcmute.careergraph.persistence.models.RoomParticipant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface InterviewRoomService {

    /**
     * Find or create a room for the given job + date combination (Daily Room Model).
     */
    InterviewRoom findOrCreateRoom(String jobId, LocalDate interviewDate, String hostAccountId);

    /**
     * Add a candidate slot to the room.
     */
    RoomParticipant addParticipantSlot(String roomId, String applicationId, String candidateId,
                                       LocalDateTime slotStart, LocalDateTime slotEnd);

    /**
     * Get room by room code.
     */
    InterviewRoom getRoomByCode(String roomCode);

    /**
     * Get room details by ID.
     */
    InterviewRoom getRoomById(String roomId);

    /**
     * HR opens the room (transition to ACTIVE).
     */
    InterviewRoom openRoom(String roomCode, String hostAccountId);

    /**
     * HR starts closing the room (transition to CLOSING with 5-min grace).
     */
    InterviewRoom closeRoom(String roomCode, String hostAccountId);

    /**
     * Mark a participant as admitted by HR.
     */
    RoomParticipant admitParticipant(String roomId, String candidateId);

    /**
     * Mark a participant as removed/kicked by HR.
     */
    RoomParticipant removeParticipant(String roomId, String candidateId);

    /**
     * Mark a participant as voluntarily left the active room session.
     */
    RoomParticipant leaveParticipant(String roomId, String candidateId);

    /**
     * Mark a participant slot as completed after interview ends.
     */
    RoomParticipant completeParticipant(String roomId, String candidateId);

    /**
     * Record a knock attempt by a candidate.
     */
    RoomParticipant recordKnock(String roomId, String candidateId);

    /**
     * Get all participants for a room.
     */
    List<RoomParticipant> getRoomParticipants(String roomId);
}
