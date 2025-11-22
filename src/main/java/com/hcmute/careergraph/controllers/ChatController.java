package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.exception.BadRequestException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.dtos.request.ChatRequest;
import com.hcmute.careergraph.services.ChatService;
import com.hcmute.careergraph.persistence.dtos.response.ChatResponse;
import com.hcmute.careergraph.persistence.models.ChatConversation;
import com.hcmute.careergraph.persistence.models.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("chat")
@RequiredArgsConstructor
public class ChatController {

    private final SecurityUtils securityUtils;
    private final ChatService chatService;

    @PostMapping("/chat")
    public RestResponse<ChatResponse> chat(Authentication authentication,
                                           @RequestBody ChatRequest.ChatMessageRequest request) {

        String userId = securityUtils.extractCandidateId(authentication);

        ChatResponse response = chatService.chat(userId, request.getMessage(), request.getConversationId());
        return RestResponse.<ChatResponse>builder()
                .status(HttpStatus.OK)
                .message("Chat response successful")
                .data(response)
                .build();
    }

    @GetMapping("/conversations")
    public RestResponse<List<ChatConversation>> getAllConversations(Authentication authentication) {
        String userId = securityUtils.extractCandidateId(authentication);
        if (userId == null) {
            throw new BadRequestException("User ID is required");
        }

        List<ChatConversation> conversations = chatService.getAllConversation(userId);
        return RestResponse.<List<ChatConversation>>builder()
                .status(HttpStatus.OK)
                .message("Fetched all conversations successfully")
                .data(conversations)
                .build();
    }

    @GetMapping("/lasted")
    public RestResponse<ChatConversation> getLastedConversation(Authentication authentication) {
        String userId = securityUtils.extractCandidateId(authentication);
        if (userId == null) {
            throw new BadRequestException("User ID is required");
        }

        ChatConversation conversations = chatService.getLastedConversation(userId);
        return RestResponse.<ChatConversation>builder()
                .status(HttpStatus.OK)
                .message("Fetched all conversations successfully")
                .data(conversations)
                .build();
    }

    @GetMapping("/history")
    public RestResponse<List<ChatMessage>> getConversationHistory(Authentication authentication,
                                                                  @RequestParam String conversationId) {
        String userId = securityUtils.extractCandidateId(authentication);
        if (userId == null) {
            throw new BadRequestException("User ID is required");
        }
        List<ChatMessage> history = chatService.getConversationHistory(userId, conversationId);
        return RestResponse.<List<ChatMessage>>builder()
                .status(HttpStatus.OK)
                .message("Fetched conversation history successfully")
                .data(history)
                .build();
    }
}
