package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.ChatResponse;
import com.hcmute.careergraph.persistence.dtos.response.ConversationResponse;
import com.hcmute.careergraph.persistence.models.ChatConversation;
import com.hcmute.careergraph.persistence.models.ChatMessage;
import com.hcmute.careergraph.persistence.models.Job;
import com.hcmute.careergraph.repositories.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatMapper {

    private final JobMapper jobMapper;
    private final JobRepository jobRepository;

    ConversationResponse toConversationResponse(ChatConversation conversation) {
        if (conversation == null) {
            return null;
        }

        ConversationResponse response = ConversationResponse.builder()
                .title(conversation.getTitle())
                .partyId(conversation.getPartyId())
                .partyType(conversation.getPartyType())
                .lastMessageAt(conversation.getLastMessageAt())
                .messages(toChatResponseList(conversation.getMessages()))
                .build();

        return response;
    }

    ChatResponse toChatResponse(ChatMessage chatMessage) {
        if (chatMessage == null) {
            return null;
        }

        List<Job> relatedJobs = jobRepository.findAllById(chatMessage.getRelatedJobIds());
        if (relatedJobs == null) {
            return null;
        }

        ChatResponse response = ChatResponse.builder()
                .conversationId(chatMessage.getChatConversation().getId())
                .message(chatMessage.getContent())
                .relatedJobs(jobMapper.toRelatedJobResponse((Job) relatedJobs))
                .build();

        return response;
    }

    List<ChatResponse> toChatResponseList(List<ChatMessage> chatMessages) {
        return chatMessages.stream()
                .map(chatMessage -> toChatResponse(chatMessage))
                .toList();
    }
}
