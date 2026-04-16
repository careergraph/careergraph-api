package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.enums.notification.NotificationType;
import com.hcmute.careergraph.persistence.dtos.response.MessagingResponses;
import com.hcmute.careergraph.persistence.models.*;

import java.util.HashMap;
import java.util.List;

public interface NotificationService {

  Notification createNotification(String recipientAccountId,
      NotificationType type,
      String title,
      String body,
      HashMap<String, Object> data);

  void createBulkNotifications(List<String> recipientAccountIds,
      NotificationType type,
      String title,
      String body,
      HashMap<String, Object> data);

  MessagingResponses.NotificationPageDto getNotifications(Account currentAccount, int page, int size);

  void markAsRead(Account currentAccount, String notificationId);

  void markAllAsRead(Account currentAccount);

  long getUnreadCount(Account currentAccount);

  void onApplicationStatusChanged(Application application,
      ApplicationStage oldStage,
      ApplicationStage newStage,
      Account changedBy);

  void onNewApplication(Application application);

  void onNewMessage(Message message, MessageThread thread);
}
