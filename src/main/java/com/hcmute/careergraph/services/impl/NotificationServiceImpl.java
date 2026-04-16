package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.enums.message.MessageContentType;
import com.hcmute.careergraph.enums.notification.NotificationType;
import com.hcmute.careergraph.exception.ForbiddenException;
import com.hcmute.careergraph.exception.NotFoundException;
import com.hcmute.careergraph.persistence.dtos.response.MessagingResponses;
import com.hcmute.careergraph.persistence.models.*;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.repositories.MessageRepository;
import com.hcmute.careergraph.repositories.NotificationRepository;
import com.hcmute.careergraph.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final AccountRepository accountRepository;
  private final MessageRepository messageRepository;

  @Override
  public Notification createNotification(String recipientAccountId,
      NotificationType type,
      String title,
      String body,
      HashMap<String, Object> data) {
    Account recipient = accountRepository.findById(recipientAccountId)
        .orElseThrow(() -> new NotFoundException("Recipient account not found"));

    Notification notification = Notification.builder()
        .recipient(recipient)
        .type(type)
        .title(title)
        .body(body)
        .data(data != null ? new HashMap<>(data) : new HashMap<>())
        .read(false)
        .build();

    return notificationRepository.save(notification);
  }

  @Override
  public void createBulkNotifications(List<String> recipientAccountIds,
      NotificationType type,
      String title,
      String body,
      HashMap<String, Object> data) {
    if (recipientAccountIds == null || recipientAccountIds.isEmpty()) {
      return;
    }
    for (String recipientId : recipientAccountIds) {
      if (!StringUtils.hasText(recipientId)) {
        continue;
      }
      try {
        createNotification(recipientId, type, title, body, data);
      } catch (Exception ex) {
        log.warn("Skip notification for recipient {} due to error: {}", recipientId, ex.getMessage());
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public MessagingResponses.NotificationPageDto getNotifications(Account currentAccount, int page, int size) {
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(100, size));
    Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdDate"));

    Page<Notification> notificationsPage = notificationRepository
        .findByRecipientIdOrderByCreatedDateDesc(currentAccount.getId(), pageable);

    long totalUnread = notificationRepository.countByRecipientIdAndReadFalse(currentAccount.getId());

    return MessagingResponses.NotificationPageDto.builder()
        .notifications(notificationsPage.getContent().stream().map(this::toDto).toList())
        .totalUnread(totalUnread)
        .hasMore(notificationsPage.hasNext())
        .build();
  }

  @Override
  public void markAsRead(Account currentAccount, String notificationId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new NotFoundException("Notification not found"));

    if (!notification.getRecipient().getId().equals(currentAccount.getId())) {
      throw new ForbiddenException("Cannot access this notification");
    }

    if (!notification.isRead()) {
      notification.setRead(true);
      notification.setReadAt(LocalDateTime.now());
      notificationRepository.save(notification);
    }
  }

  @Override
  public void markAllAsRead(Account currentAccount) {
    notificationRepository.markAllAsRead(currentAccount.getId(), LocalDateTime.now());
  }

  @Override
  @Transactional(readOnly = true)
  public long getUnreadCount(Account currentAccount) {
    return notificationRepository.countByRecipientIdAndReadFalse(currentAccount.getId());
  }

  @Override
  public void onApplicationStatusChanged(Application application,
      ApplicationStage oldStage,
      ApplicationStage newStage,
      Account changedBy) {
    if (application == null || application.getCandidate() == null) {
      return;
    }

    Optional<Account> candidateAccountOpt = accountRepository.findByCandidateId(application.getCandidate().getId());
    if (candidateAccountOpt.isEmpty()) {
      log.warn("Candidate account not found for application {}", application.getId());
      return;
    }

    String companyName = Optional.ofNullable(application.getJob())
        .map(Job::getCompany)
        .map(this::resolveCompanyName)
        .orElse("Company");
    String jobTitle = Optional.ofNullable(application.getJob())
        .map(Job::getTitle)
        .orElse("the job");

    HashMap<String, Object> data = new HashMap<>();
    data.put("applicationId", application.getId());
    if (application.getJob() != null) {
      data.put("jobId", application.getJob().getId());
    }
    data.put("oldStage", oldStage != null ? oldStage.name() : null);
    data.put("newStage", newStage != null ? newStage.name() : null);
    data.put("navigateTo", "/applications/" + application.getId());

    String title = "Application stage updated";
    String body = String.format("Your application for %s at %s is now %s",
        jobTitle,
        companyName,
        newStage != null ? newStage.name() : "UPDATED");

    if (newStage == ApplicationStage.INTERVIEW_SCHEDULED) {
      title = "Interview scheduled";
      body = String.format("%s scheduled an interview for %s", companyName, jobTitle);
    } else if (newStage == ApplicationStage.REJECTED) {
      title = "Application update";
      body = String.format("Your application for %s at %s was marked as %s",
          jobTitle,
          companyName,
          newStage.name());
    }

    createNotification(candidateAccountOpt.get().getId(),
        NotificationType.APPLICATION_STATUS_CHANGED,
        title,
        body,
        data);
  }

  @Override
  public void onNewApplication(Application application) {
    if (application == null || application.getJob() == null || application.getJob().getCompany() == null) {
      return;
    }

    Optional<Account> companyAccountOpt = accountRepository.findByCompanyId(application.getJob().getCompany().getId());
    if (companyAccountOpt.isEmpty()) {
      log.warn("Company account not found for company {}", application.getJob().getCompany().getId());
      return;
    }

    String candidateName = Optional.ofNullable(application.getCandidate())
        .map(this::resolveCandidateName)
        .orElse("A candidate");
    String jobTitle = Optional.ofNullable(application.getJob().getTitle()).orElse("your job");

    HashMap<String, Object> data = new HashMap<>();
    data.put("applicationId", application.getId());
    data.put("jobId", application.getJob().getId());
    data.put("candidateId", application.getCandidate() != null ? application.getCandidate().getId() : null);
    data.put("navigateTo", "/jobs/" + application.getJob().getId() + "/applications");

    createNotification(
        companyAccountOpt.get().getId(),
        NotificationType.NEW_APPLICATION,
        "New application received",
        candidateName + " applied for " + jobTitle,
        data);
  }

  @Override
  public void onNewMessage(Message message, MessageThread thread) {
    if (message == null || thread == null || message.getSender() == null) {
      return;
    }

    Optional<Account> candidateAccountOpt = accountRepository.findByCandidateId(thread.getCandidate().getId());
    Optional<Account> companyAccountOpt = accountRepository.findByCompanyId(thread.getCompany().getId());

    if (candidateAccountOpt.isEmpty() || companyAccountOpt.isEmpty()) {
      return;
    }

    Account sender = message.getSender();
    Account candidateAccount = candidateAccountOpt.get();
    Account companyAccount = companyAccountOpt.get();

    Account recipient = sender.getId().equals(candidateAccount.getId()) ? companyAccount : candidateAccount;
    if (recipient.getId().equals(sender.getId())) {
      return;
    }

    String senderName = resolveAccountDisplayName(sender);
    String preview = message.getContentType() == MessageContentType.TEXT
        ? truncate(message.getContent(), 80)
        : "[Attachment]";

    HashMap<String, Object> data = new HashMap<>();
    data.put("threadId", thread.getId());
    data.put("messageId", message.getId());
    data.put("senderId", sender.getId());
    data.put("navigateTo", "/messages?thread=" + thread.getId());

    createNotification(
        recipient.getId(),
        NotificationType.NEW_MESSAGE,
        senderName + " sent a message",
        preview,
        data);
  }

  private MessagingResponses.NotificationDto toDto(Notification notification) {
    return MessagingResponses.NotificationDto.builder()
        .id(notification.getId())
        .type(notification.getType())
        .title(notification.getTitle())
        .body(notification.getBody())
        .data(notification.getData() != null ? new HashMap<>(notification.getData()) : new HashMap<>())
        .read(notification.isRead())
        .createdAt(notification.getCreatedDate())
        .readAt(notification.getReadAt())
        .build();
  }

  private String resolveAccountDisplayName(Account account) {
    if (account == null) {
      return "User";
    }
    if (account.getCandidate() != null) {
      return resolveCandidateName(account.getCandidate());
    }
    if (account.getCompany() != null) {
      return resolveCompanyName(account.getCompany());
    }
    return StringUtils.hasText(account.getEmail()) ? account.getEmail() : "User";
  }

  private String resolveCandidateName(Candidate candidate) {
    if (candidate == null) {
      return "Candidate";
    }
    String firstName = StringUtils.hasText(candidate.getFirstName()) ? candidate.getFirstName().trim() : "";
    String lastName = StringUtils.hasText(candidate.getLastName()) ? candidate.getLastName().trim() : "";
    String fullName = (firstName + " " + lastName).trim();
    if (StringUtils.hasText(fullName)) {
      return fullName;
    }
    if (StringUtils.hasText(candidate.getTagname())) {
      return candidate.getTagname();
    }
    return "Candidate";
  }

  private String resolveCompanyName(Company company) {
    if (company == null) {
      return "Company";
    }
    if (StringUtils.hasText(company.getName())) {
      return company.getName().trim();
    }
    if (StringUtils.hasText(company.getTagname())) {
      return company.getTagname().trim();
    }
    return "Company";
  }

  private String truncate(String value, int maxLength) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    String normalized = value.trim();
    if (normalized.length() <= maxLength) {
      return normalized;
    }
    return normalized.substring(0, maxLength) + "...";
  }
}
