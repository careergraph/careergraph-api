package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.persistence.dtos.response.MessagingResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocketNotificationPusher {
  private static final String NOTIFY_PATH = "/internal/notify";
  private static final String UNREAD_COUNTS_PATH = "/internal/unread-counts";

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
        .uri(socketServerUrl + NOTIFY_PATH)
        .header("x-internal-api-key", internalApiKey)
        .bodyValue(payload)
        .retrieve()
        .toBodilessEntity()
        .doOnSuccess(response -> log.info(
            "Pushed notification type={} id={} to user={}",
            notification.getType(),
            notification.getId(),
            userId))
        .onErrorResume(error -> {
          log.warn(
              "Socket push skipped endpoint={} user={} type={} notificationId={} reason={}",
              NOTIFY_PATH,
              userId,
              notification.getType(),
              notification.getId(),
              summarizeError(error));
          return Mono.empty();
        })
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
        .uri(socketServerUrl + UNREAD_COUNTS_PATH)
        .header("x-internal-api-key", internalApiKey)
        .bodyValue(payload)
        .retrieve()
        .toBodilessEntity()
        .doOnSuccess(response -> log.info(
            "Pushed unread counts to user={} notifications={} messages={}",
            userId,
            notifications,
            messages))
        .onErrorResume(error -> {
          log.warn(
              "Socket push skipped endpoint={} user={} notifications={} messages={} reason={}",
              UNREAD_COUNTS_PATH,
              userId,
              notifications,
              messages,
              summarizeError(error));
          return Mono.empty();
        })
        .subscribe();
  }

  private String summarizeError(Throwable error) {
    Throwable root = error;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }

    String message = root.getMessage();
    if (message == null || message.isBlank()) {
      message = error.getMessage();
    }

    return message != null && !message.isBlank()
        ? message
        : error.getClass().getSimpleName();
  }
}
