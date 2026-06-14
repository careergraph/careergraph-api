package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.common.Role;
import com.hcmute.careergraph.enums.message.MessageContentType;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.exception.ForbiddenException;
import com.hcmute.careergraph.persistence.dtos.request.MessagingRequests;
import com.hcmute.careergraph.persistence.dtos.response.MessagingResponses;
import com.hcmute.careergraph.persistence.models.*;
import com.hcmute.careergraph.repositories.*;
import com.hcmute.careergraph.services.ApplicationService;
import com.hcmute.careergraph.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceImplTest {

  @Mock
  private MessageThreadRepository messageThreadRepository;
  @Mock
  private MessageRepository messageRepository;
  @Mock
  private MessageReadRepository messageReadRepository;
  @Mock
  private CandidateRepository candidateRepository;
  @Mock
  private CompanyRepository companyRepository;
  @Mock
  private ApplicationRepository applicationRepository;
  @Mock
  private JobRepository jobRepository;
  @Mock
  private AccountRepository accountRepository;
  @Mock
  private ThreadDeletionRepository threadDeletionRepository;
  @Mock
  private UserBlockRepository userBlockRepository;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ApplicationService applicationService;
  @Mock
  private PlatformTransactionManager transactionManager;

  @InjectMocks
  private MessageServiceImpl messageService;

  private Company company;
  private Candidate candidate;
  private Account hrAccount;
  private Account candidateAccount;
  private MessageThread thread;

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

    hrAccount = Account.builder()
        .id("acc-hr")
        .email("hr@acme.com")
        .role(Role.HR)
        .company(company)
        .build();

    candidateAccount = Account.builder()
        .id("acc-candidate")
        .email("candidate@mail.com")
        .role(Role.USER)
        .candidate(candidate)
        .build();

    thread = MessageThread.builder()
        .id("thread-1")
        .company(company)
        .candidate(candidate)
        .lastMessageAt(LocalDateTime.now())
        .lastMessagePreview("Hi")
        .build();

    when(transactionManager.getTransaction(any(TransactionDefinition.class)))
        .thenReturn(new SimpleTransactionStatus());
    doNothing().when(transactionManager).commit(any(TransactionStatus.class));
    doNothing().when(transactionManager).rollback(any(TransactionStatus.class));

    when(accountRepository.findByCompanyId(anyString())).thenReturn(Optional.of(hrAccount));
    when(accountRepository.findByCandidateId(anyString())).thenReturn(Optional.of(candidateAccount));
    when(userBlockRepository.existsByBlockerIdAndBlockedId(anyString(), anyString())).thenReturn(false);
  }

  @Test
  void getOrCreateThread_shouldCreateNewThreadForHr() {
    MessagingRequests.CreateThreadRequest request = new MessagingRequests.CreateThreadRequest();
    request.setCandidateId(candidate.getId());

    when(candidateRepository.findById(candidate.getId())).thenReturn(Optional.of(candidate));
    when(messageThreadRepository.findByCompanyIdAndCandidateId(company.getId(), candidate.getId()))
        .thenReturn(Optional.empty());
    when(messageThreadRepository.saveAndFlush(any(MessageThread.class))).thenAnswer(inv -> {
      MessageThread saved = inv.getArgument(0, MessageThread.class);
      saved.setId("thread-new");
      return saved;
    });
    when(messageRepository.countUnreadInThread(anyString(), anyString(), any(LocalDateTime.class))).thenReturn(0L);

    MessagingResponses.ThreadSummaryDto result = messageService.getOrCreateThread(hrAccount, request);

    assertEquals("thread-new", result.getThreadId());
    assertEquals("candidate-1", result.getOtherParty().getId());
    verify(messageThreadRepository).saveAndFlush(any(MessageThread.class));
  }

  @Test
  void getOrCreateThread_shouldReturnExistingThreadWhenAlreadyPresent() {
    MessagingRequests.CreateThreadRequest request = new MessagingRequests.CreateThreadRequest();
    request.setCandidateId(candidate.getId());

    when(candidateRepository.findById(candidate.getId())).thenReturn(Optional.of(candidate));
    when(messageThreadRepository.findByCompanyIdAndCandidateId(company.getId(), candidate.getId()))
        .thenReturn(Optional.of(thread));
    when(messageRepository.countUnreadInThread(anyString(), anyString(), any(LocalDateTime.class))).thenReturn(0L);

    MessagingResponses.ThreadSummaryDto result = messageService.getOrCreateThread(hrAccount, request);

    assertEquals(thread.getId(), result.getThreadId());
    verify(messageThreadRepository, never()).save(any(MessageThread.class));
  }

  @Test
  void getOrCreateThread_shouldRecoverWhenConcurrentInsertOccurs() {
    MessagingRequests.CreateThreadRequest request = new MessagingRequests.CreateThreadRequest();
    request.setCandidateId(candidate.getId());

    when(candidateRepository.findById(candidate.getId())).thenReturn(Optional.of(candidate));
    when(messageThreadRepository.findByCompanyIdAndCandidateId(company.getId(), candidate.getId()))
        .thenReturn(Optional.empty());
    when(messageThreadRepository.findWithEagerByCompanyIdAndCandidateId(company.getId(), candidate.getId()))
        .thenReturn(Optional.of(thread));
    when(messageThreadRepository.saveAndFlush(any(MessageThread.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));
    when(messageRepository.countUnreadInThread(anyString(), anyString(), any(LocalDateTime.class))).thenReturn(0L);

    MessagingResponses.ThreadSummaryDto result = messageService.getOrCreateThread(hrAccount, request);

    assertEquals(thread.getId(), result.getThreadId());
    verify(messageThreadRepository).saveAndFlush(any(MessageThread.class));
  }

  @Test
  void sendMessage_shouldPersistMessageUpdateThreadAndNotify() {
    MessagingRequests.SendMessageRequest request = new MessagingRequests.SendMessageRequest();
    request.setContent("Hello candidate");
    request.setContentType(MessageContentType.TEXT);

    when(messageThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
    when(messageReadRepository.findByThreadIdAndAccountId(thread.getId(), hrAccount.getId()))
        .thenReturn(Optional.empty());
    when(accountRepository.findByCandidateId(candidate.getId())).thenReturn(Optional.of(candidateAccount));
    when(messageReadRepository.findByThreadIdAndAccountId(thread.getId(), candidateAccount.getId()))
        .thenReturn(Optional.empty());

    Message savedMessage = Message.builder()
        .id("msg-1")
        .thread(thread)
        .sender(hrAccount)
        .content("Hello candidate")
        .contentType(MessageContentType.TEXT)
        .deleted(false)
        .build();
    savedMessage.setCreatedDate(LocalDateTime.now());

    when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
    when(messageThreadRepository.save(any(MessageThread.class))).thenAnswer(inv -> inv.getArgument(0));
    when(messageReadRepository.save(any(MessageRead.class))).thenAnswer(inv -> inv.getArgument(0));

    MessagingResponses.MessageDto result = messageService.sendMessage(hrAccount, thread.getId(), request);

    assertEquals("msg-1", result.getId());
    assertEquals("Hello candidate", result.getContent());
    assertNotNull(thread.getLastMessageAt());
    assertEquals("Hello candidate", thread.getLastMessagePreview());
    verify(notificationService).onNewMessage(any(Message.class), eq(thread));
  }

  @Test
  void sendMessage_shouldPromoteApplicationWhenHrMessagesFromKanbanThread() {
    Application application = Application.builder()
        .id("app-1")
        .candidate(candidate)
        .build();
    thread.setApplication(application);

    MessagingRequests.SendMessageRequest request = new MessagingRequests.SendMessageRequest();
    request.setContent("Hello candidate");
    request.setContentType(MessageContentType.TEXT);

    when(messageThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
    when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
    when(messageReadRepository.findByThreadIdAndAccountId(thread.getId(), hrAccount.getId()))
        .thenReturn(Optional.empty());
    when(messageReadRepository.findByThreadIdAndAccountId(thread.getId(), candidateAccount.getId()))
        .thenReturn(Optional.empty());

    Message savedMessage = Message.builder()
        .id("msg-1")
        .thread(thread)
        .sender(hrAccount)
        .content("Hello candidate")
        .contentType(MessageContentType.TEXT)
        .deleted(false)
        .build();
    savedMessage.setCreatedDate(LocalDateTime.now());

    when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
    when(messageThreadRepository.save(any(MessageThread.class))).thenAnswer(inv -> inv.getArgument(0));
    when(messageReadRepository.save(any(MessageRead.class))).thenAnswer(inv -> inv.getArgument(0));

    messageService.sendMessage(hrAccount, thread.getId(), request);

    verify(applicationService).promoteToHrContactedOnHrMessage(application.getId(), hrAccount);
  }

  @Test
  void sendMessage_shouldNotPromoteApplicationWhenCandidateReplies() {
    Application application = Application.builder()
        .id("app-1")
        .candidate(candidate)
        .build();
    thread.setApplication(application);

    MessagingRequests.SendMessageRequest request = new MessagingRequests.SendMessageRequest();
    request.setContent("Xin chao HR");
    request.setContentType(MessageContentType.TEXT);

    when(messageThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
    when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
    when(messageReadRepository.findByThreadIdAndAccountId(thread.getId(), candidateAccount.getId()))
        .thenReturn(Optional.empty());
    when(messageReadRepository.findByThreadIdAndAccountId(thread.getId(), hrAccount.getId()))
        .thenReturn(Optional.empty());

    Message savedMessage = Message.builder()
        .id("msg-candidate")
        .thread(thread)
        .sender(candidateAccount)
        .content("Xin chao HR")
        .contentType(MessageContentType.TEXT)
        .deleted(false)
        .build();
    savedMessage.setCreatedDate(LocalDateTime.now());

    when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
    when(messageThreadRepository.save(any(MessageThread.class))).thenAnswer(inv -> inv.getArgument(0));
    when(messageReadRepository.save(any(MessageRead.class))).thenAnswer(inv -> inv.getArgument(0));

    messageService.sendMessage(candidateAccount, thread.getId(), request);

    verify(applicationService, never()).promoteToHrContactedOnHrMessage(anyString(), any(Account.class));
  }

  @Test
  void sendMessage_shouldKeepUnicodeContent() {
    MessagingRequests.SendMessageRequest request = new MessagingRequests.SendMessageRequest();
    request.setContent("Xin chao 😊");
    request.setContentType(MessageContentType.TEXT);

    when(messageThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
    when(messageReadRepository.findByThreadIdAndAccountId(thread.getId(), hrAccount.getId()))
        .thenReturn(Optional.empty());
    when(accountRepository.findByCandidateId(candidate.getId())).thenReturn(Optional.of(candidateAccount));
    when(messageReadRepository.findByThreadIdAndAccountId(thread.getId(), candidateAccount.getId()))
        .thenReturn(Optional.empty());

    Message savedMessage = Message.builder()
        .id("msg-emoji")
        .thread(thread)
        .sender(hrAccount)
        .content("Xin chao 😊")
        .contentType(MessageContentType.TEXT)
        .deleted(false)
        .build();
    savedMessage.setCreatedDate(LocalDateTime.now());

    when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
    when(messageThreadRepository.save(any(MessageThread.class))).thenAnswer(inv -> inv.getArgument(0));
    when(messageReadRepository.save(any(MessageRead.class))).thenAnswer(inv -> inv.getArgument(0));

    MessagingResponses.MessageDto result = messageService.sendMessage(hrAccount, thread.getId(), request);

    assertEquals("Xin chao 😊", result.getContent());
  }

  @Test
  void sendMessage_shouldRejectBlankContent() {
    MessagingRequests.SendMessageRequest request = new MessagingRequests.SendMessageRequest();
    request.setContent("   ");
    request.setContentType(MessageContentType.TEXT);

    when(messageThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));

    assertThrows(BadRequestException.class, () -> messageService.sendMessage(hrAccount, thread.getId(), request));
  }

  @Test
  void getThread_shouldThrowForbiddenForNonParticipant() {
    Candidate outsider = Candidate.builder().id("candidate-2").firstName("Bob").build();
    Account outsiderAccount = Account.builder()
        .id("acc-outsider")
        .role(Role.USER)
        .candidate(outsider)
        .build();

    when(messageThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));

    assertThrows(ForbiddenException.class, () -> messageService.getThread(outsiderAccount, thread.getId()));
  }

  @Test
  void getThread_shouldHandleNullApplicationGracefully() {
    thread.setApplication(null);
    when(messageThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
    when(messageRepository.countUnreadInThread(anyString(), anyString(), any(LocalDateTime.class))).thenReturn(0L);

    MessagingResponses.ThreadSummaryDto result = messageService.getThread(hrAccount, thread.getId());

    assertNotNull(result);
    assertNull(result.getApplication());
  }

  @Test
  void getThreads_shouldReturnUnreadCountInSummary() {
    when(messageThreadRepository.findVisibleByCompanyAndArchived(eq(company.getId()), eq(hrAccount.getId()), eq(false),
        any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(thread)));
    when(messageRepository.countUnreadInThread(eq(thread.getId()), eq(hrAccount.getId()), any(LocalDateTime.class)))
        .thenReturn(2L);

    var result = messageService.getThreads(hrAccount, PageRequest.of(0, 20));

    assertEquals(1, result.getContent().size());
    assertEquals(2L, result.getContent().get(0).getUnreadCount());
  }

  @Test
  void deleteMessage_shouldThrowForbiddenWhenCurrentUserIsNotSender() {
    Message message = Message.builder()
        .id("msg-2")
        .sender(candidateAccount)
        .thread(thread)
        .content("Hi")
        .contentType(MessageContentType.TEXT)
        .deleted(false)
        .build();

    when(messageRepository.findById("msg-2")).thenReturn(Optional.of(message));

    assertThrows(ForbiddenException.class, () -> messageService.deleteMessage(hrAccount, "msg-2"));
  }

  @Test
  void markThreadAsRead_shouldCreateReadRecord() {
    when(messageThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
    Message latestMessage = Message.builder()
        .id("msg-latest")
        .thread(thread)
        .sender(candidateAccount)
        .content("Newest")
        .contentType(MessageContentType.TEXT)
        .build();
    latestMessage.setCreatedDate(LocalDateTime.now());

    when(messageRepository.findTopByThreadIdAndDeletedFalseOrderByCreatedDateDesc(thread.getId()))
        .thenReturn(Optional.of(latestMessage));
    when(messageReadRepository.findByThreadIdAndAccountId(thread.getId(), hrAccount.getId()))
        .thenReturn(Optional.empty());
    when(messageReadRepository.save(any(MessageRead.class))).thenAnswer(inv -> inv.getArgument(0));

    messageService.markThreadAsRead(hrAccount, thread.getId());

    ArgumentCaptor<MessageRead> captor = ArgumentCaptor.forClass(MessageRead.class);
    verify(messageReadRepository).save(captor.capture());
    assertEquals("msg-latest", captor.getValue().getLastReadMessage().getId());
  }

  @Test
  void getTotalUnread_shouldUseCurrentUsersThreads() {
    when(messageThreadRepository.findVisibleIdsByCompanyId(company.getId(), hrAccount.getId()))
        .thenReturn(List.of("thread-1", "thread-2"));
    when(messageRepository.countTotalUnreadByThreadIds(eq(hrAccount.getId()), anyList(), any(LocalDateTime.class)))
        .thenReturn(4L);

    long result = messageService.getTotalUnread(hrAccount);

    assertEquals(4L, result);
  }

  @Test
  void getMessages_shouldReturnPagedMessagesSortedByServicePageable() {
    when(messageThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
    when(messageReadRepository.findByThreadIdAndAccountId(thread.getId(), hrAccount.getId()))
        .thenReturn(Optional.empty());
    when(accountRepository.findByCandidateId(candidate.getId())).thenReturn(Optional.of(candidateAccount));
    when(messageReadRepository.findByThreadIdAndAccountId(thread.getId(), candidateAccount.getId()))
        .thenReturn(Optional.empty());

    Message m1 = Message.builder().id("m1").thread(thread).sender(hrAccount).content("A")
        .contentType(MessageContentType.TEXT).build();
    m1.setCreatedDate(LocalDateTime.now().minusMinutes(2));
    Message m2 = Message.builder().id("m2").thread(thread).sender(candidateAccount).content("B")
        .contentType(MessageContentType.TEXT).build();
    m2.setCreatedDate(LocalDateTime.now().minusMinutes(1));

    when(messageRepository.findByThreadIdAndOptionalJobId(eq(thread.getId()), isNull(), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(m1, m2)));

    var page = messageService.getMessages(hrAccount, thread.getId(), null, PageRequest.of(0, 30));

    assertEquals(2, page.getContent().size());
    assertEquals("m1", page.getContent().get(0).getId());
    assertEquals("m2", page.getContent().get(1).getId());
  }
}
