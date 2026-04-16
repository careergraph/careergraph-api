package com.hcmute.careergraph.schedule;

import com.hcmute.careergraph.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupJob {

  private final NotificationRepository notificationRepository;

  @Value("${notification.cleanup.days-to-keep:90}")
  private int daysToKeep;

  @Scheduled(cron = "0 0 2 * * ?")
  @Transactional
  public void cleanupOldNotifications() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
    int deleted = notificationRepository.deleteByCreatedDateBefore(cutoff);
    log.info("Cleaned up {} old notifications older than {} days", deleted, daysToKeep);
  }
}