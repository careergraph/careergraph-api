package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.exception.ForbiddenException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.dtos.request.MessagingRequests;
import com.hcmute.careergraph.persistence.dtos.response.MessagingResponses;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.services.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("messages")
@RequiredArgsConstructor
public class MessageController {

  private final MessageService messageService;
  private final SecurityUtils securityUtils;

  @GetMapping("/threads")
  public RestResponse<Page<MessagingResponses.ThreadSummaryDto>> getThreads(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "false") boolean archived) {
    Page<MessagingResponses.ThreadSummaryDto> result = messageService.getThreads(
        getCurrentAccount(),
        archived,
        PageRequest.of(page, size));

    return RestResponse.<Page<MessagingResponses.ThreadSummaryDto>>builder()
        .status(HttpStatus.OK)
        .message("Threads retrieved successfully")
        .data(result)
        .build();
  }

  @PostMapping("/threads")
  public RestResponse<MessagingResponses.ThreadSummaryDto> getOrCreateThread(
      @Valid @RequestBody MessagingRequests.CreateThreadRequest request) {
    MessagingResponses.ThreadSummaryDto result = messageService.getOrCreateThread(getCurrentAccount(), request);

    return RestResponse.<MessagingResponses.ThreadSummaryDto>builder()
        .status(HttpStatus.OK)
        .message("Thread prepared successfully")
        .data(result)
        .build();
  }

  @GetMapping("/threads/{threadId}")
  public RestResponse<MessagingResponses.ThreadSummaryDto> getThread(@PathVariable String threadId) {
    MessagingResponses.ThreadSummaryDto result = messageService.getThread(getCurrentAccount(), threadId);

    return RestResponse.<MessagingResponses.ThreadSummaryDto>builder()
        .status(HttpStatus.OK)
        .message("Thread retrieved successfully")
        .data(result)
        .build();
  }

  @GetMapping("/threads/{threadId}/messages")
  public RestResponse<Page<MessagingResponses.MessageDto>> getMessages(
      @PathVariable String threadId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size) {
    Page<MessagingResponses.MessageDto> result = messageService.getMessages(
        getCurrentAccount(),
        threadId,
        PageRequest.of(page, size));

    return RestResponse.<Page<MessagingResponses.MessageDto>>builder()
        .status(HttpStatus.OK)
        .message("Messages retrieved successfully")
        .data(result)
        .build();
  }

  @PostMapping("/threads/{threadId}/messages")
  public RestResponse<MessagingResponses.MessageDto> sendMessage(
      @PathVariable String threadId,
      @Valid @RequestBody MessagingRequests.SendMessageRequest request) {
    MessagingResponses.MessageDto result = messageService.sendMessage(getCurrentAccount(), threadId, request);

    return RestResponse.<MessagingResponses.MessageDto>builder()
        .status(HttpStatus.CREATED)
        .message("Message sent successfully")
        .data(result)
        .build();
  }

  @PostMapping("/threads/{threadId}/read")
  public RestResponse<Void> markAsRead(@PathVariable String threadId) {
    messageService.markThreadAsRead(getCurrentAccount(), threadId);

    return RestResponse.<Void>builder()
        .status(HttpStatus.OK)
        .message("Thread marked as read")
        .build();
  }

  @DeleteMapping("/{messageId}")
  public RestResponse<Void> deleteMessage(@PathVariable String messageId) {
    messageService.unsendMessage(getCurrentAccount(), messageId);

    return RestResponse.<Void>builder()
        .status(HttpStatus.OK)
        .message("Message deleted successfully")
        .build();
  }

  @DeleteMapping("/{messageId}/unsend")
  public RestResponse<Void> unsendMessage(@PathVariable String messageId) {
    messageService.unsendMessage(getCurrentAccount(), messageId);

    return RestResponse.<Void>builder()
        .status(HttpStatus.OK)
        .message("Message unsent successfully")
        .build();
  }

  @DeleteMapping("/threads/{threadId}")
  public RestResponse<Void> deleteThread(@PathVariable String threadId) {
    messageService.deleteThreadForMe(getCurrentAccount(), threadId);

    return RestResponse.<Void>builder()
        .status(HttpStatus.OK)
        .message("Thread deleted successfully")
        .build();
  }

  @PostMapping("/threads/{threadId}/archive")
  public RestResponse<Void> archiveThread(@PathVariable String threadId) {
    messageService.archiveThread(getCurrentAccount(), threadId, true);

    return RestResponse.<Void>builder()
        .status(HttpStatus.OK)
        .message("Thread archived successfully")
        .build();
  }

  @PostMapping("/threads/{threadId}/unarchive")
  public RestResponse<Void> unarchiveThread(@PathVariable String threadId) {
    messageService.archiveThread(getCurrentAccount(), threadId, false);

    return RestResponse.<Void>builder()
        .status(HttpStatus.OK)
        .message("Thread unarchived successfully")
        .build();
  }

  @GetMapping("/unread-count")
  public RestResponse<HashMap<String, Long>> getUnreadCount() {
    long count = messageService.getTotalUnread(getCurrentAccount());
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
