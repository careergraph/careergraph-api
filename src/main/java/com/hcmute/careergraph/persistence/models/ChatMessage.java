package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.common.Role;
import com.hcmute.careergraph.helper.JsonUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_conversation_id", columnList = "chat_conversation_id"),
                @Index(name = "idx_chat_conversation_created", columnList = "chat_conversation_id, created_date")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Convert(converter = JsonUtils.StringListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_job_ids", columnDefinition = "jsonb")
    private List<String> relatedJobIds = new ArrayList<>();

    @Convert(converter = JsonUtils.HashMapConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_conversation_id")
    @ToString.Exclude
    private ChatConversation chatConversation;
}
