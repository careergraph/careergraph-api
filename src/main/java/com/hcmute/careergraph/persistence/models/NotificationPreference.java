package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.notification.NotificationType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
    @UniqueConstraint(name = "uk_notification_pref_account_type", columnNames = { "account_id", "type" })
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true, exclude = { "account" })
@EqualsAndHashCode(callSuper = true, exclude = { "account" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationPreference extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 50)
  private NotificationType type;

  @Column(name = "in_app_enabled", nullable = false)
  private boolean inAppEnabled = true;

  @Column(name = "email_enabled", nullable = false)
  private boolean emailEnabled;
}
