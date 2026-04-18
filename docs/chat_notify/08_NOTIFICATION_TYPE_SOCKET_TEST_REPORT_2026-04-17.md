# Notification Type Socket Test Report

- Date: 2026-04-17
- Scope: Realtime notify delivery + unread-counts emission via RTC internal API
- Environment: local RTC server on port 4000
- Script: careergraph-rtc/scripts/verify-socket-events.js

## NotificationType matrix under test
Source enum:
- NEW_MESSAGE
- APPLICATION_STATUS_CHANGED
- NEW_APPLICATION
- APPLICATION_VIEWED
- APPLICATION_SHORTLISTED
- APPLICATION_REJECTED
- APPLICATION_INTERVIEW_SCHEDULED

## Test cases
1. Unauthorized internal notify request is rejected (401).
2. For each NotificationType above:
- Push notification via /internal/notify.
- Verify socket event notification is received by target user room.
- Verify payload.type matches expected NotificationType.
3. Push unread counts via /internal/unread-counts.
- Verify unread-counts payload matches expected values.

## Executed command
JWT_SIGNER_KEY='<redacted>' npm --prefix /home/theron/Desktop/careergraph/careergraph-rtc run test:sockets

## Verification logs
- [test] root-rtc: PASS
- [test] chat: PASS
- [test][notify-type] NEW_MESSAGE: PASS
- [test][notify-type] APPLICATION_STATUS_CHANGED: PASS
- [test][notify-type] NEW_APPLICATION: PASS
- [test][notify-type] APPLICATION_VIEWED: PASS
- [test][notify-type] APPLICATION_SHORTLISTED: PASS
- [test][notify-type] APPLICATION_REJECTED: PASS
- [test][notify-type] APPLICATION_INTERVIEW_SCHEDULED: PASS
- [test][notify-unread-counts] PASS
- [test] notify: PASS
- [test] All socket namespace checks passed

## Runtime server logs (evidence)
- [notify] push to notify-user-1: NEW_MESSAGE
- [notify] push to notify-user-1: APPLICATION_STATUS_CHANGED
- [notify] push to notify-user-1: NEW_APPLICATION
- [notify] push to notify-user-1: APPLICATION_VIEWED
- [notify] push to notify-user-1: APPLICATION_SHORTLISTED
- [notify] push to notify-user-1: APPLICATION_REJECTED
- [notify] push to notify-user-1: APPLICATION_INTERVIEW_SCHEDULED

## Result
PASS - Tat ca notification types trong enum da duoc verify socket delivery thanh cong.

## Risks and follow-up
- Hien tai test cover luong socket/internal API. Nen bo sung test integration API service de assert moi business event thuc su goi NotificationService hook.
- Nen them smoke test CI cho test:sockets voi env JWT_SIGNER_KEY va INTERNAL_API_KEY de tranh regression.
