package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.persistence.dtos.response.MessagingResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocketNotificationPusher {

  private final WebClient webClient;

  @Value("${socket.server.url:http://localhost:4000}")
  private String socketServerUrl;

  @Value("${socket.internal.api-key:dev-secret-change-in-prod}")
  private String internalApiKey;

  @Async("taskExecutor")
  public void pushToUser(UUID userId, MessagingResponses.NotificationDto notification) {
    if (userId == null || notification == null) {
      return;
    }

    Map<String, Object> payload = new HashMap<>();
    payload.put("userId", userId.toString());
    payload.put("notification", notification);

    webClient.post()
        .uri(socketServerUrl + "/internal/notify")
        .header("x-internal-api-key", internalApiKey)
        .bodyValue(payload)
        .retrieve()
        .toBodilessEntity()
        .doOnSuccess(response -> log.info(
            "Pushed notification type={} id={} to user={}",
            notification.getType(),
            notification.getId(),
            userId))
        .doOnError(error -> log.warn("Failed to push notification {} to user {}: {}",
            notification.getType(),
            userId,
            error.getMessage()))
        .subscribe();
  }

  @Async("taskExecutor")
  public void pushUnreadCounts(UUID userId, long messages, long notifications) {
    if (userId == null) {
      return;
    }

    Map<String, Object> payload = new HashMap<>();
    payload.put("userId", userId.toString());
    payload.put("messages", messages);
    payload.put("notifications", notifications);

    webClient.post()
        .uri(socketServerUrl + "/internal/unread-counts")
        .header("x-internal-api-key", internalApiKey)
        .bodyValue(payload)
        .retrieve()
        .toBodilessEntity()
        .doOnSuccess(response -> log.info(
            "Pushed unread counts to user={} notifications={} messages={}",
            userId,
            notifications,
            messages))
        .doOnError(error -> log.warn("Failed to push unread counts to user {}: {}",
            userId,
            error.getMessage()))
        .subscribe();
  }
}
