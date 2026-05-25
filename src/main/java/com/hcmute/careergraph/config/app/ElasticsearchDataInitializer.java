package com.hcmute.careergraph.config.app;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    @Value("${APP_ES_SYNC_JOBS_ENABLED:false}")
    private boolean syncJobsEnabled;

    @Value("${APP_ES_FORCE_FULL_SYNC:false}")
    private boolean forceFullSync;

    @Value("${APP_ES_ALLOW_GEMINI_FALLBACK:false}")
    private boolean allowGeminiFallback;

    @Value("${APP_ES_MAX_EMBEDDINGS_PER_RUN:50}")
    private int maxEmbeddingsPerRun;

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
        if (!syncJobsEnabled) {
            log.info("Skip Job Elasticsearch synchronization because APP_ES_SYNC_JOBS_ENABLED=false");
            return;
        }
        syncNow(null, null);
    }

    public ElasticsearchSyncResult syncNow(Boolean forceOverride, Integer maxEmbeddingsOverride) {
        boolean effectiveForce = forceOverride != null ? forceOverride : forceFullSync;
        int effectiveMaxEmbeddings = maxEmbeddingsOverride != null ? maxEmbeddingsOverride : maxEmbeddingsPerRun;

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

                long indexedJobsCount = jobESRepository.count();
                List<Job> jobsNeedingEmbedding = new ArrayList<>();
                List<JobES> jobsWithoutEmbeddingChanges = new ArrayList<>();

                for (Job job : allJobs) {
                    String searchText = buildSearchText(job);
                    String contentHash = hashText(searchText);
                    JobES existing = jobESRepository.findById(job.getId()).orElse(null);

                    if (!effectiveForce && existing != null && contentHash.equals(existing.getContentHash())) {
                        jobsWithoutEmbeddingChanges.add(existing);
                        continue;
                    }

                    jobsNeedingEmbedding.add(job);
                }

                if (jobsNeedingEmbedding.isEmpty() && indexedJobsCount >= allJobs.size()) {
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

    private List<float[]> embedBatch(List<String> batchTexts) {
        try {
            return huggingFaceEmbeddingService.embed(batchTexts);
        } catch (Exception ex) {
            if (!allowGeminiFallback) {
                throw new IllegalStateException(LOCAL_EMBEDDING_UNAVAILABLE_MESSAGE, ex);
            }
            log.warn("Local embedding service unavailable, falling back to configured Spring AI embedding model: {}",
                    ex.getMessage());
            return embeddingModel.embedBatch(batchTexts);
        }
    }

    private String buildSearchText(Job job) {
        return "%s\n%s\n%s".formatted(
                job.getTitle(),
                job.getJobCategory().getDisplayName(),
                job.getState());
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
                .experienceLevel(job.getExperienceLevel().name())
                .education(job.getEducation().name())
                .state(job.getState())
                .city(job.getCity())
                .companyId(job.getCompany().getId())
                .createdAt(job.getCreatedDate() != null ? job.getCreatedDate().toLocalDate() : LocalDate.now())
                .contentHash(contentHash)
                .embedding(embedding)
                .build();
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
