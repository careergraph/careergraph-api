# Báo cáo kiểm tra đồng bộ Job Elasticsearch

## Phạm vi kiểm tra

Đã kiểm tra các luồng liên quan đến việc job có xuất hiện trong search và truy cập detail hay không, tập trung vào:

- Cron/scheduler đồng bộ Elasticsearch
- Đồng bộ khi admin `approve`, `reject`, `request additional info`, `block`, `unblock` công ty
- Đồng bộ khi công ty `create`, `publish`, `activate`, `deactivate`, `delete`, `update expiry date`
- Ảnh hưởng đến:
  - `http://localhost:3000/company/verification`
  - `http://localhost:3000/jobs`
  - `http://localhost:4200/verification/{companyId}`

## Kết luận ngắn

Luồng **runtime sync** hiện tại nhìn chung đã có cho hầu hết các case quan trọng:

- `approve` công ty: **có sync lại jobs**
- `reject` hoặc `request additional info`: **có sync lại jobs**
- `block` và `unblock` công ty: **có sync lại jobs**
- `đóng job`, `activate/deactivate`, `publish`, `update expiry date`: **có sync lại job**
- `draft`: **không vào index**

Tuy nhiên, có 3 vấn đề quan trọng:

1. **Cron đồng bộ Elasticsearch không đủ chính xác để tự sửa lệch dữ liệu** trong một số case.
2. **Cron đang dùng logic `jobSearchable` khác với runtime sync**, nên có thể giữ lại job hết hạn hoặc không còn public đúng nghĩa.
3. **API xem chi tiết job** vẫn cho xem public với job đã hết hạn nếu job vẫn `ACTIVE` và công ty vẫn `APPROVED + ACTIVE`.

## Luồng runtime sync hiện tại

### 1. Khi công ty được approve, reject, yêu cầu bổ sung, block, unblock

File: `AdminCompanyVerificationServiceImpl`

- `approveRequest(...)` đi qua `markVerificationDecision(...)`
- `rejectRequest(...)` đi qua `markVerificationDecision(...)`
- `requestAdditionalInfo(...)` đi qua `markVerificationDecision(...)`
- `blockCompany(...)` gọi `jobService.syncCompanyJobsSearchDocuments(companyId)`
- `unblockCompany(...)` gọi `jobService.syncCompanyJobsSearchDocuments(companyId)`

Điểm chứng:

- `approve/reject/needs info` đều sync ở [AdminCompanyVerificationServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImpl.java:212)
- `block` sync ở [AdminCompanyVerificationServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImpl.java:160)
- `unblock` sync ở [AdminCompanyVerificationServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImpl.java:182)

### 2. Khi sync theo company

`syncCompanyJobsSearchDocuments(companyId)` lấy toàn bộ job của công ty và gọi sync từng job:

- [JobServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java:808)

### 3. Logic sync từng job

`syncJobSearchDocument(job)` hoạt động như sau:

- nếu `job.status != ACTIVE` thì xóa khỏi index
- nếu `job.status == ACTIVE` thì upsert document vào ES
- `jobSearchable` được set theo `isJobPubliclyAvailable(job)`

Điểm chứng:

- [JobServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java:1496)
- [JobServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java:818)

### 4. Logic job public thực tế của runtime

`isJobPubliclyAvailable(job)` chỉ trả `true` khi:

- job `ACTIVE`
- chưa quá hạn ứng tuyển
- công ty `APPROVED`
- công ty `ACTIVE`

Điểm chứng:

- [JobServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java:818)
- [CompanyAccessPolicyServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/CompanyAccessPolicyServiceImpl.java:57)

## Đánh giá theo từng case nghiệp vụ

### 1. Company được approve

Kết quả: **Đúng, có đồng bộ lại jobs để có thể search**

Giải thích:

- `approveRequest(...)` gọi `markVerificationDecision(...)`
- `markVerificationDecision(...)` set `company.verificationStatus = APPROVED`
- sau đó gọi `jobService.syncCompanyJobsSearchDocuments(company.getId())`
- `syncJobSearchDocument(...)` sẽ set `jobSearchable = true` cho các job:
  - `ACTIVE`
  - chưa hết hạn
  - công ty `APPROVED`
  - công ty `ACTIVE`

Kết luận:

- Nếu trước đó công ty đã có job `ACTIVE` nhưng chưa được duyệt, sau khi approve thì jobs đó **sẽ được sync lại và có thể search được**.

### 2. Company bị reject hoặc yêu cầu bổ sung thông tin

Kết quả: **Có sync lại jobs**

Giải thích:

- `reject` và `requestAdditionalInfo` cũng đi qua `markVerificationDecision(...)`
- cuối method vẫn gọi `jobService.syncCompanyJobsSearchDocuments(...)`
- khi đó `company.verificationStatus` không còn `APPROVED`
- `jobSearchable` sẽ thành `false`

Kết luận:

- Khác với giả định ban đầu, hiện tại hệ thống **có đồng bộ lại** khi `reject` hoặc `needs additional info`
- Đây là **đúng về mặt business**, vì jobs của công ty đó không nên còn xuất hiện trong search public

### 3. Company bị block

Kết quả: **Đúng, có sync lại jobs**

Giải thích:

- `blockCompany(...)` set `operationalStatus = BLOCKED`
- sau đó gọi `jobService.syncCompanyJobsSearchDocuments(companyId)`
- runtime sync sẽ giữ document nếu job còn `ACTIVE`, nhưng `jobSearchable = false`

Kết luận:

- Jobs của công ty bị block sẽ **biến mất khỏi search public**
- Nếu search phía company nội bộ, vẫn có thể thấy theo luồng riêng của company

### 4. Company được unblock

Kết quả: **Đúng, có sync lại jobs**

Giải thích:

- `unblockCompany(...)` set `operationalStatus = ACTIVE`
- gọi sync lại toàn bộ jobs công ty
- job chỉ quay lại search nếu đồng thời:
  - `verificationStatus = APPROVED`
  - job `ACTIVE`
  - chưa hết hạn

Kết luận:

- `unblock` **không tự làm job quay lại search** nếu công ty vẫn chưa `APPROVED`
- Chỉ các job hợp lệ mới quay lại public search

### 5. Company tắt job, đóng job, hoặc chuyển job khỏi public

Kết quả: **Đúng, có sync**

Điểm chứng:

- `publishJob(...)` sync ở [JobServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java:253)
- `deleteJob(...)` sync ở [JobServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java:279)
- `updateJobSettings(...)` sync ở [JobServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java:300)
- `activateJob(...)` sync ở [JobServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java:330)
- `deactivateJob(...)` sync ở [JobServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java:340)

Kết luận:

- `deactivate`, `closed`, `delete` sẽ làm job bị xóa khỏi index nếu status không còn `ACTIVE`
- `publish` hoặc `activate` sẽ index lại nếu job đủ điều kiện public

### 6. Company gia hạn thêm ngày hết hạn ứng tuyển

Kết quả: **Đúng, có sync**

Giải thích:

- `updateJobSettings(...)` có cập nhật `expiryDate` và gọi `syncJobSearchDocument(...)`
- `updateJob(...)` bản full update cũng gọi sync

Kết luận:

- Khi gia hạn thêm ngày ứng tuyển, job sẽ được sync lại và có thể quay lại search nếu trước đó bị ẩn vì hết hạn

### 7. Job bản nháp

Kết quả: **Đúng, không vào index**

Giải thích:

- `syncJobSearchDocument(...)` xóa khỏi ES nếu `job.status != ACTIVE`
- `DRAFT` không phải `ACTIVE`

Kết luận:

- Job nháp không nên search được, và hiện tại logic runtime đang đúng

## Kiểm tra cron/scheduler đồng bộ Elasticsearch

## Cron nào đang sync job ES thật sự

`DailyDigestScheduler` không phải cron sync index job.

- `DailyDigestScheduler` chỉ dùng dữ liệu ES sẵn có để build queue email
- Hai method trong class này hiện đang comment `@Scheduled`

Cron sync ES thực sự nằm ở:

- [ElasticsearchSyncScheduler.java](../../../src/main/java/com/hcmute/careergraph/config/app/ElasticsearchSyncScheduler.java:25)
- [ElasticsearchDataInitializer.java](../../../src/main/java/com/hcmute/careergraph/config/app/ElasticsearchDataInitializer.java:93)

## Cron sync hiện tại hoạt động thế nào

Scheduler gọi:

- `jobSyncInitializer.syncNow(null, batchSize)`

Trong `syncNow(...)`:

- lấy `jobRepository.findAll()`
- chỉ giữ jobs mà `shouldIndexJob(job)` trả `true`
- xóa stale docs không còn thuộc tập `activeJobs`
- chỉ re-embed/re-save nếu `contentHash` thay đổi hoặc `force=true`

Điểm chứng:

- [ElasticsearchSyncScheduler.java](../../../src/main/java/com/hcmute/careergraph/config/app/ElasticsearchSyncScheduler.java:25)
- [ElasticsearchDataInitializer.java](../../../src/main/java/com/hcmute/careergraph/config/app/ElasticsearchDataInitializer.java:125)
- [ElasticsearchDataInitializer.java](../../../src/main/java/com/hcmute/careergraph/config/app/ElasticsearchDataInitializer.java:162)

## Các vấn đề của cron sync hiện tại

### Vấn đề 1. `shouldIndexJob(...)` chưa phản ánh đúng điều kiện public

`shouldIndexJob(job)` chỉ check:

- job khác `null`
- `job.status == ACTIVE`
- có `company`

Điểm chứng:

- [ElasticsearchDataInitializer.java](../../../src/main/java/com/hcmute/careergraph/config/app/ElasticsearchDataInitializer.java:388)

Hệ quả:

- job của công ty `REJECTED`, `NEEDS_ADDITIONAL_INFO`, `BLOCKED`, `UNBLOCKED nhưng chưa APPROVED`, hoặc job đã hết hạn vẫn có thể tiếp tục nằm trong tập `activeJobs` của cron

### Vấn đề 2. `contentHash` không bao gồm các field business quan trọng

`contentHash` hiện tại chỉ dựa trên:

- `title`
- `jobCategory`
- `state`

Điểm chứng:

- `buildSearchText(job)` ở [ElasticsearchDataInitializer.java](../../../src/main/java/com/hcmute/careergraph/config/app/ElasticsearchDataInitializer.java:382)
- so sánh hash ở [ElasticsearchDataInitializer.java](../../../src/main/java/com/hcmute/careergraph/config/app/ElasticsearchDataInitializer.java:162)

Hệ quả:

- Các thay đổi sau **không làm cron tự reindex** nếu runtime sync bị miss:
  - `verificationStatus` công ty
  - `operationalStatus` công ty
  - `expiryDate`
  - `description`
  - `qualifications`
  - `minimumQualifications`
  - `responsibilities`
  - `city`

Nói ngắn gọn:

- cron hiện tại **không đủ mạnh để tự sửa lệch index** cho nhiều thay đổi business quan trọng

### Vấn đề 3. Logic `jobSearchable` của cron thiếu check hết hạn

Trong cron reindex, `jobSearchable` được set theo:

- company `APPROVED`
- company `ACTIVE`

Nhưng **không check `expiryDate`**

Điểm chứng:

- [ElasticsearchDataInitializer.java](../../../src/main/java/com/hcmute/careergraph/config/app/ElasticsearchDataInitializer.java:422)

So với runtime sync:

- runtime dùng `isJobPubliclyAvailable(job)` và có check hạn ứng tuyển ở [JobServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java:818)

Hệ quả:

- Nếu cron là nơi cập nhật cuối cùng, job đã hết hạn vẫn có thể mang `jobSearchable=true` trong ES

### Vấn đề 4. Cron có thể phát lại `JobCreatedEvent` khi reindex

Trong `toJobDocument(...)`, cron reindex có thể gọi:

- `publisher.publishEvent(new JobCreatedEvent(job.getId()))`

Điểm chứng:

- [ElasticsearchDataInitializer.java](../../../src/main/java/com/hcmute/careergraph/config/app/ElasticsearchDataInitializer.java:394)

Event này được `JobNotificationServiceImpl` dùng để thêm vào `NewlyPostedJob`:

- [JobNotificationServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/JobNotificationServiceImpl.java:41)

Hệ quả:

- reindex có thể vô tình đánh dấu lại job là “mới đăng”
- `DailyDigestScheduler.buildQueue()` sẽ lấy `newlyPostedJobIds` và query ES để gửi email gợi ý
- vì vậy cron reindex có nguy cơ làm sai semantics của daily digest

## Ảnh hưởng thực tế đến search và detail

### Candidate search `/jobs`

Luồng candidate search bằng ES sau đó map ngược DB và filter lại bằng:

- `filter(this::isJobPubliclyAvailable)`

Điểm chứng:

- [JobServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java:730)

Kết luận:

- Dù ES bị stale một phần, candidate search vẫn có lớp lọc DB cứu lại
- Vì vậy nhiều case public search vẫn ra kết quả đúng ở tầng API

### Daily digest / queue recommendation

Luồng `searchRecommendJobsFromNewlyPosted(...)` filter theo:

- `status = ACTIVE`
- `jobSearchable = true`

Điểm chứng:

- [JobESServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/JobESServiceImpl.java:543)

Nhưng luồng này **không map ngược DB để filter lại** như `/jobs/search`.

Kết luận:

- Nếu `jobSearchable` trong ES bị stale do cron/runtime lệch, daily digest có thể gợi ý sai

### Job detail `/jobs/{id}`

Đây là điểm chưa đúng case.

`validateJobAccess(job)` cho phép public nếu:

- `job.status == ACTIVE`
- `companyAccessPolicyService.isJobPubliclyAvailable(job)`

Điểm chứng:

- [JobController.java](../../../src/main/java/com/hcmute/careergraph/controllers/JobController.java:685)

Vấn đề:

- `companyAccessPolicyService.isJobPubliclyAvailable(job)` chỉ check company `APPROVED + ACTIVE`
- không check `expiryDate`

Điểm chứng:

- [CompanyAccessPolicyServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/CompanyAccessPolicyServiceImpl.java:57)

Kết luận:

- Job đã hết hạn nhưng vẫn `ACTIVE` và công ty vẫn `APPROVED + ACTIVE` thì **candidate vẫn có thể truy cập detail public**
- Trong khi apply đã chặn đúng hạn ứng tuyển ở [ApplicationServiceImpl.java](../../../src/main/java/com/hcmute/careergraph/services/impl/ApplicationServiceImpl.java:170)

## Ma trận kết quả theo case

| Case | Runtime sync | Kết quả search public | Ghi chú |
|---|---|---|---|
| Company approve | Có | Đúng | Jobs active, chưa hết hạn sẽ quay lại search |
| Company reject | Có | Đúng | Jobs bị ẩn khỏi public search |
| Company needs additional info | Có | Đúng | Jobs bị ẩn khỏi public search |
| Company block | Có | Đúng | Jobs bị ẩn khỏi public search |
| Company unblock | Có | Đúng có điều kiện | Chỉ public nếu vẫn `APPROVED` và job còn hợp lệ |
| Job publish từ draft | Có | Đúng | Job vào index khi `ACTIVE` |
| Job deactivate / close / delete | Có | Đúng | Job bị xóa khỏi index |
| Gia hạn expiry date | Có | Đúng | Job được sync lại |
| Job hết hạn tự nhiên theo ngày | Không có runtime event | Chưa chắc đúng ở ES | API search có lớp lọc DB, nhưng ES có thể stale |
| Cron tự sửa lệch approve/block/unblock nếu runtime miss | Chưa tin cậy | Chưa đủ tốt | Vì `contentHash` và `jobSearchable` chưa đúng |
| Job detail khi job đã hết hạn | Không đúng | Sai case | Vẫn có thể xem public |

## Kết luận cuối

### Những gì đang đúng

- `approve`, `reject`, `request additional info`, `block`, `unblock` đều đã gọi sync lại jobs
- `đóng job`, `deactivate`, `publish`, `activate`, `gia hạn expiry`, `draft` đều có runtime sync hợp lý
- Sau khi công ty được approve, jobs cũ của công ty **có được sync lại để vào search**

### Những gì chưa ổn

1. Cron ES hiện tại **không phải nguồn sự thật đáng tin cậy** cho các thay đổi business quan trọng.
2. Cron reindex dùng điều kiện `jobSearchable` chưa đồng nhất với runtime sync.
3. `contentHash` quá hẹp, khiến cron không reindex khi `verificationStatus`, `operationalStatus`, `expiryDate` hoặc nội dung search quan trọng thay đổi.
4. `JobCreatedEvent` đang bị phát lại trong cron reindex, có thể làm sai daily digest.
5. Job detail `/jobs/{id}` chưa chặn public access cho job hết hạn.

## Khuyến nghị ưu tiên

### Ưu tiên cao

1. Đồng bộ logic cron với runtime:
   - `ElasticsearchDataInitializer.shouldIndexJob(...)`
   - `ElasticsearchDataInitializer.toJobDocument(...).jobSearchable`
   - nên dùng cùng tiêu chí với `JobServiceImpl.isJobPubliclyAvailable(...)`

2. Mở rộng `contentHash` để bao gồm ít nhất:
   - `title`
   - `description`
   - `qualifications`
   - `minimumQualifications`
   - `responsibilities`
   - `state`
   - `city`
   - `status`
   - `expiryDate`
   - `company.verificationStatus`
   - `company.operationalStatus`

3. Sửa `validateJobAccess(...)` để check cả hết hạn, không chỉ status + company state.

### Ưu tiên trung bình

4. Bỏ `publishEvent(new JobCreatedEvent(...))` ra khỏi `ElasticsearchDataInitializer.toJobDocument(...)`.
5. Nếu cần daily digest an toàn hơn, sau khi lấy hit từ ES nên map lại DB và filter thêm bằng `isJobPubliclyAvailable(...)`.

