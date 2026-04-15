package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.message.MessageContentType;
import com.hcmute.careergraph.enums.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

public class MessagingResponses {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PartySummaryDto {
    private String id;
    private String displayName;
    private String avatar;
    private String partyType;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ApplicationSummaryDto {
    private String id;
    private String jobId;
    private String jobTitle;
    private String currentStage;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ThreadSummaryDto {
    private String threadId;
    private PartySummaryDto otherParty;
    private ApplicationSummaryDto application;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
    private boolean online;
    private boolean archived;
    private boolean blocked;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MessageDto {
    private String id;
    private String threadId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private MessageContentType contentType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private boolean deleted;
    private LocalDateTime createdAt;
    private boolean read;
    private LocalDateTime readAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NotificationDto {
    private String id;
    private NotificationType type;
    private String title;
    private String body;
    private HashMap<String, Object> data;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NotificationPageDto {
    private List<NotificationDto> notifications;
    private long totalUnread;
    private boolean hasMore;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BlockStatusDto {
    private boolean blocked;
    private LocalDateTime blockedAt;
    private String reason;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BlockedUserDto {
    private String userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private LocalDateTime blockedAt;
    private String reason;
  }
}
