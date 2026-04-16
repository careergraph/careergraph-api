# Phase 5 Messaging Production Hotfix Spec

Date: 2026-04-16
Owner: Copilot (GPT-5.3-Codex)
Scope: HR FE, Candidate FE, RTC Socket (presence + typing + layout)

## 1. Objectives

- Typing indicator must show user-friendly name instead of raw email.
- Typing indicator must stay active while user remains in input field and has not blurred.
- Presence indicator must reflect thread peer online/offline correctly.
- Messaging screens must fit one viewport without forced extra page scroll caused by stacked header/padding/footer.
- Kanban Candidate detail messaging tab must expose candidate/message status context.

## 2. Implemented Changes

### 2.1 Typing Name Resolution

- HR FE (`careergraph-hr`): typing display name now avoids email fallback.
- Candidate FE (`careergraph-client`): typing display name now avoids email fallback.
- RTC (`careergraph-rtc`): `typing-start` payload now includes `firstName`, `lastName`, and `email` to support richer client-side fallback rules.

### 2.2 Continuous Typing Behavior

- Both FE apps now send typing heartbeat every ~2.2 seconds while:
  - input has focus,
  - message text is non-empty,
  - user is still in compose state.
- Typing stops when:
  - input loses focus,
  - content becomes empty,
  - message is submitted,
  - tab/document becomes hidden.

### 2.3 Presence Accuracy

- Presence updates now prioritize thread-scoped realtime events (`threadId`) instead of relying only on `otherUser.id` matching.
- `thread-online-users` now explicitly computes whether any peer (not current user) is online in thread.
- `user-online`/`user-offline` now patch thread online flag directly.

### 2.4 Layout and Viewport Fit

- HR layout now provides a full-height container for `/messages` route without default content padding wrapper.
- Candidate default layout now hides Footer and ChatBotButton on `/messages` route to prevent vertical overflow and viewport clipping.
- Candidate dashboard layout and messages page now use `h-full/min-h-0/flex-1` strategy to avoid nested height conflicts.

### 2.5 Kanban MessagesTab Status

- Added top status strip in HR Kanban candidate messages tab:
  - candidate name,
  - candidate pipeline status badge,
  - realtime online/offline indicator from messaging thread state.
- Candidate message tab now reports resolved thread id upward so parent tab can bind status UI.

## 3. Files Changed

### HR FE

- `src/features/messaging/hooks/useChatSocket.ts`
- `src/features/messaging/components/MessageInput.tsx`
- `src/features/messaging/components/ChatWindow.tsx`
- `src/features/messaging/components/CandidateMessageTab.tsx`
- `src/pages/Kanban/CandidateTab/MessagesTab.tsx`
- `src/features/messaging/pages/MessagesPage.tsx`
- `src/layout/AppLayout.tsx`

### Candidate FE

- `src/features/messaging/hooks/useChatSocket.js`
- `src/features/messaging/components/MessageInput.jsx`
- `src/features/messaging/components/ChatWindow.jsx`
- `src/features/messaging/pages/MessagesPage.jsx`
- `src/layouts/ProfileDashboardLayout/ProfileDashboardLayout.jsx`
- `src/layouts/DefaultLayout/DefaultLayout.jsx`

### RTC

- `src/chat.js`

## 4. Validation

### Build Validation

- HR FE (`careergraph-hr`): `npm run build` => PASS
- Candidate FE (`careergraph-client`): `npm run build` => PASS

### Runtime Behavior Expected

- Typing text shows human-friendly label (candidate/HR display) instead of raw email in normal flows.
- Typing status remains visible continuously while user is actively focused in input with non-empty text.
- Online dot updates correctly when peer joins/leaves thread room.
- `/messages` pages render full messaging canvas in one viewport without needing page-level vertical scroll just to reach chat input.

## 5. Risks and Follow-up

- If JWT claims do not contain profile name, fallback labels are used by UI logic.
- For very large bundles, Vite still reports chunk-size warnings; this is outside the scope of messaging hotfix.
- Recommended next step: add lightweight socket E2E for heartbeat typing + thread presence assertions.
