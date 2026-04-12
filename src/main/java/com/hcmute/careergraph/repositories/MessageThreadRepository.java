package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.MessageThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  @Query("SELECT t.id FROM MessageThread t WHERE t.company.id = :companyId")
  List<String> findIdsByCompanyId(@Param("companyId") String companyId);

  @Query("SELECT t.id FROM MessageThread t WHERE t.candidate.id = :candidateId")
  List<String> findIdsByCandidateId(@Param("candidateId") String candidateId);
}
