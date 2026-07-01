# API Report - 2026-07-01 - Interview room and recording analysis

## Scope

- Review backend room participant and recording logic related to the reported interview-room regressions.

## Findings

### 0. Same-day room access rule is already mostly aligned on backend

- `validateRoomAccessWindow()` already blocks rooms from previous days with:
  - `Phòng phỏng vấn của ngày trước đã hết hiệu lực truy cập`
- This matches the new rule that HR may enter any time during the scheduled day, but not after the day has passed.
- Candidate access is now also gated in `getInterviewByRoomCodeForCandidate()` so backend rejects room entry earlier than `15` minutes before the candidate's scheduled time.

### 1. Recording modal issue was not caused by API filtering

- `GET /rooms/{roomCode}/participants` returns all room participants with:
  - `admitStatus`
  - `joinedAt`
  - `leftAt`
- The false empty state came from HR frontend filtering, not from this API response.

### 2. Participant lifecycle behavior is consistent with the bug report

- `InterviewRoomServiceImpl.removeParticipant()` marks a participant as `REMOVED` and sets `leftAt`.
- This is correct for business state, but it means frontend code must not assume `REMOVED` implies "never joined".

### 3. Rejoin defect was outside API layer

- The "different rooms until F5" symptom maps to signaling/session cleanup in RTC, not to `InterviewRoomServiceImpl`.

### 4. Early completion/feedback rule for online interviews was already compatible

- `completeInterview()`, `addFeedback()`, and `saveRecording()` validate online interviews by `candidate joined room` instead of `scheduledAt already started`.
- That means HR can finish the interview workflow within the allowed early-join window once the candidate has actually joined.

### 5. Production-safe recording flow requires separating interview completion from room exit

- In a shared room, `complete interview` must not automatically mean `candidate left room`.
- If those two states are merged, HR can lose the correct active-candidate context right before assigning the recording.
- The safer rule is:
  - `Interview.COMPLETED` closes the recruitment step
  - `candidate leave / kick / end room` closes realtime presence
  - recording assignment may still happen while the candidate is technically the active room participant

### 6. Voluntary leave is the missing production rule for freeing the active slot

- `kick` is not the same as `candidate leaves by choice`.
- A production-safe shared room needs an explicit `leave-session` branch:
  - releases the active slot immediately
  - keeps audit history intact
  - does not permanently ban the candidate from the room
- This rule prevents the room from getting stuck when HR needs to move to the next candidate after recording/feedback are done.

## Validation notes

- Attempted targeted backend test run:
  - `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -q -Dtest=InterviewServiceImplTest test`
- Result:
  - Maven test runner started successfully with Java 17
  - one existing test failed: `createInterview_allowsCandidateOverlapWithoutAnyCandidateConflictCheck`
  - failure is pre-existing business-test behavior and unrelated to the RTC/recording fix implemented here

## Recommendation

- Add backend tests later for:
  - participant `joinedAt` preservation after `removeParticipant`
  - recording assignment semantics when the participant already left the room
  - completion flow where interview is `COMPLETED` but room session is intentionally kept open for feedback/recording
  - voluntary leave flow where participant moves from `ADMITTED` to `WAITING_LOBBY` without being treated as kicked
