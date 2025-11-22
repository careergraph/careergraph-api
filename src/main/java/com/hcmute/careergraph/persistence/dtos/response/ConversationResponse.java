package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.common.PartyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private String title;
    private LocalDateTime lastMessageAt;
    private List<ChatResponse> messages = new ArrayList<>();
    private PartyType partyType;
    private String partyId;
}
