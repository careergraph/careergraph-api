package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.interview.AdmitStatus;
import com.hcmute.careergraph.persistence.models.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, String> {

  List<RoomParticipant> findByRoomId(String roomId);

  Optional<RoomParticipant> findByRoomIdAndCandidateId(String roomId, String candidateId);

  Optional<RoomParticipant> findByRoomIdAndApplicationId(String roomId, String applicationId);

  @Query("""
      SELECT rp FROM RoomParticipant rp
      WHERE rp.room.roomCode = :roomCode
        AND rp.application.id = :applicationId
      """)
  Optional<RoomParticipant> findByRoomCodeAndApplicationId(
      @Param("roomCode") String roomCode,
      @Param("applicationId") String applicationId);

  @Query("""
      SELECT rp FROM RoomParticipant rp
      WHERE rp.room.id = :roomId
        AND rp.admitStatus IN :statuses
      ORDER BY rp.slotStart ASC
      """)
  List<RoomParticipant> findByRoomIdAndAdmitStatusIn(
      @Param("roomId") String roomId,
      @Param("statuses") List<AdmitStatus> statuses);

  @Query("""
      SELECT rp FROM RoomParticipant rp
      WHERE rp.slotEnd < :before
        AND rp.admitStatus NOT IN :excludeStatuses
        AND rp.joinedAt IS NULL
      """)
  List<RoomParticipant> findNoShows(
      @Param("before") LocalDateTime before,
      @Param("excludeStatuses") List<AdmitStatus> excludeStatuses);
}
