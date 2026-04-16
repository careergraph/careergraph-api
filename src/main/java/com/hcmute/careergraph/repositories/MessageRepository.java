package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.Message;
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
public interface MessageRepository extends JpaRepository<Message, String> {

  Page<Message> findByThreadId(String threadId, Pageable pageable);

    @Query("""
            SELECT m
            FROM Message m
            WHERE m.thread.id = :threadId
                AND (:jobId IS NULL OR m.jobContext.id = :jobId)
            """)
    Page<Message> findByThreadIdAndOptionalJobId(@Param("threadId") String threadId,
            @Param("jobId") String jobId,
            Pageable pageable);

  Page<Message> findByThreadIdAndDeletedFalse(String threadId, Pageable pageable);

  Optional<Message> findTopByThreadIdAndDeletedFalseOrderByCreatedDateDesc(String threadId);

  @Query("""
      SELECT COUNT(m)
      FROM Message m
      WHERE m.thread.id = :threadId
        AND m.sender.id <> :accountId
        AND m.deleted = false
        AND m.createdDate > COALESCE(
            (SELECT mr.lastReadAt FROM MessageRead mr WHERE mr.thread.id = :threadId AND mr.account.id = :accountId),
            :epoch
        )
      """)
  long countUnreadInThread(@Param("threadId") String threadId,
      @Param("accountId") String accountId,
      @Param("epoch") LocalDateTime epoch);

  @Query("""
      SELECT COUNT(m)
      FROM Message m
      WHERE m.thread.id = :threadId
        AND m.jobContext.id = :jobId
        AND m.sender.id <> :accountId
        AND m.deleted = false
        AND m.createdDate > COALESCE(
            (SELECT mr.lastReadAt FROM MessageRead mr WHERE mr.thread.id = :threadId AND mr.account.id = :accountId),
            :epoch
        )
      """)
  long countUnreadInThreadByJob(@Param("threadId") String threadId,
      @Param("jobId") String jobId,
      @Param("accountId") String accountId,
      @Param("epoch") LocalDateTime epoch);

  @Query("""
      SELECT MAX(m.createdDate)
      FROM Message m
      WHERE m.thread.id = :threadId
        AND m.jobContext.id = :jobId
      """)
  Optional<LocalDateTime> findLastMessageTimeByThreadAndJob(@Param("threadId") String threadId,
      @Param("jobId") String jobId);

  @Query("""
      SELECT COUNT(m)
      FROM Message m
      WHERE m.sender.id <> :accountId
        AND m.deleted = false
        AND m.thread.id IN :threadIds
        AND m.createdDate > COALESCE(
            (SELECT mr.lastReadAt FROM MessageRead mr WHERE mr.thread.id = m.thread.id AND mr.account.id = :accountId),
            :epoch
        )
      """)
  long countTotalUnreadByThreadIds(@Param("accountId") String accountId,
      @Param("threadIds") List<String> threadIds,
      @Param("epoch") LocalDateTime epoch);

  @Query("""
      SELECT COUNT(m)
      FROM Message m
      WHERE m.sender.id <> :accountId
        AND m.deleted = false
        AND m.createdDate > COALESCE(
            (SELECT mr.lastReadAt FROM MessageRead mr WHERE mr.account.id = :accountId AND mr.thread.id = m.thread.id),
            :epoch
        )
      """)
  long countTotalUnread(@Param("accountId") String accountId,
      @Param("epoch") LocalDateTime epoch);
}
