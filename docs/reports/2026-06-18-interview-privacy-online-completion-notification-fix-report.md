# Interview Privacy + Online Completion + Notification Constraint Fix Report

- Date: 2026-06-18
- Scope: `careergraph-api`

## Issues Confirmed

1. HR hủy interview bị fail ở DB vì backend gửi `INTERVIEW_CANCELLED` nhưng constraint `notifications_type_check` chưa chứa type này.
2. Online interview bị chặn hoàn thành và thêm feedback nếu chưa tới `scheduledAt`, dù ứng viên đã vào phòng sớm hợp lệ.
3. Rule trùng lịch ứng viên đang chặn cả interview cùng công ty, gây sai nghiệp vụ:
   - cùng 1 công ty, candidate có thể interview 2 job khác nhau cùng khung giờ
   - khác công ty thì phải chặn để bảo vệ quyền riêng tư

## Backend Changes

- Added cross-company overlap query:
  - `InterviewRepository.findOverlappingByCandidateInOtherCompanies(...)`
- Centralized privacy validation in `InterviewServiceImpl.validateCandidatePrivacyConflict(...)`
- Applied privacy validation to:
  - create interview
  - HR reschedule interview
  - accept candidate reschedule proposal
- Centralized HR action timing rule in `InterviewServiceImpl.validateInterviewReadyForHrAction(...)`
  - `ONLINE`: nếu có room/app binding thì chỉ cần candidate đã `joinedAt`
  - `OFFLINE` and fallback online-without-room-data: vẫn phải tới `scheduledAt`
- Reordered reschedule/proposal flows to validate replacement slot before cancelling original interview record.

## Database Hotfix

- Added script:
  - [2026-06-18-notification-type-check-hotfix.sql](/abs/path/d:/DaiHoc/DoAn/careergraph-api/init-scripts/2026-06-18-notification-type-check-hotfix.sql)
- Script refreshes check constraints for:
  - `notifications.type`
  - `notification_preferences.type`

## Verification

- Added unit test file:
  - [InterviewServiceImplTest.java](/abs/path/d:/DaiHoc/DoAn/careergraph-api/src/test/java/com/hcmute/careergraph/services/impl/InterviewServiceImplTest.java)
- Covered cases:
  - online interview can complete before scheduled time after candidate joined
  - online interview can add feedback before scheduled time after candidate joined
  - same-company overlap across different jobs is allowed
  - cross-company overlap is blocked with privacy error
- Executed:
  - `mvn -q -Dtest=InterviewServiceImplTest test`
- Result: passed

## Production Notes

- Apply SQL hotfix before/with backend deployment if the target DB still has the old `notifications_type_check`.
- Candidate early join window was not changed in backend room access flow; this fix only changes post-join completion/feedback eligibility for online interviews.
