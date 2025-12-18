package com.hcmute.careergraph.config.app;

import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.JobESRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Order(1)
public class ElasticsearchDataInitializer implements CommandLineRunner {

    private final JobRepository jobRepository;
    private final JobESRepository jobESRepository;
    private final ElasticsearchOperations elasticsearchOperations; // Cần dùng để kiểm tra sức khỏe ELS

    // Constructor Injection
    public ElasticsearchDataInitializer(JobRepository jobRepository, JobESRepository jobESRepository, ElasticsearchOperations elasticsearchOperations) {
        this.jobRepository = jobRepository;
        this.jobESRepository = jobESRepository;
        this.elasticsearchOperations = elasticsearchOperations;
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
                    // Nếu index chưa tồn tại, có thể cần tạo thủ công hoặc để Spring Data tạo (nếu cấu hình auto-create)
                    // Việc index chưa tồn tại là nguyên nhân phổ biến gây ra lỗi "all shards failed"
                    System.out.println("Elasticsearch index not found. Waiting for Spring Data to create or retrying...");
                    throw new IllegalStateException("Index does not exist yet.");
                }

                // 2. THỰC HIỆN ĐỒNG BỘ HÓA DỮ LIỆU
                System.out.println("Attempt " + attempt + ": Starting data synchronization to Elasticsearch...");


                List<Job> allJobs = jobRepository.findAll();

                // Sử dụng bulk save (saveAll) để tối ưu hiệu suất thay vì save từng cái trong forEach
                List<JobES> postsToSave = allJobs.stream()
                        .map(post -> JobES.builder()
                                .id(post.getId().toString())
                                .title(post.getTitle())
                                .description(post.getDescription())
                                .state(post.getState())
                                .build())
                        .toList();

                jobESRepository.saveAll(postsToSave);

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
                    System.err.println("Failed to synchronize data to Elasticsearch after " + MAX_RETRIES + " attempts. Application may experience search issues.");
                    // Bạn có thể chọn ném RuntimeException ở đây nếu muốn dừng ứng dụng hoàn toàn
                }
            }
        }
    }
}
