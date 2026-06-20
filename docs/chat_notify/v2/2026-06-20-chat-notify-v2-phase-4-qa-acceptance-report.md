# Chat Notify V2 Phase 4 QA Acceptance Report

Ngay: 2026-06-20

Loai bao cao: Senior QA / Acceptance / UX Review

## 1. Ket luan nhanh

Trang thai hien tai: `CHUA DAT full acceptance`.

Ly do chinh:

- Candidate mobile hien chua co `bell entry point` ro rang, nen rule "bell chi dem notification system/job/interview" chua duoc deliver tron ven tren mobile.
- Chua the verify end-to-end realtime browser behavior local trong session nay vi shell khong co `node` / `npm`, va khong co smoke browser/staging tu session nay.

Ket luan QA:

- Domain boundary `message` vs `bell` da duoc lam dung ve mat code path va guard.
- Realtime hardening da co tien trien ro.
- Nhung acceptance production chua the chot "dat" cho toi khi fix candidate mobile bell access va chay lai browser QA P0/P1/P2.

## 2. Dat

Nhung muc duoi day dat o muc code review / static acceptance:

- `NEW_MESSAGE` da bi loai khoi bell domain o FE candidate va HR.
- Notification socket unread counts va notification list deu co guard/refetch de giam case badge dung nhung list chua sync.
- Candidate va HR deu da co tach `message unread` va `notification unread` thanh 2 domain rieng.
- Ca candidate va HR deu cap `99+` cho badge, nen co xu ly cho case counter lon hon 99.
- API -> RTC tracing da co them `notification.id`, `type`, `messages`, `notifications`, `socket.id`, `activeSockets`, huu ich cho production triage.

Bang chung local:

- `careergraph-api`: `mvn -q -DskipTests compile` pass.
- Static review cac file:
  - `careergraph-client/src/features/notifications/context/NotificationContext.jsx`
  - `careergraph-hr/src/features/notifications/hooks/useNotifications.ts`
  - `careergraph-rtc/src/notify.js`
  - `careergraph-rtc/src/internal-api.js`
  - `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/SocketNotificationPusher.java`

## 3. Chua dat

### 3.1 Candidate mobile notification access

Chua dat theo rule UX/acceptance:

- Candidate mobile co `message` entry point, nhung khong thay `bell` entry point trong mobile top nav, bottom nav, hoac mobile menu.

Anh huong:

- User mobile candidate co the nhan badge/system notification o backend, nhung khong co diem vao ro rang de mo bell dropdown.
- Rule "HR va candidate phai co entry point ro rang va nhat quan" chua dat tren candidate mobile.

### 3.2 End-to-end realtime acceptance

Chua dat o muc browser/runtime:

- Chua verify duoc local cac case:
  - candidate dang o home nhan message moi
  - candidate dang o home nhan job/interview notification
  - HR dang o dashboard nhan message moi / new application
  - socket reconnect
  - background -> foreground
  - 2 tabs cung login

Ly do:

- Session nay khong co Node toolchain tren PATH, nen khong build/chay duoc frontend local.
- Khong co browser automation/staging hook trong session nay.

## 4. Bug con lai

### High - Candidate mobile khong co bell entry point

Severity: High

Buoc tai hien:

1. Dang nhap candidate tren viewport mobile.
2. Quan sat top nav.
3. Mo mobile menu.
4. Quan sat bottom nav.

Ket qua thuc te:

- Top nav mobile chi co nut mo menu.
- Mobile menu co `Tin nhan`, `Ho so`, `Dang xuat`.
- Bottom nav chi co `Trang chu`, `Viec lam`, `Tin nhan`, `Ho so`.
- `NotificationBell` chi render trong desktop container `md:flex`.

Ket qua mong doi:

- Candidate mobile phai co `bell entry point` ro rang, tach biet voi `message`.

File code kha nghi lien quan:

- `careergraph-client/src/layouts/components/Navbar/Navbar.jsx:420`
- `careergraph-client/src/layouts/components/Navbar/Navbar.jsx:435`
- `careergraph-client/src/layouts/components/Navbar/Navbar.jsx:458`
- `careergraph-client/src/components/BottomNav/BottomNav.jsx:21`

Nhan xet QA:

- Day la blocker acceptance cho mobile candidate.

### Low - HR bell empty-state copy van nhac den "tin nhan moi"

Severity: Low

Buoc tai hien:

1. Dang nhap HR.
2. Mo bell dropdown trong trang thai khong co notification.
3. Doc empty-state copy.

Ket qua thuc te:

- Copy hien: `Khi co cap nhat he thong hoac tin nhan moi, ban se thay tai day.`

Ket qua mong doi:

- Bell empty-state chi nen nhac den `thong bao he thong/job/interview`, khong nen nhac `tin nhan moi`.

File code kha nghi lien quan:

- `careergraph-hr/src/features/notifications/components/NotificationDropdown.tsx:287`

Nhan xet QA:

- Khong pha logic counter, nhung lam mo domain boundary vua duoc tach o Phase 1 + 2.

## 5. Risk production

- Realtime path da duoc harden, nhung chua co bang chung browser-level trong session nay de xac nhan reconnect/focus/background flow that su on dinh sau khi deploy.
- `mvn -q test` dang fail local, nen confidence chung cua repo test suite chua cao. Loi thay duoc khong tap trung truc tiep vao chat_notify v2, nhung van la tin hieu repo health can luu y.
- Candidate/HR hien dua vao polling + socket + local store. Neu production env co sai `VITE_RTC_BASE_URL`, `socket.server.url`, `socket.internal.api-key`, hoac `INTERNAL_API_KEY`, user co the thay badge chi dung sau polling.

## 6. Test/build da chay

Da chay:

- `careergraph-api`: `mvn -q -DskipTests compile`
  - Ket qua: pass.
- `careergraph-api`: `mvn -q test`
  - Ket qua: fail.

Chi tiet fail local:

- `InterviewServiceImplTest.createInterview_allowsCandidateOverlapWithoutAnyCandidateConflictCheck`
  - File: `careergraph-api/src/test/java/com/hcmute/careergraph/services/impl/InterviewServiceImplTest.java:147`
- Co them loi scheduled task tren H2:
  - `Table "INTERVIEW_ROOMS" not found`
  - Nguon kha nghi: `InterviewRoomScheduler` chay trong test context nhung schema test khong day du.

Danh gia QA:

- Cac loi test nay co ve la repo-level / interview module issue, khong phai bang chung truc tiep cho regression chat_notify v2.
- Tuy vay, chung lam giam muc do "green build confidence" cua release.

Khong chay duoc trong session nay:

- `careergraph-client` build
- `careergraph-hr` build
- `careergraph-rtc` build/script

Ly do:

- Shell hien tai khong co `node` / `npm` tren `PATH`.

## 7. Muc can verify tren staging/production

Bat buoc verify tren staging hoac production-like env:

1. Candidate desktop:
   - Nhac message moi -> message badge tang, bell badge khong tang.
   - Nhac job/interview notification -> bell badge tang, message badge khong tang.
   - Mo bell ngay sau unread-counts -> list co item moi.

2. Candidate mobile:
   - Sau khi fix UI, verify co `bell entry point` ro rang.
   - Verify badge bell va message khong vo layout mobile.

3. HR:
   - Candidate gui tin nhan -> HR thay message badge khong reload.
   - Candidate apply / doi stage / interview event -> HR thay bell badge va bell list dung domain.

4. Regression:
   - Reload trang.
   - Background -> foreground.
   - Tat/bat network de tao reconnect.
   - 2 tabs cung login.
   - Counter > 99.
   - Dropdown open/close nhanh.

## 8. Log/monitoring nen co khi verify

Nen theo doi dong thoi:

- API:
  - `Pushed notification type=... id=... to user=...`
  - `Pushed unread counts to user=... notifications=... messages=...`
- RTC:
  - `emit notification ...`
  - `emit unread-counts ...`
  - `connect user=...`
  - `disconnect user=...`
- Browser:
  - `[notify socket][candidate]`
  - `[notify socket][hr]`

Muc dich:

- Khoanh vung nhanh xem su kien mat o API push, RTC emit, hay FE socket/UI sync.

## 9. De xuat follow-up

1. Sua gap candidate mobile de co `bell entry point` rieng.
2. Sua copy empty-state HR bell de bo nhac den `tin nhan moi`.
3. Chay build frontend/rtc tren may co `node` / `npm`.
4. Chay manual QA P0/P1/P2 tren staging.
5. Neu muon tang confidence production, bo sung smoke test browser hoac script E2E cho:
   - new message khong tang bell
   - new application/job/interview tang bell
   - reconnect khong double count

## 10. Phase roadmap de dung tiep

### Phase 1 - UI Boundary Separation

Muc tieu:

- Tach ro `message entry point` va `bell entry point`.
- HR va candidate deu co diem vao message rieng.
- Giu visual hierarchy nhat quan desktop/mobile.

Trang thai:

- Da dat phan lon o desktop.
- Van con debt o candidate mobile bell access, can xem nhu follow-up cua phase nay.

File/prompt dung lai:

- Plan chi tiet: `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-phased-execution-plan.md`
- Session prompt: `careergraph-api/docs/chat_notify/v2/phase-1-session-prompt.md`

### Phase 2 - Counter Domain Cleanup

Muc tieu:

- Tach unread count dung domain.
- Khong de `NEW_MESSAGE` lam sai bell count/list.
- Giu dropdown sync hop ly voi unread badge.

Trang thai:

- Da dat ve code path chinh.
- Can verify browser-level de chot acceptance that su.

File/prompt dung lai:

- Plan chi tiet: `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-phased-execution-plan.md`
- Session prompt: `careergraph-api/docs/chat_notify/v2/phase-2-session-prompt.md`

### Phase 3 - Production Realtime Hardening

Muc tieu:

- Harden reconnect, unread sync, delayed notification, tracing/logging.
- Giam case badge dung nhung list chua theo kip.

Trang thai:

- Da co tien trien.
- Van can verify tren staging/production-like env.

File/prompt dung lai:

- Session prompt: `careergraph-api/docs/chat_notify/v2/phase-3-session-prompt.md`

### Phase 4 - Senior QA / Acceptance / UX Review

Muc tieu:

- Test lai toan bo flow sau khi xong 3 phase implement.
- Chot muc `dat/chua dat`, bug con lai, va risk production.

Trang thai:

- Bao cao nay la ket qua Phase 4 hien tai.
- Ket luan: chua dat full acceptance.

File/prompt dung lai:

- Session prompt: `careergraph-api/docs/chat_notify/v2/phase-4-session-prompt.md`

## 11. Master prompt tung phase

### Master prompt cho Phase 1

```md
Ban hay dong vai tro la senior dev 15+ nam kinh nghiem, architect production, va reviewer UI/UX kho tinh.

Muc tieu:
- Tach `message` ra khoi `bell`.
- HR va candidate deu co `message entry point` rieng.
- Bell chi chua thong bao he thong/nghiep vu.

Yeu cau:
- Doc ky src truoc khi sua.
- Uu tien desktop + mobile.
- Khong doi contract API neu chua can thiet.
- Sau khi xong, build/test nhung gi co the va viet bao cao md.
```

### Master prompt cho Phase 2

```md
Ban hay dong vai tro la senior dev 15+ nam kinh nghiem va production troubleshooter.

Muc tieu:
- Tach logic unread count giua `notification` va `message`.
- `NEW_MESSAGE` khong duoc gay sai `bell badge`.
- Dropdown bell mo ra phai dong bo hop ly voi unread count.

Yeu cau:
- Xac dinh source of truth cho tung counter.
- Neu backend dang tra sai domain thi sua o nguon.
- Review socket event, polling fallback, unread refresh, va refetch khi open dropdown.
- Sau khi xong, build/test va viet bao cao risk con lai.
```

### Master prompt cho Phase 3

```md
Ban hay dong vai tro la senior dev 15+ nam kinh nghiem, architect production, va specialist realtime/socket.

Muc tieu:
- Harden realtime notification/message tren production.
- Review reconnect, duplicate event, polling fallback, va delayed UI update.
- Bo sung tracing/logging o muc production-friendly neu can.

Yeu cau:
- Doc ky RTC/API/FE notify flow.
- Chi sua contract backend/socket neu that su can thiet.
- Implement guard/refetch/logging theo huong it rui ro nhat.
- Sau khi xong, build/test va viet bao cao md.
```

### Master prompt cho Phase 4

```md
Ban hay dong vai tro la senior tester 15+ nam kinh nghiem, QA lead production, va reviewer UX kho tinh.

Muc tieu:
- Test lai chat_notify v2 sau khi da tach `message` khoi `bell`.
- Xac nhan counter dung domain.
- Xac nhan realtime on dinh cho HR va candidate.
- Danh gia lai UX header/dropdown theo chuan production.

Yeu cau:
- Bao cao theo muc: Dat / Chua dat / Bug con lai / Risk production.
- Neu co bug, neu ro severity, buoc tai hien, ket qua thuc te, ket qua mong doi, va file code kha nghi.
- Neu khong verify duoc local, ghi ro muc can verify tren staging/production.
```

## 12. Bo tai lieu prompt nen dung

- `careergraph-api/docs/chat_notify/v2/phase-1-session-prompt.md`
- `careergraph-api/docs/chat_notify/v2/phase-2-session-prompt.md`
- `careergraph-api/docs/chat_notify/v2/phase-3-session-prompt.md`
- `careergraph-api/docs/chat_notify/v2/phase-4-session-prompt.md`
- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-phased-execution-plan.md`
- `careergraph-api/docs/chat_notify/v2/2026-06-20-chat-notify-v2-master-prompt.md`
