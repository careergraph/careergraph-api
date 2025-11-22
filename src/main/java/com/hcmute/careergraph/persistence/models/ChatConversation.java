package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    @Column(name = "total_messages")
    @Builder.Default
    private Integer totalMessages = 0;

    @OneToMany(mappedBy = "chatConversation", orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> messages = new ArrayList<>();
}
