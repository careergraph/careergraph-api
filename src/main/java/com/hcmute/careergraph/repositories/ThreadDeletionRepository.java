package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.ThreadDeletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThreadDeletionRepository extends JpaRepository<ThreadDeletion, String> {

  boolean existsByThreadIdAndAccountId(String threadId, String accountId);

  Optional<ThreadDeletion> findByThreadIdAndAccountId(String threadId, String accountId);

  void deleteByThreadIdAndAccountId(String threadId, String accountId);

  void deleteByThreadId(String threadId);
}
