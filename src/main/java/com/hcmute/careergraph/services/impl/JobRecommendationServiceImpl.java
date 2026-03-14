package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.job.SendType;
import com.hcmute.careergraph.enums.job.StatusSend;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.JobNotificationQueue;
import com.hcmute.careergraph.repositories.JobNotificationHistoryRepository;
import com.hcmute.careergraph.repositories.JobNotificationQueueRepository;
import com.hcmute.careergraph.repositories.NewlyPostedJobRepository;
import com.hcmute.careergraph.services.JobESService;
import com.hcmute.careergraph.services.JobRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service gợi ý job cho candidate dựa trên Elasticsearch.
 * 
 * Flow:
 * 1. Lấy danh sách job mới đăng từ NewlyPostedJob
 * 2. Với mỗi candidate: genKeyword() → query ES với filter IN newlyPostedJobIds
 * 3. Chọn top 5 job chưa gửi → đưa vào Queue
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobRecommendationServiceImpl implements JobRecommendationService {

        private final JobESService jobESService;
        private final JobNotificationQueueRepository queueRepo;
        private final JobNotificationHistoryRepository historyRepo;
        private final NewlyPostedJobRepository newlyPostedJobRepo;

        private static final int FETCH_SIZE = 10; // Lấy top 10 job match nhất

        @Override
        public void recommendJobsForCandidate(Candidate candidate, int limit) {
                if (!Boolean.TRUE.equals(candidate.getIsOpenToNotifyNewJob())) {
                        return;
                }

                String keyword = genKeyword(candidate);
                if (keyword == null || keyword.isBlank()) {
                        // log.warn("Candidate {} has no profile data for recommendation", candidate.getId());
                        return;
                }

                // 1. Lấy danh sách job mới đăng
                List<String> newlyPostedJobIds = newlyPostedJobRepo.findAllJobIds();
                if (newlyPostedJobIds.isEmpty()) {
                        // log.debug("No newly posted jobs available");
                        return;
                }

                // 2. Lấy danh sách job đã gửi để exclude
                List<String> sentJobIds = historyRepo.findJobIdsByUserIdAndSendType(
                                candidate.getId(), SendType.DAILY);

                // 3. Query ES: tìm top 10 job match nhất từ danh sách job mới đăng
                var response = jobESService.searchRecommendJobsFromNewlyPosted(
                                keyword,
                                PageRequest.of(0, FETCH_SIZE),
                                newlyPostedJobIds,
                                sentJobIds);

                if (response == null || response.hits().hits().isEmpty()) {
                        log.debug("No matching jobs found for candidate {}", candidate.getId());
                        return;
                }

                // 4. Chọn 5 job chưa gửi và đưa vào queue
                int addedCount = 0;
                for (var hit : response.hits().hits()) {
                        if (addedCount >= limit)
                                break;

                        String jobId = hit.id();

                        // Double-check: job chưa trong queue
                        boolean alreadyQueued = queueRepo.existsByUserIdAndJobIdAndSendType(
                                        candidate.getId(), jobId, SendType.DAILY);

                        if (!alreadyQueued) {
                                try {
                                        queueRepo.save(
                                                        JobNotificationQueue.builder()
                                                                        .userId(candidate.getId())
                                                                        .jobId(jobId)
                                                                        .sendType(SendType.DAILY)
                                                                        .statusSend(StatusSend.PENDING)
                                                                        .createdDate(LocalDateTime.now())
                                                                        .build());
                                        addedCount++;
                                        log.debug("Added job {} to queue for candidate {} (score: {})",
                                                        jobId, candidate.getId(), hit.score());
                                } catch (Exception e) {
                                        log.trace("Job {} already in queue for candidate {}", jobId, candidate.getId());
                                }
                        }
                }

                log.info("Added {} jobs to queue for candidate {}", addedCount, candidate.getId());
        }

        private String genKeyword(Candidate c) {
                StringBuilder sb = new StringBuilder();

                if (c.getDesiredPosition() != null && !c.getDesiredPosition().isBlank()) {
                        sb.append(c.getDesiredPosition()).append(" ");
                }
                if (c.getIndustry() != null && !c.getIndustry().isBlank()) {
                        sb.append(c.getIndustry()).append(" ");
                }
                if (c.getLocations() != null && !c.getLocations().isEmpty()) {
                        sb.append(String.join(" ", c.getLocations()));
                }

                return sb.toString().trim();
        }
}
