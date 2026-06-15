# 01 — Phân tích Nghiệp vụ: Interview Reminder

---

## 1. Các tình huống (Case) có thể xảy ra

### Case A — Người dùng đăng nhập TRƯỚC khi cuộc họp diễn ra

| Kịch bản | Thời gian còn lại | Hành động |
|----------|------------------|-----------|
| A1 | > 30 phút | Không làm gì, ghi nhớ lịch vào memory FE |
| A2 | 25–30 phút | Hiện **Corner Popup** nhắc 30 phút nữa |
| A3 | 10–15 phút | Hiện **Center Modal** nhắc 15 phút nữa (urgent) |
| A4 | < 5 phút | Hiện **Center Modal** + badge đỏ + âm thanh |
| A5 | Đã qua giờ | Không hiện, ghi log missed |

### Case B — Người dùng đăng nhập SAU khi cuộc họp đã gần (< 5 phút)

- Hiện ngay Center Modal với "Cuộc phỏng vấn đang/sắp diễn ra!"
- Link tham gia nổi bật, nút Join màu đỏ

### Case C — Người dùng lên lịch phỏng vấn còn 5–20 phút nữa

**Phía HR lên lịch:**
- Hệ thống kiểm tra `scheduled_at - now()`
- Nếu < 30 phút → emit socket event **ngay lập tức** cho cả HR lẫn ứng viên
- Hiện popup với countdown timer

**Phía ứng viên nhận:**
- Nhận socket event `interview.scheduled.imminent`
- Hiện Center Modal với đếm ngược

### Case D — Người dùng đang online, lịch được tạo bình thường (> 1 giờ)

- Bell navbar cập nhật ngay qua socket
- Scheduler xử lý remind tại T-30 và T-15

### Case E — Người dùng đang OFFLINE khi tới giờ nhắc

- Nhắc nhở được lưu vào bảng `reminder_log` với status `PENDING`
- Khi user online lại → FE gọi API lấy pending reminders → hiện popup
- Nếu đã qua giờ họp → không hiện, cập nhật status `EXPIRED`

### Case F — Nhiều cuộc phỏng vấn trong cùng ngày

- FE lấy tất cả interviews của ngày hiện tại khi đăng nhập
- Sắp xếp theo `scheduled_at`, set timeout cho từng cái
- Nếu 2 cuộc trùng thời gian nhắc → queue popup, không chồng lên nhau

### Case G — HR xem popup trong khi đang ở trang khác

- Popup xuất hiện ở góc dưới phải (không block workflow)
- Click vào popup → navigate đến màn hình interview room

### Case H — Ứng viên hoặc HR HỦY lịch phỏng vấn

- API gửi event `interview.cancelled`
- FE hủy timeout đang chờ (clearTimeout)
- Xóa popup nếu đang hiển thị

---

## 2. Flow Tổng thể

### Flow 1: Scheduler-based Reminder (Happy Path)

```
[DB] Interview created (T = scheduled_at)
        |
[Scheduler] chạy mỗi 1 phút, query interviews:
        WHERE scheduled_at BETWEEN now()+14min AND now()+16min  → emit T-15
        WHERE scheduled_at BETWEEN now()+29min AND now()+31min  → emit T-30
        |
[Redis] Check dedup key: reminder:{interview_id}:{type}
        - Nếu key tồn tại → skip (đã gửi rồi)
        - Nếu không → set key TTL=2h, tiếp tục
        |
[RTC Service] emit to room: user:{userId} 
        Event: interview.reminder
        Payload: { interviewId, scheduledAt, roomLink, minutesBefore: 15 }
        |
[FE Client/HR] nhận event → hiện Popup
```

### Flow 2: FE-side Timeout Fallback (Defense in Depth)

```
[User Login / Page Load]
        |
[FE] GET /api/v1/interviews/today
        → trả về list interviews hôm nay có status SCHEDULED
        |
[FE] Với mỗi interview:
        remainMs = scheduledAt - Date.now()
        if remainMs > 30*60*1000:
            setTimeout(show30minPopup, remainMs - 30*60*1000)
            setTimeout(show15minPopup, remainMs - 15*60*1000)
        else if remainMs > 15*60*1000:
            setTimeout(show15minPopup, remainMs - 15*60*1000)
        else if remainMs > 0:
            showImminentPopup() immediately
        |
[FE] Lưu timeout IDs vào store để clearTimeout khi cần
```

> **Tại sao cần cả hai?**
> - Scheduler đảm bảo server-driven, độc lập với tab/browser state
> - FE timeout đảm bảo user vẫn nhận khi socket tạm mất kết nối

---

## 3. Popup Design Specification

### Corner Popup (T-30 — Soft Reminder)

```
╔══════════════════════════════════╗
║ 🗓  Nhắc nhở phỏng vấn           ║
║ Còn 30 phút: "Vị trí Frontend"  ║
║ 10:30 SA với Công ty ABC         ║
║                                  ║
║  [Tham gia ngay]  [Bỏ qua]      ║
╚══════════════════════════════════╝
  → Tự đóng sau 10 giây
  → Vị trí: bottom-right, z-index cao
```

### Center Modal (T-15 — Urgent Reminder)

```
╔══════════════════════════════════════╗
║        ⚠️  Sắp đến giờ phỏng vấn!   ║
║                                      ║
║   Còn  [14:58]  phút                 ║
║   Vị trí: Senior Frontend Developer  ║
║   Công ty: Công ty ABC               ║
║   Giờ: 10:30 SA — 11:00 SA           ║
║                                      ║
║   ┌─────────────────────────────┐    ║
║   │  🔗 Link phỏng vấn          │    ║
║   │  meet.platform.com/abc-123  │    ║
║   └─────────────────────────────┘    ║
║                                      ║
║   [  Tham gia phỏng vấn ngay  ]      ║
║           [Nhắc lại sau 5 phút]      ║
╚══════════════════════════════════════╝
  → Backdrop mờ, không tự đóng
  → Có countdown timer đếm ngược
```

---

## 4. Quy tắc Nghiệp vụ

| Rule | Mô tả |
|------|-------|
| R1 | Mỗi interview chỉ được gửi mỗi loại reminder đúng 1 lần (dedup bằng Redis) |
| R2 | Reminder T-15 quan trọng hơn T-30 → hiện Center Modal |
| R3 | Nếu user đã dismiss popup T-30 → T-15 vẫn phải hiện |
| R4 | Lịch bị hủy → clear tất cả pending popup và timeout |
| R5 | Không gửi reminder cho interviews đã CANCELLED hoặc COMPLETED |
| R6 | User được phép chọn "Snooze 5 phút" (chỉ 1 lần cho T-15) |
| R7 | Khi lên lịch mà còn < 30 phút → gửi immediate reminder |
| R8 | Không stack nhiều popup cùng lúc → dùng queue |
