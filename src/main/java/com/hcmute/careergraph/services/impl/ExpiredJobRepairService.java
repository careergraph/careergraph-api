package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.config.app.ElasticsearchSyncResult;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.JobESRepository;
import com.hcmute.careergraph.repositories.JobNotificationQueueRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.repositories.NewlyPostedJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiredJobRepairService {

    private final JobRepository jobRepository;
    private final JobESRepository jobESRepository;
    private final NewlyPostedJobRepository newlyPostedJobRepository;
    private final JobNotificationQueueRepository jobNotificationQueueRepository;

    @Transactional
    public ElasticsearchSyncResult repairExpiredJobs() {
        List<Job> expiredJobs = jobRepository.findExpiredActiveJobs(LocalDate.now().toString());
        if (expiredJobs.isEmpty()) {
            return new ElasticsearchSyncResult(
                    "expired-jobs",
                    true,
                    false,
                    0,
                    0,
                    0,
                    0,
                    "No expired active jobs required repair.");
        }

        List<String> expiredJobIds = expiredJobs.stream()
                .map(Job::getId)
                .toList();

        log.warn("Expired job repair detected stale public artifacts: expiredJobs={}, sampleIds={}",
                expiredJobIds.size(),
                expiredJobIds.stream().limit(5).toList());

        jobESRepository.deleteAllById(expiredJobIds);
        long removedQueueItems = jobNotificationQueueRepository.deleteByJobIdIn(expiredJobIds);
        long removedNewlyPosted = newlyPostedJobRepository.deleteByJobIdIn(expiredJobIds);

        log.info("Expired job repair cleaned {} search documents, {} queue items, {} newly-posted markers.",
                expiredJobIds.size(), removedQueueItems, removedNewlyPosted);

        return new ElasticsearchSyncResult(
                "expired-jobs",
                false,
                false,
                0,
                expiredJobIds.size(),
                0,
                0,
                "Expired job repair completed. Removed %d Elasticsearch documents, %d queue items, %d newly-posted markers."
                        .formatted(expiredJobIds.size(), removedQueueItems, removedNewlyPosted));
    }
}
