# PROMPT GUIDE — Cách prompt agent (bản cập nhật có Addendum)

---

## 🗂️ Danh sách file & thứ tự sử dụng

```
00_MASTER_OVERVIEW.md           ← context tổng thể
01_PHASE1_BE_FOUNDATION.md      ← Phase 1 gốc
01b_PHASE1_ADDENDUM_ADVANCED.md ← Phase 1 bổ sung (4 tính năng mới)
02_PHASE2_SOCKET_MESSAGING.md   ← Phase 2
03_PHASE3_HR_FE_CHAT.md         ← Phase 3 gốc
03b_PHASE3_ADDENDUM_ADVANCED.md ← Phase 3 bổ sung (UI cho 4 tính năng)
04_PHASE4_CANDIDATE_FE_CHAT.md  ← Phase 4
05_PHASE5_NOTIFICATION_BE.md    ← Phase 5
06_PHASE6_NOTIFICATION_FE.md    ← Phase 6 (final)
```

---

## ═══════════════════════════════════════════
## PHASE 1 — Spring Boot BE Foundation + Advanced
## ═══════════════════════════════════════════

**Files attach:**
- `01_PHASE1_BE_FOUNDATION.md`
- `01b_PHASE1_ADDENDUM_ADVANCED.md`
- Source Spring Boot: toàn bộ project (zip) HOẶC các file:
  - `pom.xml` / `build.gradle`
  - File base entity / `@MappedSuperclass`
  - `SecurityConfig.java`
  - Một entity mẫu (vd `Job.java` hoặc `Application.java`)
  - `ApplicationService.java` (file gốc)
  - Existing migration files (để biết version tiếp theo)

**Prompt:**
```
Bạn là senior developer 15+ năm kinh nghiệm Java Spring Boot.

Đọc 2 file PHASE1 và PHASE1_ADDENDUM cùng toàn bộ source tôi cung cấp 
trước khi viết bất kỳ dòng code nào.

Bắt đầu bằng việc ghi chú:
- Package structure của dự án
- ID type đang dùng (UUID hay Long?)
- Base entity pattern
- Naming convention migration files
- Các ApplicationStatus enum hiện có là gì?
- Methods nào trong ApplicationService cần thêm hook?

Sau đó implement TẤT CẢ trong 1 lần:
Phase 1 gốc (database + entities + APIs) 
+ Phase 1 Addendum (unsend/delete thread/archive/block)

Yêu cầu bắt buộc:
- Không break bất kỳ API cũ nào
- Tuân theo đúng convention của dự án
- BusinessException và error messages bằng tiếng Việt (nếu dự án dùng tiếng Việt)
- Unsend time window: 60 giây, có thể config qua properties

Sau khi xong, chạy QA checklist ở cuối CẢ 2 file và báo cáo kết quả.
```

---

## ═══════════════════════════════════════════
## PHASE 2 — Node.js Socket Layer
## ═══════════════════════════════════════════

**Files attach:**
- `02_PHASE2_SOCKET_MESSAGING.md`
- `server.js` (file hiện tại)
- `.env` hoặc `.env.example`
- `package.json`

**Prompt:**
```
Bạn là senior developer 15+ năm kinh nghiệm Node.js/Socket.io.

Đọc file PHASE2 và server.js hiện tại. Ghi chú:
- Các socket events đang có để không bị conflict tên
- ENV variables pattern

Implement:
1. Extract auth middleware thành auth.js
2. Tạo namespace /chat với đầy đủ events
3. Tạo namespace /notify 
4. Tạo internal-api.js (protected endpoint cho Spring Boot gọi vào)
5. Cập nhật server.js (KHÔNG xóa code video call hiện có)

Sau khi xong:
- Viết test script connect socket và verify từng event
- Test regression: video call events vẫn hoạt động
- Báo cáo kết quả
```

---

## ═══════════════════════════════════════════
## PHASE 3 — HR FE Chat (React TypeScript)
## ═══════════════════════════════════════════

**Files attach:**
- `03_PHASE3_HR_FE_CHAT.md`
- `03b_PHASE3_ADDENDUM_ADVANCED.md`
- Source HR FE:
  - `package.json`
  - Router config (`App.tsx` hoặc `routes.tsx`)
  - Store config (nếu Redux: `store.ts`, nếu Zustand: store files)
  - `axiosInstance.ts` (API client)
  - Auth context hoặc auth slice
  - Navigation/Layout component (để biết thêm route `/messages` vào đâu)
  - Component CandidateDetail hiện tại (tab nhắn tin)
  - 1–2 component mẫu để hiểu coding style

**Prompt:**
```
Bạn là senior developer 15+ năm kinh nghiệm React TypeScript.

Đọc PHASE3 + PHASE3_ADDENDUM và source HR FE tôi cung cấp. Ghi chú:
- State management đang dùng gì?
- UI component library gì?
- Cách navigate (useNavigate hay cách khác?)
- File CandidateDetail ở đâu? Tab nhắn tin nằm ở component nào?
- Toast/notification UI dùng thư viện gì?

Implement TẤT CẢ trong 1 lần (Phase 3 gốc + Addendum):
✓ /messages page với InboxSidebar + ChatWindow
✓ Thread context menu: archive, delete, block
✓ MessageBubble với unsend countdown (60s)
✓ BlockDialog với dropdown lý do
✓ Blocked banner trong ChatWindow
✓ Tab "Đã lưu trữ" trong inbox
✓ Kết nối tab nhắn tin trong CandidateDetail hiện có
✓ Badge unread trên navigation

Yêu cầu bắt buộc:
- TypeScript strict mode, không dùng `any`
- Optimistic UI cho gửi tin và unsend
- Skeleton loading ở mọi async operation
- Responsive: mobile + desktop
- Mọi confirm dialog phải có wording tiếng Việt rõ ràng

Sau khi xong, đóng vai tester khắt khe, chạy QA checklist của CẢ 2 file 
và báo cáo từng mục pass/fail.
```

---

## ═══════════════════════════════════════════
## PHASE 4 — Candidate FE Chat (React JavaScript)
## ═══════════════════════════════════════════

**Files attach:**
- `04_PHASE4_CANDIDATE_FE_CHAT.md`
- Source Candidate FE:
  - `package.json`
  - Router config
  - State management
  - API client
  - Auth context
  - Navigation component
  - Trang "Việc làm đã ứng tuyển" (để thêm nút "Nhắn tin với HR")

**Prompt:**
```
Bạn là senior developer 15+ năm kinh nghiệm React.
Lưu ý: dự án này dùng JavaScript (không phải TypeScript).

Đọc PHASE4 và source Candidate FE. Ghi chú coding style của dự án.

Implement:
✓ /messages page (mobile-first)
✓ Thread list với company/HR info
✓ ChatWindow (reuse logic từ Phase 3 nhưng viết JS)
✓ Nút "Nhắn tin với HR" trong trang Applications
✓ Badge unread trên Navigation
✓ Job context bar trong ChatWindow

Candidate KHÔNG có tính năng block/archive (chỉ HR có).
Candidate chỉ có unsend tin của mình (trong 60s).

Yêu cầu:
- Mobile-first (test trên viewport 375px)
- Touch targets min 44px
- Keyboard không che input khi mở trên mobile

Test end-to-end: mở HR FE + Candidate FE cùng lúc, 
chat 2 chiều, typing indicator, read receipt đều hoạt động.
Báo cáo kết quả.
```

---

## ═══════════════════════════════════════════
## PHASE 5 — Notification Backend
## ═══════════════════════════════════════════

**Files attach:**
- `05_PHASE5_NOTIFICATION_BE.md`
- `ApplicationService.java` (file GỐC, chưa được Phase 1 sửa — hoặc file đã có hook từ Phase 1)
- `MessageService.java` (từ Phase 1)
- `NotificationService.java` (skeleton từ Phase 1)
- `application.properties` / `application.yml`

**Prompt:**
```
Bạn là senior developer 15+ năm kinh nghiệm Spring Boot.

Đọc PHASE5 và các file tôi cung cấp.

Implement:
✓ NotificationService đầy đủ với tất cả notification types
✓ Rate limiting: không spam notifications
✓ Hooks vào ApplicationService (tìm đúng method, không refactor logic cũ)
✓ Hooks vào MessageService
✓ NotificationCleanupJob
✓ SocketNotificationPusher (gọi Node.js internal API)

Đặc biệt quan trọng:
- Message từ chối (REJECTED) phải lịch sự, không harsh
- Notification aggregation: đổi status 2 lần trong 5 phút → update notification cũ
- Push socket phải ASYNC, không block transaction chính

Sau khi xong, với mỗi notification type trong bảng ở đầu file,
tạo test case và verify log output. Báo cáo kết quả.
```

---

## ═══════════════════════════════════════════
## PHASE 6 — Notification FE + Final Testing
## ═══════════════════════════════════════════

**Files attach:**
- `06_PHASE6_NOTIFICATION_FE.md`
- `00_MASTER_OVERVIEW.md`
- Navigation component HR FE
- Navigation component Candidate FE

**Prompt:**
```
Bạn là senior developer 15+ năm kinh nghiệm.
Đây là PHASE CUỐI. Output phải ở chuẩn production tuyệt đối.

Implement cho cả HR FE (TypeScript) và Candidate FE (JavaScript):
✓ useNotifySocket hook
✓ NotificationBell với animated badge
✓ NotificationDropdown với icon theo type
✓ Click notification → navigate đúng trang
✓ Mark read / mark all read
✓ Browser native notification (nếu granted permission)

Sau khi implement xong, đóng vai TESTER KHẮT KHE:

Chạy tuần tự 8 integration scenarios trong file PHASE6.
Format báo cáo cho mỗi scenario:
  Scenario X: [tên]
  Cách test: [mô tả]
  Expected: [kết quả mong đợi]
  Actual: [kết quả thực tế]
  Status: ✅ PASS / ❌ FAIL
  Fix (nếu FAIL): [mô tả fix]

Sau khi tất cả scenarios PASS, đối chiếu với DoD trong 
00_MASTER_OVERVIEW.md và báo cáo từng mục ✅/❌.
```

---

## 💡 Tips xử lý vấn đề thường gặp

**Agent viết code không đúng convention:**
```
Dừng lại. Mở file [tên component mẫu] tôi đã đính kèm. 
Viết lại theo đúng pattern đó.
```

**Agent bỏ sót tính năng:**
```
Bạn chưa implement [tên tính năng] trong QA checklist. 
Implement và test lại mục đó.
```

**Agent hỏi quá nhiều trước khi làm:**
```
Đừng hỏi, hãy đọc source code và tự quyết định. 
Nếu có điểm không chắc, ghi chú assumption rồi tiếp tục.
Tôi sẽ review sau.
```

**Agent muốn refactor code cũ:**
```
KHÔNG refactor code cũ. Chỉ thêm code mới, 
không thay đổi logic đang hoạt động.
```
