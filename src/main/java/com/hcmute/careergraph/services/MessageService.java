package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.request.MessagingRequests;
import com.hcmute.careergraph.persistence.dtos.response.MessagingResponses;
import com.hcmute.careergraph.persistence.models.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageService {

  MessagingResponses.ThreadSummaryDto getOrCreateThread(Account currentAccount,
      MessagingRequests.CreateThreadRequest request);

  Page<MessagingResponses.ThreadSummaryDto> getThreads(Account currentAccount, Pageable pageable);

  MessagingResponses.ThreadSummaryDto getThread(Account currentAccount, String threadId);

  Page<MessagingResponses.MessageDto> getMessages(Account currentAccount, String threadId, Pageable pageable);

  MessagingResponses.MessageDto sendMessage(Account currentAccount,
      String threadId,
      MessagingRequests.SendMessageRequest request);

  void markThreadAsRead(Account currentAccount, String threadId);

  void deleteMessage(Account currentAccount, String messageId);

  long getTotalUnread(Account currentAccount);
}
