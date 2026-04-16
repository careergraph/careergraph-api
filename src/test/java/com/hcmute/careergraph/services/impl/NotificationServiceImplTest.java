package com.hcmute.careergraph.services.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.enums.common.Role;
import com.hcmute.careergraph.enums.message.MessageContentType;
import com.hcmute.careergraph.enums.notification.NotificationType;
import com.hcmute.careergraph.exception.ForbiddenException;
import com.hcmute.careergraph.persistence.dtos.response.MessagingResponses;
import com.hcmute.careergraph.persistence.models.*;
import com.hcmute.careergraph.repositories.AccountRepository;
import com.hcmute.careergraph.repositories.MessageRepository;
import com.hcmute.careergraph.repositories.NotificationRepository;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceImplTest {

  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private AccountRepository accountRepository;
  @Mock
  private MessageRepository messageRepository;
  @Mock
  private SocketNotificationPusher socketNotificationPusher;

  @InjectMocks
  private NotificationServiceImpl notificationService;

  private Company company;
  private Candidate candidate;
  private Job job;
  private Application application;
  private Account companyAccount;
  private Account candidateAccount;

  @BeforeEach
  void setUp() {
    company = Company.builder()
        .id("company-1")
        .name("Acme")
        .build();

    candidate = Candidate.builder()
        .id("candidate-1")
        .firstName("Alice")
        .lastName("Tran")
        .build();

    job = Job.builder()
        .id("job-1")
        .title("Backend Engineer")
        .company(company)
        .build();

    application = Application.builder()
        .id("app-1")
        .candidate(candidate)
        .job(job)
        .currentStage(ApplicationStage.APPLIED)
        .build();

    companyAccount = Account.builder()
      .id("11111111-1111-1111-1111-111111111111")
        .role(Role.HR)
        .email("hr@acme.com")
        .company(company)
        .build();

    candidateAccount = Account.builder()
      .id("22222222-2222-2222-2222-222222222222")
        .role(Role.USER)
        .email("candidate@mail.com")
        .candidate(candidate)
        .build();

    when(notificationRepository.findByRecipientIdAndTypeAndCreatedDateGreaterThanEqualOrderByCreatedDateDesc(
      anyString(), any(NotificationType.class), any(LocalDateTime.class), any(Pageable.class)))
      .thenReturn(new PageImpl<>(java.util.List.of()));
    when(notificationRepository.findByRecipientIdAndTypeAndReadFalseAndCreatedDateGreaterThanEqualOrderByCreatedDateDesc(
      anyString(), any(NotificationType.class), any(LocalDateTime.class), any(Pageable.class)))
      .thenReturn(new PageImpl<>(java.util.List.of()));
  }

  @Test
  void onNewApplication_shouldCreateNotificationForCompanyAccount() {
    ListAppender<ILoggingEvent> appender = captureLogs();
    when(accountRepository.findByCompanyId(company.getId())).thenReturn(Optional.of(companyAccount));
    when(accountRepository.findById(companyAccount.getId())).thenReturn(Optional.of(companyAccount));
    when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
      Notification n = inv.getArgument(0, Notification.class);
      n.setId("notif-1");
      return n;
    });

    notificationService.onNewApplication(application);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    Notification saved = captor.getValue();
    assertEquals(NotificationType.NEW_APPLICATION, saved.getType());
    assertEquals("11111111-1111-1111-1111-111111111111", saved.getRecipient().getId());
    assertTrue(saved.getBody().contains("Backend Engineer"));
    verify(socketNotificationPusher).pushToUser(eq(java.util.UUID.fromString(companyAccount.getId())), any(MessagingResponses.NotificationDto.class));
    assertLogContains(appender, "Created notification NEW_APPLICATION");
    detachLogs(appender);
  }

  @Test
  void onApplicationStatusChanged_shouldCreateViewedNotificationForCandidateAccount() {
    when(accountRepository.findByCandidateId(candidate.getId())).thenReturn(Optional.of(candidateAccount));
    when(accountRepository.findById(candidateAccount.getId())).thenReturn(Optional.of(candidateAccount));
    when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
    ListAppender<ILoggingEvent> appender = captureLogs();

    notificationService.onApplicationStatusChanged(
        application,
        ApplicationStage.APPLIED,
        ApplicationStage.SCREENING,
        companyAccount);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    Notification saved = captor.getValue();
    assertEquals(NotificationType.APPLICATION_VIEWED, saved.getType());
    assertEquals("22222222-2222-2222-2222-222222222222", saved.getRecipient().getId());
    assertTrue(saved.getTitle().contains("đã được xem"));
    verify(socketNotificationPusher).pushToUser(eq(java.util.UUID.fromString(candidateAccount.getId())), any(MessagingResponses.NotificationDto.class));
    assertLogContains(appender, "Created notification APPLICATION_VIEWED");
    detachLogs(appender);
  }

  @Test
  void onApplicationStatusChanged_shouldCreateShortlistedNotificationForCandidateAccount() {
    when(accountRepository.findByCandidateId(candidate.getId())).thenReturn(Optional.of(candidateAccount));
    when(accountRepository.findById(candidateAccount.getId())).thenReturn(Optional.of(candidateAccount));
    when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
    ListAppender<ILoggingEvent> appender = captureLogs();

    notificationService.onApplicationStatusChanged(
        application,
        ApplicationStage.SCREENING,
        ApplicationStage.HR_CONTACTED,
        companyAccount);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    Notification saved = captor.getValue();
    assertEquals(NotificationType.APPLICATION_SHORTLISTED, saved.getType());
    assertTrue(saved.getBody().contains("đang được xem xét"));
    assertLogContains(appender, "Created notification APPLICATION_SHORTLISTED");
    detachLogs(appender);
  }

  @Test
  void onApplicationStatusChanged_shouldCreatePoliteRejectedNotificationForCandidateAccount() {
    when(accountRepository.findByCandidateId(candidate.getId())).thenReturn(Optional.of(candidateAccount));
    when(accountRepository.findById(candidateAccount.getId())).thenReturn(Optional.of(candidateAccount));
    when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
    ListAppender<ILoggingEvent> appender = captureLogs();

    notificationService.onApplicationStatusChanged(
        application,
        ApplicationStage.INTERVIEW_SCHEDULED,
        ApplicationStage.REJECTED,
        companyAccount);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    Notification saved = captor.getValue();
    assertEquals(NotificationType.APPLICATION_REJECTED, saved.getType());
    assertTrue(saved.getBody().contains("Cảm ơn bạn đã quan tâm"));
    assertLogContains(appender, "Created notification APPLICATION_REJECTED");
    detachLogs(appender);
  }

  @Test
  void onApplicationStatusChanged_shouldAggregateRecentNotificationWithinWindow() {
    when(accountRepository.findByCandidateId(candidate.getId())).thenReturn(Optional.of(candidateAccount));
    Notification recent = Notification.builder()
        .id("notif-old")
        .recipient(candidateAccount)
        .type(NotificationType.APPLICATION_INTERVIEW_SCHEDULED)
        .title("Old title")
        .body("Old body")
        .data(new HashMap<>(java.util.Map.of("applicationId", application.getId())))
        .read(true)
        .build();
    recent.setCreatedDate(LocalDateTime.now().minusMinutes(2));

    when(notificationRepository.findByRecipientIdAndTypeAndCreatedDateGreaterThanEqualOrderByCreatedDateDesc(
        eq(candidateAccount.getId()), eq(NotificationType.APPLICATION_INTERVIEW_SCHEDULED), any(LocalDateTime.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(java.util.List.of(recent)));
    when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
    ListAppender<ILoggingEvent> appender = captureLogs();

    notificationService.onApplicationStatusChanged(
        application,
        ApplicationStage.INTERVIEW,
        ApplicationStage.INTERVIEW_SCHEDULED,
        companyAccount);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    Notification saved = captor.getValue();
    assertEquals("notif-old", saved.getId());
    assertEquals(NotificationType.APPLICATION_INTERVIEW_SCHEDULED, saved.getType());
    assertFalse(saved.isRead());
    assertLogContains(appender, "Updated notification notif-old for application app-1");
    detachLogs(appender);
  }

  @Test
  void onNewMessage_shouldNotifyOppositeParticipant() {
    MessageThread thread = MessageThread.builder()
        .id("thread-1")
        .company(company)
        .candidate(candidate)
        .build();

    Message message = Message.builder()
        .id("msg-1")
        .thread(thread)
        .sender(companyAccount)
        .content("Hello")
        .contentType(MessageContentType.TEXT)
        .build();

    when(accountRepository.findByCandidateId(candidate.getId())).thenReturn(Optional.of(candidateAccount));
    when(accountRepository.findByCompanyId(company.getId())).thenReturn(Optional.of(companyAccount));
    when(accountRepository.findById(candidateAccount.getId())).thenReturn(Optional.of(candidateAccount));
    when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
    ListAppender<ILoggingEvent> appender = captureLogs();

    notificationService.onNewMessage(message, thread);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    Notification saved = captor.getValue();
    assertEquals(NotificationType.NEW_MESSAGE, saved.getType());
    assertEquals("22222222-2222-2222-2222-222222222222", saved.getRecipient().getId());
    assertEquals("Hello", saved.getBody());
    verify(socketNotificationPusher).pushToUser(eq(java.util.UUID.fromString(candidateAccount.getId())), any(MessagingResponses.NotificationDto.class));
    assertLogContains(appender, "Created notification NEW_MESSAGE");
    detachLogs(appender);
    }

    @Test
    void onNewMessage_shouldSkipNotificationWhenRecentUnreadExists() {
    MessageThread thread = MessageThread.builder()
      .id("thread-1")
      .company(company)
      .candidate(candidate)
      .build();

    Message message = Message.builder()
      .id("msg-1")
      .thread(thread)
      .sender(companyAccount)
      .content("Hello again")
      .contentType(MessageContentType.TEXT)
      .build();

    Notification recent = Notification.builder()
      .id("notif-message")
      .recipient(candidateAccount)
      .type(NotificationType.NEW_MESSAGE)
      .title("Old")
      .body("Old")
      .data(new HashMap<>(java.util.Map.of("threadId", thread.getId())))
      .read(false)
      .build();
    recent.setCreatedDate(LocalDateTime.now().minusMinutes(1));

    when(accountRepository.findByCandidateId(candidate.getId())).thenReturn(Optional.of(candidateAccount));
    when(accountRepository.findByCompanyId(company.getId())).thenReturn(Optional.of(companyAccount));
    when(notificationRepository.findByRecipientIdAndTypeAndReadFalseAndCreatedDateGreaterThanEqualOrderByCreatedDateDesc(
      eq(candidateAccount.getId()), eq(NotificationType.NEW_MESSAGE), any(LocalDateTime.class), any(Pageable.class)))
      .thenReturn(new PageImpl<>(java.util.List.of(recent)));
    ListAppender<ILoggingEvent> appender = captureLogs();

    notificationService.onNewMessage(message, thread);

    verify(notificationRepository, never()).save(any(Notification.class));
    verify(socketNotificationPusher, never()).pushToUser(any(), any());
    verify(socketNotificationPusher).pushUnreadCounts(eq(java.util.UUID.fromString(candidateAccount.getId())), anyLong(), anyLong());
    assertLogContains(appender, "Skipped NEW_MESSAGE notification for recipient");
    detachLogs(appender);
  }

  @Test
  void markAsRead_shouldRejectOtherUsersNotification() {
    Account owner = Account.builder().id("owner").role(Role.USER).build();
    Account anotherUser = Account.builder().id("another").role(Role.USER).build();

    Notification notification = Notification.builder()
        .id("notif-2")
        .recipient(owner)
        .type(NotificationType.NEW_MESSAGE)
        .title("x")
        .body("y")
        .read(false)
        .build();

    when(notificationRepository.findById("notif-2")).thenReturn(Optional.of(notification));

    assertThrows(ForbiddenException.class, () -> notificationService.markAsRead(anotherUser, "notif-2"));
  }

  @Test
  void getNotifications_shouldReturnPageAndUnreadCount() {
    Notification n1 = Notification.builder()
        .id("n1")
        .recipient(candidateAccount)
        .type(NotificationType.NEW_MESSAGE)
        .title("title")
        .body("body")
        .read(false)
        .build();
    n1.setCreatedDate(LocalDateTime.now());

    when(notificationRepository.findByRecipientIdOrderByCreatedDateDesc(eq(candidateAccount.getId()),
        any(PageRequest.class)))
        .thenReturn(new PageImpl<>(java.util.List.of(n1)));
    when(notificationRepository.countByRecipientIdAndReadFalse(candidateAccount.getId())).thenReturn(3L);

    MessagingResponses.NotificationPageDto result = notificationService.getNotifications(candidateAccount, 0, 20);

    assertEquals(1, result.getNotifications().size());
    assertEquals(3L, result.getTotalUnread());
    assertFalse(result.isHasMore());
  }

  private ListAppender<ILoggingEvent> captureLogs() {
    Logger logger = (Logger) LoggerFactory.getLogger(NotificationServiceImpl.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    return appender;
  }

  private void detachLogs(ListAppender<ILoggingEvent> appender) {
    Logger logger = (Logger) LoggerFactory.getLogger(NotificationServiceImpl.class);
    logger.detachAppender(appender);
  }

  private void assertLogContains(ListAppender<ILoggingEvent> appender, String expected) {
    boolean match = appender.list.stream()
        .map(ILoggingEvent::getFormattedMessage)
        .anyMatch(message -> message.contains(expected));
    assertTrue(match, "Expected log containing: " + expected);
  }
}
