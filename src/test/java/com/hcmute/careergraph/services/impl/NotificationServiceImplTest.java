package com.hcmute.careergraph.services.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private AccountRepository accountRepository;
  @Mock
  private MessageRepository messageRepository;

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
        .id("acc-company")
        .role(Role.HR)
        .email("hr@acme.com")
        .company(company)
        .build();

    candidateAccount = Account.builder()
        .id("acc-candidate")
        .role(Role.USER)
        .email("candidate@mail.com")
        .candidate(candidate)
        .build();
  }

  @Test
  void onNewApplication_shouldCreateNotificationForCompanyAccount() {
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
    assertEquals("acc-company", saved.getRecipient().getId());
    assertTrue(saved.getBody().contains("Backend Engineer"));
  }

  @Test
  void onApplicationStatusChanged_shouldCreateNotificationForCandidateAccount() {
    when(accountRepository.findByCandidateId(candidate.getId())).thenReturn(Optional.of(candidateAccount));
    when(accountRepository.findById(candidateAccount.getId())).thenReturn(Optional.of(candidateAccount));
    when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

    notificationService.onApplicationStatusChanged(
        application,
        ApplicationStage.APPLIED,
        ApplicationStage.INTERVIEW_SCHEDULED,
        companyAccount);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    Notification saved = captor.getValue();
    assertEquals(NotificationType.APPLICATION_STATUS_CHANGED, saved.getType());
    assertEquals("acc-candidate", saved.getRecipient().getId());
    assertTrue(saved.getTitle().contains("Interview"));
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

    notificationService.onNewMessage(message, thread);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    Notification saved = captor.getValue();
    assertEquals(NotificationType.NEW_MESSAGE, saved.getType());
    assertEquals("acc-candidate", saved.getRecipient().getId());
    assertEquals("Hello", saved.getBody());
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
}
