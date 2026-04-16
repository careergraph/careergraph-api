# BE Multi-Job Context QA Report

Date: 2026-04-16
Owner: Copilot (GPT-5.3-Codex)
Scope: `careergraph-api` messaging backend

## 1) Implemented

- Added migration script: `init-scripts/messaging-notification-phase1-multi-job-context.sql`
  - `messages.job_context_id` (nullable FK to `jobs.id`)
  - index `(thread_id, job_context_id)`
- Added `jobContext` relation on `Message` entity.
- Extended request/response DTOs:
  - `SendMessageRequest.jobContextId`
  - `MessageDto.jobContext`
  - `ThreadJobDto`
  - `ThreadSummaryDto.jobs`, `ThreadSummaryDto.primaryJob`
- Added API capabilities:
  - `GET /messages/threads/{threadId}/jobs`
  - `GET /messages/threads/{threadId}/messages?jobId=...` (optional filter)
  - `POST /messages/threads/{threadId}/messages` now accepts optional `jobContextId`
- Added validations/business rules:
  - `jobContextId` must belong to candidate+company of the thread.
  - rejects invalid job context with `BadRequestException`.
- Added per-job repository queries for unread count and last message time.

## 2) Compatibility

- Existing messaging flow is preserved:
  - thread model unchanged (`1 thread per company-candidate`)
  - `jobContextId` is optional (null => general message)
  - existing send/read/delete/archive/block flows untouched
- Kept legacy `ThreadSummaryDto.application` field for backward compatibility.

## 3) Validation

### Static/IDE checks

- No IDE errors in updated backend files.

### Build

Command:

```bash
cd careergraph-api
./mvnw -q -DskipTests compile
```

Result:

- PASS (command completed successfully)

## 4) Checklist Mapping (Phase 1 style)

- [x] Migration added and idempotent (`IF NOT EXISTS`).
- [x] Entity/DTO/repository/service/controller updated.
- [x] Optional filter by job context added to message listing.
- [x] Dedicated thread-jobs API added for FE switcher/filter UI.
- [x] Validation added to prevent unrelated job tagging.
- [ ] Full API Postman matrix executed (not run in this pass).
- [ ] Integration tests for new endpoints (not available yet).

## 5) Risks / Follow-up

- Thread summary now resolves per-job metadata; for very large datasets this may need query optimization/caching.
- Recommended next step: add integration tests for:
  - invalid `jobContextId` send rejection,
  - `messages?jobId=` filtering,
  - `threads/{id}/jobs` ordering and unread counters.
