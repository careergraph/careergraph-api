# Interview / Kanban / Calendar QA Matrix

## Scope
- Interview scheduling and rescheduling
- Kanban stage transitions (including Offboarded)
- Room access and lifecycle constraints
- Feedback recommendation outcomes

## Company Config Preconditions
- `offerBeforeTrial=true|false`
- `enableOffboardedStage=true|false`

## Test Matrix

| ID | Area | Scenario | Precondition | Steps | Expected Result |
|---|---|---|---|---|---|
| QA-001 | Calendar Create | Create online interview in future | Candidate in schedulable stage | Create with date/time > now | Success, interview status SCHEDULED, room code generated |
| QA-002 | Calendar Create | Create interview in past | Any valid candidate | Create with date/time < now | Rejected with validation error |
| QA-003 | Calendar Create | Existing active interview without overwrite | Candidate has SCHEDULED/CONFIRMED | Create new without confirmOverwrite | Rejected with ACTIVE_INTERVIEW marker |
| QA-004 | Calendar Create | Existing active interview with overwrite | Candidate has active interview, none IN_PROGRESS | Create new with confirmOverwrite=true | Old active interviews are CANCELLED, new interview created |
| QA-005 | Calendar Create | Existing IN_PROGRESS interview | Candidate has IN_PROGRESS | Create new | Rejected |
| QA-006 | Calendar Edit | Reschedule interview | Existing interview SCHEDULED/CONFIRMED | Edit in calendar modal and save | Original interview CANCELLED, new interview created |
| QA-007 | Calendar Edit | Reschedule online interview keeps canonical daily room | Online interview | Reschedule to same job/date | New interview meetingLink is daily room code, participant slot updated |
| QA-008 | Calendar Edit | Reschedule to past date/time | Existing interview | Edit with date/time < now | Rejected |
| QA-009 | Room Access | Candidate joins before 15 minutes | Interview exists | Open room page earlier than window | Join disabled, countdown shown |
| QA-010 | Room Access | Candidate joins during allowed window | Interview exists | Open room page after early join and before endAt | Join allowed |
| QA-011 | Room Access | Candidate joins after endAt | Interview ended by time | Open room page | Join blocked with ended message |
| QA-012 | Room Access | Access room from previous day | Room interviewDate < today | Open by room code | Access rejected |
| QA-013 | Room Access | Room fully ended by time and no active session | latest endAt passed, no active statuses | Open by room code | Access rejected |
| QA-014 | Interview Room | Earliest slot consistency in room | Two candidates 13:00 then add 09:00 same room/day | Fetch room interviews | Representative interview uses earliest scheduled slot |
| QA-015 | Kanban | Move to OFFBOARDED while disabled | enableOffboardedStage=false | Drag to Offboarded | Rejected by backend |
| QA-016 | Kanban | Move to OFFBOARDED while enabled | enableOffboardedStage=true and current stage HIRED/TRIAL | Drag to Offboarded | Success |
| QA-017 | Kanban | Offer-before-trial mode strict | offerBeforeTrial=true | Move INTERVIEW_COMPLETED -> TRIAL | Rejected |
| QA-018 | Kanban | Trial-before-offer mode strict | offerBeforeTrial=false | Move INTERVIEW_COMPLETED -> OFFER_EXTENDED | Rejected |
| QA-019 | Kanban | Forward legal transition | Valid source->target | Drag stage | Success and stage history recorded |
| QA-020 | Feedback | NEXT_ROUND recommendation | Interview COMPLETED | Submit feedback NEXT_ROUND | Application stage remains INTERVIEW |
| QA-021 | Feedback | HOLD recommendation | Interview COMPLETED | Submit feedback HOLD | Application stage remains INTERVIEW |
| QA-022 | Feedback | EXTEND_OFFER recommendation | Interview COMPLETED | Submit feedback EXTEND_OFFER | Application stage becomes OFFER_EXTENDED |
| QA-023 | Feedback | REJECT recommendation | Interview COMPLETED | Submit feedback REJECT | Application stage becomes REJECTED |
| QA-024 | UX | Modal validation visibility | Invalid create/edit fields | Submit invalid form | Inline error shown and auto-scroll to first invalid field |
| QA-025 | UX | Feedback modal validation | Missing required fields | Submit feedback | Inline error in modal (no validation toast) |

## HOLD vs NEXT_ROUND Governance Checks
- HOLD and NEXT_ROUND both keep application at INTERVIEW stage.
- HR must create a new interview to continue candidate progression.
- Candidate in HOLD should appear in interview-related queue until explicitly moved to terminal stage.
- NEXT_ROUND should include note indicating intended next interview objective.

## Regression Smoke Checklist
- Create online/offline interview from Kanban and Calendar.
- Edit schedule from Calendar and verify new interview appears in list.
- Candidate can only join within valid time window.
- HR cannot re-enter previous-day room by code.
- Offboarded column visibility follows company config.
- Offer/trial order follows company config.
