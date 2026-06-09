package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.MessageThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageThreadRepository extends JpaRepository<MessageThread, String> {

  Page<MessageThread> findByCompanyId(String companyId, Pageable pageable);

  Page<MessageThread> findByCandidateId(String candidateId, Pageable pageable);

  Optional<MessageThread> findByCompanyIdAndCandidateId(String companyId, String candidateId);

  @EntityGraph(attributePaths = { "company", "candidate", "application" })
  Optional<MessageThread> findWithEagerByCompanyIdAndCandidateId(String companyId, String candidateId);

  @Query("SELECT t.id FROM MessageThread t WHERE t.company.id = :companyId")
  List<String> findIdsByCompanyId(@Param("companyId") String companyId);

  @Query("SELECT t.id FROM MessageThread t WHERE t.candidate.id = :candidateId")
  List<String> findIdsByCandidateId(@Param("candidateId") String candidateId);

  @Query("""
      SELECT t
      FROM MessageThread t
      WHERE t.company.id = :companyId
        AND t.archivedByCompany = :archived
        AND NOT EXISTS (
          SELECT 1
          FROM ThreadDeletion td
          WHERE td.thread.id = t.id
            AND td.account.id = :accountId
        )
      """)
  Page<MessageThread> findVisibleByCompanyAndArchived(@Param("companyId") String companyId,
      @Param("accountId") String accountId,
      @Param("archived") boolean archived,
      Pageable pageable);

  @Query("""
      SELECT t
      FROM MessageThread t
      WHERE t.candidate.id = :candidateId
        AND t.archivedByCandidate = :archived
        AND t.lastMessageAt IS NOT NULL
        AND NOT EXISTS (
          SELECT 1
          FROM ThreadDeletion td
          WHERE td.thread.id = t.id
            AND td.account.id = :accountId
        )
      """)
  Page<MessageThread> findVisibleByCandidateAndArchived(@Param("candidateId") String candidateId,
      @Param("accountId") String accountId,
      @Param("archived") boolean archived,
      Pageable pageable);

  @Query("""
      SELECT t.id
      FROM MessageThread t
      WHERE t.company.id = :companyId
        AND t.archivedByCompany = false
        AND NOT EXISTS (
          SELECT 1
          FROM ThreadDeletion td
          WHERE td.thread.id = t.id
            AND td.account.id = :accountId
        )
      """)
  List<String> findVisibleIdsByCompanyId(@Param("companyId") String companyId,
      @Param("accountId") String accountId);

  @Query("""
      SELECT t.id
      FROM MessageThread t
      WHERE t.candidate.id = :candidateId
        AND t.archivedByCandidate = false
        AND t.lastMessageAt IS NOT NULL
        AND NOT EXISTS (
          SELECT 1
          FROM ThreadDeletion td
          WHERE td.thread.id = t.id
            AND td.account.id = :accountId
        )
      """)
  List<String> findVisibleIdsByCandidateId(@Param("candidateId") String candidateId,
      @Param("accountId") String accountId);
}
