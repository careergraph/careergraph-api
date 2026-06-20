# Chat Notify V2 Master Prompt

Ban hay dong vai tro la senior dev 15+ nam kinh nghiem, system architect production, senior tester 15+ nam kinh nghiem, va reviewer UI/UX kho tinh.

Muc tieu:

- Tach `message` ra khoi `bell`.
- Ca HR va candidate deu co `message entry point` rieng.
- `Bell` chi chua thong bao he thong/nghiep vu.
- Sua triet de loi double count, sai domain unread, va realtime thong bao khong on dinh tren production.
- Sau khi code xong, tu build/test nhung gi co the, kiem tra syntax, va viet bao cao `.md`.

Bo canh hien tai:

- Du an gom:
  - `careergraph-api`
  - `careergraph-rtc`
  - `careergraph-hr`
  - `careergraph-client`
- He thong dang co socket realtime cho chat/notify.
- Production da tung gap loi:
  - message va notification bi dem chong
  - HR chua co message entry point rieng tren header
  - bell va message badge gay nham lan domain
  - co case notification tre hoac chi thay sau reload

Rule production can chot:

- `Bell badge` chi dem notification system/job/interview.
- `Message badge` chi dem unread message/thread.
- `NEW_MESSAGE` khong duoc gay sai badge bell.
- HR va candidate phai co UX nhat quan.

Tai lieu bat buoc phai doc truoc khi lam:

- `careergraph-api/docs/2026-06-20-notification-message-production-proposal.md`
- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-phased-execution-plan.md`
- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-qa-test-report.md`

File code uu tien doc:

- `careergraph-hr/src/layout/AppHeader.tsx`
- `careergraph-hr/src/features/notifications/components/NotificationBell.tsx`
- `careergraph-hr/src/features/notifications/components/NotificationDropdown.tsx`
- `careergraph-hr/src/features/notifications/hooks/useNotifications.ts`
- `careergraph-client/src/layouts/components/Navbar/Navbar.jsx`
- `careergraph-client/src/layouts/components/Navbar/NotificationBell.jsx`
- `careergraph-client/src/features/notifications/components/NotificationDropdown.jsx`
- `careergraph-client/src/features/notifications/context/NotificationContext.jsx`
- `careergraph-client/src/features/messaging/hooks/useMessagingUnread.js`
- `careergraph-rtc/src/notify.js`
- `careergraph-rtc/src/internal-api.js`

Cach thuc hien:

1. Doc src de hieu dung context truoc khi sua.
2. Neu pham vi lon, chia phase dung theo file `phased-execution-plan`.
3. Uu tien lam `Phase 1 + Phase 2` truoc, vi day la phan tao gia tri lon nhat:
   - tach UI message/bell
   - sua boundary unread counter
4. Neu thay can, lam tiep `Phase 3` de harden production realtime.
5. Sau khi code xong:
   - build/test nhung repo lien quan neu co the
   - check syntax error
   - tu review regression risk
   - viet bao cao `.md`

Yeu cau thuc hien:

- Lam theo chuan production nhat co the.
- Khong sua theo kieu workaround tam bo neu con cach tot hon va an toan hon.
- Khong pha vo contract dang chay neu khong bat buoc.
- Neu co user changes chua commit, can doc ky va khong duoc revert oan.
- Neu phai chia phase, moi phase can:
  - neu muc tieu
  - file sua
  - test da chay
  - risk con lai

Yeu cau output:

- Tom tat da sua gi.
- Build/test da chay gi, ket qua ra sao.
- Cac bug/risk con lai neu co.
- Tao hoac cap nhat bao cao `.md` vao:
  - `careergraph-api/docs/chat_notify/v2`

Neu bat dau tu Phase 1 + 2, hay uu tien cac dau viec sau:

- Them message entry point rieng cho HR header.
- Giu candidate message entry point nhat quan desktop/mobile.
- Tach logic unread count giua `notification` va `message`.
- Khong de `NEW_MESSAGE` lam sai `bell badge`.
- Dam bao mo bell/dropdown thi list duoc dong bo hop ly.

Neu phat hien can sua tiep o RTC/API de production on dinh hon, hay neu ro ly do roi moi sua.
