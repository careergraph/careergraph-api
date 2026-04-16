# Phase 5 Implementation Report

## Scope Completed

- Implemented notification persistence and realtime push flow in `NotificationServiceImpl`.
- Added application-status aggregation within 5 minutes.
- Added message notification cooldown to avoid spam.
- Added async socket push client for Node.js internal API.
- Added scheduled cleanup job for old notifications.
- Wired application status change hook in `ApplicationServiceImpl`.

## Files Changed

- [NotificationServiceImpl.java](../../src/main/java/com/hcmute/careergraph/services/impl/NotificationServiceImpl.java)
- [ApplicationServiceImpl.java](../../src/main/java/com/hcmute/careergraph/services/impl/ApplicationServiceImpl.java)
- [NotificationRepository.java](../../src/main/java/com/hcmute/careergraph/repositories/NotificationRepository.java)
- [MessageRepository.java](../../src/main/java/com/hcmute/careergraph/repositories/MessageRepository.java)
- [SocketNotificationPusher.java](../../src/main/java/com/hcmute/careergraph/services/impl/SocketNotificationPusher.java)
- [NotificationCleanupJob.java](../../src/main/java/com/hcmute/careergraph/schedule/NotificationCleanupJob.java)
- [application.yml](../../src/main/resources/application.yml)
- [NotificationServiceImplTest.java](../../src/test/java/com/hcmute/careergraph/services/impl/NotificationServiceImplTest.java)

## Notification Coverage

- `NEW_APPLICATION` - HR receives new application notice.
- `APPLICATION_VIEWED` - candidate receives a viewed-profile style update.
- `APPLICATION_SHORTLISTED` - candidate receives a review/in-progress notice.
- `APPLICATION_INTERVIEW_SCHEDULED` - candidate receives interview invitation.
- `APPLICATION_REJECTED` - candidate receives a polite rejection notice.
- `NEW_MESSAGE` - message notification with cooldown.

## Validation

- `mvn -q -DskipTests compile` - passed.
- `NotificationServiceImplTest` - passed, including log assertions and the cooldown/aggregation cases.

## Notes

- `MessageServiceImpl` already contained the message hook (`notificationService.onNewMessage(savedMessage, thread)`), so no logic refactor was needed there.
- The notification push is scheduled after transaction commit and dispatched through an async pusher bean.