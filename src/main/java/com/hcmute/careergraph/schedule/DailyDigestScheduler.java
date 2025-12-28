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
import com.hcmute.careergraph.services.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DailyDigestScheduler {

    private final JobNotificationQueueRepository queueRepo;
    private final JobRepository jobRepo;
    private final CandidateRepository candidateRepo;
    private final JobNotificationHistoryRepository historyRepo;
    private final MailService mailService;

//    @Scheduled(cron = "0 0 8 * * *")
    @Scheduled(cron = "0 */2 * * * ?")
    @Transactional
    public void sendDailyDigest() {
        System.out.println("chạy schedule");
        var queues = queueRepo.findBySendTypeAndStatusSend(
                SendType.DAILY,
                StatusSend.PENDING
        );

        Map<String, List<JobNotificationQueue>> byUser =
                queues.stream().collect(Collectors.groupingBy(JobNotificationQueue::getUserId));

        byUser.forEach((userId, items) -> {
            Candidate user = candidateRepo.findById(userId).orElse(null);
            if (user == null || !user.getIsOpenToNotifyNewJob() ) return;

            List<Job> jobs = items.stream()
                    .map(q -> jobRepo.findById(q.getJobId()).orElse(null))
                    .filter(Objects::nonNull)
                    .limit(5)
                    .toList();

            if (jobs.isEmpty()) return;

            mailService.sendHtml(
                    user.getAccount().getEmail(),
                    "🔥 Việc làm mới dành cho bạn",
                    JobMailTemplateBuilder.build(jobs,"http://localhost:5000")
            );

            items.forEach(q -> {
                q.setStatusSend(StatusSend.SENT);
                historyRepo.save(JobNotificationHistory.builder()
                        .userId(q.getUserId())
                        .jobId(q.getJobId())
                        .sentAt(LocalDateTime.now())
                        .sendType(q.getSendType())
                        .build());
            });

            queueRepo.deleteAll(items);
        });
    }
}
