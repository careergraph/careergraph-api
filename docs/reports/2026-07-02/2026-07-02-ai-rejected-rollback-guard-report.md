# AI Rejected Rollback Guard Report

## Scope

- Allow rollback only for applications currently in `REJECTED` because of AI screening.
- Keep HR manual rejection final and non-recoverable.
- Preserve the existing forward-only stage flow for every other transition.

## Backend changes

- Updated [`ApplicationServiceImpl.java`](/home/theron/Desktop/careergraph/careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/ApplicationServiceImpl.java) to allow `REJECTED -> APPLIED|SCREENING` only when the latest rejection history entry was created by `system:ai-screening`.
- Added guardrails so recovery still respects the active company pipeline for configurable stages.
- Left every other terminal-stage rule unchanged, including manual HR rejection.

## Tests

- Added targeted unit coverage in [`ApplicationServiceImplTest.java`](/home/theron/Desktop/careergraph/careergraph-api/src/test/java/com/hcmute/careergraph/services/impl/ApplicationServiceImplTest.java) for:
  - AI rejected application can move back to `SCREENING`
  - HR rejected application cannot move back
  - AI rejected application cannot jump directly to `INTERVIEW`

## Verification

- Passed: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -Dtest=ApplicationServiceImplTest test`

## Production review

- The rollback rule is intentionally narrow, which is safer than opening generic backward moves in the pipeline.
- The check uses auditable stage history instead of UI-only flags, so backend integrity remains the source of truth.
