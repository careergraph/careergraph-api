package com.hcmute.careergraph.listeners;

import com.hcmute.careergraph.events.FileChangedEvent;
import com.hcmute.careergraph.services.CandidateESService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * File Changed Event Listener
 * Tự động sync candidate vào Elasticsearch khi CV thay đổi:
 * - CV mới upload (CREATED) → Extract & sync
 * - CV update (UPDATED) → Re-sync (shareToFindJob toggle, extractedText updated)
 * - CV delete (DELETED) → Re-sync
 * 
 * V2.1: Event-driven approach
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileEventListener {

  private final CandidateESService candidateESService;

  /**
   * Lắng nghe FileChangedEvent
   * Async execution AFTER transaction commit để đảm bảo data consistency
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onFileChanged(FileChangedEvent event) {
    try {
      log.info("File {} event for candidateId={}, fileId={}, fileName={}", 
          event.getChangeType(), 
          event.getCandidateId(), 
          event.getFileId(), 
          event.getFileName());
      
      // Re-sync candidate to Elasticsearch
      candidateESService.syncCandidate(event.getCandidateId());
      
      log.info("Successfully synced candidate {} to ES after file {} event", 
          event.getCandidateId(), event.getChangeType());
    } catch (Exception e) {
      log.error("Failed to sync candidate {} to ES after file {} event: {}", 
          event.getCandidateId(), event.getChangeType(), e.getMessage(), e);
      // Don't rethrow - this is async, failure should not affect main flow
    }
  }
}
