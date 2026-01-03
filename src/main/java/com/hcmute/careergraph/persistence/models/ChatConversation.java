package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.common.PartyType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_conversation")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"messages"})
@EqualsAndHashCode(callSuper = true, exclude = {"messages"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatConversation extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "chatConversation", orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> messages = new ArrayList<>();

    @Column(name = "party_type")
    @Enumerated(EnumType.STRING)
    private PartyType partyType;

    @Column(name = "party_id")
    private String partyId;
}
