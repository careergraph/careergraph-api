# 03 — Kế hoạch Triển khai theo Phases + AI Prompts

---

## Tổng quan Phases

```
Phase 1 (3–4 ngày)  → Backend core: DB + Scheduler + API
Phase 2 (2–3 ngày)  → RTC integration: Socket emit + pending queue
Phase 3 (3–4 ngày)  → Frontend: UI popup + timeout fallback + socket handler
Phase 4 (1–2 ngày)  → Edge cases: cancel, reschedule, snooze, offline
Phase 5 (2 ngày)    → Testing, monitoring, hardening
```

---

## Phase 1 — Backend Core (Spring Boot)

### Mục tiêu
- Thêm `interview_reminder_log` table
- Cột `room_link`, `reminder_sent_30`, `reminder_sent_15` vào `interviews`
- `InterviewReminderScheduler` chạy mỗi phút
- Endpoints: `/today`, `/pending-reminders`, `/reminder-ack`

### Prompt AI cho Phase 1

> **Đính kèm:** Toàn bộ source `src/main/java/` của Spring Boot service, file `schema.sql` hoặc migration hiện tại, file `application.yml`, entity `Interview.java`, repository `InterviewRepository.java`

```
Bạn là senior Java/Spring Boot developer.

Tôi đang xây dựng tính năng "Interview Reminder" cho hệ thống tuyển dụng.
Dưới đây là toàn bộ source code Spring Boot hiện tại của tôi.

**Yêu cầu bạn phân tích:**
1. Đọc entity Interview, Repository, Service hiện có
2. Đọc cấu trúc package và naming convention đang dùng
3. Đọc cách project quản lý migration (Flyway/Liquibase/SQL thuần)
4. Đọc cách project xử lý authentication và lấy userId

**Sau đó thêm các thành phần sau, tuân thủ hoàn toàn naming convention và pattern hiện có:**

A. Migration SQL:
   - Bảng `interview_reminder_log` (id, interview_id, user_id, reminder_type, status, sent_at, delivered_at, dismissed_at)
   - ALTER TABLE interviews: thêm room_link, reminder_sent_30, reminder_sent_15 nếu chưa có

B. Entity + Repository:
   - `InterviewReminderLog` entity (theo pattern của entities hiện có)
   - `InterviewReminderLogRepository` với method findPendingByUserId

C. Scheduler:
   - Class `InterviewReminderScheduler` với @Scheduled(fixedDelay=60000)
   - Query interviews WHERE scheduled_at BETWEEN now+29min AND now+31min (T30)
   - Query interviews WHERE scheduled_at BETWEEN now+14min AND now+16min (T15)
   - Tích hợp Redis dedup: set key "reminder:dedup:{id}:{type}" NX TTL=2h
   - Sau khi dedup pass → publish event tới RTC service (tôi sẽ cung cấp RtcEventPublisher interface)
   - Chỉ gửi cho interviews có status = SCHEDULED

D. REST Controller endpoints:
   - GET /api/v1/interviews/today — trả InterviewReminderDTO list cho user hiện tại
   - GET /api/v1/interviews/pending-reminders — trả PendingReminderDTO
   - POST /api/v1/interviews/{id}/reminder-ack — cập nhật log status

**Lưu ý quan trọng:**
- KHÔNG sửa bất kỳ logic nghiệp vụ hiện có
- KHÔNG đổi tên package, không đổi naming convention
- Giải thích ngắn gọn lý do mỗi lựa chọn thiết kế
- Nếu có gì mâu thuẫn với code hiện tại, hỏi lại trước khi viết
```

---

## Phase 2 — RTC Service Integration

### Mục tiêu
- Spring Boot publish reminder event sang RTC service
- RTC service nhận và emit socket tới user
- Xử lý user offline → lưu pending vào Redis list
- Khi user connect → flush pending reminders

### Prompt AI cho Phase 2

> **Đính kèm:** Source RTC service (Node.js hoặc Spring WebSocket), file kết nối Redis hiện tại, file Socket.IO server setup, cách RTC hiện tại emit thông báo (tìm chỗ emit job notification hay message notification hiện có)

```
Bạn là senior Node.js / Socket.IO developer (hoặc Spring WebSocket nếu RTC dùng Java).

Tôi đang tích hợp tính năng "Interview Reminder" vào RTC service hiện có.
Dưới đây là toàn bộ source RTC service của tôi.

**Yêu cầu bạn phân tích trước:**
1. Cách hiện tại RTC nhận event từ Spring Boot API (HTTP call? Redis pub/sub? Kafka?)
2. Cách user join vào socket room (tìm chỗ socket.join)
3. Cách RTC emit event hiện tại cho job notification và message notification
4. Cách RTC xử lý reconnect và cleanup

**Sau đó thêm các thành phần sau, theo đúng pattern hiện có:**

A. Nhận reminder từ Spring Boot:
   - Thêm handler cho event/channel mà Spring Boot sẽ dùng để publish reminder
   - Parse payload ReminderSocketPayload { interviewId, jobTitle, companyName, scheduledAt, roomLink, minutesBefore, reminderType }

B. Emit tới user:
   - Kiểm tra user có trong room user:{userId} không
   - Nếu có → emit event "interview.reminder" với payload
   - Nếu không → lpush vào Redis key "reminder:pending:{userId}" với TTL 24h
   - Gọi Spring Boot /api/v1/interviews/{id}/reminder-ack với action=DELIVERED hoặc PENDING

C. Xử lý khi user connect:
   - Tìm đúng chỗ socket.on('connect') hoặc middleware authentication hiện tại
   - Thêm logic flush pending: lrange + del + forEach emit

D. Handle sự kiện từ client:
   - Lắng nghe "reminder.ack" từ client (dismiss, join, snooze)
   - Forward về Spring Boot API

**Lưu ý quan trọng:**
- KHÔNG thay đổi logic authentication socket hiện tại
- KHÔNG thay đổi cách join room hiện tại  
- Chỉ thêm, không sửa các handler đang chạy
- Comment rõ đoạn code nào là mới thêm: // [REMINDER-FEATURE]
```

---

## Phase 3 — Frontend Implementation

### Mục tiêu
- `reminderService.ts` quản lý timeout và popup queue
- React/Vue components: CornerPopup và CenterModal
- Socket event listener tích hợp với socket service hiện có
- Store/state management cho popup state

### Prompt AI cho Phase 3

> **Đính kèm:** Source frontend (src/), file socket service/hook hiện tại, file store (Redux/Zustand/Pinia/Vuex), component notification chuông hiện có, router config

```
Bạn là senior Frontend developer (React + TypeScript hoặc Vue 3 + TypeScript).

Tôi đang thêm tính năng "Interview Reminder Popup" vào frontend hiện có.
Dưới đây là toàn bộ source frontend của tôi.

**Yêu cầu bạn phân tích trước:**
1. Cách project hiện tại kết nối và lắng nghe socket event (tìm file socket service)
2. Cách project quản lý global state (Redux/Zustand/Pinia/Vuex)
3. Pattern tạo component hiện tại (functional/class, naming, folder structure)
4. Cách project hiện tại hiển thị notification chuông (để tích hợp, không duplicate)
5. Cách call API (axios instance, interceptors, auth header)

**Sau đó thêm các thành phần sau:**

A. Service: src/services/reminderService.ts
   - Class ReminderService với Map<string, NodeJS.Timeout> để track timeouts
   - Method initTodayReminders(): gọi GET /interviews/today → set local timeouts
   - Method onSocketReminder(): nhận socket event, clear duplicate timeout, enqueue popup
   - Method cancelReminders(interviewId): clear timeouts + dismiss popup
   - Popup queue: không show 2 popup cùng lúc, queue FIFO

B. Store slice/module: reminder state
   - currentPopup: ReminderPayload | null
   - popupType: 'corner' | 'center' | null
   - Phân loại: T30 → corner, T15 + IMMINENT → center

C. Component: CornerPopup
   - Vị trí: fixed bottom-right, z-index 9999
   - Hiện: jobTitle, companyName, thời gian còn lại (tĩnh, không countdown)
   - Button: "Tham gia" (mở roomLink) và "Bỏ qua"
   - Tự đóng sau 10 giây
   - Animation: slide-in từ phải

D. Component: CenterModal  
   - Backdrop overlay
   - Countdown timer đếm ngược đến scheduledAt
   - Nút "Tham gia phỏng vấn ngay" → mở roomLink
   - Nút "Nhắc lại sau 5 phút" (snooze, chỉ hiện nếu minutesBefore >= 10)
   - KHÔNG tự đóng

E. Tích hợp socket:
   - Tìm đúng chỗ đang lắng nghe socket event
   - Thêm listener cho "interview.reminder" → gọi reminderService.onSocketReminder()
   - Thêm listener cho "interview.cancelled" → gọi reminderService.cancelReminders()

F. Gọi initTodayReminders() sau khi user login thành công (tìm đúng chỗ)

**Lưu ý quan trọng:**
- Tuân thủ hoàn toàn design system / UI library hiện tại (Ant Design / MUI / shadcn / Tailwind)
- KHÔNG tạo thêm axios instance mới nếu đã có
- KHÔNG tạo thêm socket connection nếu đã có
- Comment code mới: // [REMINDER-FEATURE]
- Nếu project dùng i18n, thêm cả key dịch
```

---

## Phase 4 — Edge Cases

### Prompt AI cho Phase 4

> **Đính kèm:** Code đã viết ở Phase 1, 2, 3

```
Bạn là senior developer đang review và hardening tính năng Interview Reminder.

Đây là code đã implement ở các phase trước (đính kèm).

**Hãy xử lý các edge case sau:**

1. **Interview bị HỦY trong khi popup đang hiển thị:**
   - BE: emit "interview.cancelled" event
   - FE: dismiss popup ngay, cancel timeout
   - BE: cập nhật reminder_log status = EXPIRED

2. **Interview bị ĐỔI GIỜ:**
   - BE: emit "interview.rescheduled" với scheduledAt mới
   - FE: cancel timeout cũ, recalculate và set timeout mới
   - BE: xóa dedup key cũ trong Redis, tạo lại khi gửi lần kế

3. **Lên lịch khi còn < 30 phút:**
   - BE: trong InterviewService.createInterview(), sau khi save, check ngay
   - Nếu scheduledAt - now() < 30min → gọi ngay RtcPublisher, không chờ scheduler

4. **User SNOOZE T-15:**
   - FE: gọi POST /interviews/{id}/snooze
   - BE: set Redis key "reminder:snooze:{userId}:{interviewId}" TTL=5min
   - Sau 5 phút, scheduler gặp interview này → check snooze key, nếu hết TTL → gửi lại
   - Giới hạn snooze 1 lần / interview

5. **Multiple tabs / devices:**
   - Socket broadcast tới tất cả socket của user
   - FE: khi nhận reminder, check localStorage key "reminder:acked:{interviewId}:{type}"
   - Nếu đã ack ở tab khác → không hiện popup

6. **User offline rồi online lại sau khi đã QUA GIỜ họp:**
   - FE khi nhận pending reminder → check scheduledAt < now()
   - Nếu đã qua → KHÔNG hiện popup
   - Call API /reminder-ack với action=EXPIRED

Với mỗi edge case, viết code tối thiểu cần thêm và chỉ rõ vị trí trong code hiện tại.
```

---

## Phase 5 — Hardening & Monitoring

### Checklist trước Go-live

```markdown
Backend:
- [ ] Scheduler có retry khi Redis down không?
- [ ] Có circuit breaker khi RTC service down không?
- [ ] Có metrics cho số reminder gửi thành công/thất bại?
- [ ] Database index đúng chưa?
- [ ] Scheduler có race condition khi scale nhiều instance không? (dùng ShedLock)

RTC Service:
- [ ] Khi RTC restart, pending Redis list có bị mất không?
- [ ] Có log khi emit thất bại?

Frontend:
- [ ] setTimeout có bị garbage collect khi component unmount không?
- [ ] Popup có bị mất khi navigate route không?
- [ ] Memory leak khi nhiều interviews?
```

### Prompt AI cho Phase 5 — ShedLock (tránh gửi 2 lần khi scale)

> **Đính kèm:** pom.xml, application.yml, InterviewReminderScheduler.java

```
Tôi đang chạy Spring Boot với nhiều instances (scale horizontal).
Scheduler hiện tại sẽ chạy trên tất cả instances và gửi reminder trùng lặp.

Dù đã có Redis dedup, nhưng race condition vẫn có thể xảy ra trong vòng milliseconds.

Hãy thêm ShedLock vào project của tôi để đảm bảo scheduler chỉ chạy trên 1 instance tại một thời điểm:
1. Thêm dependency ShedLock vào pom.xml
2. Cấu hình ShedLock với backend phù hợp (Redis hoặc JDBC tùy project)
3. Annotate @SchedulerLock lên method processReminders()
4. Không thay đổi bất kỳ logic nào khác
```
