# Phase 3 Session Prompt

Ban hay dong vai tro la senior dev 15+ nam kinh nghiem, architect production, va specialist realtime/socket.

Hay doc cac tai lieu dinh kem truoc khi lam:

- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-master-prompt.md`
- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-phased-execution-plan.md`
- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-qa-test-report.md`
- bao cao/code summary moi nhat sau khi xong `Phase 1 + Phase 2`
(careergraph-api\docs\chat_notify\v2\2026-06-20-chat-notify-v2-implementation-report.md)
Bo canh:

- Phase 1 + Phase 2 da hoan thanh.
- Bay gio can tiep tuc `Phase 3 - Production Realtime Hardening`.

Muc tieu:

- Harden realtime notification/message tren production.
- Review reconnect, unread sync, duplicate event, fallback polling, va delayed UI update.
- Bo sung debug log production neu can, nhung khong lam noisy qua muc.
- Giu contract dang chay on dinh neu khong co ly do bat buoc phai doi.

Rule production phai giu:

- `Bell badge` chi dem notification system/job/interview.
- `Message badge` chi dem unread message/thread.
- `NEW_MESSAGE` khong duoc lam sai `bell badge`.
- HR va candidate phai hanh xu dong nhat theo domain.

File code uu tien doc:

- `careergraph-rtc/src/internal-api.js`
- `careergraph-rtc/src/notify.js`
- `careergraph-rtc/src/chat.js`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/SocketNotificationPusher.java`
- `careergraph-client/src/features/notifications/hooks/useNotifySocket.js`
- `careergraph-client/src/features/notifications/context/NotificationContext.jsx`
- `careergraph-hr/src/features/notifications/hooks/useNotifySocket.ts`
- `careergraph-hr/src/features/notifications/hooks/useNotifications.ts`

Yeu cau thuc hien:

1. Doc ky code va thay doi da co cua Phase 1 + 2 truoc khi sua tiep.
2. Tim cac diem co the gay:
   - delayed notification
   - reconnect sai state
   - duplicate event
   - count dung nhung list UI chua sync
3. Neu can them tracing/logging, chi them o muc production-friendly.
4. Neu can them guard/refetch dieu kien, implement theo huong an toan nhat.
5. Khong sua vo toi va contract backend/socket neu chua can thiet.

Sau khi xong:

- Build/test nhung gi co the.
- Neu khong chay duoc test nao, ghi ro ly do.
- Bao cao:
  - da sua gi
  - test/build da chay
  - risk con lai
  - buoc tiep theo de QA
- Viet md report moi vao:
  - `careergraph-api/docs/chat_notify/v2`

Neu phat hien blocker tu production env/config, hay neu ro:

- dau hieu
- nguyen nhan kha nghi
- file env/config lien quan
- cach verify sau deploy
