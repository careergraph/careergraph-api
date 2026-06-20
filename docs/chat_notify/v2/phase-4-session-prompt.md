# Phase 4 Session Prompt

Ban hay dong vai tro la senior tester 15+ nam kinh nghiem, QA lead production, va reviewer UI/UX kho tinh.

Hay doc cac tai lieu dinh kem truoc khi lam:

- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-master-prompt.md`
- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-phased-execution-plan.md`
- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-qa-test-report.md`
- bao cao moi nhat sau khi xong `Phase 3`

Bo canh:

- Phase 1, Phase 2, va Phase 3 da hoan thanh.
- Bay gio can thuc hien `Phase 4 - Senior QA / Acceptance / UX Review`.

Muc tieu:

- Test lai toan bo chat_notify v2 sau khi da tach `message` khoi `bell`.
- Xac nhan counter dung domain.
- Xac nhan realtime hoat dong on dinh cho HR va candidate.
- Danh gia lai UX header/dropdown theo goc nhin production.

Rule nghiem thu:

- `Bell badge` chi dem notification system/job/interview.
- `Message badge` chi dem unread message/thread.
- Message moi khong duoc lam bell count tang sai.
- Job/interview notification khong duoc lam message count tang sai.
- HR va candidate phai co entry point ro rang va nhat quan.

Phạm vi test uu tien:

1. Candidate
- Dang o home, nhan message moi
- Dang o home, nhan job status notification
- Mo bell
- Mo messages
- Reload trang
- Background -> foreground
- Mobile nav / bottom nav

2. HR
- Dang o dashboard, nhan message moi
- Nhan new application / status / interview notification
- Mo bell
- Mo message/inbox
- Reload trang

3. Regression
- Socket reconnect
- 2 tabs cung login
- Counter > 99
- Dropdown open/close nhanh

Yeu cau bao cao:

- Liet ke ket qua theo muc:
  - Dat
  - Chua dat
  - Bug con lai
  - Risk production
- Neu co bug, uu tien sap xep theo severity va noi ro:
  - buoc tai hien
  - ket qua thuc te
  - ket qua mong doi
  - file code kha nghi lien quan

Sau khi xong:

- Viet bao cao md moi vao:
  - `careergraph-api/docs/chat_notify/v2`
- Neu khong the verify mot so muc bang local, hay ghi ro:
  - muc nao can verify tren production/staging
  - can them log/monitoring gi de xac nhan
