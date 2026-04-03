package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.interview.RoomStatus;
import com.hcmute.careergraph.persistence.models.InterviewRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRoomRepository extends JpaRepository<InterviewRoom, String> {

    Optional<InterviewRoom> findByJobIdAndInterviewDate(String jobId, LocalDate interviewDate);

    Optional<InterviewRoom> findByRoomCode(String roomCode);

    List<InterviewRoom> findByRoomStatusIn(List<RoomStatus> statuses);

    @Query("""
        SELECT r FROM InterviewRoom r
        WHERE r.interviewDate < :date
          AND r.roomStatus IN :statuses
        """)
    List<InterviewRoom> findExpiredRooms(
            @Param("date") LocalDate date,
            @Param("statuses") List<RoomStatus> statuses);

    @Query("""
        SELECT r FROM InterviewRoom r
        WHERE r.roomStatus = :status
          AND r.openAt IS NOT NULL
          AND r.openAt < :before
        """)
    List<InterviewRoom> findStaleActiveRooms(
            @Param("status") RoomStatus status,
            @Param("before") java.time.LocalDateTime before);
}
