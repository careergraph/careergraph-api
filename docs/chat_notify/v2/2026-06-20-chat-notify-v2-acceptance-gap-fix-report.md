# Chat Notify V2 Acceptance Gap Fix Report

Ngay: 2026-06-20

## Muc tieu

- Fix acceptance gap con lai truoc khi chot QA.
- Them `bell entry point` rieng cho candidate mobile.
- Sua HR bell empty-state copy de khong con nhac den `tin nhan moi`.
- Giu nguyen production rule:
  - `Bell badge` chi dem notification system/job/interview.
  - `Message badge` chi dem unread message/thread.
  - `NEW_MESSAGE` khong duoc lam sai bell badge.

## Da sua gi

### 1. Candidate mobile co bell entry point rieng

File sua:

- `careergraph-client/src/layouts/components/Navbar/Navbar.jsx`

Thay doi:

- Them `NotificationBell` cho mobile header khi candidate da dang nhap.
- Giu `message entry point` rieng nhu cu.
- Bell moi dung lai notification store/hook hien co, nen van theo dung boundary da chot tu Phase 2 + 3.

Tac dong:

- Candidate mobile da co diem vao ro rang de mo bell, khong con phu thuoc vao desktop-only container.
- Khong doi source of truth cua unread counts.

### 2. HR bell empty-state copy da sach domain

File sua:

- `careergraph-hr/src/features/notifications/components/NotificationDropdown.tsx`

Thay doi:

- Sua empty-state copy tu thong diep co nhac `tin nhan moi` thanh thong diep chi nhac `he thong, viec lam, phong van`.

Tac dong:

- UX copy khop voi boundary bell/message da tach.
- Giam nham lan nhan thuc o HR dropdown.

## Build/Test da chay

Da chay:

- `careergraph-api`: `mvn -q -DskipTests compile`
  - Ket qua: pass.
- Kiem tra moi truong frontend:
  - `node -v`
  - `npm -v`
  - Ket qua: khong co `node`/`npm` tren `PATH`, nen khong build duoc `careergraph-client` va `careergraph-hr` trong session nay.

## Danh gia acceptance

### Da xu ly

- Gap `candidate mobile khong co bell entry point` da duoc fix o muc code.
- Gap `HR bell empty-state copy van nhac den tin nhan moi` da duoc fix.
- Rule `NEW_MESSAGE` khong duoc lam sai bell badge van duoc giu nguyen, vi thay doi lan nay chi them entry point UI va sua copy.

### Con can verify tren browser/staging

- Candidate mobile:
  - Bell moi hien dung tren mobile header.
  - Bell dropdown mo duoc va khong vo layout.
  - `NEW_MESSAGE` chi tang message badge, khong tang bell badge.
  - Job/interview/system notification tang bell badge dung domain.
- HR:
  - Empty-state copy moi hien dung trong dropdown.

## Risk con lai

- Chua co bang chung build/runtime frontend trong session nay do thieu Node toolchain.
- Van nen chay lai QA P0/P1 mobile + HR dropdown tren staging truoc khi chot `full acceptance`.
