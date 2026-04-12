package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.common.Role;
import com.hcmute.careergraph.enums.message.MessageContentType;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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

  private final MessageThreadRepository messageThreadRepository;
  private final MessageRepository messageRepository;
  private final MessageReadRepository messageReadRepository;
  private final CandidateRepository candidateRepository;
  private final CompanyRepository companyRepository;
  private final ApplicationRepository applicationRepository;
  private final AccountRepository accountRepository;
  private final NotificationService notificationService;

  @Override
  public MessagingResponses.ThreadSummaryDto getOrCreateThread(Account currentAccount,
      MessagingRequests.CreateThreadRequest request) {
    ThreadParticipants participants = resolveThreadParticipants(currentAccount, request);

    Optional<MessageThread> existing = messageThreadRepository
        .findByCompanyIdAndCandidateId(participants.company().getId(), participants.candidate().getId());

    if (existing.isPresent()) {
      MessageThread thread = existing.get();
      if (thread.getApplication() == null && participants.application() != null) {
        thread.setApplication(participants.application());
        thread = messageThreadRepository.save(thread);
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

    try {
      MessageThread saved = messageThreadRepository.save(newThread);
      return toThreadSummary(currentAccount, saved);
    } catch (DataIntegrityViolationException ex) {
      MessageThread recovered = messageThreadRepository
          .findByCompanyIdAndCandidateId(participants.company().getId(), participants.candidate().getId())
          .orElseThrow(() -> ex);
      return toThreadSummary(currentAccount, recovered);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Page<MessagingResponses.ThreadSummaryDto> getThreads(Account currentAccount, Pageable pageable) {
    Role role = resolveRole(currentAccount);
    Pageable normalized = normalizeThreadPageable(pageable);

    Page<MessageThread> page = role == Role.HR
        ? messageThreadRepository.findByCompanyId(currentAccount.getCompany().getId(), normalized)
        : messageThreadRepository.findByCandidateId(currentAccount.getCandidate().getId(), normalized);

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
  public Page<MessagingResponses.MessageDto> getMessages(Account currentAccount, String threadId, Pageable pageable) {
    MessageThread thread = findThreadOrThrow(threadId);
    validateThreadAccess(thread, currentAccount);

    Pageable normalized = normalizeMessagePageable(pageable);
    Page<Message> messages = messageRepository.findByThreadId(threadId, normalized);

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

    MessageContentType contentType = request.getContentType() != null
        ? request.getContentType()
        : MessageContentType.TEXT;

    String content = request.getContent() != null ? request.getContent().trim() : "";
    if (contentType == MessageContentType.TEXT && !StringUtils.hasText(content)) {
      throw new BadRequestException("Message content is required");
    }
    if (contentType != MessageContentType.TEXT
        && !StringUtils.hasText(request.getFileUrl())
        && !StringUtils.hasText(content)) {
      throw new BadRequestException("fileUrl or content is required for non-text messages");
    }
    if (!StringUtils.hasText(content)) {
      content = StringUtils.hasText(request.getFileName()) ? request.getFileName().trim() : "[Attachment]";
    }

    Message message = Message.builder()
        .thread(thread)
        .sender(currentAccount)
        .content(content)
        .contentType(contentType)
        .fileUrl(request.getFileUrl())
        .fileName(request.getFileName())
        .fileSize(request.getFileSize())
        .deleted(false)
        .build();

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
    Message message = messageRepository.findById(messageId)
        .orElseThrow(() -> new NotFoundException("Message not found"));

    if (!message.getSender().getId().equals(currentAccount.getId())) {
      throw new ForbiddenException("Only sender can delete this message");
    }
    if (message.isDeleted()) {
      return;
    }

    message.setDeleted(true);
    message.setDeletedAt(LocalDateTime.now());
    message.setContent("Message was deleted");
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
  @Transactional(readOnly = true)
  public long getTotalUnread(Account currentAccount) {
    Role role = resolveRole(currentAccount);

    var threadIds = role == Role.HR
        ? messageThreadRepository.findIdsByCompanyId(currentAccount.getCompany().getId())
        : messageThreadRepository.findIdsByCandidateId(currentAccount.getCandidate().getId());

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

    long unreadCount = messageRepository.countUnreadInThread(thread.getId(), currentAccount.getId(), EPOCH);

    return MessagingResponses.ThreadSummaryDto.builder()
        .threadId(thread.getId())
        .otherParty(otherParty)
        .application(applicationSummary)
        .lastMessagePreview(thread.getLastMessagePreview())
        .lastMessageAt(thread.getLastMessageAt())
        .unreadCount(unreadCount)
        .online(false)
        .build();
  }

  private MessagingResponses.PartySummaryDto resolveOtherParty(Account currentAccount, MessageThread thread) {
    Role role = resolveRole(currentAccount);
    if (role == Role.HR) {
      Candidate candidate = thread.getCandidate();
      return MessagingResponses.PartySummaryDto.builder()
          .id(candidate.getId())
          .displayName(resolveCandidateName(candidate))
          .avatar(candidate.getAvatar())
          .partyType("CANDIDATE")
          .build();
    }

    Company company = thread.getCompany();
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
        .content(message.isDeleted() ? "Message was deleted" : message.getContent())
        .contentType(message.getContentType())
        .fileUrl(message.getFileUrl())
        .fileName(message.getFileName())
        .fileSize(message.getFileSize())
        .deleted(message.isDeleted())
        .createdAt(message.getCreatedDate())
        .read(isRead)
        .readAt(isRead ? readAt : null)
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
    String firstName = StringUtils.hasText(candidate.getFirstName()) ? candidate.getFirstName().trim() : "";
    String lastName = StringUtils.hasText(candidate.getLastName()) ? candidate.getLastName().trim() : "";
    String fullName = (firstName + " " + lastName).trim();
    if (StringUtils.hasText(fullName)) {
      return fullName;
    }
    if (StringUtils.hasText(candidate.getTagname())) {
      return candidate.getTagname().trim();
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

  private record ThreadParticipants(Company company, Candidate candidate, Application application) {
  }
}
