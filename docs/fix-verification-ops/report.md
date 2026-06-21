# API Fix Report

## Scope

- Added `GET /admin/companies/{companyId}` so admin can open company controls without depending on a verification request ID.
- Added `GET /admin/dashboard-summary` to back admin dashboard cards and pending verification intake.
- Preserved verification history by creating a new verification request on HR resubmission instead of overwriting the previous record.
- Kept verification documents attached to historical requests for both HR and admin review.
- Added alias support for HR history endpoint and fixed the admin company list endpoint/query path used by frontend.
- Hardened messaging unread count so unsupported roles now receive `0` instead of a `500`.

## Production Notes

- Admin dashboard now has a compact summary contract:
  - `pendingVerification`
  - `reviewedToday`
  - `companiesMonitored`
  - `policyIncidents`
  - `latestPendingRequests`
- Company moderation pages now resolve from direct company context and still reuse the latest verification snapshot mapping.
- Company block/unblock still triggers job search sync and realtime notifications.

## Verification

- `mvn -q -DskipTests compile`: passed on June 21, 2026.
- Targeted Maven test execution was attempted, but full test resolution was blocked by local network restrictions to Maven Central in this environment.

## Risks / Follow-up

- Add dedicated service tests for the new admin dashboard summary and direct company detail methods when network-enabled test execution is available.
