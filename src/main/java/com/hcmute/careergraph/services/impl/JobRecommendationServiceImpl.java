package com.hcmute.careergraph.services.impl;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.hcmute.careergraph.enums.job.SendType;
import com.hcmute.careergraph.enums.job.StatusSend;
import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.persistence.models.JobNotificationHistory;
import com.hcmute.careergraph.persistence.models.JobNotificationQueue;
import com.hcmute.careergraph.repositories.JobNotificationHistoryRepository;
import com.hcmute.careergraph.repositories.JobNotificationQueueRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.JobESService;
import com.hcmute.careergraph.services.JobRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobRecommendationServiceImpl implements JobRecommendationService {

    private final JobESService jobESService;
    private final JobRepository jobRepository;
    private final JobNotificationQueueRepository queueRepo;
    private final JobNotificationHistoryRepository historyRepo;
    private static final int FETCH_MULTIPLIER = 5;
    @Override
    public void recommendJobsForCandidate(Candidate candidate, int limit) {
        String keyword = genKeyword(candidate);
        int fetchSize = limit * FETCH_MULTIPLIER;

        var response = jobESService.searchJobsByNavtiveAndFuzzy(
                keyword,
                PageRequest.of(0, fetchSize)
        );

        if (response == null || response.hits().hits().isEmpty())
            return;

        for (var hit : response.hits().hits()) {
            if (limit <= 0) break;

            String jobId = hit.id();

            boolean sent =
                    historyRepo.existsByUserIdAndJobIdAndSendType(
                            candidate.getId(), jobId, SendType.DAILY
                    );

            boolean queued =
                    queueRepo.existsByUserIdAndJobIdAndSendType(
                            candidate.getId(), jobId, SendType.DAILY
                    );

            if (!sent && !queued) {
                queueRepo.save(
                        JobNotificationQueue.builder()
                                .userId(candidate.getId())
                                .jobId(jobId)
                                .sendType(SendType.DAILY)
                                .statusSend(StatusSend.PENDING)
                                .build()
                );
                limit--;
            }
        }
    }

    private String genKeyword(Candidate c) {
        return String.join(" ",
                c.getDesiredPosition(),
                c.getIndustry(),
                c.getLocations().toString()
        );
    }

}
