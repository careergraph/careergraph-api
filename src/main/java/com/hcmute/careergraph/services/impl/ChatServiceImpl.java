package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.common.PartyType;
import com.hcmute.careergraph.enums.common.Role;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.exception.ForbiddenException;
import com.hcmute.careergraph.persistence.dtos.request.ChatRequest;
import com.hcmute.careergraph.persistence.dtos.response.ChatResponse;
import com.hcmute.careergraph.persistence.models.ChatConversation;
import com.hcmute.careergraph.persistence.models.ChatMessage;
import com.hcmute.careergraph.repositories.ChatConversationRepository;
import com.hcmute.careergraph.repositories.ChatMessageRepository;
import com.hcmute.careergraph.services.ChatService;
import com.hcmute.careergraph.services.FastAPIClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final FastAPIClientService fastAPIClientService;


    @Override
    public ChatResponse chat(String userId, String message, String conversationId) {

        // Step 1: Validate or create conversation
        ChatConversation conversation;
        if (conversationId == null) {
            conversation = ChatConversation.builder()
                    .partyId(userId)
                    .partyType(PartyType.CANDIDATE)
                    .title(generateTitle(message))
                    .status(Status.ACTIVE)
                    .build();
            conversation = chatConversationRepository.save(conversation);
            conversationId = conversation.getId();

            log.info("Created new conversation: {} for user: {}", conversationId, userId);
        } else {

            conversation = chatConversationRepository.findById(conversationId)
                    .filter(c -> c.getPartyId().equals(userId))
                    .orElseThrow(() -> new ForbiddenException("Conversation not found or access denied"));
        }

        // Step 2: Query messages for context
        List<String> previousMessages = chatMessageRepository
                .findLastFormattedMessage(conversationId);

        // Step 3: Build request for FastAPI
        ChatRequest request = ChatRequest.builder()
                .message(message)
                .conversationId(conversationId)
                .userId(userId)
                .previousMessages(previousMessages)
                .build();

        // Step 4: Gọi FastAPI để generate AI response
        ChatResponse aiResponse = fastAPIClientService.chat(request);

        // Step 5: Lưu USER message
        ChatConversation chatConversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new BadRequestException("Chat conversation not found"));


        ChatMessage userMsg = ChatMessage.builder()
                .chatConversation(chatConversation)
                .role(Role.USER)
                .content(message)
                .createdBy(userId)
                .build();
        chatMessageRepository.save(userMsg);

        // Step 6: Lưu ASSISTANT response
        List<String> jobIds = aiResponse.getRelatedJobs() != null
                ? aiResponse.getRelatedJobs().stream()
                        .map(job -> job.getJobId())
                        .toList()
                : Collections.emptyList();

        ChatMessage assistantMsg = ChatMessage.builder()
                .chatConversation(chatConversation)
                .role(Role.ASSISTANT)
                .content(aiResponse.getMessage())
                .relatedJobIds(jobIds)
                .createdBy("_sys")
                .build();
        chatMessageRepository.save(assistantMsg);

        log.info("Chat completed. Conversation: {}", conversationId);

        return aiResponse;
    }

    @Override
    public List<ChatConversation> getAllConversation(String userId) {

        List<ChatConversation> conversations = chatConversationRepository
                .findAllByPartyIdAndPartyType(userId, PartyType.CANDIDATE);

        if (conversations == null) {
            return new ArrayList<>();
        }

        return conversations;
    }

    @Override
    public List<ChatMessage> getConversationHistory(String userId, String conversationId) {
        return List.of();
    }

    @Override
    public ChatConversation getLastedConversation(String userId) {

        if (userId == null) {
            // TODO: Handle case user not login to system
        }

        ChatConversation conversation = chatConversationRepository.findLastedConversation(userId, PartyType.CANDIDATE)
                .orElse(ChatConversation.builder()
                        .partyId(userId)
                        .partyType(PartyType.CANDIDATE)
                        .title(generateTitle("Hyra AI chào ứng viên. Hyra là trợ lý thông minh chuyên về tìm kiếm việc làm và tuyển dụng tại CareerGraph"))
                        .status(Status.ACTIVE)
                        .build());

        return conversation;
    }

    private String generateTitle(String message) {
        return message.length() <= 50 ? message : message.substring(0, 47) + "...";
    }
}
