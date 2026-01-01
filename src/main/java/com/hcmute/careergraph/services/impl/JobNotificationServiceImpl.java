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

        Job job = jobRepo.findById(event.jobId()).orElse(null);
        if (job == null) return;
        List<Candidate> candidates =
                candidateRepo.findAll();

        for (Candidate c : candidates) {
            try {
                boolean sent =
                        historyRepo.existsByJobIdAndUserId(event.jobId(), c.getId());
                if (sent) return;
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
}
