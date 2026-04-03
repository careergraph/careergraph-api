# Tài liệu hệ thống tuyển dụng — Index

> **Dự án:** CareerGraph HR Platform  
> **Cập nhật:** 2026-04

---

## Danh sách tài liệu

| File | Mô tả | Liên quan |
|---|---|---|
| `interview-system-requirements.md` | Toàn bộ hệ thống phỏng vấn: room lifecycle, admit/kick, recording, evaluation, reschedule | Core |
| `kanban.md` | Board quản lý ứng viên: drag & drop, pipeline stages, tạo lịch từ kanban | → interview |
| `calendar.md` | Lịch phỏng vấn: tạo lịch, phân loại, chi tiết, upcoming | → interview |
Source HR: careergraph-hr/
Source Client( candidate): careergraph-client/
Source API: careergraph-api/
---

## Luồng chính

```
Ứng viên nộp hồ sơ
    │
    ▼
Kanban (Ứng tuyển → Liên hệ)
    │
    ▼
Kéo vào cột Phỏng vấn  ──OR──  Tạo từ Calendar
    │
    ▼
interview-system (Room, Admit, Record, Evaluation)
    │
    ├── PASS  → Kanban chuyển cột tiếp (Thử việc → Nhận chính thức)
    ├── FAIL  → Kanban → Từ chối
    └── HOLD  → Giữ nguyên cột Phỏng vấn, badge "Cần xem xét"
```

---

## Các enum quan trọng

### InterviewStatus

```java
SCHEDULED           // HR tạo lịch, chờ ứng viên xác nhận
CONFIRMED           // Ứng viên đã xác nhận
PENDING_RESCHEDULE  // Ứng viên/HR đang đề xuất đổi lịch
RESCHEDULE_REJECTED // HR từ chối đề xuất (trạng thái trung gian)
IN_PROGRESS         // Đang phỏng vấn
COMPLETED           // Hoàn thành
CANCELLED           // Đã hủy (kèm cancel_reason)
NO_SHOW             // Ứng viên không đến
```

### RoomStatus

```
SCHEDULED → WAITING → ACTIVE → CLOSING → ENDED
                                        ↗ EXPIRED (cronjob)
```

### ApplicationStage (Kanban columns)

```
APPLIED → CONTACTED → INTERVIEW → PROBATION → HIRED
                                ↘ REJECTED
```
