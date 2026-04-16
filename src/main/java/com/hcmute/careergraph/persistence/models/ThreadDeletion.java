package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "thread_deletions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_thread_deletion_thread_account", columnNames = { "thread_id", "account_id" })
}, indexes = {
    @Index(name = "idx_thread_deletion_account", columnList = "account_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true, exclude = { "thread", "account" })
@EqualsAndHashCode(callSuper = true, exclude = { "thread", "account" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThreadDeletion extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "thread_id", nullable = false)
  private MessageThread thread;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(name = "deleted_at", nullable = false)
  private LocalDateTime deletedAt;

  @Override
  public void prePersist() {
    super.prePersist();
    if (deletedAt == null) {
      deletedAt = LocalDateTime.now();
    }
  }
}
