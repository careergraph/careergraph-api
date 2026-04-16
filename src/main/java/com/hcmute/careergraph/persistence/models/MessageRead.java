package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_reads", uniqueConstraints = {
    @UniqueConstraint(name = "uk_message_read_thread_account", columnNames = { "thread_id", "account_id" })
}, indexes = {
    @Index(name = "idx_message_read_account", columnList = "account_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true, exclude = { "thread", "account", "lastReadMessage" })
@EqualsAndHashCode(callSuper = true, exclude = { "thread", "account", "lastReadMessage" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageRead extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "thread_id", nullable = false)
  private MessageThread thread;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "last_read_message_id")
  private Message lastReadMessage;

  @Column(name = "last_read_at", nullable = false)
  private LocalDateTime lastReadAt;

  @Override
  public void prePersist() {
    super.prePersist();
    if (lastReadAt == null) {
      lastReadAt = LocalDateTime.now();
    }
  }
}
