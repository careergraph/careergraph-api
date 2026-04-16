package com.hcmute.careergraph.repositories;

import com.hcmute.careergraph.enums.notification.NotificationType;
import com.hcmute.careergraph.persistence.models.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

  Page<Notification> findByRecipientIdOrderByCreatedDateDesc(String recipientId, Pageable pageable);

  long countByRecipientIdAndReadFalse(String recipientId);

  Page<Notification> findByRecipientIdAndTypeAndCreatedDateGreaterThanEqualOrderByCreatedDateDesc(
      String recipientId,
      NotificationType type,
      LocalDateTime since,
      Pageable pageable);

  Page<Notification> findByRecipientIdAndTypeAndReadFalseAndCreatedDateGreaterThanEqualOrderByCreatedDateDesc(
      String recipientId,
      NotificationType type,
      LocalDateTime since,
      Pageable pageable);

  @Modifying
  @Query("UPDATE Notification n SET n.read = true, n.readAt = :now WHERE n.recipient.id = :accountId AND n.read = false")
  int markAllAsRead(@Param("accountId") String accountId, @Param("now") LocalDateTime now);

  @Modifying
  @Query("DELETE FROM Notification n WHERE n.createdDate < :cutoff")
  int deleteByCreatedDateBefore(@Param("cutoff") LocalDateTime cutoff);
}
