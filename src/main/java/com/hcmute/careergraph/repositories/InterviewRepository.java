package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.interview.InterviewStatus;
import com.hcmute.careergraph.persistence.models.Interview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, String> {

    Page<Interview> findByCompanyIdAndInterviewStatusIn(
            String companyId, List<InterviewStatus> statuses, Pageable pageable);

    long countByCompanyIdAndInterviewStatusInAndScheduledAtBetween(
            String companyId,
            List<InterviewStatus> statuses,
            LocalDateTime from,
            LocalDateTime to);

    Page<Interview> findByCompanyId(String companyId, Pageable pageable);

    List<Interview> findByCandidateIdAndInterviewStatusIn(
            String candidateId, List<InterviewStatus> statuses);

    List<Interview> findByCandidateIdAndInterviewStatusInAndHiddenFromCandidateFalse(
            String candidateId, List<InterviewStatus> statuses);

    List<Interview> findByCompanyIdAndScheduledAtBetween(
            String companyId, LocalDateTime start, LocalDateTime end);

    Optional<Interview> findByMeetingLink(String meetingLink);

    List<Interview> findByMeetingLinkOrderByScheduledAtAsc(String meetingLink);

    @Query("""
            SELECT i FROM Interview i
            WHERE i.candidate.id = :candidateId
              AND i.interviewStatus IN :statuses
              AND i.scheduledAt < :proposedEnd
              AND i.endAt > :proposedStart
            """)
    List<Interview> findOverlappingByCandidate(
            @Param("candidateId") String candidateId,
            @Param("proposedStart") LocalDateTime proposedStart,
            @Param("proposedEnd") LocalDateTime proposedEnd,
            @Param("statuses") List<InterviewStatus> statuses);

    @Query("""
            SELECT i FROM Interview i
            JOIN i.participants p
            WHERE p.account.id = :accountId
              AND i.interviewStatus IN :statuses
              AND i.scheduledAt < :proposedEnd
              AND i.endAt > :proposedStart
            """)
    List<Interview> findOverlappingByParticipant(
            @Param("accountId") String accountId,
            @Param("proposedStart") LocalDateTime proposedStart,
            @Param("proposedEnd") LocalDateTime proposedEnd,
            @Param("statuses") List<InterviewStatus> statuses);

    List<Interview> findByApplicationId(String applicationId);

    @Query("""
            SELECT i FROM Interview i
            WHERE i.application.id = :applicationId
                AND i.job.id = :jobId
                AND i.interviewStatus IN :statuses
            ORDER BY i.scheduledAt ASC
            """)
    List<Interview> findActiveByApplicationAndJob(
            @Param("applicationId") String applicationId,
            @Param("jobId") String jobId,
            @Param("statuses") List<InterviewStatus> statuses);

    @Query("""
            SELECT DISTINCT i.application.id FROM Interview i
            WHERE i.job.id = :jobId
              AND i.interviewStatus IN :statuses
            """)
    List<String> findScheduledApplicationIdsByJobId(
            @Param("jobId") String jobId,
            @Param("statuses") List<InterviewStatus> statuses);

    @Query("""
            SELECT i FROM Interview i
            WHERE i.candidate.id = :candidateId
              AND i.interviewStatus IN :statuses
            ORDER BY i.scheduledAt ASC
            """)
    List<Interview> findUpcomingByCandidate(
            @Param("candidateId") String candidateId,
            @Param("statuses") List<InterviewStatus> statuses);
}
