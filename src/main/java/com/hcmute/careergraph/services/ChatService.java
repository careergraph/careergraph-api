package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.response.ChatResponse;
import com.hcmute.careergraph.persistence.models.ChatConversation;
import com.hcmute.careergraph.persistence.models.ChatMessage;

import java.util.List;

public interface ChatService {

    ChatResponse chat(String userId, String message, String conversationId);

    List<ChatConversation> getAllConversation(String userId);

    List<ChatMessage> getConversationHistory(String userId, String conversationId);

    ChatConversation getLastedConversation(String userId);
}
