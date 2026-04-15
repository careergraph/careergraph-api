package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.message.MessageContentType;
import com.hcmute.careergraph.enums.message.MessageDeleteType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_thread_created", columnList = "thread_id, created_date"),
    @Index(name = "idx_messages_sender", columnList = "sender_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true, exclude = { "thread", "sender" })
@EqualsAndHashCode(callSuper = true, exclude = { "thread", "sender" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "thread_id", nullable = false)
  private MessageThread thread;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private Account sender;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "content_type", nullable = false, length = 20)
  private MessageContentType contentType;

  @Column(name = "file_url", length = 500)
  private String fileUrl;

  @Column(name = "file_name", length = 255)
  private String fileName;

  @Column(name = "file_size")
  private Long fileSize;

  @Column(name = "is_deleted", nullable = false)
  private boolean deleted;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "delete_type", length = 20)
  private MessageDeleteType deleteType;

  @Override
  public void prePersist() {
    super.prePersist();
    if (contentType == null) {
      contentType = MessageContentType.TEXT;
    }
  }
}
