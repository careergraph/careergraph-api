package com.hcmute.careergraph.schedule;

import com.hcmute.careergraph.enums.job.SendType;
import com.hcmute.careergraph.enums.job.StatusSend;
import com.hcmute.careergraph.helper.JobMailTemplateBuilder;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.persistence.models.JobNotificationHistory;
import com.hcmute.careergraph.persistence.models.JobNotificationQueue;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.JobNotificationHistoryRepository;
import com.hcmute.careergraph.repositories.JobNotificationQueueRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.repositories.NewlyPostedJobRepository;
import com.hcmute.careergraph.services.CompanyAccessPolicyService;
import com.hcmute.careergraph.services.JobRecommendationService;
import com.hcmute.careergraph.services.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Scheduler cho Daily Job Digest.
 * 
 * Flow:
 * 1. buildQueue() chạy trước (ví dụ 7:00 AM):
 * - Với mỗi candidate: query ES tìm job match từ NewlyPostedJob
 * - Đưa vào JobNotificationQueue
 * 
 * 2. sendDailyDigest() chạy sau (ví dụ 8:00 AM):
 * - Gửi email cho từng candidate
 * - Lưu history
 * - Xóa khỏi queue
 * - Xóa toàn bộ NewlyPostedJob (reset cho ngày mai)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyDigestScheduler {

    private final JobNotificationQueueRepository queueRepo;
    private final JobRepository jobRepo;
    private final CandidateRepository candidateRepo;
    private final JobNotificationHistoryRepository historyRepo;
    private final NewlyPostedJobRepository newlyPostedJobRepo;
    private final MailService mailService;
    private final JobRecommendationService recommendService;
    private final CompanyAccessPolicyService companyAccessPolicyService;

    /**
     * STEP 1: Build queue - Tìm job phù hợp cho từng candidate
     * Chạy trước sendDailyDigest (ví dụ 7:00 AM)
     */
    // @Scheduled(cron = "0 0 7 * * *") // Production: 7:00 AM mỗi ngày
    // @Scheduled(cron = "0 */2 * * * *") // Test: mỗi 2 phút
    @Transactional
    public void buildQueue() {
        log.info("🔨 Starting buildQueue - Finding matching jobs for candidates...");

        List<String> newlyPostedJobIds = newlyPostedJobRepo.findAllJobIds();
        if (newlyPostedJobIds.isEmpty()) {
            log.info("No newly posted jobs to process");
            return;
        }
        log.info("Found {} newly posted jobs", newlyPostedJobIds.size());

        List<Candidate> candidates = candidateRepo.findAllByIsOpenToNotifyNewJob(true);
        log.info("Found {} candidates with notifications enabled", candidates.size());

        for (Candidate c : candidates) {
            try {
                recommendService.recommendJobsForCandidate(c, 5);
            } catch (Exception e) {
                log.error("Error recommending jobs for candidate {}: {}", c.getId(), e.getMessage());
            }
        }

        log.info("✅ buildQueue completed");
    }

    /**
     * STEP 2: Send daily digest emails
     * Chạy sau buildQueue (ví dụ 8:00 AM)
     */
    // @Scheduled(cron = "0 0 8 * * *") // Production: 8:00 AM mỗi ngày
    // @Scheduled(cron = "0 0 8 ? * MON,FRI")
    // @Scheduled(cron = "0 */3 * * * ?") // Test: mỗi 3 phút
    @Transactional
    public void sendDailyDigest() {
        log.info("📧 Starting sendDailyDigest...");

        var queues = queueRepo.findBySendTypeAndStatusSend(
                SendType.DAILY,
                StatusSend.PENDING);

        if (queues.isEmpty()) {
            log.info("No pending jobs in queue to send");
            clearNewlyPostedJobs();
            return;
        }

        Map<String, List<JobNotificationQueue>> byUser = queues.stream()
                .collect(Collectors.groupingBy(JobNotificationQueue::getUserId));

        log.info("Processing {} users with pending jobs", byUser.size());

        byUser.forEach((userId, items) -> {
            Candidate user = candidateRepo.findById(userId).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getIsOpenToNotifyNewJob()))
                return;

            Map<String, Job> jobsById = items.stream()
                    .map(JobNotificationQueue::getJobId)
                    .distinct()
                    .map(jobId -> jobRepo.findById(jobId).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Job::getId, Function.identity()));

            List<JobNotificationQueue> invalidItems = items.stream()
                    .filter(item -> !companyAccessPolicyService.isJobPubliclyAvailable(jobsById.get(item.getJobId())))
                    .toList();
            if (!invalidItems.isEmpty()) {
                queueRepo.deleteAll(invalidItems);
            }

            List<JobNotificationQueue> itemsToSend = items.stream()
                    .filter(item -> !invalidItems.contains(item))
                    .limit(5)
                    .toList();

            List<Job> jobs = itemsToSend.stream()
                    .map(q -> jobsById.get(q.getJobId()))
                    .filter(Objects::nonNull)
                    .filter(companyAccessPolicyService::isJobPubliclyAvailable)
                    .toList();

            if (jobs.isEmpty())
                return;

            try {
                mailService.sendHtml(
                        user.getAccount().getEmail(),
                        "🔥 Việc làm mới dành cho bạn",
                        JobMailTemplateBuilder.build(jobs, "http://localhost:5000"));

                // Đánh dấu và xóa những items đã gửi
                itemsToSend.forEach(q -> {
                    q.setStatusSend(StatusSend.SENT);
                    historyRepo.save(JobNotificationHistory.builder()
                            .userId(q.getUserId())
                            .jobId(q.getJobId())
                            .sentAt(LocalDateTime.now())
                            .sendType(q.getSendType())
                            .build());
                });

                queueRepo.deleteAll(itemsToSend);
                log.info("✅ Sent {} jobs to {}", jobs.size(), user.getAccount().getEmail());

            } catch (Exception e) {
                log.error("Failed to send email to {}: {}", user.getAccount().getEmail(), e.getMessage());
            }
        });

        // Sau khi gửi xong cho tất cả → Xóa toàn bộ NewlyPostedJob
        clearNewlyPostedJobs();

        log.info("📧 sendDailyDigest completed");
    }

    /**
     * Xóa toàn bộ NewlyPostedJob sau khi gửi xong
     * Để ngày mai có job mới sẽ bắt đầu fresh
     */
    private void clearNewlyPostedJobs() {
        long count = newlyPostedJobRepo.count();
        if (count > 0) {
            newlyPostedJobRepo.deleteAll();
            log.info("🗑️ Cleared {} newly posted jobs", count);
        }
    }
}



