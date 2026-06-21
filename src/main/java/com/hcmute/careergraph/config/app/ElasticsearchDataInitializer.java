package com.hcmute.careergraph.config.app;

import com.hcmute.careergraph.config.properties.ElasticsearchSyncProperties;
import com.hcmute.careergraph.config.properties.EmbeddingRuntimeProperties;
import com.hcmute.careergraph.enums.company.CompanyOperationalStatus;
import com.hcmute.careergraph.enums.company.CompanyVerificationStatus;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.helper.VietnamProvinceUtils;
import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.persistence.event.JobCreatedEvent;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.JobESRepository;
import com.hcmute.careergraph.repositories.JobNotificationHistoryRepository;
import com.hcmute.careergraph.repositories.JobNotificationQueueRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.repositories.NewlyPostedJobRepository;
import com.hcmute.careergraph.services.EmbedService;
import com.hcmute.careergraph.services.HuggingFaceEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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
    private final ApplicationEventPublisher publisher;
    private final JobNotificationHistoryRepository historyRepo;
    private final JobNotificationQueueRepository queueRepo;
    private final NewlyPostedJobRepository newlyPostedJobRepo;
    private final EmbedService embeddingModel;
    private final ElasticsearchSyncProperties syncProperties;
    private final EmbeddingRuntimeProperties embeddingRuntimeProperties;

    private static final List<String> KEYWORDS = List.of(
            "java",
            "developer",
            "backend",
            "frontend",
            "react",
            "fullstack");

    private String normalize(String text) {
        return text
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean matchTitle(Job job) {
        String title = normalize(job.getTitle());
        // String title = job.getTitle().toLowerCase();
        return KEYWORDS.stream().anyMatch(title::contains);
    }

    @Override
    public void run(String... args) throws Exception {
        if (!syncProperties.getJobs().isSyncEnabled()) {
            log.info("Skip Job Elasticsearch synchronization because APP_ES_SYNC_JOBS_ENABLED=false");
            return;
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
                        .filter(this::shouldIndexJob)
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
                    String searchText = buildSearchText(job);
                    String contentHash = hashText(searchText);
                    JobES existing = jobESRepository.findById(job.getId()).orElse(null);

                    if (!effectiveForce && existing != null && contentHash.equals(existing.getContentHash())) {
                        jobsWithoutEmbeddingChanges.add(existing);
                        continue;
                    }

                    jobsNeedingEmbedding.add(job);
                }

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
                List<String> texts = jobsToEmbed.stream().map(this::buildSearchText).toList();
                List<float[]> vectors = new ArrayList<>();

                for (int start = 0; start < texts.size(); start += EMBEDDING_BATCH_SIZE) {
                    int end = Math.min(start + EMBEDDING_BATCH_SIZE, texts.size());
                    List<String> batchTexts = texts.subList(start, end);
                    vectors.addAll(embedBatch(batchTexts));
                }

                if (effectiveForce) {
                    queueRepo.deleteAll();
                    historyRepo.deleteAll();
                    newlyPostedJobRepo.deleteAll();
                    clearIndexData();
                }

                List<JobES> jobsToSave = IntStream.range(0, jobsToEmbed.size())
                        .mapToObj(i -> toJobDocument(jobsToEmbed.get(i), vectors.get(i)))
                        .toList();

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

                System.err.println("Attempt " + attempt + " failed. Reason: " + e.getMessage());

                if (attempt < MAX_RETRIES) {
                    System.out.println("Retrying in " + DELAY_SECONDS + " seconds...");
                    try {
                        TimeUnit.SECONDS.sleep(DELAY_SECONDS);
                    } catch (InterruptedException ie) {
                        // Nếu luồng bị gián đoạn, chúng ta nên khôi phục lại trạng thái gián đoạn
                        // để các mã gọi ở tầng cao hơn biết và xử lý.
                        Thread.currentThread().interrupt();
                        System.err.println("Synchronization thread was interrupted during delay.");
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
                    System.err.println("Failed to synchronize data to Elasticsearch after " + MAX_RETRIES
                            + " attempts. Application may experience search issues.");
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

    private String buildSearchText(Job job) {
        return "%s\n%s\n%s".formatted(
                job.getTitle(),
                job.getJobCategory().getDisplayName(),
                job.getState());
    }

    private boolean shouldIndexJob(Job job) {
        return job != null
                && job.getStatus() == Status.ACTIVE
                && job.getCompany() != null;
    }

    private JobES toJobDocument(Job job, float[] embedding) {
        String searchText = buildSearchText(job);
        String contentHash = hashText(searchText);
        if (matchTitle(job)) {
            publisher.publishEvent(new JobCreatedEvent(job.getId()));
            System.out.println("➡️ Marked as newly posted: " + job.getTitle());
        }
        return JobES.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .status(job.getStatus().name())
                .jobCategory(job.getJobCategory().name())
                .employmentType(job.getEmploymentType().name())
                .experienceLevel(toEnumName(job.getExperienceLevel()))
                .education(toEnumName(job.getEducation()))
                .state(job.getState())
                .provinceSlug(VietnamProvinceUtils.slugFromStateName(job.getState()))
                .provinceCode(VietnamProvinceUtils.codeFromStateName(job.getState()))
                .city(job.getCity())
                .companyId(job.getCompany().getId())
                .companyVerificationStatus(job.getCompany().getVerificationStatus() != null
                        ? job.getCompany().getVerificationStatus().name()
                        : CompanyVerificationStatus.NOT_SUBMITTED.name())
                .companyOperationalStatus(job.getCompany().getOperationalStatus() != null
                        ? job.getCompany().getOperationalStatus().name()
                        : CompanyOperationalStatus.ACTIVE.name())
                .companyBlocked(job.getCompany().getOperationalStatus() == CompanyOperationalStatus.BLOCKED)
                .jobSearchable(job.getCompany().getVerificationStatus() == CompanyVerificationStatus.APPROVED
                        && job.getCompany().getOperationalStatus() == CompanyOperationalStatus.ACTIVE)
                .createdAt(job.getCreatedDate() != null ? job.getCreatedDate().toLocalDate() : LocalDate.now())
                .contentHash(contentHash)
                .embedding(embedding)
                .build();
    }

    private String toEnumName(Enum<?> value) {
        return value != null ? value.name() : null;
    }

    private String hashText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format(Locale.ROOT, "%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
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
