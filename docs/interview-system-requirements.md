# Hệ thống Phỏng Vấn Trực Tuyến — Tài Liệu Yêu Cầu Đầy Đủ

> **Phiên bản:** 2.0  
> **Cập nhật:** 2026-04  
> **Phạm vi:** Toàn bộ module Interview — Room, Media, Admit, Record, Evaluation, Kanban
Source HR: careergraph-hr/
Source Client( candidate): careergraph-client/
Source API: careergraph-api/
---

## Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Data Model](#2-data-model)
3. [Room Lifecycle](#3-room-lifecycle)
4. [Flow tạo phòng & lên lịch](#4-flow-tạo-phòng--lên-lịch)
5. [Flow vào phòng — HR](#5-flow-vào-phòng--hr)
6. [Flow vào phòng — Ứng viên](#6-flow-vào-phòng--ứng-viên)
7. [Admit / Kick / Reload](#7-admit--kick--reload)
8. [Tính năng Media (Camera, Mic, Screen Share)](#8-tính-năng-media-camera-mic-screen-share)
9. [Tính năng Recording](#9-tính-năng-recording)
10. [Đánh giá ứng viên (Evaluation)](#10-đánh-giá-ứng-viên-evaluation)
11. [Kết thúc phỏng vấn — End Interview Flow](#11-kết-thúc-phỏng-vấn--end-interview-flow)
12. [Kanban & Trạng thái ứng viên sau phỏng vấn](#12-kanban--trạng-thái-ứng-viên-sau-phỏng-vấn)
13. [Socket Events Reference](#13-socket-events-reference)
14. [Notification Flow](#14-notification-flow)
15. [Security & Production Checklist](#15-security--production-checklist)
16. [Các edge case & xử lý lỗi](#16-các-edge-case--xử-lý-lỗi)

---

## 1. Tổng quan kiến trúc

### Mô hình phòng theo ngày (Daily Room Model)

Mỗi ngày phỏng vấn của một vị trí tuyển dụng → tạo **1 room duy nhất** dùng chung cho tất cả ứng viên được lên lịch trong ngày đó.

```
job_id + interview_date  →  1 interview_room (unique)
```

**HR** ở lại trong phòng suốt cả ngày. **Ứng viên** lần lượt vào theo đúng slot của mình.

- Nhiều job position cùng ngày → tạo room **riêng biệt** cho từng position
- Ứng viên chỉ được vào đúng slot của mình (kiểm soát bằng token + thời gian)
- HR không cần rời phòng giữa các lượt ứng viên

### Production Update 2026-04-05 (Bắt buộc)

- Room có thể dùng chung cho nhiều interview trong ngày, nhưng mọi nghiệp vụ phải xử lý theo từng candidate/interview slot.
- Không được khóa toàn room chỉ vì một interview đại diện đã `COMPLETED` nếu room còn ứng viên chưa phỏng vấn.
- Candidate vào room phải được đối chiếu theo interview của chính candidate trong room đó.
- Chỉ candidate đã thật sự vào phòng (`room_participants.joined_at != null`) mới được:
    - đánh giá (feedback),
    - gán bản ghi (recording),
    - chuyển `COMPLETED`.
- Khi HR hẹn lại cùng `room_code + application_id` trong cùng ngày, backend phải cập nhật slot hiện có thay vì tạo slot mới vô hạn.
- Một candidate có thể có nhiều interview ở các ngày khác nhau trong cùng job, nhưng mỗi room/day chỉ giữ 1 slot canonical cho đúng application.
- Trong cùng một job, mỗi application chỉ được phép có tối đa 1 interview active tại một thời điểm (`SCHEDULED/CONFIRMED/PENDING_RESCHEDULE/IN_PROGRESS`).
- Nếu HR tạo lịch mới khi đã có interview active, hệ thống bắt buộc hiển thị xác nhận ghi đè trước khi tự động hủy lịch active cũ.

### Production Update 2026-04-08 (Bắt buộc)

- HR không bị giới hạn vào phòng theo mốc "vào trước 15 phút" như candidate. HR có thể vào phòng bất kỳ lúc nào để chuẩn bị/vận hành phiên.
- Nếu room/day có nhiều interview slot, UI phải chọn slot đại diện theo ngữ cảnh thực tế:
    - Ưu tiên `IN_PROGRESS`.
    - Nếu không có `IN_PROGRESS`, ưu tiên slot chưa kết thúc gần nhất (upcoming/active).
    - Chỉ fallback về slot quá khứ khi toàn bộ slot hợp lệ đã kết thúc.
- Khi toàn bộ slot hợp lệ trong room đã kết thúc, màn hình HR phải hiển thị trạng thái "Phòng phỏng vấn đã kết thúc" riêng biệt; không dùng wording "Chưa đến giờ phỏng vấn".
- Với room có nhiều ứng viên, phần tóm tắt phòng phải hiển thị thông tin tổng quan (job/date/total candidates), tránh hiển thị duy nhất một ứng viên gây hiểu sai ngữ cảnh.
- Nút "Đánh giá ứng viên" chỉ được hiển thị khi còn ít nhất 1 ứng viên `COMPLETED` và chưa có feedback.
- Modal đánh giá chỉ được nạp danh sách ứng viên `COMPLETED` chưa đánh giá. Nếu tại thời điểm mở modal không còn ứng viên hợp lệ, ẩn hành động đánh giá và hiển thị thông báo trạng thái đã được cập nhật.
- Ở candidate portal, cảnh báo "Lịch phỏng vấn này đã quá thời gian..." chỉ dùng cho lịch quá hạn nhưng chưa ở trạng thái kết thúc nghiệp vụ (`COMPLETED/CANCELLED/NO_SHOW`).

### Production Update 2026-04-08-B (Calendar & Reschedule Governance)

- Calendar edit phải tách rõ 3 nhóm thao tác nghiệp vụ:
    - `Reschedule` (đổi ngày/giờ): tạo interview version mới + đóng interview cũ (`CANCELLED`, có `rescheduled_from_id`).
    - `Update content` (ghi chú, địa điểm, thời lượng cùng slot): cập nhật trực tiếp trên record hiện tại, không tạo record mới.
    - `Update status` (SCHEDULED/CONFIRMED/PENDING_RESCHEDULE/CANCELLED...): cập nhật trạng thái trên record hiện tại, không đi qua luồng reschedule.
- Cấm dùng luồng reschedule để xử lý thao tác đổi status thuần túy.
- Nếu HR hủy hoặc dời lịch, record cũ phải vẫn hiển thị cho candidate theo policy lịch sử (không hidden mặc định cho trường hợp này).
- Candidate portal cần có chế độ xem lịch sử (`HISTORY`) để theo dõi toàn bộ biến động lịch hẹn liên quan phiên bản cũ/mới.
- Calendar sorting cho danh sách theo ngày phải theo mức độ vận hành:
    1. Cần theo dõi (`IN_PROGRESS`, `PENDING_RESCHEDULE`)
    2. Đã xác nhận (`CONFIRMED`)
    3. Chờ xác nhận (`SCHEDULED`)
    4. Cần xử lý (`CANCELLED`, `NO_SHOW`)
    5. Hoàn thành (`COMPLETED`)
    Sau đó mới đến thứ tự thời gian trong ngày.

### Production Update 2026-04-08-C (HR Interview List & Scheduling Consistency)

- Thống kê room ở màn HR `/interviews` phải đếm theo **ứng viên duy nhất** (`application_id`), không đếm theo số bản ghi interview version.
- Mỗi `application_id` trong room chỉ lấy **version mới nhất** làm bản ghi đại diện cho trạng thái hiện tại của ứng viên.
- Số lượng "đã hủy/không tham gia" phải tính theo số ứng viên có trạng thái hiện tại là `CANCELLED` hoặc `NO_SHOW`, không tính theo số lịch sử bị hủy.
- UI room card phải cho phép mở danh sách chi tiết các ứng viên đã hủy/không tham gia và click vào từng record để xem chi tiết.

- Ở màn chỉnh sửa lịch hẹn:
        - Nếu người dùng không thay đổi dữ liệu (thời gian, trạng thái, ghi chú), frontend **không được gửi request update**.
        - Khi lịch đã `CANCELLED` và người dùng chọn lại đúng trạng thái hủy, frontend phải chặn submit và hiển thị thông báo no-op.
        - Nút hành động phải tách rõ intent: `Lưu thay đổi` (update/reschedule) và `Hủy lịch phỏng vấn` (cancel).

- Eligibility cho API danh sách ứng viên có thể lên lịch (`/interviews/job/{jobId}/unscheduled`):
        - Cho phép các hồ sơ ở stage `INTERVIEW_COMPLETED` được lên lịch lại nếu **không có interview active**.
        - Điều kiện loại trừ vẫn dựa trên active interview statuses:
            `SCHEDULED`, `CONFIRMED`, `PENDING_RESCHEDULE`, `IN_PROGRESS`.
        - Điều này đảm bảo ứng viên có lịch đã hủy hoặc đã hoàn tất vòng trước nhưng cần hẹn vòng tiếp theo vẫn xuất hiện đúng trong luồng tạo lịch.

### Stack kỹ thuật (khuyến nghị)

| Layer | Công nghệ |
|---|---|
| Signaling / Room state | Socket.io |
| Media streaming | WebRTC (SFU: LiveKit / mediasoup) |
| Recording | Server-side composite hoặc LiveKit built-in |
| Auth | JWT (short-lived token per participant) |
| DB | PostgreSQL |
| Background jobs | BullMQ / pg-cron |

---

## 2. Data Model

### Bảng `interview_rooms`

```sql
id              UUID PRIMARY KEY
job_id          UUID REFERENCES jobs(id)
interview_date  DATE                          -- ngày phỏng vấn
room_code       VARCHAR(20) UNIQUE NOT NULL   -- vd: JOB-20241201-ABC
status          ENUM(SCHEDULED, WAITING, ACTIVE, CLOSING, ENDED, EXPIRED)
host_id         UUID REFERENCES users(id)     -- HR chủ phòng
open_at         TIMESTAMPTZ                   -- lúc HR mở phòng (set ACTIVE)
closed_at       TIMESTAMPTZ                   -- lúc kết thúc thực tế
max_duration    INT DEFAULT 480               -- phút, tối đa cả ngày
created_at      TIMESTAMPTZ DEFAULT now()
```

### Bảng `room_participants` (slot của từng ứng viên)

```sql
id              UUID PRIMARY KEY
room_id         UUID REFERENCES interview_rooms(id)
application_id  UUID REFERENCES applications(id)
candidate_id    UUID REFERENCES users(id)
slot_start      TIMESTAMPTZ
slot_end        TIMESTAMPTZ
join_token      VARCHAR                        -- JWT signed, expires = slot_end + 30min
session_token   VARCHAR                        -- token tạm thời khi đang trong phòng (dùng cho reconnect)
admit_status    ENUM(PENDING, WAITING_LOBBY, ADMITTED, REJECTED, REMOVED, COMPLETED)
knock_count     INT DEFAULT 0
last_knock_at   TIMESTAMPTZ
joined_at       TIMESTAMPTZ
left_at         TIMESTAMPTZ
hr_note         TEXT                           -- ghi chú nhanh của HR trong phòng
created_at      TIMESTAMPTZ DEFAULT now()
```

### Bảng `interview_recordings`

```sql
id              UUID PRIMARY KEY
room_id         UUID REFERENCES interview_rooms(id)
participant_id  UUID REFERENCES room_participants(id)  -- nullable: chưa gán
file_url        TEXT
file_size       BIGINT
duration_sec    INT
started_at      TIMESTAMPTZ
ended_at        TIMESTAMPTZ
assigned_by     UUID REFERENCES users(id)              -- HR đã gán
assigned_at     TIMESTAMPTZ
created_at      TIMESTAMPTZ DEFAULT now()
```

### Bảng `interview_evaluations`

```sql
id              UUID PRIMARY KEY
room_id         UUID REFERENCES interview_rooms(id)
participant_id  UUID REFERENCES room_participants(id)
application_id  UUID REFERENCES applications(id)
evaluator_id    UUID REFERENCES users(id)   -- HR thực hiện đánh giá
overall_rating  INT CHECK (1..5)
criteria        JSONB                        -- {communication: 4, technical: 3, attitude: 5, ...}
strengths       TEXT
weaknesses      TEXT
recommendation  ENUM(PASS, FAIL, HOLD)
note            TEXT
evaluated_at    TIMESTAMPTZ DEFAULT now()
created_at      TIMESTAMPTZ DEFAULT now()
```

---

## 3. Room Lifecycle

```
SCHEDULED → WAITING → ACTIVE → CLOSING → ENDED
                                        ↗
                               (auto-expire) EXPIRED
```

| Status | Mô tả | Ai trigger |
|---|---|---|
| `SCHEDULED` | Room đã tạo, chưa ai vào | Hệ thống (khi lên lịch) |
| `WAITING` | HR đã vào lobby, chờ giờ mở | HR join trước ≤ 15 phút |
| `ACTIVE` | Phòng mở, ứng viên có thể knock | HR bấm "Mở phòng" |
| `CLOSING` | HR kết thúc, grace period 5 phút | HR bấm "Kết thúc phỏng vấn" |
| `ENDED` | Phòng đã đóng hoàn toàn, không ai vào được | Tự động sau grace / cronjob |
| `EXPIRED` | Hết thời gian mà chưa được mở (no-show) | Cronjob buổi sáng hôm sau |

**Quy tắc bất biến:**
- Room đã `ENDED` hoặc `EXPIRED` không bao giờ được tái sử dụng
- `room_code` là unique global — không recycle
- Record DB luôn được giữ lại để audit, không xóa

---

## 4. Flow tạo phòng & lên lịch

```
HR tạo lịch phỏng vấn cho ứng viên (ngày + giờ slot)
    │
    ▼
Hệ thống kiểm tra: đã có room cho (job_id + interview_date) chưa?
    │
    ├─ Chưa có → Tạo interview_room mới (status: SCHEDULED)
    │            Sinh room_code duy nhất
    │
    └─ Đã có  → Dùng lại room đó
    │
    ▼
Tạo room_participant record cho ứng viên
    slot_start, slot_end, sinh join_token (JWT)
    │
    ▼
Gửi thông báo:
    - HR: xác nhận lịch, link vào phòng host
    - Ứng viên: thông tin phỏng vấn, link riêng kèm join_token
```

**Lưu ý:**
- Link của HR không cần token slot (là host của room)
- Link của ứng viên mang JWT riêng — không thể dùng link của người khác
- Nếu lịch bị hủy, set `admit_status = REMOVED`, gửi email thông báo ứng viên

---

## 5. Flow vào phòng — HR

```
HR click link → Validate HR là host_id của room
    │
    ├─ Room status = SCHEDULED và còn ≥ 30 phút trước giờ đầu tiên
    │       → Hiển thị "Phòng chưa mở, quay lại sau"
    │
    ├─ Room status = SCHEDULED và trong 30 phút trước giờ → set WAITING
    │       → HR vào lobby host, thấy danh sách ứng viên hôm nay + slot giờ
    │
    └─ Room status = WAITING/ACTIVE
            → Vào thẳng room, nhận đủ quyền host
    │
    ▼
HR trong phòng có thể:
    - Bấm "Mở phòng" → set ACTIVE (ứng viên mới được knock)
    - Thấy danh sách ứng viên đang chờ lobby (knock)
    - Admit / Reject từng ứng viên
    - Kick ứng viên đang trong phòng
    - Bật/tắt camera, mic
    - Chia sẻ màn hình
    - Bắt đầu / Dừng recording
    - Đánh giá ứng viên bất kỳ lúc nào trong phỏng vấn
    - Kết thúc phỏng vấn
```

**Production UX (HR):**
- Khi HR mở link phòng, UI luôn qua bước "Đang kiểm tra phòng" trước khi render WebRTC.
- Nếu API trả 404 (room code không tồn tại/hết hiệu lực), hiển thị trang lỗi chuyên dụng với mã phòng + CTA quay lại danh sách phỏng vấn.
- Nếu lỗi mạng/5xx, hiển thị trang "Không thể kết nối tới phòng" với nút thử lại (không crash trang).

---

## 6. Flow vào phòng — Ứng viên

```
Ứng viên click link (kèm join_token)
    │
    ▼
Server validate:
    ├─ Token hợp lệ + chưa hết hạn?          (nếu không → 401)
    ├─ candidate_id match participant record?  (nếu không → 403)
    ├─ Room tồn tại?                          (nếu không → 404)
    ├─ Room status ≠ ENDED/EXPIRED?           (nếu ENDED → "Phòng đã đóng")
    └─ admit_status ≠ REMOVED?               (nếu REMOVED → "Bạn đã bị loại khỏi phòng này")
    │
    ▼
Kiểm tra thời gian slot:
    ├─ Trước slot_start > 15 phút  → "Chưa đến giờ, vui lòng quay lại lúc HH:MM"
    ├─ Trước slot_start ≤ 15 phút  → Vào lobby, chờ, chưa knock được
    ├─ Trong slot (start → end)    → Vào lobby, có thể knock
    ├─ Sau slot_end ≤ 15 phút      → Cảnh báo "Bạn vào muộn", vẫn cho knock
    ├─ Sau slot_end 15-30 phút     → HR phải xác nhận override mới cho knock
    └─ Sau slot_end > 30 phút      → Từ chối hoàn toàn "Slot đã hết hạn"
    │
    ▼
Ứng viên trong lobby:
    - Thấy màn hình chờ "Đang chờ HR xác nhận"
    - Có thể test camera/mic trước khi vào
    - Bấm "Gõ cửa" (knock) → HR nhận thông báo
```

**Production UX (Candidate):**
- Khi ứng viên mở link phòng, client phải hiển thị bước "Đang kiểm tra phòng phỏng vấn" để validate room trước.
- Room không tồn tại/hết hiệu lực (404) phải đi đến trang lỗi chuyên dụng, không chỉ hiển thị toast.
- Lỗi tạm thời (mạng/5xx) hiển thị màn hình lỗi có nút "Thử lại", không cho vào pre-join lobby khi chưa validate room.

---

## 7. Admit / Kick / Reload

### 7.1 Admit flow (HR xác nhận cho vào)

```
Ứng viên gõ cửa (emit knock)
    │
    ▼
HR nhận popup thông báo:
    "Nguyễn Văn A đang chờ — [Cho vào] [Từ chối]"
    │
    ├─ HR bấm "Cho vào"
    │       → emit admit_granted → ứng viên kết nối WebRTC → vào phòng
    │       → set admit_status = ADMITTED, joined_at = now()
    │       → nếu interview đang `SCHEDULED` hoặc `CONFIRMED` thì auto chuyển `IN_PROGRESS`
    │
    └─ HR bấm "Từ chối" (có thể kèm lý do tùy chọn)
            → emit admit_rejected + reason → ứng viên thấy thông báo
            → set admit_status = REJECTED
            → ứng viên có thể knock lại sau 2 phút (cooldown)

Timeout: Nếu HR không phản hồi sau 60 giây → giữ nguyên WAITING_LOBBY,
         hệ thống nhắc HR lần nữa (không tự admit)
```

### 7.2 Kick flow (HR kick ứng viên ra)

```
HR bấm nút "Kick" bên cạnh tên ứng viên
    │
    ▼
Confirm dialog: "Bạn có chắc muốn loại [Tên] ra khỏi phòng?"
    │
    ▼
Server emit participant_removed → ứng viên nhận thông báo "Bạn đã bị loại khỏi phòng"
    │
    ▼
Set admit_status = REMOVED, left_at = now()
    │
    ▼
Ứng viên BỊ CHẶN hoàn toàn:
    - Không thể knock lại trong phiên này
    - Nếu reload/truy cập lại → thấy màn hình "Bạn đã bị loại khỏi phòng phỏng vấn này"
    - Không có nút thử lại
    - HR phải xóa participant khỏi danh sách đang hoạt động để room state không còn slot rỗng
```

### 7.3 Reload / Reconnect (QUAN TRỌNG)

#### Ứng viên reload (bình thường — không bị kick):

```
Ứng viên reload trang / mất mạng ngắn
    │
    ▼
Client kiểm tra localStorage: có session_token không?
    │
    ├─ Có session_token và còn hạn
    │       → Gửi rejoin_room { session_token }
    │       → Server kiểm tra admit_status = ADMITTED và room = ACTIVE
    │       → Cho vào thẳng, KHÔNG cần knock lại
    │       → KHÔNG cần HR xác nhận lại
    │
    └─ Không có session_token / hết hạn
            → Dùng join_token gốc
            → Nếu admit_status = ADMITTED → cho vào thẳng (trong 30 phút sau joined_at)
            → Nếu admit_status = WAITING_LOBBY → knock lại bình thường
```

#### HR reload:

```
HR reload trang
    → Luôn được vào lại phòng với đủ quyền host
    → Room state được giữ nguyên trên server
    → Danh sách participant hiện tại được load lại
```

#### Quy tắc session:

| Trạng thái | Reload | Kết quả |
|---|---|---|
| `ADMITTED` | Không bị kick | Vào thẳng, không cần admit lại |
| `ADMITTED` | Bị kick trước đó | Màn hình "Bị loại khỏi phòng" |
| `WAITING_LOBBY` | Reload | Quay lại lobby, knock lại bình thường |
| `REJECTED` | Reload sau 2 phút | Cho knock lại |
| `REMOVED` | Bất kỳ lúc nào | Bị chặn vĩnh viễn trong phiên này |

---

## 8. Tính năng Media (Camera, Mic, Screen Share)

### 8.1 Camera & Microphone

#### Khởi tạo thiết bị (Device Init)

Khi người dùng vào lobby (trước khi join phòng), hệ thống thực hiện:

1. **Liệt kê thiết bị** qua `enumerateDevices()` để xác định loại thiết bị khả dụng (videoinput, audioinput)
2. **Yêu cầu quyền** chỉ với những loại thiết bị đã phát hiện — không yêu cầu quyền cho thiết bị không tồn tại
3. **Phân loại kết quả** thành 3 nhóm:

| Kết quả | Hành vi | Cho phép join? |
|---|---|---|
| Có đủ camera + mic | Vào phòng bình thường | ✅ |
| Chỉ có camera hoặc chỉ có mic | Vào phòng với thiết bị hiện có, cảnh báo nhẹ (toast warning) | ✅ |
| Không có thiết bị nào | Vào phòng ở **chế độ xem** (view-only), nút camera/mic bị vô hiệu hóa | ✅ |
| Quyền bị chặn (NotAllowedError) | Thông báo lỗi rõ ràng kèm hướng dẫn cách mở lại quyền | ❌ |
| Thiết bị đang bị chiếm dụng (NotReadableError) | Thông báo lỗi, hướng dẫn đóng ứng dụng khác | ❌ |

> Nguyên tắc: Thiếu thiết bị ≠ lỗi. Chỉ block join khi lỗi có thể khắc phục (permission, device busy).

#### Hiển thị trạng thái thiết bị trong Lobby

- Trước khi join: hiển thị 2 indicator (Camera sẵn sàng / Không có camera, Mic sẵn sàng / Không có mic)
- Nếu phát hiện lỗi blocking: hiển thị banner đỏ kèm hướng dẫn cụ thể
- Nếu không có thiết bị: hiển thị banner thông tin "Bạn sẽ tham gia ở chế độ xem"
- Nút "Tham gia" đổi label thành "Tham gia (chế độ xem)" khi không có thiết bị

#### Quyền điều khiển media

| Hành động | HR | Ứng viên |
|---|---|---|
| Bật/tắt camera của mình | ✅ (nếu có camera) | ✅ (nếu có camera) |
| Bật/tắt mic của mình | ✅ (nếu có mic) | ✅ (nếu có mic) |
| Tắt camera người khác | ✅ | ❌ |
| Tắt mic người khác | ✅ | ❌ |

**UI controls:**
- Icon camera / mic ở bottom bar — click để toggle
- Nút camera/mic bị `disabled` (màu xám, `cursor-not-allowed`) khi không có thiết bị tương ứng
- Trạng thái off hiển thị icon gạch đỏ, thumbnail blur hoặc avatar placeholder
- Khi HR tắt camera/mic người khác → hiển thị toast thông báo cho cả 2 phía
- Khi HR tắt media ứng viên từ xa → track local ứng viên tự động disable, UI đồng bộ

### 8.2 Screen Share

| Tình huống | Xử lý |
|---|---|
| HR share màn hình | Hiển thị to ở center, camera thu nhỏ về góc (picture-in-picture) |
| Ứng viên share màn hình | Tương tự, HR có thể dừng share của ứng viên |
| Cả 2 cùng share | Chỉ cho phép 1 người share tại 1 thời điểm. Người sau share → hỏi xác nhận dừng share của người trước |
| Dừng share | Quay lại layout camera bình thường |
| Browser không hỗ trợ | Hiển thị thông báo, hướng dẫn dùng Chrome/Edge |

**Permissions:** Trình duyệt yêu cầu permission share screen → nếu từ chối → toast lỗi thân thiện.

---

## 9. Tính năng Recording

### 9.1 Bắt đầu & Dừng record

```
HR bấm nút Record (nút đỏ tròn)
    │
    ▼
Confirm: "Bắt đầu ghi hình phiên phỏng vấn này?"
    │
    ▼
Server bắt đầu ghi (server-side recording)
    - Emit recording_started → tất cả trong phòng thấy indicator "● REC"
    - Tạo record trong bảng interview_recordings (participant_id = null, chưa gán)
    │
    ▼
HR bấm "Dừng record"
    │
    ▼
Server dừng ghi, xử lý file
    - File lưu vào storage (S3/...)
    - Cập nhật interview_recordings: file_url, duration_sec, ended_at
```

### 9.2 Modal gán video sau khi dừng record ← TÍNH NĂNG MỚI

```
Sau khi dừng record thành công
    │
    ▼
Hiện modal: "Video này dành cho ứng viên nào?"
    ┌─────────────────────────────────────────┐
    │  Gán video cho ứng viên                │
    │  ─────────────────────────────────────  │
    │  Chọn ứng viên:                        │
    │  ○ Nguyễn Văn A  (10:00 - 10:30)       │
    │  ○ Trần Thị B    (10:30 - 11:00)       │
    │  ○ Lê Văn C      (11:00 - 11:30)       │
    │  ○ Không gán (lưu chung cho cả phòng)  │
    │                                         │
    │  [Bỏ qua]           [Lưu & Gán]        │
    └─────────────────────────────────────────┘
```

- Danh sách hiển thị: ứng viên đã ADMITTED trong phòng hôm nay + slot giờ
- Production hiện tại: bản ghi được lưu theo `interview_id` (mỗi ứng viên là một interview riêng, dùng chung `meeting_link = room_code`)
- Sau khi gán: client map ứng viên đã chọn -> interview tương ứng trong room, rồi lưu recording vào interview đó
- Bấm "Bỏ qua": recording vẫn lưu vào interview hiện tại
- HR có thể gán lại sau từ trang chi tiết ứng viên

### 9.3 Quy tắc recording

- Chỉ HR mới có quyền bắt đầu / dừng record
- Có thể record nhiều đoạn trong 1 phiên (mỗi lần là 1 record riêng)
- Ứng viên được thông báo khi đang bị record (indicator "● REC" hiển thị với tất cả)
- File recording không bị xóa khi room ENDED — lưu vĩnh viễn theo application
- Backend phải từ chối lưu/gán recording nếu candidate của interview chưa có `joined_at` trong room participant slot.
- UI chỉ hiển thị candidate đủ điều kiện gán recording khi candidate đã vào phòng.

---

## 10. Đánh giá ứng viên (Evaluation)

### 10.1 Đánh giá trong lúc phỏng vấn ← TÍNH NĂNG MỚI

HR có thể đánh giá bất kỳ lúc nào mà không cần kết thúc phỏng vấn:

```
Danh sách ứng viên bên sidebar → bấm icon ✏️ bên cạnh tên
    │
    ▼
Mở panel đánh giá (slide-in từ phải, không đóng phòng)
    │
    ▼
Form đánh giá (xem 10.3 bên dưới)
    │
    ▼
Bấm "Lưu đánh giá"
    → Lưu vào interview_feedbacks (production)
    → Icon ✏️ đổi thành ✅ (đã đánh giá)
    → HR vẫn ở trong phòng, tiếp tục phỏng vấn bình thường
```

**Rule production:** feedback được phép gửi khi interview ở `CONFIRMED`, `IN_PROGRESS`, hoặc `COMPLETED`.
Nếu ứng viên chưa bấm Confirm nhưng đã được HR admit vào room, backend phải tự chuyển interview sang `IN_PROGRESS`
để không chặn quy trình đánh giá trong buổi phỏng vấn thực tế.
Khi room có nhiều ứng viên, HR phải chọn đúng ứng viên (tương ứng đúng interview) trước khi submit.

### 10.2 Đánh giá sau khi kết thúc phỏng vấn

Xem mục 11 — form tự hiện ngay sau khi phòng kết thúc.

### 10.3 Form đánh giá (chi tiết)

```
Form đánh giá ứng viên: [Tên ứng viên]
─────────────────────────────────────────

Tiêu chí đánh giá (mỗi tiêu chí: 1-5 sao)
  - Giao tiếp
  - Kiến thức chuyên môn
  - Thái độ
  - Khả năng giải quyết vấn đề
  - Phù hợp văn hóa công ty

Đánh giá tổng thể: ★★★★☆  (1-5)

Điểm mạnh: [textarea]
Điểm cần cải thiện: [textarea]
Ghi chú thêm: [textarea]

Kết quả đề xuất:
  ○ PASS — Chuyển sang vòng tiếp theo
  ○ HOLD — Cần xem xét thêm
  ○ FAIL — Không phù hợp

[Hủy]    [Lưu đánh giá]
```

**Lưu vào DB production:** `interview_feedback` — ghi đủ criteria, rating, recommendation, reviewer_id, created_date.

**Sau khi lưu:**
- Cập nhật `applications.stage` theo `recommendation`:
  - `PASS` → chuyển sang stage tiếp theo trong pipeline
  - `FAIL` → chuyển sang stage "Từ chối"
  - `HOLD` → giữ nguyên stage hiện tại, thêm tag "Cần xem xét"
- Ghi log vào `application_activities`

### 10.4 UI / Giao diện form đánh giá

- **Nền:** Modal với backdrop semi-transparent (nhìn thấy phía sau) — `rgba(0,0,0,0.4)`
- **Background modal:** Trắng (`#FFFFFF`)
- **Chữ:** Đen / xám đậm
- **Style:** Tương tự màn hình lên lịch phỏng vấn — clean, minimal
- **Không** có gradient, không có màu nền sặc sỡ

---

## 11. Kết thúc phỏng vấn — End Interview Flow

### 11.1 Flow đúng (sau khi fix bug)

```
HR bấm "Kết thúc phỏng vấn"
    │
    ▼
Confirm dialog: "Kết thúc phỏng vấn? Tất cả ứng viên sẽ bị đưa ra khỏi phòng."
    │
    ▼
Server thực hiện NGAY LẬP TỨC:
    1. Set room.status = CLOSING
    2. Emit room_ending { reason: "host_ended", grace_seconds: 0 } → TẤT CẢ participants
    3. Kick toàn bộ ứng viên ra (emit participant_removed với reason "interview_ended")
    4. Set tất cả admit_status = COMPLETED (không phải REMOVED)
    5. Set room.closed_at = now()
    6. Sau 5 giây → set room.status = ENDED
    │
    ▼
Phía ứng viên:
    - Nhận event room_ending → redirect về trang "Phỏng vấn đã kết thúc. Cảm ơn bạn đã tham gia!"
    - KHÔNG có nút "Tham gia lại"
    - KHÔNG hiển thị form đánh giá (đây là phía ứng viên)
    │
    ▼
Phía HR — NGAY LẬP TỨC hiện form đánh giá:
    - KHÔNG hiện nút "Tham gia lại"
    - KHÔNG redirect đi đâu
    - Modal đánh giá tự hiện overlay lên (xem 10.3, 10.4)
    - Nếu hôm nay có nhiều ứng viên chưa được đánh giá → hiện danh sách chọn
      "Bạn muốn đánh giá ứng viên nào trước?"
    - HR đánh giá xong từng người → bấm "Hoàn thành" → redirect về dashboard/kanban
```

**Production rule:**

- "Kết thúc phiên họp" chỉ đóng room/session, không tự động complete một interview đại diện.
- Hoàn thành interview phải thao tác theo từng candidate đã vào phòng.

### 11.2 Các trường hợp kết thúc khác

| Trigger | Xử lý |
|---|---|
| Tất cả participants rời phòng > 10 phút | Cronjob set CLOSING → ENDED, không hiện form |
| `max_duration` vượt quá (mặc định 8h) | Auto ENDED, gửi email nhắc HR đánh giá |
| HR mất kết nối > 5 phút (không reconnect) | Set CLOSING, ứng viên thấy thông báo "HR đã ngắt kết nối" |
| Ngày hôm sau room vẫn chưa ENDED | Cronjob 1h sáng → force EXPIRED + log cảnh báo |

---

## 12. Kanban & Trạng thái ứng viên sau phỏng vấn

### 12.1 Vấn đề cần sửa

> **Bug hiện tại:** Sau khi phỏng vấn xong hoặc lên lịch phỏng vấn, ứng viên bị hiển thị sai cột trên Kanban — SAI.

### 12.2 Logic đúng

```
Khi HR tạo lịch phỏng vấn: application.stage = "INTERVIEW_SCHEDULED"
    │
    ▼
Trong & sau phỏng vấn:
    │
    ├─ HR chưa đánh giá     → Giữ nguyên stage = "INTERVIEW_SCHEDULED"
    │                          Hiển thị badge "Chờ đánh giá" trên Kanban
    │
    ├─ HR đánh giá PASS     → stage = stage_tiếp_theo (vd: "Offer")
    │
    ├─ HR đánh giá FAIL     → stage = "REJECTED"
    │                          Color: đỏ / muted
    │
    └─ HR đánh giá HOLD     → Giữ nguyên stage = "INTERVIEW_SCHEDULED"
                               Hiển thị badge "Cần xem xét"
```

**Quy tắc:**
- Kết thúc phỏng vấn KHÔNG tự động thay đổi stage
- Chỉ `interview_evaluations.recommendation` mới trigger thay đổi stage
- Lên lịch phỏng vấn từ Kanban phải cập nhật application sang `INTERVIEW_SCHEDULED`, không đẩy trực tiếp sang `INTERVIEW`
- Kanban real-time cập nhật ngay khi HR lưu đánh giá (socket emit hoặc polling 30s)

### 12.3 Hiển thị trên Kanban card sau phỏng vấn

```
┌─────────────────────────────────┐
│  Nguyễn Văn A                   │
│  Frontend Developer             │
│  ⏱ Phỏng vấn: 01/12/2024       │
│  🎥 Có recording                │
│  ⚠ Chờ đánh giá                │  ← badge nổi bật nếu chưa đánh giá
└─────────────────────────────────┘
```

---

## 13. Socket Events Reference

> Ghi chú: bảng dưới đây mô tả event mục tiêu. Implementation RTC hiện tại đang dùng nhóm event thực tế:
> `join-room`, `join-request`, `admit-user`, `reject-user`, `kick-user`, `offer`, `answer`, `ice-candidate`,
> `recording-started`, `recording-stopped`.
> Payload `join-request` đã mở rộng thêm `displayName` để HR hiển thị đúng tên ứng viên.

### Client → Server

| Event | Payload | Ai gửi |
|---|---|---|
| `join_lobby` | `{ room_code, token }` | Ứng viên |
| `rejoin_room` | `{ session_token }` | Ứng viên (reload) |
| `knock` | `{ room_id, participant_id }` | Ứng viên |
| `hr_join` | `{ room_id, hr_token }` | HR |
| `admit_user` | `{ participant_id }` | HR |
| `reject_user` | `{ participant_id, reason? }` | HR |
| `remove_user` | `{ participant_id }` | HR |
| `open_room` | `{ room_id }` | HR |
| `end_room` | `{ room_id }` | HR |
| `start_recording` | `{ room_id }` | HR |
| `stop_recording` | `{ room_id }` | HR |
| `assign_recording` | `{ recording_id, participant_id }` | HR |
| `toggle_media` | `{ type: camera/mic, enabled }` | HR / Ứng viên |
| `toggle_other_media` | `{ user_id, type, enabled }` | HR only |
| `start_screenshare` | `{ room_id }` | HR / Ứng viên |
| `stop_screenshare` | `{ room_id }` | HR / Ứng viên |

### Server → Client

| Event | Payload | Gửi đến |
|---|---|---|
| `lobby_joined` | `{ status, slot_info, room_status }` | Ứng viên |
| `knock_received` | `{ participant_id, name, slot_time }` | HR |
| `admit_granted` | `{ webrtc_token, session_token }` | Ứng viên |
| `admit_rejected` | `{ reason }` | Ứng viên |
| `participant_joined` | `{ user_info }` | Tất cả trong phòng |
| `participant_left` | `{ user_id, reason }` | Tất cả trong phòng |
| `participant_removed` | `{ user_id, reason }` | Tất cả + user bị kick |
| `media_state_changed` | `{ user_id, type, enabled }` | Tất cả trong phòng |
| `screenshare_started` | `{ user_id }` | Tất cả |
| `screenshare_stopped` | `{ user_id }` | Tất cả |
| `recording_started` | `{ recording_id }` | Tất cả |
| `recording_stopped` | `{ recording_id }` | Tất cả |
| `room_opening` | `{ room_id }` | Ứng viên trong lobby |
| `room_ending` | `{ reason, grace_seconds }` | Tất cả |
| `room_ended` | `{ reason }` | Tất cả |
| `show_evaluation_form` | `{ participants[] }` | HR only |

---

## 14. Notification Flow

| Thời điểm | Gửi cho | Kênh |
|---|---|---|
| Tạo lịch phỏng vấn | HR + Ứng viên | Email (link + room_code) |
| 1 ngày trước | HR + Ứng viên | Email reminder |
| 2 giờ trước | Ứng viên | Email + Push notification |
| 30 phút trước | Ứng viên | Email + Push notification |
| Ứng viên knock | HR | Socket popup realtime |
| Ứng viên vào muộn (> slot_end 15 phút) | HR | In-app toast |
| Room còn 30 phút so với max_duration | HR | In-app banner |
| Room kết thúc | HR | Prompt đánh giá ngay trong app |
| Ứng viên chưa được đánh giá sau 24h | HR | Email nhắc |
| Recording xử lý xong | HR | In-app notification + email |

---

## 15. Security & Production Checklist

### Authentication & Authorization

- Mọi socket event phải kèm JWT — server verify signature + expiry + participant_id trước khi xử lý
- `host_id` kiểm tra trong DB trước khi xử lý `end_room`, `admit_user`, `remove_user`, `start_recording`
- `session_token` cho reconnect: expire sau 30 phút kể từ `left_at`, chỉ dùng 1 lần

### Rate Limiting

- Knock: tối đa 1 lần / 2 phút / participant
- Join_lobby: tối đa 10 lần / phút / IP
- End_room: chỉ host, không rate limit nhưng phải confirm

### Data Integrity

- `room_code` unique global — không tái sử dụng
- Admit_status = `REMOVED` không bao giờ được phép chuyển về `ADMITTED`
- Room đã `ENDED` không nhận bất kỳ socket event nào (reject hết)

### Audit Log

Ghi log toàn bộ:
- Ai vào phòng lúc nào
- Ai admit / reject ai, timestamp
- Ai bị kick, lý do, timestamp
- Khi nào end_room được gọi
- Recording bắt đầu / kết thúc / được gán cho ai
- Đánh giá được lưu bởi ai, lúc nào

### Reconnection & Graceful Shutdown

- Client tự reconnect socket trong 30 giây — không cần knock lại nếu đã ADMITTED
- Trước khi deploy, emit `room_closing` với countdown 60 giây cho các phòng đang ACTIVE
- Health check endpoint cho room service: `/health/rooms`

### Cronjobs

| Job | Schedule | Hành động |
|---|---|---|
| Close idle rooms | Mỗi 5 phút | Rooms ACTIVE không có participant > 10 phút → CLOSING |
| Force close overtime | Mỗi 5 phút | Rooms vượt max_duration → ENDED |
| Expire zombie rooms | 01:00 mỗi ngày | Rooms SCHEDULED/WAITING của hôm qua → EXPIRED |
| Remind evaluation | 09:00 mỗi ngày | HR chưa đánh giá ứng viên hôm qua → gửi email |

---

## 16. Các edge case & xử lý lỗi

### Media

| Case | Xử lý |
|---|---|
| Không có camera lẫn mic (Linux PC, server,...) | Cho vào phòng ở **chế độ xem**. HR vẫn điều hành admit/kick/record/screen share bình thường. Ứng viên vào lobby và chờ admit bình thường |
| Chỉ có mic mà không có camera (hoặc ngược lại) | Cho vào phòng với thiết bị hiện có, toast cảnh báo nhẹ. Nút thiết bị thiếu bị vô hiệu hóa |
| Cắm tai nghe/headset USB | Được nhận diện qua `enumerateDevices()` là `audioinput`, xử lý bình thường |
| Quyền camera/mic bị chặn (NotAllowedError) | Thông báo lỗi rõ ràng kèm hướng dẫn mở quyền trình duyệt. **Block join** — người dùng cần fix trước |
| Thiết bị đang bị app khác chiếm (NotReadableError) | Thông báo lỗi, hướng dẫn đóng ứng dụng chiếm thiết bị. **Block join** |
| Quyền bị chặn cho 1 thiết bị, thiết bị kia ok | Vào phòng với thiết bị hoạt động, toast warning ghi rõ thiết bị nào bị chặn |
| Kết nối kém, video giật | Tự giảm resolution (WebRTC adaptive bitrate) |
| Browser không hỗ trợ WebRTC | Hiển thị thông báo, suggest dùng Chrome/Edge |
| HR mất kết nối đang giữa record | Server vẫn record, HR reconnect → record tiếp tục |
| Screen share khi không có camera/mic | Cho phép screen share bình thường — tạo MediaStream mới chỉ chứa screen track |

### Room & Session

| Case | Xử lý |
|---|---|
| 2 ứng viên cùng slot giờ | Cả 2 được knock, HR chọn admit ai trước, người kia chờ |
| Ứng viên refresh liên tục (spam) | Rate limit join_lobby theo IP |
| HR vô tình đóng tab | Phòng vẫn ACTIVE 5 phút, HR có thể vào lại bình thường |
| Server restart trong phỏng vấn | Emit graceful shutdown trước, client reconnect tự động |
| Recording file lỗi / không xử lý được | Notify HR qua in-app + email, lưu error log |
| Ứng viên join từ 2 thiết bị | Chỉ cho session mới nhất, session cũ bị expire |
| Truy cập room code không tồn tại | UI hiển thị trang lỗi room-not-found chuyên dụng (có mã phòng + CTA quay lại lịch phỏng vấn) |
| API room tạm thời không phản hồi (5xx/network) | UI hiển thị trang "không thể kết nối" + nút thử lại, không render màn hình pre-join |

### Evaluation

| Case | Xử lý |
|---|---|
| HR quên đánh giá, đóng tab | Vẫn có thể vào lại trang chi tiết ứng viên để đánh giá sau |
| Đánh giá nhiều lần cho 1 ứng viên | Cho phép update (last_write wins), ghi log lịch sử |
| HR đánh giá PASS nhưng stage pipeline không có stage tiếp theo | Giữ stage hiện tại, flag để admin xử lý |

---

---

## 17. Interview Status Enum & Lifecycle per Candidate

> Lưu ý: Enum này áp dụng cho từng **interview record** của ứng viên (1 ứng viên = 1 interview record), khác với `room.status` quản lý phòng chung.

### 17.1 Enum hiện tại & đánh giá

```java
public enum InterviewStatus {
    SCHEDULED,            // HR tạo lịch, ứng viên chưa xác nhận
    CONFIRMED,            // Ứng viên đã xác nhận tham dự
    PENDING_RESCHEDULE,   // Đang có đề xuất đổi lịch (chưa rõ ai đề xuất)
    IN_PROGRESS,          // Đang phỏng vấn
    COMPLETED,            // Hoàn thành
    CANCELLED,            // Đã hủy
    NO_SHOW               // Ứng viên không xuất hiện
}
```

### 17.2 Vấn đề & đề xuất bổ sung

**Vấn đề 1:** `PENDING_RESCHEDULE` không phân biệt được **ai đề xuất** — ứng viên hay HR. Logic xử lý phía UI và notification khác nhau hoàn toàn.

**Vấn đề 2:** Thiếu trạng thái khi HR **từ chối** đề xuất đổi lịch → hệ thống không biết quay về `CONFIRMED` hay `SCHEDULED`.

**Đề xuất bổ sung:**

```java
public enum InterviewStatus {
    SCHEDULED,                    // HR tạo lịch, chờ ứng viên xác nhận
    CONFIRMED,                    // Ứng viên đã xác nhận tham dự
    PENDING_RESCHEDULE,           // Ứng viên đề xuất đổi lịch, chờ HR duyệt
    RESCHEDULE_REJECTED,          // HR từ chối đề xuất → ứng viên quay về lịch cũ
    IN_PROGRESS,                  // Đang phỏng vấn (room đã ACTIVE)
    COMPLETED,                    // Phỏng vấn hoàn thành + đã đánh giá
    CANCELLED,                    // Đã hủy (kèm cancel_reason)
    NO_SHOW                       // Ứng viên không xuất hiện sau grace 15 phút
}
```

**Bổ sung thêm vào DB record `room_participants` / `interviews`:**

```sql
cancel_reason   ENUM(RESCHEDULED, CANDIDATE_REQUEST, HR_CANCEL, SYSTEM)
reschedule_note TEXT    -- lý do ứng viên muốn đổi
proposed_time   TIMESTAMPTZ  -- thời gian ứng viên đề xuất
proposed_by     UUID    -- ai đề xuất (candidate_id hoặc hr_id)
```

### 17.3 Vòng đời trạng thái đầy đủ

```
                    ┌─────────────────────────────────────┐
                    │                                     │
HR tạo lịch → SCHEDULED → ứng viên xác nhận → CONFIRMED  │
                    │                │                    │
                    │         ứng viên đề xuất đổi        │
                    │                │                    │
                    │         PENDING_RESCHEDULE           │
                    │                │                    │
                    │    ┌───────────┴───────────┐        │
                    │    │                       │        │
                    │  HR từ chối           HR chấp nhận  │
                    │    │                       │        │
                    │  RESCHEDULE_REJECTED    CANCELLED   │
                    │    │  (cancel_reason=    (tạo       │
                    │    │   RESCHEDULED)    interview mới)│
                    │    │                       │        │
                    │  Quay về CONFIRMED     SCHEDULED ───┘
                    │
                    │
              (vào phòng) → IN_PROGRESS → COMPLETED
                                    └──→ NO_SHOW
                                    └──→ CANCELLED
```

---

## 18. Flow Đề Xuất Đổi Lịch (Reschedule)

### 18.1 Tổng quan flow

```
HR tạo lịch phỏng vấn → Ứng viên nhận thông báo
    │
    ▼
Ứng viên có 2 lựa chọn:
    ├─ Xác nhận tham dự  → status = CONFIRMED  (flow bình thường)
    └─ Đề xuất đổi lịch → status = PENDING_RESCHEDULE
```

### 18.2 Ứng viên đề xuất đổi lịch

**Điều kiện cho phép đề xuất:**
- Interview đang ở trạng thái `SCHEDULED` hoặc `CONFIRMED`
- Còn ít nhất **24 giờ** trước giờ phỏng vấn (không cho đổi phút chót)
- Ứng viên chưa dùng quá **2 lần đề xuất** cho cùng 1 vị trí (tránh lạm dụng)

**UI phía ứng viên:**

```
Thông báo lịch phỏng vấn:
  Vị trí: Frontend Developer
  Thời gian: 09:00 - 09:30, 15/12/2024
  Phòng: [Link tham gia]

  [✅ Xác nhận tham dự]   [🔄 Đề xuất đổi lịch]
```

**Khi ứng viên bấm "Đề xuất đổi lịch":**

```
Modal đề xuất:
  ─────────────────────────────────
  Thời gian bạn muốn phỏng vấn
  
  Ngày đề xuất: [Date picker]
  Giờ đề xuất:  [Time picker]
  
  Lý do (không bắt buộc):
  [textarea — vd: "Tôi có lịch họp trùng vào buổi sáng"]
  
  Lưu ý: HR sẽ xem xét và phản hồi trong vòng 24 giờ.
  ─────────────────────────────────
  [Hủy]                [Gửi đề xuất]
```

**Sau khi gửi:**
- Set `interview.status = PENDING_RESCHEDULE`
- Lưu `proposed_time`, `reschedule_note`, `proposed_by = candidate_id`
- Gửi notification cho HR (email + in-app)
- Ứng viên thấy badge "Đang chờ HR xác nhận đổi lịch"

### 18.3 HR xem và xử lý đề xuất

**Notification HR nhận:**

```
📅 Nguyễn Văn A đề xuất đổi lịch phỏng vấn
   Lịch cũ:    09:00, 15/12/2024
   Đề xuất:    14:00, 16/12/2024
   Lý do:      "Tôi có lịch họp trùng vào buổi sáng"

   [Xem chi tiết]
```

**HR có 3 lựa chọn:**

```
┌──────────────────────────────────────────────────────┐
│  Đề xuất đổi lịch từ Nguyễn Văn A                   │
│                                                      │
│  Lịch hiện tại: 09:00 – 09:30, 15/12/2024           │
│  Đề xuất mới:   14:00 – 14:30, 16/12/2024           │
│  Lý do: "Tôi có lịch họp trùng vào buổi sáng"       │
│                                                      │
│  [❌ Từ chối]  [✅ Chấp nhận]  [🔄 Đề xuất lại giờ khác] │
└──────────────────────────────────────────────────────┘
```

#### Option A — HR chấp nhận đề xuất

```
HR bấm "Chấp nhận"
    │
    ▼
Hệ thống thực hiện:
    1. Set interview cũ: status = CANCELLED, cancel_reason = RESCHEDULED
    2. Kiểm tra room mới:
       ├─ Đã có room cho (job_id + ngày đề xuất)?
       │       → Thêm participant vào room đó
       └─ Chưa có → Tạo room mới (status: SCHEDULED)
    3. Tạo interview record mới: status = CONFIRMED
       (đã confirmed vì HR chủ động chấp nhận)
    4. Sinh join_token mới cho ứng viên
    5. Gửi thông báo ứng viên: "HR đã chấp nhận, lịch mới: [thời gian]"
    6. Gửi email xác nhận lịch mới kèm link mới
```

#### Option B — HR từ chối đề xuất

```
HR bấm "Từ chối" (có thể kèm lý do)
    │
    ▼
Hệ thống thực hiện:
    1. Set interview: status = RESCHEDULE_REJECTED
    2. Sau 10 giây tự động → quay về CONFIRMED (lịch cũ giữ nguyên)
    3. Gửi thông báo ứng viên: "HR không thể đổi lịch. Lịch phỏng vấn vẫn giữ nguyên: [giờ cũ]"
    4. Ứng viên vẫn có thể đề xuất lại (nếu chưa đạt 2 lần)
```

#### Option C — HR đề xuất lại giờ khác

```
HR bấm "Đề xuất lại giờ khác"
    │
    ▼
Modal cho HR chọn giờ:
  "Giờ bạn đề xuất: [Date + Time picker]"
  [Gửi đề xuất lại]
    │
    ▼
Hệ thống:
    - Giữ nguyên status = PENDING_RESCHEDULE
    - Cập nhật proposed_time = giờ HR đề xuất, proposed_by = hr_id
    - Gửi notification ứng viên: "HR đề xuất giờ khác: [giờ mới]"
    │
    ▼
Ứng viên nhận notification, có thể:
    ├─ Chấp nhận → status = CONFIRMED (lịch = giờ HR đề xuất)
    └─ Từ chối   → Quay về lịch gốc (CONFIRMED) hoặc thương lượng tiếp
```

### 18.4 Timeout & Tự động xử lý

| Tình huống | Sau bao lâu | Hành động tự động |
|---|---|---|
| HR không phản hồi đề xuất | 24 giờ | Nhắc HR lần 2 qua email |
| HR không phản hồi đề xuất | 48 giờ | Auto từ chối, giữ lịch cũ, notify ứng viên |
| Ứng viên không confirm sau HR đề xuất lại | 12 giờ | Auto confirm lịch HR đề xuất |
| Lịch cũ đã qua trong lúc đang PENDING | Realtime | Force CANCELLED + tạo lịch mới bắt buộc |

### 18.5 Giới hạn đề xuất

- Tối đa **2 lần đề xuất đổi lịch** / interview (tránh ứng viên lạm dụng)
- Lần đề xuất thứ 3 → UI báo "Bạn đã đạt giới hạn đề xuất. Vui lòng liên hệ HR trực tiếp."
- Chỉ đề xuất được khi còn **≥ 24 giờ** trước giờ phỏng vấn

### 18.6 Kanban hiển thị trạng thái reschedule

```
┌─────────────────────────────────┐
│  Nguyễn Văn A                   │
│  Frontend Developer             │
│  📅 15/12/2024 09:00            │
│  🔄 Đang đề xuất đổi lịch      │  ← badge amber/warning
└─────────────────────────────────┘
```

- `PENDING_RESCHEDULE` → badge màu vàng/amber "Chờ xác nhận đổi lịch"
- `RESCHEDULE_REJECTED` → badge tạm thời (5 giây) rồi về trạng thái bình thường
- `CONFIRMED` (lịch mới) → badge xanh "Đã xác nhận lịch mới"

---

*Tài liệu này mô tả toàn bộ logic nghiệp vụ của module Interview. Mọi thay đổi cần cập nhật tài liệu này đồng thời.*
