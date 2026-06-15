# 🔔 Interview Reminder Feature — Tài liệu Kỹ thuật

> **Phiên bản:** 1.0.0  
> **Tác giả:** Senior Dev & System Architect  
> **Ngày:** 2025-06-15  
> **Trạng thái:** Bản thiết kế gốc

---

## 📁 Cấu trúc tài liệu

| File | Nội dung |
|------|----------|
| `01-business-analysis.md` | Phân tích nghiệp vụ, các case xảy ra, flow người dùng |
| `02-architecture-design.md` | Thiết kế kiến trúc, data model, API contract |
| `03-phase-plan.md` | Kế hoạch triển khai theo phases + prompt AI cho từng phase |
| `04-qa-report.md` | Báo cáo kiểm thử từ góc nhìn QA Tester 15 năm kinh nghiệm |

---

## 🎯 Mục tiêu tính năng

Hệ thống nhắc nhở phỏng vấn tự động theo thời gian thực cho phép:

- Người dùng (ứng viên + HR) nhận **popup nhắc nhở** trước 15 phút và 30 phút khi có lịch phỏng vấn
- Popup xuất hiện ở **góc dưới phải** (soft reminder) hoặc **giữa màn hình** (urgent reminder)
- Kèm theo **link tham gia phỏng vấn** trực tiếp ngay trong popup
- Tích hợp với **socket service** (RTC) hiện có — không xây mới

---

## 🏗️ Kiến trúc tổng quan

```
[Client App]  ←──── WebSocket ────→  [RTC Service]
[HR App]      ←──── WebSocket ────→  [RTC Service]
                                             ↑
                                    emit reminder event
                                             |
[Spring Boot API] ←── REST ──→ [Reminder Scheduler]
        |                              |
    [Database]                   [Redis Cache]
    interviews                   dedup guard
    reminders_log
```

---

## ⚙️ Stack hiện tại (assumed)

| Layer | Technology |
|-------|------------|
| Frontend Client | React / Vue (ứng viên) |
| Frontend HR | React / Vue (HR dashboard) |
| Backend API | Spring Boot (Java 17+) |
| Realtime | Node.js Socket.IO hoặc Spring WebSocket |
| Database | PostgreSQL / MySQL |
| Cache | Redis |
| Message Queue | Kafka hoặc RabbitMQ (nếu có) |
