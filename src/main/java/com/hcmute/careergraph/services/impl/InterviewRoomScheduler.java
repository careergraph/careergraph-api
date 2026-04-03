package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.interview.*;
import com.hcmute.careergraph.persistence.models.InterviewRoom;
import com.hcmute.careergraph.persistence.models.RoomParticipant;
import com.hcmute.careergraph.repositories.InterviewRepository;
import com.hcmute.careergraph.repositories.InterviewRoomRepository;
import com.hcmute.careergraph.repositories.RoomParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled tasks for interview room lifecycle management.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewRoomScheduler {

    private final InterviewRoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final InterviewRepository interviewRepository;

    /**
     * Daily at 06:00 — expire rooms from yesterday that were never opened.
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void expireStaleRooms() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<RoomStatus> nonTerminal = List.of(RoomStatus.SCHEDULED, RoomStatus.WAITING);

        List<InterviewRoom> staleRooms = roomRepository.findExpiredRooms(yesterday, nonTerminal);
        for (InterviewRoom room : staleRooms) {
            room.setRoomStatus(RoomStatus.EXPIRED);
            roomRepository.save(room);
            log.info("[scheduler] Room {} expired (date={}, was={})",
                    room.getRoomCode(), room.getInterviewDate(), RoomStatus.SCHEDULED);
        }

        if (!staleRooms.isEmpty()) {
            log.info("[scheduler] Expired {} stale rooms", staleRooms.size());
        }
    }

    /**
     * Every 5 minutes — close rooms that have been in CLOSING for > 5 minutes.
     */
    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void finalizeClosingRooms() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<InterviewRoom> closingRooms = roomRepository.findStaleActiveRooms(RoomStatus.CLOSING, fiveMinutesAgo);

        for (InterviewRoom room : closingRooms) {
            room.setRoomStatus(RoomStatus.ENDED);
            if (room.getClosedAt() == null) {
                room.setClosedAt(LocalDateTime.now());
            }
            roomRepository.save(room);
            log.info("[scheduler] Room {} ended (was CLOSING since {})",
                    room.getRoomCode(), room.getClosedAt());
        }
    }

    /**
     * Daily at 08:00 — detect no-show candidates from yesterday.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void detectNoShows() {
        LocalDateTime cutoff = LocalDate.now().minusDays(1).atTime(23, 59, 59);
        List<AdmitStatus> excludeStatuses = List.of(
                AdmitStatus.ADMITTED, AdmitStatus.COMPLETED, AdmitStatus.REMOVED);

        List<RoomParticipant> noShows = participantRepository.findNoShows(cutoff, excludeStatuses);

        for (RoomParticipant participant : noShows) {
            participant.setAdmitStatus(AdmitStatus.COMPLETED);
            participantRepository.save(participant);

            // Update corresponding interview to NO_SHOW
            String applicationId = participant.getApplication() != null
                    ? participant.getApplication().getId() : null;
            if (applicationId != null) {
                interviewRepository.findByApplicationId(applicationId).stream()
                        .filter(i -> i.getInterviewStatus() == InterviewStatus.SCHEDULED
                                || i.getInterviewStatus() == InterviewStatus.CONFIRMED)
                        .filter(i -> i.getScheduledAt().isBefore(cutoff))
                        .forEach(i -> {
                            i.setInterviewStatus(InterviewStatus.NO_SHOW);
                            interviewRepository.save(i);
                            log.info("[scheduler] Interview {} marked NO_SHOW for candidate slot",
                                    i.getId());
                        });
            }
        }

        if (!noShows.isEmpty()) {
            log.info("[scheduler] Detected {} no-shows", noShows.size());
        }
    }
}
