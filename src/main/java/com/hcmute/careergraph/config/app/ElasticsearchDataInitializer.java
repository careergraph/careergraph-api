package com.hcmute.careergraph.config.app;

import com.hcmute.careergraph.config.properties.ElasticsearchSyncProperties;
import com.hcmute.careergraph.config.properties.EmbeddingRuntimeProperties;
import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.JobESRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.EmbedService;
import com.hcmute.careergraph.services.HuggingFaceEmbeddingService;
import com.hcmute.careergraph.services.impl.ExpiredJobRepairService;
import com.hcmute.careergraph.services.impl.JobSearchDocumentFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Order(1)
@Slf4j
public class ElasticsearchDataInitializer implements CommandLineRunner {

    private static final String LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE = "Local embedding service unavailable and Gemini fallback is disabled.";

    private final JobRepository jobRepository;
    private final JobESRepository jobESRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final HuggingFaceEmbeddingService huggingFaceEmbeddingService;
    private static final int EMBEDDING_BATCH_SIZE = 100;
    private final EmbedService embeddingModel;
    private final ElasticsearchSyncProperties syncProperties;
    private final EmbeddingRuntimeProperties embeddingRuntimeProperties;
    private final JobSearchDocumentFactory jobSearchDocumentFactory;
    private final ExpiredJobRepairService expiredJobRepairService;

    @Override
    public void run(String... args) throws Exception {
        if (!syncProperties.getJobs().isSyncEnabled()) {
            log.info("Skip Job Elasticsearch synchronization because APP_ES_SYNC_JOBS_ENABLED=false");
            return;
        }
        try {
            ElasticsearchSyncResult expiredRepairResult = expiredJobRepairService.repairExpiredJobs();
            log.info("Startup expired-job repair finished: indexed={}, skipped={}, message={}",
                    expiredRepairResult.indexed(),
                    expiredRepairResult.skipped(),
                    expiredRepairResult.message());
        } catch (Exception exception) {
            log.error("Startup expired-job repair failed: {}", exception.getMessage(), exception);
        }
        syncNow(null, null);
    }

    public ElasticsearchSyncResult syncNow(Boolean forceOverride, Integer maxEmbeddingsOverride) {
        boolean effectiveForce = forceOverride != null ? forceOverride : syncProperties.getJobs().isForceFullSync();
        int effectiveMaxEmbeddings = maxEmbeddingsOverride != null ? maxEmbeddingsOverride : syncProperties.getJobs().getMaxEmbeddingsPerRun();
        boolean recreatedIndexAfterDimensionMismatch = false;

        final int MAX_RETRIES = 5;
        final long DELAY_SECONDS = 10;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // 1. KIỂM TRA SỨC KHỎE CỦA INDEX/CLUSTER (RẤT QUAN TRỌNG)
                if (!elasticsearchOperations.indexOps(JobES.class).exists()) {
                    // Nếu index chưa tồn tại, có thể cần tạo thủ công hoặc để Spring Data tạo (nếu
                    // cấu hình auto-create)
                    // Việc index chưa tồn tại là nguyên nhân phổ biến gây ra lỗi "all shards
                    // failed"
                    System.out
                            .println("Elasticsearch index not found. Waiting for Spring Data to create or retrying...");
                    throw new IllegalStateException("Index does not exist yet.");
                }

                // 2. THỰC HIỆN ĐỒNG BỘ HÓA DỮ LIỆU
                System.out.println("Attempt " + attempt + ": Starting data synchronization to Elasticsearch...");

                List<Job> allJobs = jobRepository.findAll();
                if (allJobs.isEmpty()) {
                    log.info("No jobs found in database. Skipping Elasticsearch synchronization.");
                    return new ElasticsearchSyncResult("jobs", true, effectiveForce, effectiveMaxEmbeddings, 0, 0, 0,
                            "No jobs found in database.");
                }

                List<Job> activeJobs = allJobs.stream()
                        .filter(jobSearchDocumentFactory::shouldIndex)
                        .toList();

                Set<String> activeJobIds = activeJobs.stream()
                        .map(Job::getId)
                        .collect(java.util.stream.Collectors.toCollection(HashSet::new));

                List<String> staleDocumentIds = new ArrayList<>();
                for (JobES indexedJob : jobESRepository.findAll()) {
                    if (indexedJob.getId() != null && !activeJobIds.contains(indexedJob.getId())) {
                        staleDocumentIds.add(indexedJob.getId());
                    }
                }

                if (!staleDocumentIds.isEmpty()) {
                    jobESRepository.deleteAllById(staleDocumentIds);
                    log.warn(
                            "Job Elasticsearch drift detected before sync: staleDocuments={}, sampleIds={}",
                            staleDocumentIds.size(),
                            sampleIds(staleDocumentIds));
                }

                if (activeJobs.isEmpty()) {
                    log.info("No active jobs found in database. Cleared stale Elasticsearch job documents.");
                    return new ElasticsearchSyncResult(
                            "jobs",
                            true,
                            effectiveForce,
                            effectiveMaxEmbeddings,
                            0,
                            0,
                            0,
                            "No active jobs found in database.");
                }

                long indexedJobsCount = jobESRepository.count();
                List<Job> jobsNeedingEmbedding = new ArrayList<>();
                List<JobES> jobsWithoutEmbeddingChanges = new ArrayList<>();

                for (Job job : activeJobs) {
                    String contentHash = jobSearchDocumentFactory.buildContentHash(job);
                    JobES existing = jobESRepository.findById(job.getId()).orElse(null);

                    if (!effectiveForce && existing != null && contentHash.equals(existing.getContentHash())) {
                        jobsWithoutEmbeddingChanges.add(existing);
                        continue;
                    }

                    jobsNeedingEmbedding.add(job);
                }

                log.info(
                        "Job Elasticsearch sync state: totalJobs={}, eligibleJobs={}, staleDocuments={}, changedJobs={}, unchangedJobs={}, force={}, batchSize={}",
                        allJobs.size(),
                        activeJobs.size(),
                        staleDocumentIds.size(),
                        jobsNeedingEmbedding.size(),
                        jobsWithoutEmbeddingChanges.size(),
                        effectiveForce,
                        effectiveMaxEmbeddings);

                if (jobsNeedingEmbedding.isEmpty() && indexedJobsCount >= activeJobs.size()) {
                    log.info("Skip Job Elasticsearch synchronization because no jobs changed since the last index run.");
                    return new ElasticsearchSyncResult(
                            "jobs",
                            true,
                            effectiveForce,
                            effectiveMaxEmbeddings,
                            0,
                            jobsWithoutEmbeddingChanges.size(),
                            0,
                            "No jobs changed since the last index run.");
                }

                int cappedCount = Math.min(jobsNeedingEmbedding.size(), Math.max(1, effectiveMaxEmbeddings));
                List<Job> jobsToEmbed = jobsNeedingEmbedding.subList(0, cappedCount);
                List<String> texts = jobsToEmbed.stream()
                        .map(jobSearchDocumentFactory::buildEmbeddingText)
                        .toList();
                List<float[]> vectors = new ArrayList<>();

                for (int start = 0; start < texts.size(); start += EMBEDDING_BATCH_SIZE) {
                    int end = Math.min(start + EMBEDDING_BATCH_SIZE, texts.size());
                    List<String> batchTexts = texts.subList(start, end);
                    vectors.addAll(embedBatch(batchTexts));
                }

                if (effectiveForce) {
                    clearIndexData();
                }

                List<JobES> jobsToSave = new ArrayList<>(jobsToEmbed.size());
                for (int index = 0; index < jobsToEmbed.size(); index++) {
                    jobsToSave.add(jobSearchDocumentFactory.toDocument(jobsToEmbed.get(index), vectors.get(index)));
                }

                jobESRepository.saveAll(jobsToSave);

                log.info(
                        "Job Elasticsearch synchronization complete. Indexed {} changed jobs in this run, skipped {} unchanged jobs, pending {} more changed jobs.",
                        jobsToSave.size(),
                        jobsWithoutEmbeddingChanges.size(),
                        Math.max(0, jobsNeedingEmbedding.size() - jobsToSave.size()));
                return new ElasticsearchSyncResult(
                        "jobs",
                        false,
                        effectiveForce,
                        effectiveMaxEmbeddings,
                        jobsToSave.size(),
                        jobsWithoutEmbeddingChanges.size(),
                        Math.max(0, jobsNeedingEmbedding.size() - jobsToSave.size()),
                        "Job Elasticsearch synchronization completed.");

            } catch (Exception e) {
                if (LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE.equals(e.getMessage())) {
                    log.warn("{} Skipping Job Elasticsearch synchronization.", LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE);
                    return new ElasticsearchSyncResult(
                            "jobs",
                            true,
                            effectiveForce,
                            effectiveMaxEmbeddings,
                            0,
                            0,
                            0,
                            LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE);
                }
                if (isDenseVectorDimensionMismatch(e) && !recreatedIndexAfterDimensionMismatch) {
                    log.warn("Detected Job Elasticsearch dense_vector dimension mismatch. Recreating index with the current mapping before retrying.");
                    recreateIndex();
                    recreatedIndexAfterDimensionMismatch = true;
                    continue;
                }
                        if (isRateLimitError(e)) {
                            log.warn("Skipping Job Elasticsearch synchronization retries because the embedding provider is rate-limited: {}",
                                e.getMessage());
                            return new ElasticsearchSyncResult(
                                "jobs",
                                true,
                                effectiveForce,
                                effectiveMaxEmbeddings,
                                0,
                                0,
                                0,
                                "Embedding provider rate-limited. Skipped retries for this run: " + e.getMessage());
                        }

                log.warn("Job Elasticsearch synchronization attempt {} failed: {}", attempt, e.getMessage(), e);

                if (attempt < MAX_RETRIES) {
                    log.info("Retrying job Elasticsearch synchronization in {} seconds.", DELAY_SECONDS);
                    try {
                        TimeUnit.SECONDS.sleep(DELAY_SECONDS);
                    } catch (InterruptedException ie) {
                        // Nếu luồng bị gián đoạn, chúng ta nên khôi phục lại trạng thái gián đoạn
                        // để các mã gọi ở tầng cao hơn biết và xử lý.
                        Thread.currentThread().interrupt();
                        log.warn("Job Elasticsearch synchronization thread was interrupted during retry delay.");
                        return new ElasticsearchSyncResult(
                                "jobs",
                                true,
                                effectiveForce,
                                effectiveMaxEmbeddings,
                                0,
                                0,
                                0,
                                "Synchronization thread was interrupted during delay.");
                    }
                } else {
                    log.error("Failed to synchronize jobs to Elasticsearch after {} attempts. Search drift may persist.",
                            MAX_RETRIES);
                    return new ElasticsearchSyncResult(
                            "jobs",
                            true,
                            effectiveForce,
                            effectiveMaxEmbeddings,
                            0,
                            0,
                            0,
                            "Failed to synchronize data to Elasticsearch after retries: " + e.getMessage());
                }
            }
        }

        return new ElasticsearchSyncResult(
                "jobs",
                true,
                effectiveForce,
                effectiveMaxEmbeddings,
                0,
                0,
                0,
                "Job Elasticsearch synchronization finished without processing.");
    }

    private String sampleIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return "[]";
        }
        return ids.stream()
                .limit(5)
                .toList()
                .toString();
    }

    private boolean isRateLimitError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("429")
                        || normalized.contains("quota exceeded")
                        || normalized.contains("rate limit")
                        || normalized.contains("too many requests")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isDenseVectorDimensionMismatch(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("dense_vector")
                        && normalized.contains("dimensions")
                        && normalized.contains("mapping")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private void recreateIndex() {
        var indexOps = elasticsearchOperations.indexOps(JobES.class);

        if (indexOps.exists()) {
            indexOps.delete();
        }

        indexOps.create();
        indexOps.putMapping(indexOps.createMapping(JobES.class));
        indexOps.refresh();
    }

    private List<float[]> embedBatch(List<String> batchTexts) {
        if (embeddingRuntimeProperties.isUseLocalFirst()) {
            try {
                return huggingFaceEmbeddingService.embed(batchTexts);
            } catch (Exception ex) {
                if (!embeddingRuntimeProperties.isAllowGeminiFallback()) {
                    throw new IllegalStateException(LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE, ex);
                }
                log.warn("Local embedding service unavailable, falling back to configured Spring AI embedding model: {}",
                        ex.getMessage());
                return embeddingModel.embedBatch(batchTexts);
            }
        }

        try {
            return embeddingModel.embedBatch(batchTexts);
        } catch (Exception geminiEx) {
            log.warn("Primary Spring AI embedding model unavailable, falling back to local embedding service: {}",
                    geminiEx.getMessage());
            try {
                return huggingFaceEmbeddingService.embed(batchTexts);
            } catch (Exception localEx) {
                localEx.addSuppressed(geminiEx);
                throw new IllegalStateException("Gemini-primary batch embedding failed and local fallback is unavailable.", localEx);
            }
        }
    }

    private void clearIndexData() {
        var indexOps = elasticsearchOperations.indexOps(JobES.class);

        if (!indexOps.exists()) {
            System.out.println("Index does not exist, skip clearing.");
            return;
        }

        System.out.println("Clearing all documents in Elasticsearch index...");

        elasticsearchOperations.delete(
                org.springframework.data.elasticsearch.core.query.Query.findAll(),
                JobES.class);

        // Bắt buộc refresh để đảm bảo index trống ngay
        indexOps.refresh();
    }

}
