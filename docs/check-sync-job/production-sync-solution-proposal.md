# Đề xuất production cho đồng bộ Job Elasticsearch

## Mục tiêu thiết kế

Chuẩn production cho module này nên đảm bảo đồng thời 4 điều:

1. `jobs_es` luôn phản ánh đúng tập job public tại thời điểm hiện tại.
2. runtime mutation là đường đồng bộ chính, còn startup/cron là đường repair an toàn.
3. reindex không được tạo side effect nghiệp vụ.
4. mọi read path public gồm search, personalized, similar, popular, job detail dùng cùng một chuẩn public-eligibility.

## Public contract cần chốt

Một job được coi là public khi và chỉ khi thỏa toàn bộ:

- job tồn tại
- `job.status = ACTIVE`
- `expiryDate` chưa qua
- company tồn tại
- `company.verificationStatus = APPROVED`
- `company.operationalStatus = ACTIVE`

Khuyến nghị đặt tên thống nhất:

- `isJobPubliclyAvailable(job)` là rule duy nhất

Rule này phải là nguồn sự thật cho:

- runtime sync vào ES
- startup/cron repair
- candidate search
- personalized recommendation
- daily digest
- job detail public
- apply job

## Quyết định kiến trúc index

### Khuyến nghị giai đoạn hiện tại

Giữ một index `jobs_es`, nhưng coi nó là **public index sạch**.

Nghĩa là:

- job public hợp lệ thì document tồn tại
- job không còn public thì document bị xóa khỏi index

Không nên tiếp tục dùng tư duy:

- index nhiều dữ liệu không public
- rồi chặn bằng `jobSearchable`

`jobSearchable` nếu giữ lại thì chỉ là guard phụ, không phải cơ chế chính.

### Khi nào mới cần 2 index

Chỉ tách:

- `jobs_public_es`
- `jobs_internal_es`

khi thực sự có nhu cầu semantic search cho draft/inactive/internal moderation.

Với codebase hiện tại, tách 2 index ngay là quá sớm.

## Những thay đổi bắt buộc để đạt chuẩn production

### 1. Runtime sync phải xóa document khi job không còn public

Hiện tại runtime sync chỉ xóa khi `job.status != ACTIVE`.

Nên đổi thành:

- nếu `!isJobPubliclyAvailable(job)` thì `deleteById(jobId)`
- nếu public thì upsert document

Hệ quả tốt:

- company bị reject/needs-info/block
- job expired
- company mất approved

đều bị loại thẳng khỏi public index.

### 2. Startup/cron repair phải dùng cùng rule với runtime

`ElasticsearchDataInitializer.shouldIndexJob(...)` phải đổi sang logic tương đương:

- `return jobService.isJobPubliclyAvailable(job)` hoặc helper thuần tương đương

`toJobDocument(...).jobSearchable` nếu còn giữ thì phải bám cùng rule đó.

Không được để startup/cron có tiêu chí rộng hơn runtime.

### 3. Content hash phải bao gồm toàn bộ field ảnh hưởng search/relevance/public visibility

`contentHash` hiện tại chưa đủ.

Khuyến nghị hash tối thiểu các field:

- `title`
- `description`
- `jobCategory`
- `employmentType`
- `experienceLevel`
- `education`
- `state`
- `city`
- `qualifications`
- `minimumQualifications`
- `responsibilities`
- `status`
- `expiryDate`
- `company.id`
- `company.verificationStatus`
- `company.operationalStatus`

Nếu muốn rõ ràng hơn, tạo một `IndexProjection` canonical string rồi hash.

### 4. Reindex/repair không được publish `JobCreatedEvent`

`JobCreatedEvent` chỉ được bắn từ luồng nghiệp vụ tạo job thật sự.

Tuyệt đối không bắn từ:

- startup sync
- cron sync
- manual repair
- backfill

Nếu vẫn cần rebuild `NewlyPostedJob`, phải có job riêng với rule nghiệp vụ rõ ràng, không piggyback trong reindex.

### 5. Tách xử lý expired jobs thành processor riêng

Hiện thiếu một processor theo thời gian cho job hết hạn.

Khuyến nghị thêm job nền riêng:

- chạy mỗi 5-15 phút
- tìm job vừa hết hạn
- xóa khỏi public ES index
- tùy roadmap, có thể update DB `ACTIVE -> EXPIRED`

### 6. Job detail public phải dùng đúng cùng rule

Khuyến nghị:

- validate access trước khi tăng views
- public candidate/anonymous chỉ xem được khi `isJobPubliclyAvailable(job) == true`

Admin và owner company có thể xem theo policy riêng.

### 7. Search response phải có total nhất quán

Với search candidate qua ES:

- nên đảm bảo ES chỉ trả ra public docs sạch
- khi đó không cần DB filter rơi rớt nhiều

Nếu vẫn phải post-filter ở DB:

- total phải phản ánh semantics rõ ràng
- tốt nhất là repair index để không cần hậu lọc nhiều

## Đề xuất state machine cho verification và moderation

### Verification

Trạng thái verification nên là:

- `NOT_SUBMITTED`
- `PENDING_REVIEW`
- `APPROVED`
- `REJECTED`
- `NEEDS_ADDITIONAL_INFO`

Rule production khuyến nghị:

- `PENDING_REVIEW -> APPROVED | REJECTED | NEEDS_ADDITIONAL_INFO`
- `REJECTED -> PENDING_REVIEW` qua resubmission mới
- `NEEDS_ADDITIONAL_INFO -> PENDING_REVIEW` qua resubmission mới
- `APPROVED` là trạng thái kết thúc của một verification cycle

Không nên:

- đổi trực tiếp `APPROVED -> REJECTED`
- đổi trực tiếp `APPROVED -> NEEDS_ADDITIONAL_INFO`

Nếu company đã được approve mà sau đó có vấn đề, xử lý bằng:

- `BLOCKED` hoặc `SUSPENDED` ở operational status
- hoặc tạo một luồng `re-verification` mới, không mutate ngược request cũ

### Moderation

Moderation nên là state machine riêng:

- `ACTIVE`
- `BLOCKED`
- `SUSPENDED`

Điều này giúp tách:

- “có đủ giấy tờ hợp lệ hay chưa”
- khỏi “đang bị chế tài vận hành hay không”

## Lộ trình triển khai khuyến nghị

### Phase 1: sửa contract để hệ thống an toàn

Mục tiêu:

- public index sạch
- không side effect khi repair
- job detail public đúng rule

Việc cần làm:

1. sửa runtime sync thành delete document khi không public
2. sửa startup/cron dùng cùng rule public
3. bỏ publish `JobCreatedEvent` khỏi `ElasticsearchDataInitializer`
4. sửa job detail validate trước, tăng views sau
5. chặn `APPROVED -> REJECTED/NEEDS_ADDITIONAL_INFO`

### Phase 2: thêm expired job processor

Mục tiêu:

- giảm phụ thuộc vào query-time expiry filtering
- làm sạch index theo thời gian

Việc cần làm:

1. thêm scheduler riêng cho expired jobs
2. chọn tần suất 5-15 phút
3. tùy chọn update DB status sang `EXPIRED`

### Phase 3: tăng observability và repairability

Mục tiêu:

- production support dễ hơn

Việc cần làm:

1. metric số doc public expected vs actual
2. metric số doc bị delete vì mất public eligibility
3. metric số repair run indexed/unchanged/deleted
4. endpoint nội bộ/manual trigger cho:
   - repair jobs index
   - repair một company
   - repair một job

## Checklist production acceptance

Chỉ nên coi module này đạt chuẩn production khi pass hết checklist:

- startup sync và cron sync dùng cùng public rule với runtime
- reindex không publish domain event nghiệp vụ
- expired jobs không còn xuất hiện ở public search sau tối đa 15 phút
- candidate không xem được public detail của expired job
- candidate không apply được expired/inactive/non-approved job
- company block/unblock và approve/reject có tác động đúng lên ES ngay sau transaction
- approved verification không bị mutate ngược thành rejected/needs-info
- repair job có log và metric rõ ràng

## Config khuyến nghị

Các config hiện có có thể giữ, nhưng nên bổ sung nhóm riêng cho expired processor:

```env
APP_ES_SYNC_JOBS_ENABLED=true
APP_ES_CRON_ENABLED=true
APP_ES_CRON_JOBS_ENABLED=true
APP_ES_CRON_JOBS_FIXED_DELAY_MS=300000
APP_ES_CRON_JOBS_INITIAL_DELAY_MS=120000
APP_ES_CRON_JOBS_BATCH_SIZE=100

APP_JOB_EXPIRATION_ENABLED=true
APP_JOB_EXPIRATION_FIXED_DELAY_MS=900000
APP_JOB_EXPIRATION_INITIAL_DELAY_MS=180000
APP_JOB_EXPIRATION_BATCH_SIZE=200
APP_JOB_EXPIRATION_UPDATE_STATUS=false
```

## Kết luận

Hướng đúng cho hệ thống hiện tại không phải là thêm nhiều filter ở query layer, mà là chốt lại một public contract duy nhất rồi bắt:

- runtime sync
- startup repair
- cron repair
- job detail
- verification workflow

cùng tuân theo contract đó.

Khi làm được điều này, `jobs_es` mới thực sự trở thành public search index đáng tin cậy cho production.
