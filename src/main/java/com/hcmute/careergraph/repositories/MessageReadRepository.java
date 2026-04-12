package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.MessageRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageReadRepository extends JpaRepository<MessageRead, String> {

  Optional<MessageRead> findByThreadIdAndAccountId(String threadId, String accountId);
}
