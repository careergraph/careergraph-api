package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.exception.ForbiddenException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.dtos.response.MessagingResponses;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;
  private final SecurityUtils securityUtils;

  @GetMapping
  public RestResponse<MessagingResponses.NotificationPageDto> getNotifications(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    MessagingResponses.NotificationPageDto result = notificationService.getNotifications(
        getCurrentAccount(),
        page,
        size);

    return RestResponse.<MessagingResponses.NotificationPageDto>builder()
        .status(HttpStatus.OK)
        .message("Notifications retrieved successfully")
        .data(result)
        .build();
  }

  @PostMapping("/{id}/read")
  public RestResponse<Void> markAsRead(@PathVariable String id) {
    notificationService.markAsRead(getCurrentAccount(), id);

    return RestResponse.<Void>builder()
        .status(HttpStatus.OK)
        .message("Notification marked as read")
        .build();
  }

  @PostMapping("/read-all")
  public RestResponse<Void> markAllAsRead() {
    notificationService.markAllAsRead(getCurrentAccount());

    return RestResponse.<Void>builder()
        .status(HttpStatus.OK)
        .message("All notifications marked as read")
        .build();
  }

  @GetMapping("/unread-count")
  public RestResponse<HashMap<String, Long>> getUnreadCount() {
    long count = notificationService.getUnreadCount(getCurrentAccount());
    HashMap<String, Long> data = new HashMap<>();
    data.put("count", count);

    return RestResponse.<HashMap<String, Long>>builder()
        .status(HttpStatus.OK)
        .message("Unread count retrieved successfully")
        .data(data)
        .build();
  }

  private Account getCurrentAccount() {
    return securityUtils.getCurrentAccount()
        .orElseThrow(() -> new ForbiddenException("Authentication required"));
  }
}
