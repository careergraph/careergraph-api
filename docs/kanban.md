# Kanban — Quản lý ứng viên theo giai đoạn tuyển dụng

> **Phạm vi:** Module Kanban — drag & drop, xem chi tiết ứng viên, tạo phỏng vấn từ kanban  
> **Liên quan:** `interview-system-requirements.md`, `calendar.md`
Source HR: careergraph-hr/
Source Client( candidate): careergraph-client/
Source API: careergraph-api/
---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Cấu trúc cột (Pipeline stages)](#2-cấu-trúc-cột-pipeline-stages)
3. [Kanban Card](#3-kanban-card)
4. [Drag & Drop — Chuyển giai đoạn](#4-drag--drop--chuyển-giai-đoạn)
5. [Flow đặc biệt: Kéo vào cột Phỏng vấn](#5-flow-đặc-biệt-kéo-vào-cột-phỏng-vấn)
6. [Chi tiết ứng viên (Side panel)](#6-chi-tiết-ứng-viên-side-panel)
7. [Trạng thái ứng viên sau phỏng vấn](#7-trạng-thái-ứng-viên-sau-phỏng-vấn)
8. [Phân quyền](#8-phân-quyền)
9. [Edge cases](#9-edge-cases)

---

## 1. Tổng quan

Kanban là màn hình chính để HR quản lý toàn bộ ứng viên của **một vị trí tuyển dụng (job)**. Mỗi job có một board riêng. Ứng viên được di chuyển qua các cột theo tiến trình tuyển dụng.

**URL pattern:** `/kanbans/{job_id}`

**Đặc điểm:**
- Mỗi cột = 1 giai đoạn tuyển dụng (stage)
- Kéo thả card = chuyển stage
- Một số stage có **trigger action** (vd: kéo vào Phỏng vấn → hiện modal lên lịch)
- Cột và thứ tự cột có thể cấu hình theo từng công ty/job

---

## 2. Cấu trúc cột (Pipeline stages)

### Các cột mặc định (theo thứ tự)

| Thứ tự | Tên cột | Stage key | Màu | Mô tả |
|---|---|---|---|---|
| 1 | Ứng tuyển | `APPLIED` | Xanh dương | Ứng viên mới nộp hồ sơ |
| 2 | Liên hệ | `CONTACTED` | Cam | HR đã liên hệ, chưa xác nhận |
| 3 | Phỏng vấn | `INTERVIEW` | Tím | Đã/đang lên lịch phỏng vấn |
| 4 | Thử việc | `PROBATION` | Vàng sao | Ứng viên đã pass, đang thử việc |
| 5 | Nhận chính thức | `HIRED` | Xanh lá | Đã ký hợp đồng chính thức |

**Stage đặc biệt (không hiển thị trên board chính):**

| Stage key | Hiển thị | Mô tả |
|---|---|---|
| `REJECTED` | Ẩn (filter được) | Đã từ chối |
| `WITHDRAWN` | Ẩn | Ứng viên tự rút |

### Số liệu trên header cột

```
┌────────────────────────────────┐
│  👤 PHỎNG VẤN              3  │   ← badge số lượng ứng viên
│  3 ứng viên                    │
├────────────────────────────────┤
│  [card]                        │
│  [card]                        │
│  [card]                        │
└────────────────────────────────┘
```

---

## 3. Kanban Card

### Thông tin hiển thị trên card

```
┌────────────────────────────────────┐
│  NV   Nguyen Van A                 │  ← avatar + tên
│       Backend Engineer (Spring Boot)│  ← vị trí ứng tuyển
│       ┌──────────┐                  │
│       │ ƯU TIÊN  │                  │  ← badge priority (nếu có)
│       └──────────┘                  │
│       📅 2026-03-04 at 07:00        │  ← ngày ứng tuyển
│       📍 District 1                 │  ← địa điểm
└────────────────────────────────────┘
```

### Badge trạng thái bổ sung (hiển thị theo context)

| Badge | Màu | Điều kiện hiển thị |
|---|---|---|
| Ưu tiên | Cam | `priority = HIGH` |
| Chờ đánh giá | Vàng | Sau phỏng vấn, chưa có evaluation |
| Có recording | Xanh nhạt | `interview_recordings` tồn tại |
| 🔄 Đang đổi lịch | Amber | `interview.status = PENDING_RESCHEDULE` |
| ✅ Lịch mới xác nhận | Xanh | Vừa reschedule thành công |
| Không đến | Đỏ nhạt | `interview.status = NO_SHOW` |

### Card actions (hover / right-click menu)

- Xem chi tiết ứng viên
- Gửi email
- Gửi tin nhắn
- Đổi người phụ trách
- Đánh dấu ưu tiên
- Chuyển cột nhanh (không cần kéo)
- Từ chối ứng viên

---

## 4. Drag & Drop — Chuyển giai đoạn

### Flow cơ bản

```
HR kéo card từ cột A → thả vào cột B
    │
    ▼
Kiểm tra: cột B có trigger action không?
    │
    ├─ Không có trigger → Cập nhật stage ngay lập tức
    │                      Gọi API: PATCH /applications/{id}/stage
    │                      Optimistic update UI (không reload)
    │
    └─ Có trigger (vd: INTERVIEW) → Hiện modal tương ứng
                                     (xem mục 5)
```

### Các chuyển stage được phép

| Từ | Đến | Ghi chú |
|---|---|---|
| Bất kỳ | Bất kỳ (tiến) | Luôn cho phép nếu đi về phía trước trong pipeline |
| Bất kỳ | Bất kỳ (lùi) | Bị chặn ở UI và backend |
| Bất kỳ | `REJECTED` | Confirm + nhập lý do từ chối |
| `INTERVIEW` | `APPLIED`/`CONTACTED` | Không cho kéo lùi; nếu cần hạ stage phải đi qua quy trình riêng |

**Điểm điều khiển rule:**
- Backend kiểm soát ở `ApplicationServiceImpl.validateStageTransition(...)`.
- Danh sách stage forward-only nằm trong `APPLICATION_STAGE_FLOW`.
- Muốn nới/siết quy tắc, chỉ sửa một điểm này thay vì sửa từng màn hình.

### Lùi stage (kéo ngược)

```
HR kéo card lùi về cột trước
    │
    ▼
Confirm dialog:
    "Bạn muốn chuyển [Tên] về giai đoạn [Tên cột]?
     Lưu ý: Lịch phỏng vấn hiện tại (nếu có) sẽ không bị hủy tự động."
    │
    ├─ Xác nhận → Cập nhật stage, giữ nguyên interview records
    └─ Hủy → Hoàn tác, card quay về vị trí cũ
```

### Kéo vào REJECTED

```
HR kéo vào cột "Từ chối" (hoặc chọn từ menu)
    │
    ▼
Modal từ chối:
    Lý do từ chối (chọn từ list + nhập thêm):
    ○ Không đủ kinh nghiệm
    ○ Không phù hợp văn hóa
    ○ Mức lương không phù hợp
    ○ Vị trí đã có người
    ○ Khác: [textarea]

    Gửi email thông báo cho ứng viên: [checkbox, default: ON]
    │
    ▼
Cập nhật stage = REJECTED
Nếu có lịch phỏng vấn đang SCHEDULED/CONFIRMED → auto CANCELLED
Gửi email (nếu checkbox ON)
```

---

## 5. Flow đặc biệt: Kéo vào cột Phỏng vấn

### 5.1 Trigger modal lên lịch

```
HR kéo card ứng viên → thả vào cột "Phỏng vấn"
    │
    ▼
Kiểm tra: ứng viên đã có interview SCHEDULED/CONFIRMED chưa?
    │
    ├─ Đã có lịch → Hiện thông báo:
    │               "Ứng viên này đã có lịch phỏng vấn vào [ngày giờ].
    │                Bạn có muốn tạo thêm lịch mới không?"
    │               [Xem lịch cũ] [Tạo lịch mới]
    │
    └─ Chưa có lịch → Hiện modal "Lên lịch phỏng vấn" (xem 5.2)
    │
    ▼
Nếu HR hủy modal (không lưu):
    → Card tự động quay về cột cũ (hoàn tác kéo thả)
    → Stage KHÔNG bị thay đổi
```

### 5.2 Modal lên lịch phỏng vấn (từ Kanban)

Form bao gồm:

```
Lên lịch phỏng vấn
─────────────────────────────────────────

Ứng viên (đã được điền sẵn, không thay đổi được):
  ┌──────────────────────────────────────────┐
  │ Ứng viên: Nguyen Van A                   │
  │ Email:    candidate@email.com            │
  │ Vị trí:   Backend Engineer (Spring Boot) │
  └──────────────────────────────────────────┘

Ngày:           [Date picker]
Giờ bắt đầu:   [Time picker — default 09:00 AM]
Thời lượng:     [Dropdown: 30 phút / 60 phút / 90 phút / 120 phút]
Hình thức:      [Dropdown: Online (WebRTC) / Offline]

ℹ️ Link phòng WebRTC sẽ được tạo tự động sau khi lên lịch

Ghi chú:        [Textarea]

[Hủy]                          [Lên lịch phỏng vấn]
```

**Validation:**
- Ngày và giờ bắt đầu là bắt buộc, không được ở quá khứ
- Nếu cùng ngày đã có room cho job này → gắn vào room đó (Daily Room Model)
- Nếu chưa có room → tạo room mới
- Nếu cùng room + cùng application đã có slot trước đó nhưng slot chưa diễn ra, backend sẽ cập nhật row slot hiện có thay vì tạo slot mới
- Nếu đã tồn tại interview active cho cùng application + job, UI phải bật popup xác nhận ghi đè trước khi gửi lại request
- Khi HR xác nhận ghi đè, hệ thống tự động hủy interview active cũ rồi tạo lịch mới
- Chỉ cho phép lên lịch nếu hồ sơ ứng tuyển đang ở stage hợp lệ cho phỏng vấn

### 5.3 Sau khi lưu lịch

```
API tạo interview thành công
    │
    ▼
1. Stage ứng viên = INTERVIEW (chính thức chuyển cột)
2. Interview status = SCHEDULED
3. Room: tạo mới hoặc gắn vào room đã có cùng ngày + job
4. Sinh join_token cho ứng viên
5. Gửi email thông báo lịch phỏng vấn cho ứng viên
6. Card trên Kanban cập nhật: hiển thị ngày phỏng vấn
7. Toast: "Đã lên lịch phỏng vấn thành công"
```

**Quy tắc production cho rebook cùng ngày:**
- Nếu HR hẹn lại ứng viên cùng job/cùng ngày/cùng room, hệ thống giữ 1 slot canonical cho application đó trong room.
- Việc hẹn lại sẽ cập nhật `slot_start` và `slot_end` của row hiện có.
- Nếu slot đã được dùng thật sự (`ADMITTED` hoặc `COMPLETED`), backend chặn để tránh ghi đè lịch sử thực thi.
- Audit lịch sử phỏng vấn vẫn nằm ở `interviews`, không phụ thuộc vào room slot.

---

## 6. Chi tiết ứng viên (Side panel)

### Mở side panel

- Click vào card bất kỳ → mở side panel từ phải
- URL cập nhật: `/kanbans/{job_id}?candidate={application_id}`
- Có thể share link trực tiếp đến side panel này

### Các tab trong side panel

#### Tab 1 — Thông tin chi tiết

```
[Header]
Avatar | Tên ứng viên | ID ứng viên
       | Vị trí ứng tuyển | Địa điểm
       | Trạng thái hiện tại (badge)
                                    | Apply: [ngày ứng tuyển]

[Metrics row]
KINH NGHIỆM | HOẠT ĐỘNG GẦN NHẤT | NGƯỜI PHỤ TRÁCH

[Section: Thông tin công việc mong muốn]
- Vị trí, Mức lương mong muốn
- Hình thức làm việc, Ngành nghề
- Nơi làm việc

[Section: Thông tin cá nhân]
- Ngày sinh, Giới tính
- Tình trạng hôn nhân, Email
```

#### Tab 2 — Kinh nghiệm

- Danh sách kinh nghiệm làm việc theo timeline
- Học vấn, chứng chỉ

#### Tab 3 — CV

- Preview CV (PDF viewer inline)
- Nút download CV

#### Tab 4 — Đánh giá phỏng vấn

```
Danh sách các lần phỏng vấn:
  ┌─────────────────────────────────────────┐
  │ 📅 15/12/2024 — 09:00-09:30             │
  │ HR: Nguyen Cong Quy                     │
  │ Kết quả: ★★★★☆  — PASS                 │
  │ Ghi chú: "Candidate có kỹ năng tốt..."  │
  │ 🎥 Có recording                         │
  │ [Xem đánh giá chi tiết]                 │
  └─────────────────────────────────────────┘

  ┌─────────────────────────────────────────┐
  │ 📅 20/12/2024 — 14:00-14:30             │
  │ Vòng 2: Kỹ thuật                        │
  │ Chưa có đánh giá  ⚠                    │
  │ [Đánh giá ngay]                         │
  └─────────────────────────────────────────┘
```

#### Tab 5 — Tin nhắn

- Thread tin nhắn nội bộ giữa HR team về ứng viên này
- Không gửi cho ứng viên

#### Tab 6 — Email

- Lịch sử email đã gửi cho ứng viên
- Compose email mới ngay trong panel

### Actions trong side panel

- **Đổi người phụ trách:** Assign HR khác
- **Đổi cột:** Dropdown chuyển stage (thay cho kéo thả)
- **Lên lịch phỏng vấn:** Mở modal (tương đương kéo vào cột Phỏng vấn)
- **Từ chối:** Mở modal từ chối
- **Gửi email:** Compose email

---

## 7. Trạng thái ứng viên sau phỏng vấn

> **Quy tắc quan trọng:** Kết thúc phỏng vấn KHÔNG tự động thay đổi stage trên Kanban.  
> Chỉ `interview_evaluations.recommendation` mới trigger thay đổi.

### Mapping recommendation → stage

| Recommendation | Stage mới | Ghi chú |
|---|---|---|
| `PASS` | Stage kế tiếp trong pipeline (vd: `PROBATION`) | Tự động chuyển cột |
| `FAIL` | `REJECTED` | Chuyển sang rejected, card biến khỏi board chính |
| `HOLD` | Giữ nguyên `INTERVIEW` | Thêm badge "Cần xem xét" |

### Kanban realtime update

- Sau khi HR lưu đánh giá → Kanban cập nhật ngay (socket emit hoặc polling 30 giây)
- Không cần reload trang
- Nếu card chuyển cột → animation slide sang cột mới

---

## 8. Phân quyền

| Hành động | HR | Admin | Viewer |
|---|---|---|---|
| Xem board | ✅ | ✅ | ✅ |
| Kéo thả card | ✅ | ✅ | ❌ |
| Tạo phỏng vấn | ✅ | ✅ | ❌ |
| Từ chối ứng viên | ✅ | ✅ | ❌ |
| Xem chi tiết ứng viên | ✅ | ✅ | ✅ |
| Đánh giá phỏng vấn | ✅ | ✅ | ❌ |
| Xóa ứng viên khỏi pipeline | ❌ | ✅ | ❌ |

---

## 9. Edge cases

| Tình huống | Xử lý |
|---|---|
| Kéo vào cột Phỏng vấn nhưng hủy modal | Card hoàn tác về cột cũ, stage không đổi |
| Ứng viên có 2 lịch phỏng vấn cùng lúc | Cảnh báo khi tạo, vẫn cho tạo nếu HR confirm |
| Kéo từ Phỏng vấn về Liên hệ khi đang có lịch ACTIVE | Cảnh báo mạnh "Đang có phòng phỏng vấn đang chạy", không tự hủy |
| Nhiều HR cùng kéo 1 card | Last-write-wins, người sau thấy toast "Card đã được cập nhật bởi [tên]" |
| Board có > 50 ứng viên 1 cột | Lazy load — chỉ render 20 card đầu, scroll để load thêm |
| Ứng viên apply 2 job khác nhau | Xuất hiện ở 2 board riêng biệt, độc lập nhau |
