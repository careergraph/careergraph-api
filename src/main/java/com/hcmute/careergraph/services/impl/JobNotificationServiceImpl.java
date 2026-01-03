package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.persistence.event.JobCreatedEvent;
import com.hcmute.careergraph.persistence.models.NewlyPostedJob;
import com.hcmute.careergraph.repositories.NewlyPostedJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service xử lý đánh dấu job mới đăng.
 * 
 * Flow:
 * 1. HR tạo job → JobCreatedEvent được publish
 * 2. onJobCreated() → Lưu jobId vào NewlyPostedJob (chỉ đánh dấu, KHÔNG đưa vào
 * queue)
 * 3. DailyDigestScheduler.buildQueue() → Query ES với filter từ NewlyPostedJob
 * 4. DailyDigestScheduler.sendDailyDigest() → Gửi email + xóa NewlyPostedJob
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobNotificationServiceImpl {

    private final NewlyPostedJobRepository newlyPostedJobRepository;

    /**
     * Event listener khi HR tạo job mới.
     * Chỉ đánh dấu job là "mới đăng", KHÔNG đưa vào queue.
     * Việc match với candidate sẽ do Elasticsearch xử lý trong buildQueue().
     */
    @EventListener
    @Async
    @Transactional
    public void onJobCreated(JobCreatedEvent event) {
        log.info("📌 Marking job as newly posted: {}", event.jobId());

        // Kiểm tra job đã được đánh dấu chưa
        if (newlyPostedJobRepository.existsByJobId(event.jobId())) {
            log.debug("Job {} already marked as newly posted", event.jobId());
            return;
        }

        try {
            newlyPostedJobRepository.save(NewlyPostedJob.builder()
                    .jobId(event.jobId())
                    .postedAt(LocalDateTime.now())
                    .status(Status.ACTIVE)
                    .build());
            log.info("✅ Job {} marked as newly posted", event.jobId());
        } catch (Exception e) {
            // Duplicate key hoặc lỗi khác → skip
            log.warn("Failed to mark job {} as newly posted: {}", event.jobId(), e.getMessage());
        }
    }
}
