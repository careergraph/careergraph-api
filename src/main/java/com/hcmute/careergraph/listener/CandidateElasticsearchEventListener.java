package com.hcmute.careergraph.listener;

import com.hcmute.careergraph.persistence.event.CandidateUpdatedEvent;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.services.CandidateESService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

/**
 * Event listener for Candidate updates.
 * Handles synchronization between PostgreSQL and Elasticsearch.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CandidateElasticsearchEventListener {

  private final CandidateRepository candidateRepository;
  private final CandidateESService candidateESService;

  /**
   * Listen to CandidateUpdatedEvent and sync to Elasticsearch
   * Runs after transaction commits to ensure data consistency
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  public void handleCandidateUpdated(CandidateUpdatedEvent event) {
    log.info("📥 Received CandidateUpdatedEvent: candidateId={}, type={}",
        event.candidateId(), event.updateType());

    try {
      Optional<Candidate> candidateOpt = candidateRepository.findById(event.candidateId());

      if (candidateOpt.isEmpty()) {
        log.warn("Candidate not found for id: {}", event.candidateId());
        return;
      }

      Candidate candidate = candidateOpt.get();

      // Async listener must not rely on request SecurityContext; all data is loaded
      // directly from repositories with candidate id from the domain event.
      boolean shouldIndex = shouldIndexCandidate(candidate, event.updateType());

      if (shouldIndex) {
        candidateESService.indexCandidate(candidate);
        log.info("✅ Synced candidate {} to Elasticsearch", candidate.getId());
      } else {
        // Remove from Elasticsearch if exists and no longer should be indexed
        candidateESService.deleteCandidate(candidate.getId());
        log.info("🗑️ Removed candidate {} from Elasticsearch (isOpenToWork=false)", candidate.getId());
      }

    } catch (Exception e) {
      log.error("❌ Error handling CandidateUpdatedEvent for candidateId={}: {}",
          event.candidateId(), e.getMessage(), e);
    }
  }

  /**
   * Determine if candidate should be indexed based on update type
   */
  private boolean shouldIndexCandidate(Candidate candidate, CandidateUpdatedEvent.CandidateUpdateType updateType) {
    return Boolean.TRUE.equals(candidate.getIsOpenToWork());
  }

}
