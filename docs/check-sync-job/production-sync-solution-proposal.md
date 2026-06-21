# Đề xuất giải pháp production cho đồng bộ Job Elasticsearch

## Mục tiêu

Tài liệu này đề xuất hướng xử lý theo chuẩn production cho bài toán:

- Job nào nên có mặt trong Elasticsearch
- Khi nào cần đồng bộ lại index
- Cách xử lý job hết hạn, công ty bị block/unblock, approve/reject verification
- Cơ chế chạy định kỳ và chạy thủ công

Tài liệu ưu tiên **giải pháp nghiệp vụ trước**, sau đó mới đến **hướng code đề xuất**.

## 1. Nguyên tắc nghiệp vụ nên chốt trước

## 1.1. Xác định rõ public search và internal search là hai bài toán khác nhau

Theo chuẩn production, nên phân biệt:

- `Public search`
  - candidate nhìn thấy
  - recommendation/daily digest dùng
  - chỉ chứa job thực sự công khai

- `Internal search`
  - HR/company/admin nhìn thấy
  - có thể bao gồm draft, inactive, blocked company jobs, expired jobs để quản trị

Kết luận nghiệp vụ:

- **Index public không nên chứa job không còn hợp lệ để public**
- các nhu cầu nội bộ không nên cố tận dụng cùng một public index nếu không thật sự cần

## 1.2. Điều kiện một job được coi là public

Một job nên được coi là đủ điều kiện public khi đồng thời thỏa:

- job tồn tại
- `job.status = ACTIVE`
- job chưa hết hạn ứng tuyển
- company tồn tại
- `company.verificationStatus = APPROVED`
- `company.operationalStatus = ACTIVE`

Đây nên là **nguồn sự thật duy nhất** cho:

- search public
- job detail public
- recommendation
- daily digest
- Elasticsearch public indexing

## 1.3. Chuẩn production nên ưu tiên “không index dữ liệu không public”

Về nghiệp vụ, hướng production tốt nhất là:

- **job không còn public thì không nằm trong public index**

thay vì:

- cứ index tất cả rồi dùng `jobSearchable` để chặn lúc query

Lý do:

- index sạch hơn
- giảm rủi ro query quên filter
- recommendation và digest ít bị lệch
- giảm số document vô ích
- giảm chi phí reindex và storage

## 1.4. `jobSearchable` có nên giữ không

Nên xem `jobSearchable` là:

- **guard rail phụ**
- không phải cơ chế chính

Khuyến nghị:

- vẫn có thể giữ `jobSearchable`
- nhưng mục tiêu chính phải là:
  - document không public thì bị xóa khỏi public index

Tức là:

- production chuẩn: `jobSearchable` là lớp an toàn dự phòng
- không nên phụ thuộc hoàn toàn vào `jobSearchable`

## 2. Đề xuất kiến trúc production

## 2.1. Hướng khuyến nghị mạnh nhất

### Phương án A: Một public index sạch

Chỉ dùng `jobs_es` cho public jobs.

Nguyên tắc:

- job public hợp lệ thì có document
- job không còn public thì xóa document

Ưu điểm:

- đơn giản
- đúng business
- query candidate rất sạch
- recommendation và digest dùng lại được

Nhược điểm:

- nếu sau này HR cần search semantic trên cả draft/inactive thì phải làm riêng

### Phương án B: Tách 2 index

- `jobs_public_es`
- `jobs_internal_es`

Nguyên tắc:

- `jobs_public_es` chỉ chứa job public
- `jobs_internal_es` chứa tất cả job cần phục vụ vận hành nội bộ

Ưu điểm:

- chuẩn enterprise hơn
- mỗi index phục vụ một nhóm use case rõ ràng

Nhược điểm:

- code và vận hành phức tạp hơn

## 2.2. Đề xuất phù hợp với hệ thống hiện tại

Với codebase hiện tại, hướng thực tế và hợp lý nhất là:

- **giai đoạn 1**: giữ một index `jobs_es`, nhưng biến nó thành **public index sạch**
- **giai đoạn 2**: nếu cần internal semantic search mạnh hơn thì mới tách `jobs_internal_es`

Kết luận:

- trước mắt nên đi theo **Phương án A**

## 3. Các sự kiện nghiệp vụ cần làm thay đổi public index

## 3.1. Sự kiện từ phía company/job

Cần re-evaluate public eligibility và sync lại khi:

- tạo job
- publish job
- activate job
- deactivate job
- close/delete job
- cập nhật expiry date
- cập nhật nội dung search quan trọng:
  - title
  - description
  - qualifications
  - minimumQualifications
  - responsibilities
  - location
  - employment type
  - experience level
  - education
  - category

## 3.2. Sự kiện từ phía company verification / admin moderation

Cần re-evaluate toàn bộ job của company khi:

- approve company
- reject verification
- request additional info
- block company
- unblock company

## 3.3. Sự kiện thời gian

Cần xử lý theo thời gian khi:

- job vừa bước sang trạng thái hết hạn
- job đã hết hạn lâu và cần dọn dữ liệu liên quan

## 4. Đề xuất nghiệp vụ cho “job hết hạn”

## 4.1. Khi job hết hạn thì public behavior phải ra sao

Ngay khi hết hạn:

- candidate không thấy trong public search
- recommendation không dùng nữa
- daily digest không gửi nữa
- job detail public nên bị chặn hoặc trả trạng thái “đã hết hạn”

## 4.2. Có nên cập nhật DB status từ `ACTIVE` sang `EXPIRED`

Theo chuẩn production, đây là hướng tốt.

Khuyến nghị:

- thêm trạng thái nghiệp vụ `EXPIRED`
- khi quá hạn, scheduler cập nhật từ `ACTIVE -> EXPIRED`

Ưu điểm:

- rõ ràng về business state
- tránh phải mỗi nơi tự check `expiryDate`
- thống kê, dashboard, admin filter chính xác hơn

Nếu chưa muốn thay enum/status ngay:

- vẫn có thể để DB giữ `ACTIVE`
- nhưng public index bắt buộc phải xóa document expired
- và API detail/apply/search phải check hết hạn nhất quán

## 4.3. Scheduler xử lý expired job nên chạy thế nào

Nên có **một scheduler riêng** cho expired jobs, không trộn vào scheduler sync embedding chung.

Nhiệm vụ scheduler này:

- tìm job vừa hết hạn
- cập nhật status nghiệp vụ nếu hệ thống dùng `EXPIRED`
- xóa khỏi public ES index
- ghi log số lượng đã xử lý

## 4.4. Tần suất chạy khuyến nghị

Không nên để 1 tuần mới chạy một lần.

Về business, job hết hạn là dữ liệu nhạy thời gian. Nếu để 1 tuần:

- job quá hạn vẫn còn hiện trên search nhiều ngày
- UX và trust bị ảnh hưởng

Khuyến nghị:

- production tối thiểu: chạy **mỗi 1 giờ**
- tốt hơn: chạy **mỗi 15 phút**
- nếu tải thấp và cần chính xác cao: chạy **mỗi 5 phút**

Nếu muốn dọn dữ liệu cũ sâu hơn, có thể tách riêng:

- `expire public availability`: chạy 5-15 phút/lần
- `cleanup old expired jobs artifacts`: chạy 1 ngày/lần hoặc 1 tuần/lần

## 5. Cơ chế vận hành nên có trong production

## 5.1. Tách 3 loại job nền

Nên chia rõ:

1. `Incremental sync job`
- đồng bộ incremental cho document thay đổi

2. `Expired job processor`
- xử lý job quá hạn

3. `Repair / backfill job`
- chạy thủ công hoặc theo lịch thưa để sửa lệch dữ liệu

## 5.2. Cần có config qua biến môi trường

Theo chuẩn production, nên đưa hành vi scheduler ra env vars thay vì hardcode.

### Nhóm config đề xuất cho sync chính

```env
APP_ES_SYNC_JOBS_ENABLED=false
APP_ES_CRON_ENABLED=true
APP_ES_CRON_JOBS_ENABLED=true
APP_ES_CRON_JOBS_FIXED_DELAY_MS=300000
APP_ES_CRON_JOBS_INITIAL_DELAY_MS=120000
APP_ES_CRON_JOBS_BATCH_SIZE=50
```

Ý nghĩa:

- sync incremental job search index
- chạy 5 phút/lần

### Nhóm config đề xuất cho expired job processor

```env
APP_JOB_EXPIRATION_ENABLED=true
APP_JOB_EXPIRATION_FIXED_DELAY_MS=900000
APP_JOB_EXPIRATION_INITIAL_DELAY_MS=180000
APP_JOB_EXPIRATION_BATCH_SIZE=200
APP_JOB_EXPIRATION_UPDATE_STATUS=true
APP_JOB_EXPIRATION_DELETE_FROM_ES=true
```

Ý nghĩa:

- xử lý expired jobs 15 phút/lần
- có thể bật/tắt việc update DB status
- có thể bật/tắt việc xóa ES document

### Nhóm config đề xuất cho repair/reconciliation

```env
APP_ES_REPAIR_ENABLED=false
APP_ES_REPAIR_FIXED_DELAY_MS=86400000
APP_ES_REPAIR_INITIAL_DELAY_MS=3600000
APP_ES_REPAIR_BATCH_SIZE=500
APP_ES_REPAIR_FORCE=false
```

Ý nghĩa:

- job repair chạy thưa, ví dụ mỗi ngày
- dùng để sửa lệch mà incremental miss

## 5.3. Nên có endpoint nội bộ hoặc admin để chạy thủ công

Theo chuẩn production, nên có khả năng trigger bằng tay cho vận hành.

Khuyến nghị:

- giữ endpoint nội bộ hiện có cho sync ES
- bổ sung endpoint cho expired job cleanup / repair

### Endpoint nên có

1. Sync incremental jobs

```http
POST /internal/elasticsearch/sync?target=jobs
```

2. Force rebuild public jobs index

```http
POST /internal/elasticsearch/sync?target=jobs&force=true
```

3. Chạy xử lý expired jobs

```http
POST /internal/jobs/expiration/run
```

4. Chạy repair đồng bộ public eligibility

```http
POST /internal/jobs/public-index-repair/run
```

### Có nên cho admin UI bấm chạy không

Có thể có, nhưng nên qua backend admin secured:

- chỉ `ADMIN`
- có audit log
- có rate limit hoặc cooldown

Ví dụ:

```http
POST /admin/operations/jobs/expiration/run
POST /admin/operations/elasticsearch/jobs/repair
```

## 6. Hướng code đề xuất

## 6.1. Gom logic eligibility về một chỗ

Hiện tại logic public eligibility đang nằm rải rác.

Theo production chuẩn, nên có một hàm nguồn sự thật duy nhất, ví dụ:

- `isJobPubliclyAvailable(job)`

và mọi nơi đều dùng chung:

- runtime sync
- cron sync
- recommendation
- detail access
- apply validation

Khuyến nghị:

- không để `ElasticsearchDataInitializer` tự định nghĩa logic khác
- không để `JobController.validateJobAccess(...)` tự check khác

## 6.2. `ElasticsearchDataInitializer` nên đổi hướng

Thay vì:

- lấy mọi `ACTIVE` job vào tập sync
- set `jobSearchable`

nên làm:

- chỉ lấy những job đủ public eligibility vào tập index
- job không đủ public eligibility thì xóa khỏi public index

Nói ngắn gọn:

- `shouldIndexJob(job)` nên tương đương public eligibility

Khuyến nghị:

- đổi `shouldIndexJob(...)` để check:
  - status
  - expiry
  - company verification
  - company operational status

## 6.3. `jobSearchable` nên để thế nào

Khuyến nghị:

- vẫn giữ field `jobSearchable`
- nhưng luôn set `true` cho document public index

Nếu một document đã không public:

- document nên bị xóa khỏi public index
- không chỉ set `jobSearchable=false`

## 6.4. `contentHash` phải mở rộng

Hiện tại hash quá hẹp.

Theo production chuẩn, hash nên phản ánh toàn bộ dữ liệu có thể ảnh hưởng đến:

- khả năng search
- eligibility public
- ranking relevance

Khuyến nghị hash bao gồm tối thiểu:

- job.id
- title
- description
- qualifications
- minimumQualifications
- responsibilities
- state
- city
- employmentType
- experienceLevel
- education
- jobCategory
- expiryDate
- status
- company.id
- company.verificationStatus
- company.operationalStatus

Nếu có compensation / salary được dùng trong filter hoặc ranking thì cũng nên đưa vào hash.

## 6.5. Không nên phát `JobCreatedEvent` trong reindex

Theo chuẩn production:

- `JobCreatedEvent` là business event
- reindex là technical event

Hai loại này không nên trộn.

Khuyến nghị:

- chỉ phát `JobCreatedEvent` ở luồng business tạo job thật
- không phát lại trong `ElasticsearchDataInitializer.toJobDocument(...)`

## 6.6. Detail job cũng phải dùng cùng luật public

Hiện tại detail access có nguy cơ lệch với search.

Khuyến nghị:

- `GET /jobs/{id}` khi truy cập public phải dùng cùng `isJobPubliclyAvailable(job)`
- trong đó phải bao gồm check `expiryDate`

Nếu không:

- search không thấy nhưng detail vẫn xem được
- tạo inconsistency trong behavior

## 6.7. Recommendation và digest nên có lớp xác nhận cuối

Theo production chuẩn, với các luồng nhạy như digest:

- ES search là bước shortlist
- trước khi enqueue hoặc gửi thật, nên xác nhận lại DB eligibility

Khuyến nghị:

- sau khi lấy hit từ ES, map ngược DB rồi filter lại `isJobPubliclyAvailable(job)`

Điều này giúp:

- giảm rủi ro nếu index tạm thời stale

## 7. Đề xuất tiến trình triển khai

## Giai đoạn 1: Chuẩn hóa logic

- chốt định nghĩa `public job eligibility`
- sửa detail access dùng cùng logic
- sửa runtime sync và cron sync dùng cùng logic

## Giai đoạn 2: Cải thiện incremental sync

- mở rộng `contentHash`
- bỏ phát `JobCreatedEvent` trong reindex
- giữ `jobSearchable` như lớp phụ

## Giai đoạn 3: Bổ sung vận hành production

- thêm expired job processor
- thêm env configs
- thêm endpoint internal/admin để chạy thủ công
- thêm logging/audit

## Giai đoạn 4: Repair và quan sát

- thêm repair endpoint
- thêm metrics:
  - số job indexed
  - số job deleted
  - số job expired processed
  - số job skipped
  - số job repaired

## 8. Kết luận đề xuất cuối cùng

Theo hướng chuẩn production, đề xuất cuối cùng là:

1. `jobs_es` nên được xem là **public index sạch**
2. job không còn public thì **xóa khỏi index**, không chỉ set `jobSearchable=false`
3. vẫn có thể giữ `jobSearchable` như guard rail phụ
4. thêm một scheduler riêng xử lý job hết hạn
5. scheduler xử lý expired nên chạy **5-15 phút/lần**, không phải 1 tuần/lần
6. các hành vi scheduler nên cấu hình qua **biến môi trường**
7. nên có **endpoint internal/admin** để chạy thủ công khi cần vận hành hoặc repair

Nếu đi theo hướng này, hệ thống sẽ:

- đúng business hơn
- ít lệch dữ liệu hơn
- dễ vận hành production hơn
- an toàn hơn cho search, recommendation và daily digest

