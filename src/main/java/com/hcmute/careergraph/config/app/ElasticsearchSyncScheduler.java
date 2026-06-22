package com.hcmute.careergraph.config.app;

import com.hcmute.careergraph.config.properties.ElasticsearchSyncProperties;
import com.hcmute.careergraph.services.impl.ExpiredJobRepairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSyncScheduler {

  private final ElasticsearchDataInitializer jobSyncInitializer;
  private final CandidateElasticsearchDataInitializer candidateSyncInitializer;
  private final ExpiredJobRepairService expiredJobRepairService;
  private final ElasticsearchSyncProperties syncProperties;

  private final AtomicBoolean jobSyncRunning = new AtomicBoolean(false);
  private final AtomicBoolean candidateSyncRunning = new AtomicBoolean(false);

  @Scheduled(
    fixedDelayString = "${app.es.cron.jobs.fixed-delay-ms:120000}",
    initialDelayString = "${app.es.cron.jobs.initial-delay-ms:120000}")
  public void syncJobsOnSchedule() {
    if (!syncProperties.getCron().isEnabled()) {
      return;
    }
    if (!syncProperties.getCron().getJobs().isEnabled()) {
      return;
    }
    if (!jobSyncRunning.compareAndSet(false, true)) {
      log.info("Skip scheduled job Elasticsearch sync because the previous run is still in progress.");
      return;
    }

    try {
      try {
        ElasticsearchSyncResult expiryRepairResult = expiredJobRepairService.repairExpiredJobs();
        log.info("Scheduled expired-job repair finished: indexed={}, skipped={}, message={}",
          expiryRepairResult.indexed(),
          expiryRepairResult.skipped(),
          expiryRepairResult.message());
      } catch (Exception expiredRepairException) {
        log.error("Scheduled expired-job repair failed: {}", expiredRepairException.getMessage(), expiredRepairException);
      }
      ElasticsearchSyncResult result = jobSyncInitializer.syncNow(null, syncProperties.getCron().getJobs().getBatchSize());
      log.info("Scheduled job Elasticsearch sync finished: batchSize={}, indexed={}, unchanged={}, pending={}, skipped={}, message={}",
        result.batchSize(),
        result.indexed(),
        result.unchanged(),
        result.pending(),
        result.skipped(),
        result.message());
    } catch (Exception ex) {
      log.error("Scheduled job Elasticsearch sync failed: {}", ex.getMessage(), ex);
    } finally {
      jobSyncRunning.set(false);
    }
  }

  @Scheduled(
    fixedDelayString = "${app.es.cron.candidates.fixed-delay-ms:240000}",
    initialDelayString = "${app.es.cron.candidates.initial-delay-ms:240000}")
  public void syncCandidatesOnSchedule() {
    if (!syncProperties.getCron().isEnabled()) {
      return;
    }
    if (!syncProperties.getCron().getCandidates().isEnabled()) {
      return;
    }
    if (!candidateSyncRunning.compareAndSet(false, true)) {
      log.info("Skip scheduled candidate Elasticsearch sync because the previous run is still in progress.");
      return;
    }

    try {
      ElasticsearchSyncResult result = candidateSyncInitializer.syncNow(null, syncProperties.getCron().getCandidates().getBatchSize());
      log.info("Scheduled candidate Elasticsearch sync finished: batchSize={}, indexed={}, unchanged={}, pending={}, skipped={}, message={}",
        result.batchSize(),
        result.indexed(),
        result.unchanged(),
        result.pending(),
        result.skipped(),
        result.message());
    } catch (Exception ex) {
      log.error("Scheduled candidate Elasticsearch sync failed: {}", ex.getMessage(), ex);
    } finally {
      candidateSyncRunning.set(false);
    }
  }
}
