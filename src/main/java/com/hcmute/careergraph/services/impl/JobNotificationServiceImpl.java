package com.hcmute.careergraph.services.impl;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.enums.job.SendType;
import com.hcmute.careergraph.enums.job.StatusSend;
import com.hcmute.careergraph.helper.JobMailTemplateBuilder;
import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.persistence.event.JobCreatedEvent;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.persistence.models.JobNotificationHistory;
import com.hcmute.careergraph.persistence.models.JobNotificationQueue;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.JobNotificationHistoryRepository;
import com.hcmute.careergraph.repositories.JobNotificationQueueRepository;
import com.hcmute.careergraph.repositories.JobRepository;
import com.hcmute.careergraph.services.EmbedService;
import com.hcmute.careergraph.services.JobESService;
import com.hcmute.careergraph.services.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobNotificationServiceImpl {

    private final CandidateRepository candidateRepo;
    private final JobRepository jobRepo;
    private final JobNotificationHistoryRepository historyRepo;
    private final JobESService jobESService;
    private final EmbedService embedService;
    private final MailService mailService;
    private final JobNotificationQueueRepository jobNotificationQueueRepository;

    @EventListener
    @Transactional
    public void onJobCreated(JobCreatedEvent event) {

        List<Candidate> candidates =
                candidateRepo.findAll();

        for (Candidate c : candidates) {
            try {
                jobNotificationQueueRepository.save(JobNotificationQueue.builder()
                        .userId(c.getId())
                        .jobId(event.jobId())
                        .sendType(SendType.DAILY)
                        .statusSend(StatusSend.PENDING)
                        .createdDate(LocalDateTime.now())
                        .build());
            } catch (Exception ignore) {
                // duplicate → auto skip
            }
        }
    }
//    @EventListener
//    @Transactional
//    public void onJobCreated(JobCreatedEvent event) {
//
//        List<Candidate> candidates =
//                candidateRepo.findAll();
//
//        for (Candidate c : candidates) {
//
//            List<String> sent =
//                    historyRepo.findSentJobIds(c.getId());
//
//            String profile =
//                    c.getDesiredPosition() + " " +
//                            c.getIndustries() + " " +
//                            c.getLocations();
//
//            float[] vector = embedService.embed(profile);
//
//            SearchResponse<JobES> esJobs =
//                    jobESService.knnSearch(vector, 10);
//
//
//            // 1. Thu thập tất cả ID từ kết quả Elasticsearch
//            List<String> esIds = esJobs.hits().hits().stream()
//                    .map(hit -> hit.id())
//                    .toList();
//
//            // 2. Query database một lần duy nhất cho tất cả IDs
//            // Sau đó filter các điều kiện: Active, chưa gửi, và giới hạn 5 job
//            List<Job> jobs = jobRepo.findAllById(esIds).stream()
////                    .filter(j -> j.getStatus() == Status.ACTIVE)
//                    .filter(j -> !sent.contains(j.getId()))
//                    .limit(5)
//                    .toList();
//
//            if (jobs.isEmpty()) continue;
//
//            String html = JobMailTemplateBuilder.build(jobs);
//
//            mailService.sendHtml(
//                    c.getAccount().getEmail(),
//                    "🔥 Việc làm phù hợp với bạn",
//                    html
//            );
//
//            jobs.forEach(j ->
//                    historyRepo.save(
//                            JobNotificationHistory.builder()
//                                    .userId(c.getId())
//                                    .jobId(j.getId())
//                                    .sentAt(LocalDateTime.now())
//                                    .build()
//                    )
//            );
//        }
//    }
}
