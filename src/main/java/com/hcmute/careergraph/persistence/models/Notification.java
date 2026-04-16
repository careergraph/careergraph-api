package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.notification.NotificationType;
import com.hcmute.careergraph.helper.JsonUtils;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_recipient_created", columnList = "recipient_id, created_date"),
    @Index(name = "idx_notification_recipient_read", columnList = "recipient_id, is_read")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true, exclude = { "recipient" })
@EqualsAndHashCode(callSuper = true, exclude = { "recipient" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class Notification extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipient_id", nullable = false)
  private Account recipient;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 50)
  private NotificationType type;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "body", nullable = false, length = 500)
  private String body;

  @Convert(converter = JsonUtils.HashMapConverter.class)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data", columnDefinition = "jsonb")
  private HashMap<String, Object> data = new HashMap<>();

  @Column(name = "is_read", nullable = false)
  private boolean read;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  @Override
  public void prePersist() {
    super.prePersist();
    if (data == null) {
      data = new HashMap<>();
    }
  }
}
