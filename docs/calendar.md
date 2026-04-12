# Calendar — Lịch phỏng vấn & Quản lý lịch hẹn

> **Phạm vi:** Module Calendar — xem lịch, tạo lịch phỏng vấn, phân loại lịch  
> **Liên quan:** `interview-system-requirements.md`, `kanban.md`
Source HR: careergraph-hr/
Source Client( candidate): careergraph-client/
Source API: careergraph-api/
---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Layout & Views](#2-layout--views)
3. [Metric cards](#3-metric-cards)
4. [Phân loại lịch (sidebar)](#4-phân-loại-lịch-sidebar)
5. [Flow tạo lịch từ Calendar](#5-flow-tạo-lịch-từ-calendar)
6. [Modal tạo lịch phỏng vấn](#6-modal-tạo-lịch-phỏng-vấn)
7. [Chi tiết lịch hẹn](#7-chi-tiết-lịch-hẹn)
8. [Sắp diễn ra (Upcoming)](#8-sắp-diễn-ra-upcoming)
9. [Đồng bộ & Notification](#9-đồng-bộ--notification)
10. [Edge cases](#10-edge-cases)

---

## 1. Tổng quan

Calendar là màn hình quản lý lịch **toàn bộ job** (không giới hạn theo từng job như Kanban). HR thấy tất cả lịch phỏng vấn của mình trong 1 view duy nhất theo ngày/tuần/tháng.

**Điểm khác biệt so với Kanban:**
- Kanban: 1 job → nhiều ứng viên
- Calendar: tất cả job → tất cả lịch phỏng vấn → view theo thời gian

**URL:** `/calendar`

---

## 2. Layout & Views

### Cấu trúc màn hình

```
┌─────────────────────────────────────────────────────────┐
│  [Hôm nay]  [Tạo lịch mới]          [Tháng|Tuần|Ngày]  │  ← toolbar
├──────────────────────────────┬──────────────────────────┤
│                              │  PHÂN LOẠI TRẠNG THÁI    │
│                              │  ● Chờ xác nhận     [0]  │
│   CALENDAR GRID              │  ● Đã xác nhận      [0]  │
│   (tháng/tuần/ngày)          │  ● Cần theo dõi     [0]  │
│                              │  ● Cần xử lý        [0]  │
│                              ├──────────────────────────┤
│                              │  CHI TIẾT LỊCH HẸN       │
│                              │  (click vào lịch để xem) │
│                              ├──────────────────────────┤
│                              │  SẮP DIỄN RA             │
│                              │  (upcoming interviews)   │
└──────────────────────────────┴──────────────────────────┘
```

### View Tháng

- Hiển thị toàn bộ tháng dạng grid (7 cột × 5-6 hàng)
- Mỗi ô ngày: hiển thị tối đa 2-3 event, còn lại hiện "+N more"
- Click vào ngày → mở modal tạo lịch (ngày đó được pre-fill)
- Click vào event → hiện chi tiết trong sidebar phải

### View Tuần

- Hiển thị 7 ngày, chia theo giờ (timeline dọc)
- Mỗi event hiển thị tên ứng viên + giờ
- Dễ thấy xung đột lịch (overlap)

### View Ngày

- Timeline chi tiết theo giờ của 1 ngày
- Thấy rõ từng slot phỏng vấn
- Có thể kéo thả event để đổi giờ (xem edge cases)

---

## 3. Metric cards

Hiển thị ở trên cùng, cập nhật realtime:

| Metric | Mô tả |
|---|---|
| **Tổng lịch hẹn** | Tổng số interview records của HR này (mọi trạng thái) |
| **Trong 7 ngày tới** | Số lịch SCHEDULED/CONFIRMED trong 7 ngày tới |
| **Hôm nay** | Số lịch phỏng vấn diễn ra hôm nay |
| **Ứng viên tham gia** | Số ứng viên distinct đã có lịch với HR này |

> **Lưu ý sản phẩm:** các badge trên dashboard là số liệu dẫn xuất từ API theo phạm vi dữ liệu đang tải. Chúng không phải là nguồn sự thật duy nhất; khi HR tạo/cancel/reschedule, hệ thống cần refresh calendar để badge khớp dữ liệu mới.

---

## 4. Phân loại lịch (sidebar)

Bộ lọc nhanh, click để filter calendar:

| Nhãn | Màu | Ánh xạ trạng thái | Mô tả |
|---|---|---|---|
| Chờ xác nhận | Xanh dương | `interview.status = SCHEDULED` | Lịch mới tạo, đang chờ phản hồi |
| Đã xác nhận | Xanh lá | `interview.status = CONFIRMED` | Ứng viên đã xác nhận tham dự |
| Cần theo dõi | Vàng amber | `interview.status = IN_PROGRESS/PENDING_RESCHEDULE` | Buổi phỏng vấn đang chạy hoặc cần HR xử lý |
| Cần xử lý | Đỏ | `interview.status = CANCELLED/NO_SHOW/PENDING_RESCHEDULE` | Cần HR xem lại hoặc xử lý ngay |

**Quy ước hiển thị:**
- `Primary`, `Success`, `Warning`, `Danger` chỉ là tên nhóm màu nội bộ trong code.
- UI phải hiển thị label tiếng Việt như trên, không hiển thị tên nhóm màu thô.
- Nếu muốn đổi màu hoặc mô tả, chỉnh ở `calendar-utils.ts`.

**Số badge** = số lượng interview đang ở trạng thái đó, cập nhật realtime.

**Filter behavior:**
- Click 1 nhãn: chỉ hiện event thuộc nhãn đó
- Click nhiều nhãn: OR logic (hiện event thuộc bất kỳ nhãn nào được chọn)
- Click lại nhãn đã chọn: bỏ filter

---

## 5. Flow tạo lịch từ Calendar

### 5.1 Trigger mở modal

Có 2 cách mở modal tạo lịch từ Calendar:

**Cách 1 — Bấm nút "Tạo lịch mới" trên toolbar:**
```
Bấm [Tạo lịch mới]
    │
    ▼
Modal mở, tất cả fields trống (không pre-fill ngày)
```

**Cách 2 — Click vào ô ngày trên calendar grid:**
```
Click vào ngày [15/12] trên calendar
    │
    ▼
Modal mở, field "Ngày" được pre-fill = 15/12
```

### 5.2 Thứ tự điền form (bắt buộc theo thứ tự)

Calendar khác Kanban ở chỗ: phải chọn **Job trước** rồi mới chọn **Ứng viên**.

```
Bước 1: Chọn công việc (job)
    │  → Load danh sách ứng viên đủ điều kiện
    ▼
Bước 2: Chọn ứng viên
    │  Chỉ hiển thị ứng viên:
    │  - Đang ở stage INTERVIEW của job đó
    │  - HOẶC đang ở stage CONTACTED (HR muốn tạo lịch trước khi kéo Kanban)
    │  - Chưa có interview SCHEDULED/CONFIRMED với job này
    ▼
Bước 3: Điền thông tin lịch
    (xem mục 6)
```

**Tại sao load ứng viên theo job?**
- Tránh HR nhầm tạo lịch sai job
- Lọc ra đúng ứng viên cần phỏng vấn
- Đảm bảo room được gắn đúng `job_id + interview_date`

---

## 6. Modal tạo lịch phỏng vấn

### Toàn bộ form

```
Tạo lịch phỏng vấn
Ghi rõ thông tin để ứng viên và đội ngũ phối hợp hiệu quả.
────────────────────────────────────────────────────────

Chọn công việc *
[Dropdown — danh sách job HR đang quản lý]
  vd: Frontend Engineer (React)

Chọn ứng viên *
[Dropdown — load sau khi chọn job]
  vd: Tran Thi B

  ┌────────────────────────────────────────┐
  │ Ứng viên: Tran Thi B                  │   ← info box tự hiện
  │ Email:    tranb@email.com             │
  │ Vị trí:   Frontend Engineer (React)   │
  └────────────────────────────────────────┘

Hình thức phỏng vấn *        Thời lượng (phút) *
[Online (WebRTC) ▼]          [60 phút ▼]

  ℹ️ Link phòng WebRTC sẽ được tạo tự động sau khi lên lịch

Ngày phỏng vấn *             Giờ bắt đầu *
[Date picker]                [Time picker]

  ⚠️ [Cảnh báo xung đột nếu có — xem mục 6.1]

Tiêu đề lịch
[Input — tự động gợi ý: "PV {Tên ứng viên} - {Vị trí}"]

Ghi chú
[Textarea]

────────────────────────────────────────────────────────
[Hủy]                              [Lên lịch phỏng vấn]
```

### 6.1 Kiểm tra xung đột real-time

Khi HR chọn ngày + giờ, hệ thống kiểm tra ngay:

```
Kiểm tra 1: HR đã có lịch khác trong slot này không?
    → Nếu có: hiện banner vàng "Bạn đã có lịch [Tên ứng viên] lúc [giờ]"
    → Vẫn cho tạo (warning, không block)

Kiểm tra 2: Ứng viên đã có lịch khác trong slot này không?
    → Nếu có: hiện banner đỏ "Ứng viên đã có lịch phỏng vấn lúc [giờ]"
    → Vẫn cho tạo nhưng cảnh báo rõ hơn

Kiểm tra 3: Ngày này đã có room cho job này chưa?
    → Nếu có: hiện thông tin nhẹ "Sẽ thêm vào phòng phỏng vấn ngày [ngày] đã tạo"
    → Không phải warning, chỉ là thông tin
```

### 6.2 Sau khi bấm "Lên lịch phỏng vấn"

```
Validate toàn bộ form
    │
    ├─ Lỗi validation → Highlight field lỗi, không submit
    │
    └─ Hợp lệ → Gọi API
                    │
                    ▼
                Tạo interview record (status: SCHEDULED)
                Tạo/cập nhật room (Daily Room Model)
                Sinh join_token cho ứng viên
                    │
                    ▼
                Gửi email lịch phỏng vấn cho ứng viên
                    │
                    ▼
                Đóng modal
                Calendar cập nhật: event mới xuất hiện trên đúng ngày
                Toast: "Đã tạo lịch phỏng vấn thành công"
                Badge "Chờ xác nhận" +1 (sidebar phân loại)
```

### 6.3 Validation rules

| Field | Rule |
|---|---|
| Công việc | Bắt buộc |
| Ứng viên | Bắt buộc, phải thuộc job đã chọn |
| Hình thức | Bắt buộc |
| Thời lượng | Bắt buộc, min 15 phút |
| Ngày | Bắt buộc, không được ở quá khứ |
| Giờ bắt đầu | Bắt buộc, không được ở quá khứ |

**Rule production về dữ liệu:**
- Một candidate có thể có nhiều interview ở các ngày khác nhau trong cùng một job.
- Không cho trùng thời gian hoạt động cho cùng candidate và cùng interviewer.
- Trong cùng job, mỗi application chỉ có tối đa 1 interview active tại một thời điểm.
- Nếu đã có interview active, hệ thống phải hiển thị popup xác nhận ghi đè trước khi hủy lịch cũ và tạo lịch mới.
- Khi HR hẹn lại cùng room + cùng application trong cùng ngày, backend sẽ cập nhật slot hiện có thay vì sinh thêm slot mới.
- Chỉ cho phép tạo lịch khi hồ sơ ứng tuyển đang ở stage có thể lên lịch (`HR_CONTACTED/SCHEDULED/INTERVIEW/INTERVIEW_SCHEDULED`).
- Khi tạo lịch thành công cho hồ sơ hợp lệ, hệ thống tự động đồng bộ stage hồ sơ về `INTERVIEW`.

---

## 7. Chi tiết lịch hẹn

Click vào event trên calendar → sidebar phải hiện chi tiết:

```
CHI TIẾT LỊCH HẸN
──────────────────────────────────

📅 Thứ Hai, 15/12/2024
⏰ 09:00 – 09:30 (30 phút)

👤 Ứng viên: Nguyen Van A
💼 Vị trí:   Backend Engineer (Spring Boot)
📋 Job:      [link sang job]

Trạng thái: [badge CONFIRMED / SCHEDULED / PENDING_RESCHEDULE...]

Hình thức: Online (WebRTC)
Link phòng: [copy link] [Vào phòng]

Ghi chú: "..."

──────────────────────────────────
[✏️ Chỉnh sửa]  [🗑 Hủy lịch]  [→ Vào phòng]
```

### Actions từ chi tiết lịch:

| Action | Điều kiện | Kết quả |
|---|---|---|
| Vào phòng | Status = ACTIVE hoặc gần giờ | Mở phòng WebRTC |
| Chỉnh sửa | Status = SCHEDULED/CONFIRMED | Mở lại modal với data đã điền |
| Hủy lịch | Status ≠ IN_PROGRESS/COMPLETED | Confirm → CANCELLED, gửi email ứng viên |
| Xem ứng viên | Bất kỳ | Navigate đến Kanban + mở side panel ứng viên |

### Chỉnh sửa lịch (từ Calendar)

```
HR bấm "Chỉnh sửa"
    │
    ▼
Modal mở lại với data cũ đã điền
HR chỉnh sửa ngày/giờ/thời lượng/ghi chú
    │
    ▼
Bấm "Lưu thay đổi"
    │
    ▼
Cập nhật interview record
Nếu đổi ngày → kiểm tra room:
    - Ngày cũ: remove participant khỏi room (nếu room đó không còn ai → set CANCELLED)
    - Ngày mới: gắn vào room ngày mới (tạo nếu chưa có)
Gửi email thông báo đổi lịch cho ứng viên
```

---

## 8. Sắp diễn ra (Upcoming)

Sidebar phải, phần dưới cùng. Hiển thị danh sách lịch phỏng vấn sắp tới (trong 7 ngày):

```
SẮP DIỄN RA
──────────────────────────────
📅 Hôm nay
  ⏰ 09:00  Nguyen Van A — Backend Eng.   [Vào phòng]
  ⏰ 14:00  Tran Thi B — Frontend Eng.   [Vào phòng]

📅 Ngày mai
  ⏰ 10:00  Le Van C — Product Manager   [Chi tiết]

📅 16/12
  ⏰ 09:30  Pham Thi D — Designer        [Chi tiết]
──────────────────────────────
Chưa có lịch hẹn mới. Hãy tạo lịch để giữ kết nối với ứng viên.
(khi rỗng)
```

**Logic hiển thị:**
- Chỉ hiện status = SCHEDULED / CONFIRMED / PENDING_RESCHEDULE
- Sắp xếp theo thời gian tăng dần
- Lịch hôm nay: nút "Vào phòng" (nếu trong vòng 30 phút hoặc đang diễn ra)
- Lịch tương lai: nút "Chi tiết"
- Max hiển thị 10 items, "Xem tất cả" để mở trang danh sách đầy đủ

---

## 9. Đồng bộ & Notification

### Realtime updates trên Calendar

| Sự kiện | Cập nhật Calendar |
|---|---|
| Tạo lịch mới (từ Kanban hoặc Calendar) | Event xuất hiện ngay |
| Ứng viên confirm lịch | Badge đổi từ "Chờ xác nhận" → "Đã xác nhận" |
| Ứng viên đề xuất đổi lịch | Badge đổi → "Cần theo dõi", toast thông báo HR |
| Phỏng vấn bắt đầu | Event highlight (đang diễn ra) |
| Phỏng vấn kết thúc | Event muted, badge chuyển về đúng trạng thái |
| Lịch bị hủy | Event xóa khỏi calendar (hoặc gạch ngang nếu HR muốn xem lịch sử) |

### Notification trong app

| Thời điểm | Nội dung | Kênh |
|---|---|---|
| 1 ngày trước | "Bạn có [N] lịch phỏng vấn ngày mai" | Email + In-app |
| 1 giờ trước | "Sắp đến giờ phỏng vấn [Tên] lúc [giờ]" | In-app banner |
| 15 phút trước | "Phỏng vấn [Tên] bắt đầu sau 15 phút" | In-app + push |
| Ứng viên confirm | "[Tên] đã xác nhận tham dự" | In-app toast |
| Ứng viên đề xuất đổi lịch | "[Tên] muốn đổi sang [giờ mới]" | In-app + email |

---

## 10. Edge cases

| Tình huống | Xử lý |
|---|---|
| Tạo lịch từ Calendar cho ứng viên chưa ở stage INTERVIEW | Tự động chuyển stage sang INTERVIEW sau khi tạo lịch thành công |
| Click vào ngày trong quá khứ | Có thể nhập trên form nhưng backend từ chối khi submit với thông báo lỗi rõ ràng |
| Ứng viên không có trong dropdown (đã có lịch rồi) | Hiện tooltip: "Ứng viên này đã có lịch phỏng vấn. Vào Kanban để xem." |
| Tạo 2 lịch cùng giờ cùng phòng | Cho phép (daily room model gộp chung), cảnh báo HR biết |
| Kéo event sang ngày khác (drag trên calendar) | Confirm dialog "Đổi lịch phỏng vấn sang [ngày mới]?" → cập nhật + gửi email ứng viên |
| Event ở view tháng bị cắt do quá nhiều | Hiện "+N more" → click → expand hoặc chuyển sang view ngày |
| HR xóa lịch đang IN_PROGRESS | Không cho xóa, chỉ cho "Kết thúc sớm" |
