# Interview Room Readmit Fix Report

Date: 2026-07-01
Repo: `careergraph-api`

## Scope

Fix backend state mismatch when HR kicks a candidate out of an online interview room and later admits the same candidate back into the same room.

## Root Cause

- `removeParticipant(...)` changed participant state to `REMOVED`.
- `admitParticipant(...)` rejected any participant already marked `REMOVED`.
- The RTC layer still allowed HR to approve the same candidate again, so:
  - signaling layer considered the candidate re-admitted,
  - backend room participant state still behaved as removed,
  - downstream features depending on room participants, especially recording assignment, saw inconsistent data.

## Minimal Change Applied

File: `src/main/java/com/hcmute/careergraph/services/impl/InterviewRoomServiceImpl.java`

- Removed the hard block that prevented `REMOVED` participants from being admitted again in the same session.
- On readmit, reset `leftAt` to `null`.
- Kept existing `joinedAt = now()` behavior so room participation remains aligned with current join.

Relevant lines after fix:

- `admitParticipant(...)` around line 181
- `setJoinedAt(...)` around line 186
- `setLeftAt(null)` around line 187

## Why This Is Safe

- Change is isolated to room participant state transition only.
- No endpoint contract changed.
- Existing room, interview, and recording APIs remain unchanged.
- The behavior now matches current product logic: HR kick is temporary unless a permanent ban flow exists separately.

## Verification

- `mvn -q -DskipTests compile` passed.

## QA Notes

Expected regression checks:

1. HR admits candidate for the first time: participant becomes `ADMITTED`.
2. HR kicks candidate: participant gets `leftAt`, candidate leaves call.
3. Candidate requests to rejoin: HR can admit again successfully.
4. Recording assignment modal can still find the candidate after rejoin.
5. Completing interview after rejoin still works.

## UX / Production Notes

- Current naming `REMOVED` is slightly misleading because actual behavior is now "removed from current call, may rejoin later".
- For long-term maintainability, consider a future explicit status split such as `KICKED_TEMP` vs `BANNED_PERMANENT`, but this was intentionally not changed in this patch to keep impact minimal.
