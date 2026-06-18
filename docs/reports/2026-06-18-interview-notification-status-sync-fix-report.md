# Interview Notification Status Sync Fix Report - 2026-06-18

## Scope

- Added interview-status notification coverage for both HR and candidate flows.
- Kept existing realtime socket push flow and extended it with new notification types.
- Ensured notification payloads carry stable redirect targets for frontend refresh-on-click behavior.

## Backend Changes

- Added new `NotificationType` values:
  - `INTERVIEW_CONFIRMED`
  - `INTERVIEW_DECLINED`
  - `INTERVIEW_CANCELLED`
  - `INTERVIEW_RESCHEDULE_PROPOSED`
  - `INTERVIEW_RESCHEDULE_ACCEPTED`
  - `INTERVIEW_RESCHEDULE_REJECTED`
- Extended `NotificationService` and `NotificationServiceImpl` with dedicated interview event handlers.
- Hooked notifications into `InterviewServiceImpl` for:
  - candidate confirms interview
  - candidate declines interview
  - HR cancels interview
  - candidate proposes alternate time
  - HR accepts alternate time
  - HR rejects alternate time
- Standardized interview notification payload data:
  - `interviewId`
  - `applicationId`
  - `jobId`
  - `candidateId`
  - `companyId`
  - `scheduledAt`
  - `interviewStatus`
  - `navigateTo`

## Test Result

- `./mvnw -q -DskipTests compile` - PASS

## QA Notes

- Interview lifecycle now emits business-meaningful notifications instead of only initial scheduling.
- Payload design is production-safer because frontend no longer has to guess destination context from incomplete data.
- Residual risk:
  - There are still no automated integration tests asserting notification creation per interview action.

## Recommended Follow-up

- Add service-level tests for every interview notification event.
- Add API tests that validate `navigateTo` and `interviewStatus` payload content.
