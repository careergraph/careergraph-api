# 🏗️ MASTER OVERVIEW — Messaging & Notification System
> **Dự án:** CareerGraph — Tính năng Nhắn tin & Thông báo Realtime  
> **Phiên bản tài liệu:** 1.0  
> **Vai trò agent:** Senior Developer 15+ năm kinh nghiệm + QA Tester khắt khe

---

## 📐 Kiến trúc tổng thể

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                                │
│  ┌──────────────────────┐        ┌──────────────────────────────┐   │
│  │   FE Candidate       │        │      FE HR                   │   │
│  │   (React JS)         │        │      (React TS)               │   │
│  │  - Inbox trang riêng │        │  - Inbox management page     │   │
│  │  - Chat UI           │        │  - Chat UI (từ nhiều nơi)    │   │
│  │  - Notification bell │        │  - Notification bell         │   │
│  └─────────┬────────────┘        └─────────────┬────────────────┘   │
│            │  Socket.io                        │  Socket.io         │
└────────────┼──────────────────────────────────┼────────────────────┘
             │                                  │
┌────────────▼──────────────────────────────────▼────────────────────┐
│                     GATEWAY / SOCKET LAYER                         │
│  ┌────────────────────────────────────────────────────────────────┐│
│  │         RTC + Messaging Socket Server (Node.js)               ││
│  │  - Namespace /chat  (messaging realtime)                      ││
│  │  - Namespace /notify (notifications realtime)                 ││
│  │  - Namespace /rtc   (existing video call)                     ││
│  └────────────────────────────┬───────────────────────────────────┘│
└───────────────────────────────┼────────────────────────────────────┘
                                │ HTTP REST / Internal Events
┌───────────────────────────────▼────────────────────────────────────┐
│                     SPRING BOOT BACKEND                            │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────────┐   │
│  │ Message API  │  │ Notification │  │  Existing APIs         │   │
│  │ - CRUD msg   │  │ API          │  │  - Application status  │   │
│  │ - Threads    │  │ - Events     │  │  - Kanban              │   │
│  │ - Read status│  │ - Prefs      │  │  - Jobs                │   │
│  └──────┬───────┘  └──────┬───────┘  └────────────────────────┘   │
└─────────┼─────────────────┼──────────────────────────────────────┘
          │                 │
┌─────────▼─────────────────▼──────────────────────────────────────┐
│                     DATABASE (PostgreSQL)                         │
│  message_threads, messages, message_reads                        │
│  notifications, notification_preferences                         │
└──────────────────────────────────────────────────────────────────┘
```

---

## 📦 Phân chia Phases

| Phase | Tên | Service | Thời gian ước tính |
|-------|-----|---------|-------------------|
| 1 | BE Foundation — Database & REST APIs | Spring Boot | 2–3 ngày |
| 2 | Socket Layer — Messaging Realtime | Node.js RTC | 1–2 ngày |
| 3 | HR FE — Inbox & Chat UI | React TS | 2–3 ngày |
| 4 | Candidate FE — Inbox & Chat UI | React JS | 2–3 ngày |
| 5 | Notification System — BE + Socket | Spring Boot + Node.js | 2 ngày |
| 6 | Notification FE + Integration Test | React TS + React JS | 2 ngày |
| 7 | Realtime Lifecycle Hardening (presence/subscription/reconnect) | React TS + React JS + Node.js RTC | 1–2 ngày |

**Tổng:** ~12–17 ngày

---

## 🔑 Nguyên tắc bắt buộc cho Agent

### Code Quality
- TypeScript strict mode cho FE HR (không dùng `any`)
- Java 17+ features cho Spring Boot
- Không để dead code, console.log thừa trong production
- Error boundaries đầy đủ ở FE
- Loading states và skeleton UI ở mọi async operation

### UI/UX Production Standards
- Responsive (mobile + desktop)
- Dark/light mode nếu dự án đã có
- Transition/animation mượt (200–300ms)
- Empty states có design đẹp, không để trang trắng
- Optimistic UI updates (tin nhắn hiện ngay, không đợi server)
- Infinite scroll / pagination cho lịch sử chat

### Security
- JWT validation ở mọi socket event
- Rate limiting cho message send
- XSS sanitization cho nội dung tin nhắn
- File upload validation (nếu có)

### Testing (QA Gate — bắt buộc trước khi hoàn thành mỗi phase)
Agent phải tự đóng vai **tester khắt khe** và kiểm tra:
1. Happy path hoạt động đúng
2. Edge cases (mất mạng, reconnect, message rất dài, emoji, ký tự đặc biệt)
3. Race conditions (2 người gửi cùng lúc)
4. Unauthorized access bị chặn
5. UI không bị vỡ layout ở các breakpoints

---

## 🗂️ Cấu trúc file documents

```
docs/
├── 00_MASTER_OVERVIEW.md          ← file này
├── 01_PHASE1_BE_FOUNDATION.md
├── 02_PHASE2_SOCKET_MESSAGING.md
├── 03_PHASE3_HR_FE_CHAT.md
├── 04_PHASE4_CANDIDATE_FE_CHAT.md
├── 05_PHASE5_NOTIFICATION_BE.md
└── 06_PHASE6_NOTIFICATION_FE.md
```

---

## 🚦 Definition of Done (DoD) — Toàn bộ dự án

- [ ] HR có trang `/messages` quản lý tất cả hội thoại
- [ ] Candidate có trang `/messages` xem tất cả hội thoại
- [ ] Gửi/nhận tin nhắn realtime < 200ms latency
- [ ] Typing indicator hiển thị đúng
- [ ] Read receipt (đã xem) cập nhật realtime
- [ ] Unread badge cập nhật realtime
- [ ] Lịch sử chat load đúng, infinite scroll hoạt động
- [ ] Notification bell hiển thị đúng count
- [ ] Click notification điều hướng đúng
- [ ] Candidate nhận notification khi HR đổi trạng thái hồ sơ
- [ ] HR nhận notification khi có ứng viên apply job
- [ ] Cả 2 nhận notification khi có tin nhắn mới
- [ ] Reconnect socket tự động khi mất kết nối
- [ ] Subscription kênh chat/notify hoạt động ổn định ngay cả khi user không đứng ở trang `/messages`
- [ ] Presence online không bị reset sai khi refresh thread list
- [ ] Không có memory leak
- [ ] Performance: FCP < 1.5s, không lag khi scroll chat
