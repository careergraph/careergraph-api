package com.hcmute.careergraph.config.app;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

  private final AtomicBoolean jobSyncRunning = new AtomicBoolean(false);
  private final AtomicBoolean candidateSyncRunning = new AtomicBoolean(false);

  @Value("${APP_ES_CRON_ENABLED:false}")
  private boolean cronEnabled;

  @Value("${APP_ES_CRON_JOBS_ENABLED:true}")
  private boolean jobCronEnabled;

  @Value("${APP_ES_CRON_CANDIDATES_ENABLED:true}")
  private boolean candidateCronEnabled;

  @Value("${APP_ES_CRON_JOBS_BATCH_SIZE:10}")
  private int jobCronBatchSize;

  @Value("${APP_ES_CRON_CANDIDATES_BATCH_SIZE:5}")
  private int candidateCronBatchSize;

  @Scheduled(
    fixedDelayString = "${APP_ES_CRON_JOBS_FIXED_DELAY_MS:120000}",
    initialDelayString = "${APP_ES_CRON_JOBS_INITIAL_DELAY_MS:120000}")
  public void syncJobsOnSchedule() {
    if (!cronEnabled) {
      return;
    }
    if (!jobCronEnabled) {
      return;
    }
    if (!jobSyncRunning.compareAndSet(false, true)) {
      log.info("Skip scheduled job Elasticsearch sync because the previous run is still in progress.");
      return;
    }

    try {
      ElasticsearchSyncResult result = jobSyncInitializer.syncNow(null, jobCronBatchSize);
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
    fixedDelayString = "${APP_ES_CRON_CANDIDATES_FIXED_DELAY_MS:240000}",
    initialDelayString = "${APP_ES_CRON_CANDIDATES_INITIAL_DELAY_MS:240000}")
  public void syncCandidatesOnSchedule() {
    if (!cronEnabled) {
      return;
    }
    if (!candidateCronEnabled) {
      return;
    }
    if (!candidateSyncRunning.compareAndSet(false, true)) {
      log.info("Skip scheduled candidate Elasticsearch sync because the previous run is still in progress.");
      return;
    }

    try {
      ElasticsearchSyncResult result = candidateSyncInitializer.syncNow(null, candidateCronBatchSize);
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