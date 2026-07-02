# Kanban stage contract review

## Scope reviewed

- `GET /applications/{id}`
- `PUT /applications/{id}/stage`
- Existing enum transition behavior around `HR_CONTACTED`, `INTERVIEW`, and `INTERVIEW_SCHEDULED`

## Findings

- No backend source change was required for this request.
- The current API contract already supports:
  - fetching a single application for UI resync
  - updating application stage explicitly
  - preserving interview scheduling logic via `INTERVIEW_SCHEDULED`
- FE fix now consumes the existing contract more correctly.

## QA note

- The UI stale-state bug was a frontend synchronization issue, not a backend transition issue.
- Because backend code was not changed, no API regression was introduced by this task.

## Recommendation

- If future work expands auto-stage behavior after messaging, documenting that rule in API docs would help FE avoid ambiguous assumptions.
