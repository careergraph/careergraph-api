package com.hcmute.careergraph.persistence.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @JsonProperty("message")
    private String message;

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("previous_messages")
    private List<String> previousMessages;

    @JsonProperty("user_location")
    private String userLocation;

    // Request send from UI web
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageRequest {
        private String message;
        private String conversationId;
    }
}
