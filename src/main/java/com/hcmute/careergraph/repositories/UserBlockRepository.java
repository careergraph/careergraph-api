package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.persistence.models.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, String> {

  boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

  Optional<UserBlock> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

  List<UserBlock> findByBlockerIdOrderByBlockedAtDesc(String blockerId);
}
