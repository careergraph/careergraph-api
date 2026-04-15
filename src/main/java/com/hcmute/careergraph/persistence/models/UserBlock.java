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
@Table(name = "user_blocks", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_block_blocker_blocked", columnNames = { "blocker_account_id", "blocked_account_id" })
}, indexes = {
    @Index(name = "idx_user_block_blocker", columnList = "blocker_account_id"),
    @Index(name = "idx_user_block_blocked", columnList = "blocked_account_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true, exclude = { "blocker", "blocked" })
@EqualsAndHashCode(callSuper = true, exclude = { "blocker", "blocked" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserBlock extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "blocker_account_id", nullable = false)
  private Account blocker;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "blocked_account_id", nullable = false)
  private Account blocked;

  @Column(name = "reason", length = 255)
  private String reason;

  @Column(name = "blocked_at", nullable = false)
  private LocalDateTime blockedAt;

  @Override
  public void prePersist() {
    super.prePersist();
    if (blockedAt == null) {
      blockedAt = LocalDateTime.now();
    }
  }
}
