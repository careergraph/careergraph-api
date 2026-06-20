# Chat Notify V2 Phased Execution Plan

Ngay: 2026-06-20

## 1. Ket luan nhanh

Phan viec nay nen chia phase.

Ly do:

- Khong chi la doi UI header.
- No dong vao boundary giua `notification domain` va `messaging domain`.
- Co lien quan den socket realtime, unread counter, dropdown behavior, mobile header, desktop header, va QA regression cho 2 app `careergraph-client` va `careergraph-hr`.
- Neu sua mot lan qua nhieu diem, rat kho debug khi production van con tre noti hoac double count.

Danh gia thuc te:

- Neu chi sua giao dien hien icon message cho HR: nho.
- Neu sua dung chuan production nhu yeu cau: trung binh -> lon vua, nen chia phase.

Khuyen nghi production:

- Chia thanh `3 phase implementation` + `1 phase QA/report`.
- Moi phase deploy doc lap duoc.
- Moi phase co rollback boundary ro rang.

## 2. Muc tieu chat_notify v2

Chot rule can dat duoc sau cung:

- `Bell` chi dem `system/job/interview notifications`.
- `Message icon` chi dem `message unread`.
- HR va Candidate deu co `message entry point` rieng.
- `NEW_MESSAGE` khong duoc lam sai badge bell.
- Counter khong bi nhay `2 -> 1` do trung domain.
- Dropdown list va badge count dong bo on dinh sau socket event va sau refresh.

## 3. Cac phase de xuat

## Phase 1 - UI Boundary Separation

### Muc tieu

Tach ro diem vao `Message` va `Bell` tren ca HR va Candidate, nhung chua can doi contract backend.

### Viec can lam

- HR:
  - Them `message icon` vao `AppHeader.tsx`.
  - Badge unread cua message dung store/hook messaging rieng.
  - Giu `bell` cho notification nhu hien tai.
- Candidate:
  - Review lai header desktop/mobile va bottom nav de dam bao message badge hien nhat quan.
- Dong bo visual hierarchy:
  - `message icon` va `bell` ngang cap.
  - Badge style, hover, keyboard focus, mobile spacing.

### File du kien dong vao

- `careergraph-hr/src/layout/AppHeader.tsx`
- `careergraph-hr/src/features/messaging/...`
- `careergraph-client/src/layouts/components/Navbar/Navbar.jsx`
- `careergraph-client/src/components/BottomNav/BottomNav.jsx`

### Tieu chi hoan thanh

- HR co message icon rieng tren header.
- Candidate va HR deu co 2 diem vao ro rang: `message`, `bell`.
- Khong co syntax error, build pass.

### Prompt thuc hien phase 1

```md
Ban hay dong vai tro la senior dev 15+ nam kinh nghiem va architect production.
Hay sua UI header cho he thong CareerGraph theo rule:
- Tach message ra khoi bell.
- HR va candidate deu phai co message entry point rieng.
- Bell chi la notification system/nghiep vu.

Yeu cau:
- Doc ky src de hieu context truoc khi sua.
- Giu lai design system hien co, khong sua vo van style.
- Uu tien desktop + mobile.
- Khong doi contract API o phase nay.
- Sau khi sua, hay tu build/test nhung gi co the va bao cao file da doi.
```

### File nen dinh kem cho phase 1

- `careergraph-hr/src/layout/AppHeader.tsx`
- `careergraph-hr/src/features/notifications/components/NotificationBell.tsx`
- `careergraph-client/src/layouts/components/Navbar/Navbar.jsx`
- `careergraph-client/src/layouts/components/Navbar/NotificationBell.jsx`
- `careergraph-client/src/components/BottomNav/BottomNav.jsx`

## Phase 2 - Counter Domain Cleanup

### Muc tieu

Tach boundary unread count cho dung domain, giam double count va nhay so.

### Viec can lam

- Candidate:
  - Review `NotificationContext`.
  - Bo hoac vo hieu hoa tac dong cua `NEW_MESSAGE` len bell unread rule.
  - Chi giu `message unread` cho `useMessagingUnread` / messaging store.
- HR:
  - Review `useNotifications.ts`.
  - Dam bao `payload.messages` chi sync sang messaging store, khong lam sai notification unread.
- Review logic local merge:
  - socket event
  - polling fallback
  - unread-count refresh
  - dropdown open refetch

### File du kien dong vao

- `careergraph-client/src/features/notifications/context/NotificationContext.jsx`
- `careergraph-client/src/features/notifications/components/NotificationDropdown.jsx`
- `careergraph-client/src/features/notifications/hooks/useNotifySocket.js`
- `careergraph-client/src/features/messaging/hooks/useMessagingUnread.js`
- `careergraph-hr/src/features/notifications/hooks/useNotifications.ts`
- `careergraph-hr/src/features/notifications/components/NotificationDropdown.tsx`

### Tieu chi hoan thanh

- Co 1 job notification + 1 new message:
  - Bell badge = 1
  - Message badge = 1
- Khong con hien tuong message lam bell count nhay sai.
- Dropdown mo ra khong con tinh trang co badge nhung list trong do chua sync.

### Prompt thuc hien phase 2

```md
Ban hay dong vai tro la senior dev 15+ nam kinh nghiem va production troubleshooter.
Hay sua boundary unread counter cho CareerGraph theo rule:
- Bell count chi dem notification system/nghiep vu.
- Message count chi dem unread message.
- NEW_MESSAGE khong duoc gay double count cho bell.

Yeu cau:
- Doc ky NotificationContext, useNotifications, notification dropdown, messaging unread hooks.
- Neu can giu NEW_MESSAGE trong list activity thi duoc, nhung khong duoc tinh vao bell unread badge.
- Kiem tra ky luong sync giua socket event, polling fallback, refetch khi open dropdown.
- Sau khi sua, build/test va viet ngan gon cac risk con lai.
```

### File nen dinh kem cho phase 2

- `careergraph-client/src/features/notifications/context/NotificationContext.jsx`
- `careergraph-client/src/features/notifications/components/NotificationDropdown.jsx`
- `careergraph-client/src/features/messaging/hooks/useMessagingUnread.js`
- `careergraph-hr/src/features/notifications/hooks/useNotifications.ts`
- `careergraph-hr/src/features/notifications/components/NotificationDropdown.tsx`

## Phase 3 - Production Realtime Hardening

### Muc tieu

On dinh behavior tren production khi socket tre, reconnect, hoac internal push den cham.

### Viec can lam

- Rasoat log production cho:
  - notify socket connect/disconnect/connect_error
  - unread-counts event
  - internal push tu API -> RTC
- Dam bao dropdown/list refetch khi unread count tang ma local list chua co item.
- Can nhac bo sung guard de tranh duplicate event khi reconnect.
- Review xem co can bo sung metadata type filter o FE hay BE.

### File du kien dong vao

- `careergraph-rtc/src/internal-api.js`
- `careergraph-rtc/src/notify.js`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/SocketNotificationPusher.java`
- `careergraph-client/src/features/notifications/...`
- `careergraph-hr/src/features/notifications/...`

### Tieu chi hoan thanh

- Production debug log du de xac nhan duong di:
  - API push
  - RTC receive
  - browser socket receive
  - UI update
- Reconnect khong gay loạn counter ro rang.
- Co kha nang truy vet nhanh khi production tre noti.

### Prompt thuc hien phase 3

```md
Ban hay dong vai tro la senior dev 15+ nam kinh nghiem, architect production va specialist realtime/socket.
Hay harden notification/message realtime cho CareerGraph production.

Muc tieu:
- Khi API day unread-counts va notification sang RTC, FE nhan va cap nhat on dinh.
- Co debug log du de truy vet tren production.
- Reconnect/polling fallback khong gay nhay counter.

Yeu cau:
- Doc ky RTC notify/internal-api, API SocketNotificationPusher, va cac hook notification FE.
- Neu thay can them logging, guard duplicate, hay refetch dieu kien thi implement theo huong it rui ro nhat.
- Khong pha contract dang chay neu khong thuc su can thiet.
- Sau cung build/test cac repo lien quan neu co the.
```

### File nen dinh kem cho phase 3

- `careergraph-rtc/src/internal-api.js`
- `careergraph-rtc/src/notify.js`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/SocketNotificationPusher.java`
- `careergraph-client/src/features/notifications/hooks/useNotifySocket.js`
- `careergraph-hr/src/features/notifications/hooks/useNotifySocket.ts`

## Phase 4 - Senior Tester QA va Report

### Muc tieu

Test lai end-to-end sau khi xong 3 phase tren.

### Checklist QA bat buoc

#### Candidate

- Co message moi khi dang o home:
  - message badge tang ngay
  - bell badge khong tang neu chi la message
- Co job status notification:
  - bell badge tang
  - mo bell thay item moi ngay hoac sau refetch hop ly
- Vao `/messages`:
  - unread message giam dung
  - bell khong bi anh huong sai
- Mobile navbar / bottom nav:
  - badge dung va khong vo layout

#### HR

- Dang o dashboard, candidate nhan tin:
  - HR thay message badge ma khong can reload
- Candidate nop CV / doi status / lich phong van:
  - HR thay bell badge dung
- Mo bell:
  - list co item dung domain
- Mo message:
  - unread giam dung, bell khong bi tru oan

#### Regression

- Reload trang
- Logout/Login
- Socket reconnect
- Browser background -> foreground
- Open 2 tabs
- Count lon hon 99

### Prompt thuc hien phase 4

```md
Ban hay dong vai tro la senior tester 15+ nam kinh nghiem, QA lead production va reviewer UX kho tinh.
Hay test chat_notify v2 cua CareerGraph sau khi da tach message khoi bell.

Yeu cau:
- Kiem tra dung/sai theo domain counter.
- Kiem tra desktop, mobile, socket reconnect, reload, focus/visibility.
- Neu thay bat thuong, uu tien bao cao theo muc do nghiem trong.
- Viet bao cao bang md, gom:
  - ket qua dat/chua dat
  - bug con lai
  - risk production
  - de xuat follow-up
```

## 4. Thu tu trien khai khuyen nghi

Thu tu an toan nhat:

1. Phase 1
2. Phase 2
3. Phase 3
4. Phase 4

Ly do:

- Phase 1 tach UI truoc de user nhin dung domain.
- Phase 2 sua counter boundary, la phan de gay bug nhat.
- Phase 3 harden production va them tracing.
- Phase 4 test sau cung de tranh bao cao nham tren code dang bien dong.

## 5. Co nen lam mot lan trong 1 session khong

Khuyen nghi: khong.

Co the lam trong 1 session neu:

- Chi implement Phase 1 + 2
- Khong sua contract backend lon
- Chap nhan test manual la chinh

Nhung neu muc tieu la "production nhat co the", nen tach theo phase nhu tren.

## 6. File dinh kem tong hop cho session tiep theo

Neu muon dua cho agent/session tiep theo, dinh kem toi thieu:

- `careergraph-api/docs/2026-06-20-notification-message-production-proposal.md`
- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-phased-execution-plan.md`
- `careergraph-hr/src/layout/AppHeader.tsx`
- `careergraph-hr/src/features/notifications/components/NotificationBell.tsx`
- `careergraph-hr/src/features/notifications/components/NotificationDropdown.tsx`
- `careergraph-hr/src/features/notifications/hooks/useNotifications.ts`
- `careergraph-client/src/layouts/components/Navbar/Navbar.jsx`
- `careergraph-client/src/layouts/components/Navbar/NotificationBell.jsx`
- `careergraph-client/src/features/notifications/components/NotificationDropdown.jsx`
- `careergraph-client/src/features/notifications/context/NotificationContext.jsx`
- `careergraph-client/src/features/messaging/hooks/useMessagingUnread.js`

## 7. Ket luan cuoi

Huong nay nen chia phase.

Neu anh muon lam dung chuan production, phase hop ly nhat la:

- Phase 1: Tach UI entry point
- Phase 2: Sua domain counter
- Phase 3: Harden realtime production
- Phase 4: QA report

Neu anh muon, session tiep theo minh co the lam ngay `Phase 1 + Phase 2` trong mot dot, vi day la phan tao gia tri ro nhat va giai quyet truc tiep loi double count hien tai.
