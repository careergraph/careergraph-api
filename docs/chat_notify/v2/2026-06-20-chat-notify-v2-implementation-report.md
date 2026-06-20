# Chat Notify V2 Implementation Report

Ngay: 2026-06-20

## Scope da lam

Da implement theo uu tien `Phase 1 + Phase 2`, co bo sung hardening nho cho behavior mo bell:

- Tach `message entry point` rieng cho HR header.
- Giu candidate tiep tuc dung `message entry point` rieng cho desktop/mobile.
- Tach boundary unread giua `notification` va `message`.
- Loai `NEW_MESSAGE` khoi bell unread va bell list.
- Tang do dong bo giua badge bell va dropdown list khi mo bell.

## Phase 1 - UI Boundary Separation

### Muc tieu

- HR phai co message entry point rieng, ngang cap voi bell.
- Khong de UX HR va candidate lech domain nhan thuc.

### File sua

- `careergraph-hr/src/layout/AppHeader.tsx`

### Ket qua

- Them icon `message` rieng tren HR header, link toi `/messages`.
- Badge message cua HR dung `totalUnread` tu messaging store, khong dung chung voi notification.
- Bell van giu vai tro thong bao he thong/nghiep vu.

### Test da chay

- Chua build duoc `careergraph-hr` trong session nay vi moi truong shell khong co `node`/`npm` tren `PATH`

### Risk con lai

- Header HR se phu thuoc vao messaging bootstrap/store de hien unread som sau login. Hien tai luong nay da ton tai va van dang duoc dung o sidebar/bottom nav.

## Phase 2 - Counter Domain Cleanup

### Muc tieu

- `Bell badge` chi dem system/job/interview notifications.
- `Message badge` chi dem unread message/thread.
- `NEW_MESSAGE` khong duoc lam sai bell badge/list.

### File sua

- `careergraph-api/src/main/java/com/hcmute/careergraph/repositories/NotificationRepository.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/NotificationServiceImpl.java`
- `careergraph-client/src/features/notifications/context/NotificationContext.jsx`
- `careergraph-client/src/features/notifications/components/NotificationDropdown.jsx`
- `careergraph-hr/src/features/notifications/hooks/useNotifications.ts`
- `careergraph-hr/src/features/notifications/components/NotificationDropdown.tsx`

### Ket qua

- API notification list va unread count da loai `NEW_MESSAGE` khoi bell domain.
- Socket unread counts tu API -> RTC -> FE se day `notifications` da sach domain hon.
- FE candidate va HR deu bo qua `NEW_MESSAGE` trong notification stream de tranh sai so tam thoi truoc/sau refetch.
- Bell dropdown chi con list system/job/interview notifications.

### Hardening bo sung

- Khi mo bell, neu unread badge lon hon unread local trong list thi FE se refetch lai dropdown ngay, giam case `co badge nhung list chua theo kip`.

### Test da chay

- Compile `careergraph-api` voi Maven
- Chua build duoc `careergraph-client` / `careergraph-hr` trong session nay vi moi truong shell khong co `node`/`npm` tren `PATH`

### Risk con lai

- Ban ghi `Notification` type `NEW_MESSAGE` van duoc tao trong database de phuc vu audit/cooldown/browser push, nhung khong con di vao bell domain.
- Neu san pham sau nay muon co `activity feed` tong hop, can them entry point rieng thay vi tai su dung bell hien tai.

## Build/Test da chay

- `careergraph-client`: `npm run build`
- `careergraph-hr`: `npm run build`
- `careergraph-api`: `mvn -q -DskipTests compile`

Ket qua thuc te:

- `careergraph-api`: compile thanh cong.
- `careergraph-client`: khong chay duoc lenh vi `npm` khong ton tai trong shell hien tai.
- `careergraph-hr`: khong chay duoc lenh vi `npm` khong ton tai trong shell hien tai.

## Danh gia regression

### Dat duoc

- HR co message entry point rieng.
- Candidate/HR deu tach ro `message` va `bell`.
- `NEW_MESSAGE` khong con lam sai bell list/badge o boundary FE + API.
- Mo bell co co che refetch hop ly hon khi local list chua bat kip unread badge.

### Chua verify end-to-end trong session nay

- 2 tabs cung login.
- Socket reconnect duoi network cham.
- Flow production that su qua API -> RTC -> browser tren moi role.

## De xuat follow-up

- Chay manual QA P0/P1 theo checklist trong `2026-06-20-chat-notify-v2-qa-test-report.md`.
- Neu production van con case tre noti sau reconnect, uu tien lam tiep `Phase 3` o huong tracing/logging va conditional refetch.
