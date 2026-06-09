package com.hcmute.careergraph.listener;

import com.hcmute.careergraph.persistence.event.CandidateUpdatedEvent;
import com.hcmute.careergraph.services.CandidateESService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Synchronizes candidate search documents after committed profile/CV changes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CandidateElasticsearchEventListener {

  private final CandidateESService candidateESService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  public void handleCandidateUpdated(CandidateUpdatedEvent event) {
    log.info("Received CandidateUpdatedEvent: candidateId={}, type={}", event.candidateId(), event.updateType());

    try {
      candidateESService.syncCandidate(event.candidateId());
      log.info("Synchronized candidate {} to Elasticsearch", event.candidateId());
    } catch (Exception e) {
      log.error("Error synchronizing candidate {} to Elasticsearch: {}", event.candidateId(), e.getMessage(), e);
    }
  }
}
