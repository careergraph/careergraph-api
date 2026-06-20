# Phase 2 Session Prompt

Ban hay dong vai tro la senior dev 15+ nam kinh nghiem, production troubleshooter, va architect boundary/state.

Hay doc cac tai lieu dinh kem truoc khi lam:

- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-master-prompt.md`
- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-phased-execution-plan.md`
- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-qa-test-report.md`
- bao cao moi nhat sau khi xong `Phase 1`

Bo canh:

- Phase 1 da tach UI entry point hoac da xac dinh boundary can tach.
- Van de chinh cua phase nay la unread counter, socket sync, va dropdown behavior.

Muc tieu:

- Tach boundary unread count giua `notification` va `message`.
- `NEW_MESSAGE` khong duoc lam sai `bell badge`.
- Message unread phai thuoc messaging domain, bell unread phai thuoc notification domain.
- Mo bell/dropdown phai sync hop ly voi unread count.

Rule production phai giu:

- `Bell badge` chi dem notification system/job/interview.
- `Message badge` chi dem unread message/thread.
- `NEW_MESSAGE` khong duoc dong thoi la source of truth cho bell count.

File code uu tien doc:

- `careergraph-client/src/features/notifications/context/NotificationContext.jsx`
- `careergraph-client/src/features/notifications/components/NotificationDropdown.jsx`
- `careergraph-client/src/features/notifications/hooks/useNotifySocket.js`
- `careergraph-client/src/features/messaging/hooks/useMessagingUnread.js`
- `careergraph-hr/src/features/notifications/hooks/useNotifications.ts`
- `careergraph-hr/src/features/notifications/components/NotificationDropdown.tsx`
- Neu can: `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/NotificationServiceImpl.java`

Yeu cau thuc hien:

1. Doc ky code va xac dinh source of truth cho tung counter.
2. Neu FE chi la symptom ma backend dang tra sai domain, sua o nguon dung.
3. Review ky socket event, polling fallback, refresh unread count, va refetch khi open dropdown.
4. Neu can giu `NEW_MESSAGE` trong audit/activity feed thi duoc, nhung khong duoc tinh vao bell badge chinh.
5. Sau khi sua, build/test nhung gi co the va noi ro residual risks.

Tieu chi dat:

- Co 1 new message + 1 job notification:
  - bell badge = 1
  - message badge = 1
- Mark read bell khong anh huong message unread.
- Mark read message khong anh huong bell unread.

Sau khi xong:

- Bao cao:
  - da sua gi
  - boundary nao da duoc chot
  - test/build da chay
  - risk con lai
- Viet md report moi vao:
  - `careergraph-api/docs/chat_notify/v2`
