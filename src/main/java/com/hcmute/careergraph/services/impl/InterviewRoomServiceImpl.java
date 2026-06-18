package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.interview.AdmitStatus;
import com.hcmute.careergraph.enums.interview.InterviewStatus;
import com.hcmute.careergraph.enums.interview.RoomStatus;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.persistence.models.*;
import com.hcmute.careergraph.repositories.*;
import com.hcmute.careergraph.services.InterviewRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InterviewRoomServiceImpl implements InterviewRoomService {

    private final InterviewRoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final AccountRepository accountRepository;
    private final InterviewRepository interviewRepository;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public InterviewRoom findOrCreateRoom(String jobId, LocalDate interviewDate, String hostAccountId) {
        return roomRepository.findByJobIdAndInterviewDate(jobId, interviewDate)
                .orElseGet(() -> {
                    Job job = jobRepository.findById(jobId)
                            .orElseThrow(() -> new BadRequestException("Job not found: " + jobId));

                    Account host = hostAccountId != null
                            ? accountRepository.findById(hostAccountId).orElse(null)
                            : null;

                    String roomCode = generateRoomCode(interviewDate);

                    InterviewRoom room = InterviewRoom.builder()
                            .job(job)
                            .interviewDate(interviewDate)
                            .roomCode(roomCode)
                            .roomStatus(RoomStatus.SCHEDULED)
                            .host(host)
                            .maxDuration(480)
                            .build();

                    InterviewRoom saved = roomRepository.save(room);
                    log.info("Created interview room {} for job {} on {}",
                            saved.getRoomCode(), jobId, interviewDate);
                    return saved;
                });
    }

    @Override
    public RoomParticipant addParticipantSlot(String roomId, String applicationId, String candidateId,
            LocalDateTime slotStart, LocalDateTime slotEnd) {
        InterviewRoom room = getRoomById(roomId);
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException("Application not found"));
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new BadRequestException("Candidate not found"));

        RoomParticipant existing = participantRepository.findByRoomIdAndApplicationId(roomId, applicationId)
                .orElse(null);

        if (existing != null) {
            if (existing.getAdmitStatus() == AdmitStatus.ADMITTED
                    || existing.getAdmitStatus() == AdmitStatus.COMPLETED) {
                throw new BadRequestException("Candidate already has an active slot in this room");
            }

            existing.setCandidate(candidate);
            existing.setApplication(application);
            existing.setSlotStart(slotStart);
            existing.setSlotEnd(slotEnd);
            existing.setAdmitStatus(AdmitStatus.PENDING);
            existing.setKnockCount(0);
            existing.setLastKnockAt(null);
            existing.setJoinedAt(null);
            existing.setLeftAt(null);
            existing.setJoinToken(null);
            existing.setSessionToken(null);
            existing.setHrNote(null);

            RoomParticipant saved = participantRepository.save(existing);
            log.info("Updated participant slot for candidate {} in room {} ({} - {})",
                    candidateId, room.getRoomCode(), slotStart, slotEnd);
            return saved;
        }

        RoomParticipant participant = RoomParticipant.builder()
                .room(room)
                .application(application)
                .candidate(candidate)
                .slotStart(slotStart)
                .slotEnd(slotEnd)
                .admitStatus(AdmitStatus.PENDING)
                .knockCount(0)
                .build();

        RoomParticipant saved = participantRepository.save(participant);
        log.info("Added participant slot for candidate {} in room {} ({} - {})",
                candidateId, room.getRoomCode(), slotStart, slotEnd);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewRoom getRoomByCode(String roomCode) {
        return roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new BadRequestException("Room not found: " + roomCode));
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewRoom getRoomById(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BadRequestException("Room not found: " + roomId));
    }

    @Override
    public InterviewRoom openRoom(String roomCode, String hostAccountId) {
        InterviewRoom room = getRoomByCode(roomCode);

        if (room.getRoomStatus() == RoomStatus.ENDED || room.getRoomStatus() == RoomStatus.EXPIRED) {
            throw new BadRequestException("Room has already ended and cannot be reopened");
        }

        if (room.getRoomStatus() != RoomStatus.SCHEDULED && room.getRoomStatus() != RoomStatus.WAITING) {
            if (room.getRoomStatus() == RoomStatus.ACTIVE) {
                return room; // Already active, idempotent
            }
            throw new BadRequestException("Room cannot be opened from status: " + room.getRoomStatus());
        }

        room.setRoomStatus(RoomStatus.ACTIVE);
        room.setOpenAt(LocalDateTime.now());

        // Update host if provided
        if (hostAccountId != null) {
            accountRepository.findById(hostAccountId).ifPresent(room::setHost);
        }

        InterviewRoom saved = roomRepository.save(room);
        log.info("Room {} opened by host {}", roomCode, hostAccountId);
        return saved;
    }

    @Override
    public InterviewRoom closeRoom(String roomCode, String hostAccountId) {
        InterviewRoom room = getRoomByCode(roomCode);

        if (room.getRoomStatus() != RoomStatus.ACTIVE) {
            throw new BadRequestException("Room can only be closed from ACTIVE status");
        }

        room.setRoomStatus(RoomStatus.CLOSING);
        room.setClosedAt(LocalDateTime.now());

        InterviewRoom saved = roomRepository.save(room);
        log.info("Room {} closing initiated by host {}", roomCode, hostAccountId);
        return saved;
    }

    @Override
    public RoomParticipant admitParticipant(String roomId, String candidateId) {
        RoomParticipant participant = participantRepository.findByRoomIdAndCandidateId(roomId, candidateId)
                .orElseThrow(() -> new BadRequestException("Participant not found in this room"));

        if (participant.getAdmitStatus() == AdmitStatus.REMOVED) {
            throw new BadRequestException("Participant has been removed and cannot be readmitted in this session");
        }

        participant.setAdmitStatus(AdmitStatus.ADMITTED);
        participant.setJoinedAt(LocalDateTime.now());
        RoomParticipant saved = participantRepository.save(participant);
        autoTransitionInterviewToInProgress(saved);
        return saved;
    }

    @Override
    public RoomParticipant removeParticipant(String roomId, String candidateId) {
        RoomParticipant participant = participantRepository.findByRoomIdAndCandidateId(roomId, candidateId)
                .orElseThrow(() -> new BadRequestException("Participant not found in this room"));

        participant.setAdmitStatus(AdmitStatus.REMOVED);
        participant.setLeftAt(LocalDateTime.now());
        return participantRepository.save(participant);
    }

    @Override
    public RoomParticipant completeParticipant(String roomId, String candidateId) {
        RoomParticipant participant = participantRepository.findByRoomIdAndCandidateId(roomId, candidateId)
                .orElseThrow(() -> new BadRequestException("Participant not found in this room"));

        if (participant.getJoinedAt() == null) {
            throw new BadRequestException("Participant has not joined the room yet");
        }

        if (participant.getAdmitStatus() != AdmitStatus.ADMITTED
                && participant.getAdmitStatus() != AdmitStatus.COMPLETED) {
            throw new BadRequestException("Participant cannot be completed in current status");
        }

        participant.setAdmitStatus(AdmitStatus.COMPLETED);
        participant.setLeftAt(LocalDateTime.now());
        return participantRepository.save(participant);
    }

    @Override
    public RoomParticipant recordKnock(String roomId, String candidateId) {
        RoomParticipant participant = participantRepository.findByRoomIdAndCandidateId(roomId, candidateId)
                .orElseThrow(() -> new BadRequestException("Participant not found in this room"));

        if (participant.getAdmitStatus() == AdmitStatus.REMOVED) {
            throw new BadRequestException("You have been removed from this room");
        }

        participant.setKnockCount(participant.getKnockCount() + 1);
        participant.setLastKnockAt(LocalDateTime.now());

        if (participant.getAdmitStatus() == AdmitStatus.PENDING) {
            participant.setAdmitStatus(AdmitStatus.WAITING_LOBBY);
        }

        return participantRepository.save(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomParticipant> getRoomParticipants(String roomId) {
        return participantRepository.findByRoomId(roomId);
    }

    private void autoTransitionInterviewToInProgress(RoomParticipant participant) {
        if (participant.getApplication() == null || participant.getRoom() == null) {
            return;
        }

        String applicationId = participant.getApplication().getId();
        String roomCode = participant.getRoom().getRoomCode();
        LocalDateTime slotStart = participant.getSlotStart();

        interviewRepository.findByApplicationId(applicationId).stream()
                .filter(i -> roomCode.equals(i.getMeetingLink()))
                .filter(i -> i.getInterviewStatus() == InterviewStatus.SCHEDULED
                        || i.getInterviewStatus() == InterviewStatus.CONFIRMED)
                .min(Comparator.comparingLong(i -> {
                    if (slotStart == null || i.getScheduledAt() == null) {
                        return Long.MAX_VALUE;
                    }
                    return Math.abs(ChronoUnit.SECONDS.between(i.getScheduledAt(), slotStart));
                }))
                .ifPresent(i -> {
                    i.setInterviewStatus(InterviewStatus.IN_PROGRESS);
                    interviewRepository.save(i);
                    log.info("Interview {} auto-transitioned to IN_PROGRESS when candidate {} admitted in room {}",
                            i.getId(),
                            participant.getCandidate() != null ? participant.getCandidate().getId() : "unknown",
                            roomCode);
                });
    }

    private String generateRoomCode(LocalDate date) {
        String dateStr = date.format(DATE_CODE_FMT);
        String suffix = generateRandomSuffix(4);
        return "RM-" + dateStr + "-" + suffix;
    }

    private String generateRandomSuffix(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
