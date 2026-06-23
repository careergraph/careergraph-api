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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Weekly digest scheduler.
 *
 * Flow:
 * 1. buildQueue() runs before the send window and prepares matched jobs per candidate.
 * 2. sendDailyDigest() sends weekly digest emails in small user batches.
 * 3. Successfully sent items are written to history and removed from the queue.
 * 4. Newly posted jobs are cleared after the digest window finishes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyDigestScheduler {

    private static final int DIGEST_BATCH_SIZE = 50;
    private static final int MAX_JOBS_PER_EMAIL = 5;

    @Value("${application.web.base-url:http://localhost:5000}")
    private String webBaseUrl;

    @Value("${application.schedule.digest.enabled:true}")
    private boolean digestEnabled;

    private final JobNotificationQueueRepository queueRepo;
    private final JobRepository jobRepo;
    private final CandidateRepository candidateRepo;
    private final JobNotificationHistoryRepository historyRepo;
    private final NewlyPostedJobRepository newlyPostedJobRepo;
    private final MailService mailService;
    private final JobRecommendationService recommendService;
    private final CompanyAccessPolicyService companyAccessPolicyService;

    /**
     * Build the queue before the weekly digest send window.
     * Default schedule: 07:00 every Monday.
     */
    @Scheduled(cron = "${application.schedule.digest.build-cron:0 0 7 ? * MON}")
    @Transactional
    public void buildQueue() {
        if (!digestEnabled) {
            log.info("Skipping buildQueue because digest scheduling is disabled");
            return;
        }

        log.info("Starting buildQueue - finding matching jobs for candidates");

        List<String> newlyPostedJobIds = newlyPostedJobRepo.findAllJobIds();
        if (newlyPostedJobIds.isEmpty()) {
            log.info("No newly posted jobs to process");
            return;
        }
        log.info("Found {} newly posted jobs", newlyPostedJobIds.size());

        List<Candidate> candidates = candidateRepo.findAllByIsOpenToNotifyNewJob(true);
        log.info("Found {} candidates with notifications enabled", candidates.size());

        for (Candidate candidate : candidates) {
            try {
                recommendService.recommendJobsForCandidate(candidate, MAX_JOBS_PER_EMAIL);
            } catch (Exception exception) {
                log.error("Error recommending jobs for candidate {}: {}",
                        candidate.getId(),
                        exception.getMessage());
            }
        }

        log.info("buildQueue completed");
    }

    /**
     * Send weekly digest emails.
     * Default schedule: 08:00 every Monday.
     */
    @Scheduled(cron = "${application.schedule.digest.send-cron:0 0 8 ? * MON}")
    @Transactional
    public void sendDailyDigest() {
        if (!digestEnabled) {
            log.info("Skipping sendDailyDigest because digest scheduling is disabled");
            return;
        }

        log.info("Starting sendDailyDigest");

        List<JobNotificationQueue> queues = queueRepo.findBySendTypeAndStatusSend(
                SendType.DAILY,
                StatusSend.PENDING);

        if (queues.isEmpty()) {
            log.info("No pending jobs in queue to send");
            clearNewlyPostedJobs();
            return;
        }

        Map<String, List<JobNotificationQueue>> byUser = queues.stream()
                .collect(Collectors.groupingBy(JobNotificationQueue::getUserId));

        List<Map.Entry<String, List<JobNotificationQueue>>> userEntries = new ArrayList<>(byUser.entrySet());
        List<List<Map.Entry<String, List<JobNotificationQueue>>>> batches = partition(userEntries, DIGEST_BATCH_SIZE);

        int successCount = 0;
        int skippedOrFailedCount = 0;

        log.info("Processing {} users with pending jobs in {} batches", userEntries.size(), batches.size());

        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            List<Map.Entry<String, List<JobNotificationQueue>>> batch = batches.get(batchIndex);
            log.info("Processing digest batch {}/{} with {} users", batchIndex + 1, batches.size(), batch.size());

            for (Map.Entry<String, List<JobNotificationQueue>> entry : batch) {
                boolean sent = processUserDigest(entry.getKey(), entry.getValue());
                if (sent) {
                    successCount++;
                } else {
                    skippedOrFailedCount++;
                }
            }
        }

        log.info("Digest sending summary: {} successful users, {} skipped or failed users",
                successCount,
                skippedOrFailedCount);

        clearNewlyPostedJobs();
        log.info("sendDailyDigest completed");
    }

    private boolean processUserDigest(String userId, List<JobNotificationQueue> items) {
        Candidate user = candidateRepo.findById(userId).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsOpenToNotifyNewJob())) {
            return false;
        }

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
                .limit(MAX_JOBS_PER_EMAIL)
                .toList();

        List<Job> jobs = itemsToSend.stream()
                .map(queueItem -> jobsById.get(queueItem.getJobId()))
                .filter(Objects::nonNull)
                .filter(companyAccessPolicyService::isJobPubliclyAvailable)
                .toList();

        if (jobs.isEmpty()) {
            return false;
        }

        try {
            mailService.sendHtmlSync(
                    user.getAccount().getEmail(),
                    "Weekly job digest for you",
                    JobMailTemplateBuilder.build(jobs, webBaseUrl));

            LocalDateTime sentAt = LocalDateTime.now();
            itemsToSend.forEach(queueItem -> {
                queueItem.setStatusSend(StatusSend.SENT);
                historyRepo.save(JobNotificationHistory.builder()
                        .userId(queueItem.getUserId())
                        .jobId(queueItem.getJobId())
                        .sentAt(sentAt)
                        .sendType(queueItem.getSendType())
                        .build());
            });

            queueRepo.deleteAll(itemsToSend);
            log.info("Sent {} jobs to {}", jobs.size(), user.getAccount().getEmail());
            return true;
        } catch (Exception exception) {
            log.error("Failed to send email to {}: {}", user.getAccount().getEmail(), exception.getMessage());
            return false;
        }
    }

    private void clearNewlyPostedJobs() {
        long count = newlyPostedJobRepo.count();
        if (count > 0) {
            newlyPostedJobRepo.deleteAll();
            log.info("Cleared {} newly posted jobs", count);
        }
    }

    private <T> List<List<T>> partition(List<T> source, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int start = 0; start < source.size(); start += batchSize) {
            int end = Math.min(start + batchSize, source.size());
            batches.add(source.subList(start, end));
        }
        return batches;
    }
}
