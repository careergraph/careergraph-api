# Chat Notify V2 Phase 3 Hardening Report

Ngay: 2026-06-20

## Muc tieu phase 3

- Harden realtime notification/message tren production.
- Giam rui ro duplicate event khi reconnect.
- Giam case badge dung nhung dropdown list chua bat kip.
- Bo sung tracing API -> RTC -> browser o muc production-friendly.

## Da sua gi

### 1. Candidate: guard duplicate bell increment + refetch an toan hon

File:

- `careergraph-client/src/features/notifications/context/NotificationContext.jsx`

Thay doi:

- Them guard theo `notification.id` de event socket trung khong cong thua `bell unread`.
- `ensureLoaded()` refresh unread truoc, sau do quyet dinh refetch bang count moi nhat thay vi state cu.
- Khi mo bell va van con unread, dropdown se refetch first page de uu tien list moi nhat, giam case count dung nhung item local cu.
- Khi notify socket reconnect, neu user da tung load bell hoac dang co unread, se refetch lai list bell an toan.

Tac dong:

- `NEW_MESSAGE` van khong di vao bell domain.
- Duplicate `notification` event sau reconnect khong con de day `bell badge` len sai tam thoi.

### 2. HR: dong bo behavior voi candidate

File:

- `careergraph-hr/src/features/notifications/hooks/useNotifications.ts`
- `careergraph-hr/src/features/notifications/components/NotificationRealtimeBootstrap.tsx`

Thay doi:

- `ensureLoaded()` cua HR cung refresh unread truoc khi ra quyet dinh refetch.
- Khi bell da tung duoc load hoac van con unread, reconnect se refetch list bell first page.
- Expose `initialized` tu notification hook de bootstrap co the quyet dinh reconnect sync an toan hon.

Tac dong:

- HR va candidate co behavior gan nhu dong nhat khi socket reconnect / dropdown mo lai.

### 3. RTC + API: them tracing va payload guard nhe

File:

- `careergraph-rtc/src/notify.js`
- `careergraph-rtc/src/internal-api.js`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/SocketNotificationPusher.java`

Thay doi:

- RTC log them `activeSockets`, `socket.id`, `reason disconnect`, `notification.id`, `notification.type`, va unread counts summary.
- Internal API reject som payload `/unread-counts` neu `messages` hoac `notifications` khong phai number.
- Internal API warn ro hon khi `/notify`, `/notify/bulk`, `/unread-counts` payload thieu hoac sai.
- API log push notification/unread-counts day du hon de truy vet `userId`, `notification.id`, `type`, `messages`, `notifications`.

Tac dong:

- De khoanh vung nhanh hon xem tre noti nam o API push, RTC emit, hay FE reconnect/sync.
- Khong doi contract socket/backend hien tai.

## Build/Test da chay

- `careergraph-api`: `mvn -q -DskipTests compile`
  - Ket qua: pass.

Chua chay duoc trong session nay:

- `careergraph-client`
- `careergraph-hr`
- `careergraph-rtc`

Ly do:

- Shell hien tai khong co `node` / `npm` tren `PATH`, nen khong build duoc cac package JS/TS.

## Risk con lai

- Reconnect refetch hien tai chi reload first page bell khi user da tung load bell hoac dang co unread. Neu production can dong bo lich su sau reconnect sau muc first page, van can manual QA them.
- Candidate/HR hien tai uu tien freshness khi mo bell neu con unread, nen se tang them 1 request fetch first page trong cac lan mo bell co unread. Danh doi nay co chu y thuc va an toan hon cho production.
- Chua verify duoc end-to-end browser build/runtime trong shell nay do thieu Node toolchain.

## Blocker / env-config can verify sau deploy

### 1. Frontend build env

Dau hieu:

- Khong chay duoc build frontend/rtc trong session.

Nguyen nhan kha nghi:

- May shell hoac CI agent thieu `node` / `npm` tren `PATH`.

File/env lien quan:

- Moi truong runtime/build cua `careergraph-client`, `careergraph-hr`, `careergraph-rtc`

Cach verify sau deploy:

- Xac nhan `node -v`, `npm -v` tren build agent.
- Chay build cho 3 app JS/TS truoc khi promote production.

### 2. Socket route/config

Dau hieu:

- FE co the thay `connect_error`, badge chi dung sau polling, hoac notification toi cham.

Nguyen nhan kha nghi:

- Sai `socket.server.url`, `socket.internal.api-key`, `INTERNAL_API_KEY`, hoac `VITE_RTC_BASE_URL`.

File/env lien quan:

- `careergraph-api/.env.example`
- env runtime cua `careergraph-api`
- env runtime cua `careergraph-rtc`
- env runtime cua `careergraph-client`
- env runtime cua `careergraph-hr`

Cach verify sau deploy:

- Kiem tra log moi:
  - API: `Pushed notification type=... id=... to user=...`
  - API: `Pushed unread counts to user=... notifications=... messages=...`
  - RTC: `emit notification ...`
  - RTC: `emit unread-counts ...`
  - Browser: `[notify socket][candidate]` / `[notify socket][hr]`
- Tao 1 event P0 cho candidate va HR, doi chieu duong di API -> RTC -> browser.

## Buoc tiep theo de QA

1. Candidate:
   - Tao 1 job/interview notification khi user dang online.
   - Xac nhan bell badge tang ngay, bell list co item moi sau khi mo dropdown.
   - Lap lai trong luc tab background -> foreground va sau khi tat/bat network.

2. Candidate messaging boundary:
   - Gui `NEW_MESSAGE`.
   - Xac nhan message badge tang, bell badge khong tang.

3. HR:
   - Lap lai 2 test tren voi dashboard HR.
   - Kiem tra reconnect socket sau refresh va sau visibility change.

4. Regression:
   - 2 tabs cung login.
   - Counter dang co unread, reconnect socket, mo bell ngay sau reconnect.
   - Mark all read o bell khong anh huong message badge.
