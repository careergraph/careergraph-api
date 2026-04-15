package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.request.MessagingRequests;
import com.hcmute.careergraph.persistence.dtos.response.MessagingResponses;
import com.hcmute.careergraph.persistence.models.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MessageService {

  MessagingResponses.ThreadSummaryDto getOrCreateThread(Account currentAccount,
      MessagingRequests.CreateThreadRequest request);

  Page<MessagingResponses.ThreadSummaryDto> getThreads(Account currentAccount, Pageable pageable);

  Page<MessagingResponses.ThreadSummaryDto> getThreads(Account currentAccount, boolean archived, Pageable pageable);

  MessagingResponses.ThreadSummaryDto getThread(Account currentAccount, String threadId);

  Page<MessagingResponses.MessageDto> getMessages(Account currentAccount, String threadId, Pageable pageable);

  MessagingResponses.MessageDto sendMessage(Account currentAccount,
      String threadId,
      MessagingRequests.SendMessageRequest request);

  void markThreadAsRead(Account currentAccount, String threadId);

  void deleteMessage(Account currentAccount, String messageId);

  void unsendMessage(Account currentAccount, String messageId);

  void deleteThreadForMe(Account currentAccount, String threadId);

  void archiveThread(Account currentAccount, String threadId, boolean archive);

  void blockCandidate(Account currentAccount, String candidateId, String reason);

  void unblockCandidate(Account currentAccount, String candidateId);

  MessagingResponses.BlockStatusDto getBlockStatus(Account currentAccount, String candidateId);

  List<MessagingResponses.BlockedUserDto> getBlockedCandidates(Account currentAccount);

  long getTotalUnread(Account currentAccount);
}
