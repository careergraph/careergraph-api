# Phase 1 Session Prompt

Ban hay dong vai tro la senior dev 15+ nam kinh nghiem, architect production, va reviewer UI/UX kho tinh.

Hay doc cac tai lieu dinh kem truoc khi lam:

- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-master-prompt.md`
- `careergraph-api/docs/2026-06-20-notification-message-production-proposal.md`
- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-phased-execution-plan.md`
- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-qa-test-report.md`

Bo canh:

- He thong dang bi lan domain giua `message` va `bell`.
- Candidate da co message badge rieng o mot so diem, nhung can review lai tinh nhat quan.
- HR hien tai can co `message entry point` rieng tren header.

Muc tieu:

- Tach ro `message entry point` va `bell entry point` tren UI.
- HR va candidate deu phai co diem vao message rieng.
- Bell chi giu vai tro thong bao he thong/nghiep vu.
- Chua doi contract backend o phase nay neu khong bat buoc.

Rule phai giu:

- `Bell badge` chua ban chat la unread notification domain.
- `Message badge` chua ban chat la unread messaging domain.
- Khong duoc tron UX bell va message tren cung mot entry point.

File code uu tien doc:

- `careergraph-hr/src/layout/AppHeader.tsx`
- `careergraph-hr/src/features/notifications/components/NotificationBell.tsx`
- `careergraph-client/src/layouts/components/Navbar/Navbar.jsx`
- `careergraph-client/src/layouts/components/Navbar/NotificationBell.jsx`
- `careergraph-client/src/components/BottomNav/BottomNav.jsx`

Yeu cau thuc hien:

1. Doc ky src de hieu context truoc khi sua.
2. Giu lai visual language hien co, khong redesign vo toi.
3. Uu tien desktop + mobile.
4. Neu thay candidate mobile chua co `bell entry point` ro rang, ghi ro trong bao cao de phase sau xu ly.
5. Khong sua workaround tam bo neu con cach UI boundary ro rang hon.

Sau khi xong:

- Build/test nhung gi co the.
- Bao cao:
  - da sua gi
  - file nao da doi
  - test/build da chay
  - risk con lai
- Viet md report moi vao:
  - `careergraph-api/docs/chat_notify/v2`
