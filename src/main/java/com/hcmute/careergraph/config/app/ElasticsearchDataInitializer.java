package com.hcmute.careergraph.config.app;

import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.persistence.event.JobCreatedEvent;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.JobESRepository;
import com.hcmute.careergraph.repositories.JobNotificationHistoryRepository;
import com.hcmute.careergraph.repositories.JobNotificationQueueRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.repositories.NewlyPostedJobRepository;
import com.hcmute.careergraph.services.HuggingFaceEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Order(1)
public class ElasticsearchDataInitializer implements CommandLineRunner {

    private final JobRepository jobRepository;
    private final JobESRepository jobESRepository;
    private final ElasticsearchOperations elasticsearchOperations; // Cần dùng để kiểm tra sức khỏe ELS
    private final EmbeddingModel embeddingModel;
    private final HuggingFaceEmbeddingService huggingFaceEmbeddingService;
    private static final int EMBEDDING_BATCH_SIZE = 100;
    private final ApplicationEventPublisher publisher;
    private final JobNotificationHistoryRepository historyRepo;
    private final JobNotificationQueueRepository queueRepo;
    private final NewlyPostedJobRepository newlyPostedJobRepo;

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
        synchronizeDataWithRetry();
    }

    private void synchronizeDataWithRetry() {
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
                List<String> texts = allJobs.stream()
                        // .map(job -> job.getTitle()
                        .map(job -> """
                                %s
                                %s
                                %s
                                """.formatted(
                                job.getTitle(),
                                job.getJobCategory().getDisplayName(),
                                job.getState()))
                        .toList();

                List<float[]> vectors = new java.util.ArrayList<>();

                for (int start = 0; start < texts.size(); start += EMBEDDING_BATCH_SIZE) {
                    int end = Math.min(start + EMBEDDING_BATCH_SIZE, texts.size());

                    List<String> batchTexts = texts.subList(start, end);

                    List<float[]> batchVectors = embeddingModel.embedForResponse(batchTexts)
                            .getResults()
                            .stream()
                            .map(Embedding::getOutput)
                            .toList();

                    vectors.addAll(batchVectors);
                }
                queueRepo.deleteAll();
                historyRepo.deleteAll();
                newlyPostedJobRepo.deleteAll(); // Clear newly posted jobs

                List<JobES> jobsToSave = IntStream.range(0, allJobs.size())
                        .mapToObj(i -> {
                            Job job = allJobs.get(i);
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
                                    .createdAt(job.getCreatedDate() != null ? job.getCreatedDate().toLocalDate()
                                            : LocalDate.now())
                                    .embedding(vectors.get(i))
                                    .build();
                        })
                        .toList();
                clearIndexData();

                jobESRepository.saveAll(jobsToSave);

                System.out.println("Data synchronization complete. Total posts: " + allJobs.size());
                return; // Thành công, thoát khỏi vòng lặp

            } catch (Exception e) {
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
                        return; // Thoát khỏi vòng lặp và kết thúc
                    }
                } else {
                    System.err.println("Failed to synchronize data to Elasticsearch after " + MAX_RETRIES
                            + " attempts. Application may experience search issues.");
                    // Bạn có thể chọn ném RuntimeException ở đây nếu muốn dừng ứng dụng hoàn toàn
                }
            }
        }
    }

    private LocalDate safeParseDate(String date) {
        try {
            return (date == null || date.isBlank())
                    ? null
                    : LocalDate.parse(date);
        } catch (Exception e) {
            return null; // ES cho phép null
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
