package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.common.Role;
import com.hcmute.careergraph.enums.message.MessageContentType;
import com.hcmute.careergraph.enums.message.MessageDeleteType;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.exception.ForbiddenException;
import com.hcmute.careergraph.exception.NotFoundException;
import com.hcmute.careergraph.persistence.dtos.request.MessagingRequests;
import com.hcmute.careergraph.persistence.dtos.response.MessagingResponses;
import com.hcmute.careergraph.persistence.models.*;
import com.hcmute.careergraph.repositories.*;
import com.hcmute.careergraph.services.MessageService;
import com.hcmute.careergraph.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

  private static final int DEFAULT_THREAD_PAGE_SIZE = 20;
  private static final int DEFAULT_MESSAGE_PAGE_SIZE = 30;
  private static final int PREVIEW_MAX_LENGTH = 100;
  private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);
  private static final String CONTENT_UNSENT = "[Tin nhắn đã được gỡ]";
  private static final String CONTENT_DELETED = "Tin nhắn đã được thu hồi";

  private final MessageThreadRepository messageThreadRepository;
  private final MessageRepository messageRepository;
  private final MessageReadRepository messageReadRepository;
  private final CandidateRepository candidateRepository;
  private final CompanyRepository companyRepository;
  private final ApplicationRepository applicationRepository;
  private final JobRepository jobRepository;
  private final AccountRepository accountRepository;
  private final ThreadDeletionRepository threadDeletionRepository;
  private final UserBlockRepository userBlockRepository;
  private final NotificationService notificationService;
  private final org.springframework.transaction.PlatformTransactionManager transactionManager;

  @Value("${app.messaging.unsend-window-seconds:60}")
  private long unsendWindowSeconds;

  @Override
  @Transactional
  public MessagingResponses.ThreadSummaryDto getOrCreateThread(Account currentAccount,
      MessagingRequests.CreateThreadRequest request) {
    ThreadParticipants participants = resolveThreadParticipants(currentAccount, request);

    Optional<MessageThread> existing = messageThreadRepository
      .findByCompanyIdAndCandidateId(participants.company().getId(), participants.candidate().getId());

    if (existing.isPresent()) {
      MessageThread thread = existing.get();
      if (thread.getApplication() == null && participants.application() != null) {
        thread.setApplication(participants.application());
        thread = messageThreadRepository.saveAndFlush(thread);
      }
      return toThreadSummary(currentAccount, thread);
    }

    MessageThread newThread = MessageThread.builder()
        .company(participants.company())
        .candidate(participants.candidate())
        .application(participants.application())
        .lastMessageAt(null)
        .lastMessagePreview(null)
        .build();

    MessageThread saved = createThreadInNewTransaction(newThread);
    if (saved != null) {
      return toThreadSummary(currentAccount, saved);
    }

    MessageThread recovered = findThreadInNewTransaction(participants.company().getId(), participants.candidate().getId())
        .orElseThrow(() -> new DataIntegrityViolationException("duplicate thread insert failed and recovery lookup returned empty"));
    
    // Re-attach recovered entity to current session
    MessageThread mergedThread = messageThreadRepository.findWithEagerByCompanyIdAndCandidateId(
        recovered.getCompany().getId(), 
        recovered.getCandidate().getId())
      .orElse(recovered);
    
    return toThreadSummary(currentAccount, mergedThread);
  }

  private MessageThread createThreadInNewTransaction(MessageThread newThread) {
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    template.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return template.execute(status -> {
      try {
        return messageThreadRepository.saveAndFlush(newThread);
      } catch (DataIntegrityViolationException ex) {
        status.setRollbackOnly();
        return null;
      }
    });
  }

  private Optional<MessageThread> findThreadInNewTransaction(String companyId, String candidateId) {
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    template.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return Optional.ofNullable(template.execute(status ->
        messageThreadRepository.findWithEagerByCompanyIdAndCandidateId(companyId, candidateId).orElse(null)));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<MessagingResponses.ThreadSummaryDto> getThreads(Account currentAccount, Pageable pageable) {
    return getThreads(currentAccount, false, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<MessagingResponses.ThreadSummaryDto> getThreads(Account currentAccount,
      boolean archived,
      Pageable pageable) {
    Role role = resolveRole(currentAccount);
    Pageable normalized = normalizeThreadPageable(pageable);

    Page<MessageThread> page = role == Role.HR
        ? messageThreadRepository.findVisibleByCompanyAndArchived(
            currentAccount.getCompany().getId(),
            currentAccount.getId(),
            archived,
            normalized)
        : messageThreadRepository.findVisibleByCandidateAndArchived(
            currentAccount.getCandidate().getId(),
            currentAccount.getId(),
            archived,
            normalized);

    return page.map(thread -> toThreadSummary(currentAccount, thread));
  }

  @Override
  @Transactional(readOnly = true)
  public MessagingResponses.ThreadSummaryDto getThread(Account currentAccount, String threadId) {
    MessageThread thread = findThreadOrThrow(threadId);
    validateThreadAccess(thread, currentAccount);
    return toThreadSummary(currentAccount, thread);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MessagingResponses.ThreadJobDto> getThreadJobs(Account currentAccount, String threadId) {
    MessageThread thread = findThreadOrThrow(threadId);
    validateThreadAccess(thread, currentAccount);
    return resolveThreadJobs(currentAccount, thread);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<MessagingResponses.MessageDto> getMessages(Account currentAccount,
      String threadId,
      String jobId,
      Pageable pageable) {
    MessageThread thread = findThreadOrThrow(threadId);
    validateThreadAccess(thread, currentAccount);

    String normalizedJobId = normalizeJobContextId(jobId);
    if (normalizedJobId != null
        && !applicationRepository.existsByCandidateIdAndJobIdAndJobCompanyId(
            thread.getCandidate().getId(),
            normalizedJobId,
            thread.getCompany().getId())) {
      throw new BadRequestException("Job không thuộc cuộc trò chuyện này");
    }

    Pageable normalized = normalizeMessagePageable(pageable);
    Page<Message> messages = messageRepository.findByThreadIdAndOptionalJobId(threadId, normalizedJobId, normalized);

    LocalDateTime currentReadAt = messageReadRepository
        .findByThreadIdAndAccountId(threadId, currentAccount.getId())
        .map(MessageRead::getLastReadAt)
        .orElse(null);

    LocalDateTime otherReadAt = resolveOtherAccount(currentAccount, thread)
        .flatMap(otherAccount -> messageReadRepository.findByThreadIdAndAccountId(threadId, otherAccount.getId()))
        .map(MessageRead::getLastReadAt)
        .orElse(null);

    return messages.map(message -> toMessageDto(currentAccount, message, currentReadAt, otherReadAt));
  }

  @Override
  public MessagingResponses.MessageDto sendMessage(Account currentAccount,
      String threadId,
      MessagingRequests.SendMessageRequest request) {
    MessageThread thread = findThreadOrThrow(threadId);
    validateThreadAccess(thread, currentAccount);
    ensureThreadNotBlocked(thread);

    MessageContentType contentType = request.getContentType() != null
        ? request.getContentType()
        : MessageContentType.TEXT;

    String content = request.getContent() != null ? request.getContent().trim() : "";
    if (contentType == MessageContentType.TEXT && !StringUtils.hasText(content)) {
      throw new BadRequestException("Nội dung tin nhắn không được để trống");
    }
    if (contentType != MessageContentType.TEXT
        && !StringUtils.hasText(request.getFileUrl())
        && !StringUtils.hasText(content)) {
      throw new BadRequestException("Tin nhắn tệp cần có nội dung hoặc đường dẫn tệp");
    }
    if (!StringUtils.hasText(content)) {
      content = StringUtils.hasText(request.getFileName()) ? request.getFileName().trim() : "[Attachment]";
    }

    Job jobContext = null;
    String jobContextId = normalizeJobContextId(request.getJobContextId());
    if (jobContextId != null) {
      boolean belongsToThread = applicationRepository.existsByCandidateIdAndJobIdAndJobCompanyId(
          thread.getCandidate().getId(),
          jobContextId,
          thread.getCompany().getId());
      if (!belongsToThread) {
        throw new BadRequestException("Job không thuộc cuộc trò chuyện này");
      }
      jobContext = jobRepository.findById(jobContextId)
          .orElseThrow(() -> new NotFoundException("Không tìm thấy job context"));
    }

    Message message = Message.builder()
        .thread(thread)
        .sender(currentAccount)
        .jobContext(jobContext)
        .content(content)
        .contentType(contentType)
        .fileUrl(request.getFileUrl())
        .fileName(request.getFileName())
        .fileSize(request.getFileSize())
        .deleted(false)
        .build();

      // New message should bring the thread back to both participants' inbox.
      threadDeletionRepository.deleteByThreadId(threadId);
      thread.setArchivedByCompany(false);
      thread.setArchivedByCompanyAt(null);
      thread.setArchivedByCandidate(false);
      thread.setArchivedByCandidateAt(null);

    Message savedMessage = messageRepository.save(message);

    LocalDateTime now = savedMessage.getCreatedDate() != null ? savedMessage.getCreatedDate() : LocalDateTime.now();
    thread.setLastMessageAt(now);
    thread.setLastMessagePreview(buildPreview(savedMessage));
    messageThreadRepository.save(thread);

    MessageRead senderRead = messageReadRepository.findByThreadIdAndAccountId(threadId, currentAccount.getId())
        .orElse(MessageRead.builder()
            .thread(thread)
            .account(currentAccount)
            .build());
    senderRead.setLastReadMessage(savedMessage);
    senderRead.setLastReadAt(now);
    messageReadRepository.save(senderRead);

    notificationService.onNewMessage(savedMessage, thread);

    LocalDateTime otherReadAt = resolveOtherAccount(currentAccount, thread)
        .flatMap(otherAccount -> messageReadRepository.findByThreadIdAndAccountId(threadId, otherAccount.getId()))
        .map(MessageRead::getLastReadAt)
        .orElse(null);

    return toMessageDto(currentAccount, savedMessage, senderRead.getLastReadAt(), otherReadAt);
  }

  @Override
  public void markThreadAsRead(Account currentAccount, String threadId) {
    MessageThread thread = findThreadOrThrow(threadId);
    validateThreadAccess(thread, currentAccount);

    Message latestMessage = messageRepository
        .findTopByThreadIdAndDeletedFalseOrderByCreatedDateDesc(threadId)
        .orElse(null);

    MessageRead read = messageReadRepository.findByThreadIdAndAccountId(threadId, currentAccount.getId())
        .orElse(MessageRead.builder()
            .thread(thread)
            .account(currentAccount)
            .build());

    if (latestMessage != null) {
      read.setLastReadMessage(latestMessage);
      read.setLastReadAt(latestMessage.getCreatedDate() != null
          ? latestMessage.getCreatedDate()
          : LocalDateTime.now());
    } else {
      read.setLastReadAt(LocalDateTime.now());
    }

    messageReadRepository.save(read);
  }

  @Override
  public void deleteMessage(Account currentAccount, String messageId) {
    unsendMessage(currentAccount, messageId);
  }

  @Override
  public void unsendMessage(Account currentAccount, String messageId) {
    Message message = messageRepository.findById(messageId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy tin nhắn"));

    if (!message.getSender().getId().equals(currentAccount.getId())) {
      throw new ForbiddenException("Bạn chỉ có thể gỡ tin nhắn do mình gửi");
    }
    if (message.isDeleted()) {
      return;
    }

    LocalDateTime createdAt = message.getCreatedDate() != null ? message.getCreatedDate() : LocalDateTime.now();
    long elapsedSeconds = Math.max(0, Duration.between(createdAt, LocalDateTime.now()).getSeconds());
    if (elapsedSeconds > unsendWindowSeconds) {
      throw new BadRequestException("Chỉ có thể gỡ tin nhắn trong vòng " + unsendWindowSeconds + " giây sau khi gửi");
    }

    message.setDeleted(true);
    message.setDeletedAt(LocalDateTime.now());
    message.setDeleteType(MessageDeleteType.UNSEND);
    message.setContent(CONTENT_UNSENT);
    messageRepository.save(message);

    MessageThread thread = message.getThread();
    Message latestMessage = messageRepository
        .findTopByThreadIdAndDeletedFalseOrderByCreatedDateDesc(thread.getId())
        .orElse(null);

    if (latestMessage == null) {
      thread.setLastMessageAt(null);
      thread.setLastMessagePreview(null);
    } else {
      thread.setLastMessageAt(latestMessage.getCreatedDate());
      thread.setLastMessagePreview(buildPreview(latestMessage));
    }
    messageThreadRepository.save(thread);
  }

  @Override
  public void deleteThreadForMe(Account currentAccount, String threadId) {
    MessageThread thread = findThreadOrThrow(threadId);
    validateThreadAccess(thread, currentAccount);

    if (threadDeletionRepository.existsByThreadIdAndAccountId(threadId, currentAccount.getId())) {
      return;
    }

    ThreadDeletion threadDeletion = ThreadDeletion.builder()
        .thread(thread)
        .account(currentAccount)
        .deletedAt(LocalDateTime.now())
        .build();
    threadDeletionRepository.save(threadDeletion);
  }

  @Override
  public void archiveThread(Account currentAccount, String threadId, boolean archive) {
    MessageThread thread = findThreadOrThrow(threadId);
    validateThreadAccess(thread, currentAccount);

    Role role = resolveRole(currentAccount);
    LocalDateTime now = LocalDateTime.now();

    if (role == Role.HR) {
      thread.setArchivedByCompany(archive);
      thread.setArchivedByCompanyAt(archive ? now : null);
    } else {
      thread.setArchivedByCandidate(archive);
      thread.setArchivedByCandidateAt(archive ? now : null);
    }

    messageThreadRepository.save(thread);
  }

  @Override
  public void blockCandidate(Account currentAccount, String candidateId, String reason) {
    if (resolveRole(currentAccount) != Role.HR) {
      throw new ForbiddenException("Chỉ HR mới có quyền chặn ứng viên");
    }
    if (!StringUtils.hasText(candidateId)) {
      throw new BadRequestException("Thiếu candidateId");
    }

    Account candidateAccount = accountRepository.findByCandidateId(candidateId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản ứng viên"));

    if (userBlockRepository.existsByBlockerIdAndBlockedId(currentAccount.getId(), candidateAccount.getId())) {
      return;
    }

    UserBlock userBlock = UserBlock.builder()
        .blocker(currentAccount)
        .blocked(candidateAccount)
        .reason(StringUtils.hasText(reason) ? reason.trim() : null)
        .blockedAt(LocalDateTime.now())
        .build();
    userBlockRepository.save(userBlock);
  }

  @Override
  public void unblockCandidate(Account currentAccount, String candidateId) {
    if (resolveRole(currentAccount) != Role.HR) {
      throw new ForbiddenException("Chỉ HR mới có quyền bỏ chặn ứng viên");
    }
    if (!StringUtils.hasText(candidateId)) {
      throw new BadRequestException("Thiếu candidateId");
    }

    Account candidateAccount = accountRepository.findByCandidateId(candidateId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản ứng viên"));

    userBlockRepository.findByBlockerIdAndBlockedId(currentAccount.getId(), candidateAccount.getId())
        .ifPresent(userBlockRepository::delete);
  }

  @Override
  @Transactional(readOnly = true)
  public MessagingResponses.BlockStatusDto getBlockStatus(Account currentAccount, String candidateId) {
    if (resolveRole(currentAccount) != Role.HR) {
      throw new ForbiddenException("Chỉ HR mới có quyền xem danh sách chặn");
    }

    if (!StringUtils.hasText(candidateId)) {
      throw new BadRequestException("Thiếu candidateId");
    }

    Account candidateAccount = accountRepository.findByCandidateId(candidateId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản ứng viên"));

    Optional<UserBlock> block = userBlockRepository.findByBlockerIdAndBlockedId(currentAccount.getId(), candidateAccount.getId());

    return MessagingResponses.BlockStatusDto.builder()
        .blocked(block.isPresent())
        .blockedAt(block.map(UserBlock::getBlockedAt).orElse(null))
        .reason(block.map(UserBlock::getReason).orElse(null))
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public List<MessagingResponses.BlockedUserDto> getBlockedCandidates(Account currentAccount) {
    if (resolveRole(currentAccount) != Role.HR) {
      throw new ForbiddenException("Chỉ HR mới có quyền xem danh sách chặn");
    }

    return userBlockRepository.findByBlockerIdOrderByBlockedAtDesc(currentAccount.getId())
        .stream()
        .map(this::toBlockedUserDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long getTotalUnread(Account currentAccount) {
    Role role = resolveRole(currentAccount);

    var threadIds = role == Role.HR
        ? messageThreadRepository.findVisibleIdsByCompanyId(currentAccount.getCompany().getId(), currentAccount.getId())
        : messageThreadRepository.findVisibleIdsByCandidateId(currentAccount.getCandidate().getId(), currentAccount.getId());

    if (threadIds == null || threadIds.isEmpty()) {
      return 0L;
    }

    return messageRepository.countTotalUnreadByThreadIds(currentAccount.getId(), threadIds, EPOCH);
  }

  private ThreadParticipants resolveThreadParticipants(Account currentAccount,
      MessagingRequests.CreateThreadRequest request) {
    Role role = resolveRole(currentAccount);
    Application application = resolveApplication(request.getApplicationId());

    if (role == Role.HR) {
      Company company = currentAccount.getCompany();
      if (company == null) {
        throw new ForbiddenException("Current HR account does not have a company");
      }

      Candidate candidate;
      if (application != null) {
        if (application.getJob() == null
            || application.getJob().getCompany() == null
            || !company.getId().equals(application.getJob().getCompany().getId())) {
          throw new ForbiddenException("You cannot create thread for this application");
        }
        candidate = application.getCandidate();
      } else {
        if (!StringUtils.hasText(request.getCandidateId())) {
          throw new BadRequestException("candidateId or applicationId is required");
        }
        candidate = candidateRepository.findById(request.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Candidate not found"));
      }

      return new ThreadParticipants(company, candidate, application);
    }

    Candidate candidate = currentAccount.getCandidate();
    if (candidate == null) {
      throw new ForbiddenException("Current candidate account is invalid");
    }

    Company company;
    if (application != null) {
      if (application.getCandidate() == null || !candidate.getId().equals(application.getCandidate().getId())) {
        throw new ForbiddenException("You cannot create thread for this application");
      }
      company = Optional.ofNullable(application.getJob())
          .map(Job::getCompany)
          .orElseThrow(() -> new BadRequestException("Application company is missing"));
    } else {
      if (!StringUtils.hasText(request.getCompanyId())) {
        throw new BadRequestException("companyId or applicationId is required");
      }
      company = companyRepository.findById(request.getCompanyId())
          .orElseThrow(() -> new NotFoundException("Company not found"));
    }

    return new ThreadParticipants(company, candidate, application);
  }

  private MessageThread findThreadOrThrow(String threadId) {
    return messageThreadRepository.findById(threadId)
        .orElseThrow(() -> new NotFoundException("Message thread not found"));
  }

  private Application resolveApplication(String applicationId) {
    if (!StringUtils.hasText(applicationId)) {
      return null;
    }
    return applicationRepository.findById(applicationId)
        .orElseThrow(() -> new NotFoundException("Application not found"));
  }

  private void validateThreadAccess(MessageThread thread, Account currentAccount) {
    Role role = resolveRole(currentAccount);

    boolean allowed = role == Role.HR
        ? currentAccount.getCompany() != null && thread.getCompany().getId().equals(currentAccount.getCompany().getId())
        : currentAccount.getCandidate() != null
            && thread.getCandidate().getId().equals(currentAccount.getCandidate().getId());

    if (!allowed) {
      throw new ForbiddenException("You cannot access this thread");
    }
  }

  private Role resolveRole(Account currentAccount) {
    if (currentAccount == null || currentAccount.getRole() == null) {
      throw new ForbiddenException("Current account is invalid");
    }
    if (currentAccount.getRole() != Role.HR && currentAccount.getRole() != Role.USER) {
      throw new ForbiddenException("Only HR and candidate accounts can use messaging");
    }
    return currentAccount.getRole();
  }

  private Optional<Account> resolveOtherAccount(Account currentAccount, MessageThread thread) {
    Role role = resolveRole(currentAccount);
    if (role == Role.HR) {
      return accountRepository.findByCandidateId(thread.getCandidate().getId());
    }
    return accountRepository.findByCompanyId(thread.getCompany().getId());
  }

  private MessagingResponses.ThreadSummaryDto toThreadSummary(Account currentAccount, MessageThread thread) {
    MessagingResponses.PartySummaryDto otherParty = resolveOtherParty(currentAccount, thread);

    MessagingResponses.ApplicationSummaryDto applicationSummary = null;
    if (thread.getApplication() != null) {
      applicationSummary = MessagingResponses.ApplicationSummaryDto.builder()
          .id(thread.getApplication().getId())
          .jobId(thread.getApplication().getJob() != null ? thread.getApplication().getJob().getId() : null)
          .jobTitle(thread.getApplication().getJob() != null ? thread.getApplication().getJob().getTitle() : null)
          .currentStage(thread.getApplication().getCurrentStage() != null
              ? thread.getApplication().getCurrentStage().name()
              : null)
          .build();
    }

            List<MessagingResponses.ThreadJobDto> jobs = resolveThreadJobs(currentAccount, thread);
            MessagingResponses.ThreadJobDto primaryJob = resolvePrimaryJob(jobs);

    long unreadCount = messageRepository.countUnreadInThread(thread.getId(), currentAccount.getId(), EPOCH);
    boolean isHr = resolveRole(currentAccount) == Role.HR;
    boolean archived = isHr ? thread.isArchivedByCompany() : thread.isArchivedByCandidate();
    String hrAccountId = resolveHrAccountId(thread);
    String candidateAccountId = resolveCandidateAccountId(thread);
    boolean blocked = StringUtils.hasText(hrAccountId)
      && StringUtils.hasText(candidateAccountId)
      && userBlockRepository.existsByBlockerIdAndBlockedId(hrAccountId, candidateAccountId);

    return MessagingResponses.ThreadSummaryDto.builder()
        .threadId(thread.getId())
        .otherParty(otherParty)
        .application(applicationSummary)
        .jobs(jobs)
        .primaryJob(primaryJob)
        .lastMessagePreview(thread.getLastMessagePreview())
        .lastMessageAt(thread.getLastMessageAt())
        .unreadCount(unreadCount)
        .online(false)
      .archived(archived)
      .blocked(blocked)
        .build();
  }

  private MessagingResponses.PartySummaryDto resolveOtherParty(Account currentAccount, MessageThread thread) {
    Role role = resolveRole(currentAccount);
    if (role == Role.HR) {
      Candidate candidate = thread.getCandidate();
      // Ensure candidate is initialized to avoid LazyInitializationException
      Hibernate.initialize(candidate);
      return MessagingResponses.PartySummaryDto.builder()
          .id(candidate.getId())
          .displayName(resolveCandidateName(candidate))
          .avatar(candidate.getAvatar())
          .partyType("CANDIDATE")
          .build();
    }

    Company company = thread.getCompany();
    // Ensure company is initialized to avoid LazyInitializationException
    Hibernate.initialize(company);
    return MessagingResponses.PartySummaryDto.builder()
        .id(company.getId())
        .displayName(resolveCompanyName(company))
        .avatar(company.getAvatar())
        .partyType("COMPANY")
        .build();
  }

  private MessagingResponses.MessageDto toMessageDto(Account currentAccount,
      Message message,
      LocalDateTime currentReadAt,
      LocalDateTime otherReadAt) {
    boolean sentByCurrentUser = currentAccount.getId().equals(message.getSender().getId());
    LocalDateTime readAt = sentByCurrentUser ? otherReadAt : currentReadAt;
    boolean isRead = readAt != null
        && message.getCreatedDate() != null
        && !message.getCreatedDate().isAfter(readAt);

    return MessagingResponses.MessageDto.builder()
        .id(message.getId())
        .threadId(message.getThread().getId())
        .senderId(message.getSender().getId())
        .senderName(resolveAccountName(message.getSender()))
        .senderAvatar(resolveAccountAvatar(message.getSender()))
        .content(message.isDeleted() ? CONTENT_DELETED : message.getContent())
        .contentType(message.getContentType())
        .fileUrl(message.getFileUrl())
        .fileName(message.getFileName())
        .fileSize(message.getFileSize())
        .deleted(message.isDeleted())
        .jobContext(toJobContextDto(message.getThread(), message.getJobContext()))
        .createdAt(message.getCreatedDate())
        .read(isRead)
        .readAt(isRead ? readAt : null)
        .build();
  }

  private String normalizeJobContextId(String jobContextId) {
    if (!StringUtils.hasText(jobContextId)) {
      return null;
    }
    return jobContextId.trim();
  }

  private List<MessagingResponses.ThreadJobDto> resolveThreadJobs(Account currentAccount, MessageThread thread) {
    List<Application> applications = applicationRepository.findThreadContextApplications(
        thread.getCandidate().getId(),
        thread.getCompany().getId());

    Map<String, MessagingResponses.ThreadJobDto> groupedJobs = new LinkedHashMap<>();

    for (Application application : applications) {
      Job job = application.getJob();
      if (job == null || !StringUtils.hasText(job.getId())) {
        continue;
      }

      String jobId = job.getId();
      if (groupedJobs.containsKey(jobId)) {
        continue;
      }

      LocalDateTime lastMessageAt = messageRepository
          .findLastMessageTimeByThreadAndJob(thread.getId(), jobId)
          .orElse(null);
      long unreadCount = messageRepository.countUnreadInThreadByJob(thread.getId(), jobId, currentAccount.getId(), EPOCH);

      groupedJobs.put(jobId, MessagingResponses.ThreadJobDto.builder()
          .jobId(jobId)
          .jobTitle(job.getTitle())
          .jobStatus(application.getCurrentStage() != null ? application.getCurrentStage().name() : null)
          .unreadCount(unreadCount)
          .lastMessageAt(lastMessageAt)
          .hasMessages(lastMessageAt != null)
          .build());
    }

    return groupedJobs.values().stream()
        .sorted(Comparator.comparing(MessagingResponses.ThreadJobDto::getLastMessageAt,
            Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }

  private MessagingResponses.ThreadJobDto resolvePrimaryJob(List<MessagingResponses.ThreadJobDto> jobs) {
    if (jobs == null || jobs.isEmpty()) {
      return null;
    }

    for (MessagingResponses.ThreadJobDto job : jobs) {
      if (job.isHasMessages()) {
        return job;
      }
    }

    return jobs.get(0);
  }

  private MessagingResponses.JobContextDto toJobContextDto(MessageThread thread, Job jobContext) {
    if (jobContext == null) {
      return null;
    }

    String jobStatus = applicationRepository
        .findFirstByCandidateIdAndJobIdOrderByCreatedDateDesc(thread.getCandidate().getId(), jobContext.getId())
        .map(Application::getCurrentStage)
        .map(Enum::name)
        .orElse(null);

    return MessagingResponses.JobContextDto.builder()
        .jobId(jobContext.getId())
        .jobTitle(jobContext.getTitle())
        .jobStatus(jobStatus)
        .build();
  }

  private String buildPreview(Message message) {
    if (message.getContentType() != MessageContentType.TEXT) {
      return "[Attachment]";
    }

    String content = message.getContent() != null
        ? message.getContent().trim().replaceAll("\\s+", " ")
        : "";
    if (content.length() <= PREVIEW_MAX_LENGTH) {
      return content;
    }
    return content.substring(0, PREVIEW_MAX_LENGTH);
  }

  private Pageable normalizeThreadPageable(Pageable pageable) {
    int page = pageable != null ? pageable.getPageNumber() : 0;
    int size = pageable != null ? pageable.getPageSize() : DEFAULT_THREAD_PAGE_SIZE;
    Sort sort = pageable != null && pageable.getSort().isSorted()
        ? pageable.getSort()
        : Sort.by(Sort.Direction.DESC, "lastMessageAt").and(Sort.by(Sort.Direction.DESC, "createdDate"));
    return PageRequest.of(Math.max(0, page), Math.max(1, size), sort);
  }

  private Pageable normalizeMessagePageable(Pageable pageable) {
    int page = pageable != null ? pageable.getPageNumber() : 0;
    int size = pageable != null ? pageable.getPageSize() : DEFAULT_MESSAGE_PAGE_SIZE;
    Sort sort = pageable != null && pageable.getSort().isSorted()
        ? pageable.getSort()
        : Sort.by(Sort.Direction.ASC, "createdDate");
    return PageRequest.of(Math.max(0, page), Math.max(1, size), sort);
  }

  private String resolveAccountName(Account account) {
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
    
    // Ensure candidate is initialized to access lazy properties
    try {
      String firstName = StringUtils.hasText(candidate.getFirstName()) ? candidate.getFirstName().trim() : "";
      String lastName = StringUtils.hasText(candidate.getLastName()) ? candidate.getLastName().trim() : "";
      String fullName = (firstName + " " + lastName).trim();
      if (StringUtils.hasText(fullName)) {
        return fullName;
      }
      if (StringUtils.hasText(candidate.getTagname())) {
        return candidate.getTagname().trim();
      }
    } catch (Exception e) {
      log.warn("Failed to resolve candidate name for id: {}, fallback to tagname or default", 
          candidate.getId(), e);
    }
    return "Candidate";
  }

  private String resolveCompanyName(Company company) {
    if (StringUtils.hasText(company.getName())) {
      return company.getName().trim();
    }
    if (StringUtils.hasText(company.getTagname())) {
      return company.getTagname().trim();
    }
    return "Company";
  }

  private String resolveAccountAvatar(Account account) {
    if (account == null) {
      return null;
    }
    if (account.getCandidate() != null) {
      return account.getCandidate().getAvatar();
    }
    if (account.getCompany() != null) {
      return account.getCompany().getAvatar();
    }
    return null;
  }

  private void ensureThreadNotBlocked(MessageThread thread) {
    String hrAccountId = resolveHrAccountId(thread);
    String candidateAccountId = resolveCandidateAccountId(thread);
    if (!StringUtils.hasText(hrAccountId) || !StringUtils.hasText(candidateAccountId)) {
      return;
    }

    if (userBlockRepository.existsByBlockerIdAndBlockedId(hrAccountId, candidateAccountId)) {
      throw new ForbiddenException("Không thể gửi tin nhắn: cuộc trò chuyện đã bị HR chặn");
    }
  }

  private String resolveHrAccountId(MessageThread thread) {
    if (thread.getCompany() == null) {
      return null;
    }
    Optional<Account> account = accountRepository.findByCompanyId(thread.getCompany().getId());
    if (account == null) {
      return null;
    }
    return account.map(Account::getId).orElse(null);
  }

  private String resolveCandidateAccountId(MessageThread thread) {
    if (thread.getCandidate() == null) {
      return null;
    }
    Optional<Account> account = accountRepository.findByCandidateId(thread.getCandidate().getId());
    if (account == null) {
      return null;
    }
    return account.map(Account::getId).orElse(null);
  }

  private MessagingResponses.BlockedUserDto toBlockedUserDto(UserBlock userBlock) {
    Account blocked = userBlock.getBlocked();
    Candidate candidate = blocked != null ? blocked.getCandidate() : null;

    return MessagingResponses.BlockedUserDto.builder()
        .userId(candidate != null ? candidate.getId() : (blocked != null ? blocked.getId() : null))
        .fullName(candidate != null ? resolveCandidateName(candidate) : resolveAccountName(blocked))
        .email(blocked != null ? blocked.getEmail() : null)
        .avatarUrl(blocked != null ? resolveAccountAvatar(blocked) : null)
        .blockedAt(userBlock.getBlockedAt())
        .reason(userBlock.getReason())
        .build();
  }

  private record ThreadParticipants(Company company, Candidate candidate, Application application) {
  }
}
