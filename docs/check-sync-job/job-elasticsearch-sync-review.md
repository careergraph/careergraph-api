# Báo cáo audit đồng bộ Job Elasticsearch và truy vấn job

## Mục tiêu

Tài liệu này audit luồng `job -> jobs_es` theo góc nhìn production, bám trên source hiện tại của:

- app startup reindex
- cron/scheduler reindex
- runtime sync khi job/company thay đổi
- query public search, personalized, popular, similar, job detail
- luồng verification/block company ảnh hưởng đến public availability

Phạm vi source chính đã đọc:

- `src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java`
- `src/main/java/com/hcmute/careergraph/services/impl/JobESServiceImpl.java`
- `src/main/java/com/hcmute/careergraph/config/app/ElasticsearchDataInitializer.java`
- `src/main/java/com/hcmute/careergraph/config/app/ElasticsearchSyncScheduler.java`
- `src/main/java/com/hcmute/careergraph/controllers/JobController.java`
- `src/main/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImpl.java`
- `src/main/java/com/hcmute/careergraph/services/impl/CompanyVerificationServiceImpl.java`
- `src/main/java/com/hcmute/careergraph/services/impl/CompanyAccessPolicyServiceImpl.java`
- `src/main/java/com/hcmute/careergraph/repositories/JobRepository.java`
- `src/main/java/com/hcmute/careergraph/schedule/DailyDigestScheduler.java`
- `src/main/java/com/hcmute/careergraph/services/impl/JobRecommendationServiceImpl.java`

## Kết luận điều hành

Runtime sync hiện tại đã có mặt ở hầu hết mutation quan trọng của `job` và `company`, nên nếu mọi request runtime chạy thành công thì dữ liệu public search thường sẽ đúng.

Tuy nhiên hệ thống **chưa đạt chuẩn production** vì còn các lệch lớn sau:

1. `startup sync` và `cron sync` đang dùng tiêu chí public khác với runtime sync, nên không phải cơ chế repair đáng tin cậy.
2. `cron sync` dùng `contentHash` quá hẹp, không đủ sửa drift khi dữ liệu search quan trọng thay đổi.
3. `full sync` có thể phát sinh lại `JobCreatedEvent`, làm job cũ bị coi là job mới cho daily digest.
4. `job detail` public đang cho xem job đã hết hạn nếu job vẫn `ACTIVE` và company vẫn `APPROVED + ACTIVE`.
5. workflow verification hiện cho phép admin đổi `APPROVED -> REJECTED/NEEDS_ADDITIONAL_INFO`, trái với state machine production nên có nguy cơ gây side effect nghiệp vụ và side effect index không mong muốn.

## Các điểm đang làm đúng

### Runtime sync khi job thay đổi

Các case dưới đây đều gọi `syncJobSearchDocument(...)` sau khi save:

- tạo job: `JobServiceImpl.createJob(...)`
- cập nhật full job: `JobServiceImpl.updateJob(...)`
- publish job: `JobServiceImpl.publishJob(...)`
- soft delete/close job: `JobServiceImpl.deleteJob(...)`
- update settings, gồm `expiryDate` và `status`: `JobServiceImpl.updateJobSettings(...)`
- activate/deactivate: `JobServiceImpl.activateJob(...)`, `JobServiceImpl.deactivateJob(...)`

Điểm chứng:

- `JobServiceImpl.java:95-109`
- `JobServiceImpl.java:190-225`
- `JobServiceImpl.java:252-271`
- `JobServiceImpl.java:279-294`
- `JobServiceImpl.java:300-325`
- `JobServiceImpl.java:330-345`

### Runtime sync khi company verification hoặc moderation thay đổi

Các case dưới đây đều sync lại toàn bộ job của company:

- approve/reject/request additional info: `AdminCompanyVerificationServiceImpl.markVerificationDecision(...)`
- block/unblock company
- company submit verification hoặc resubmit verification

Điểm chứng:

- `AdminCompanyVerificationServiceImpl.java:212-242`
- `AdminCompanyVerificationServiceImpl.java:160-199`
- `CompanyVerificationServiceImpl.java:47-63`
- `CompanyVerificationServiceImpl.java:67-92`
- `JobServiceImpl.java:808-815`

### Public DB queries đang lọc khá đúng

Các query DB public cho list/by-company/popular/similar đều check:

- `job.status = ACTIVE`
- `company.verificationStatus = APPROVED`
- `company.operationalStatus = ACTIVE`
- `expiryDate >= currentDate` hoặc `expiryDate IS NULL`

Điểm chứng:

- `JobRepository.java:32-59`
- `JobRepository.java:72-87`
- `JobRepository.java:124-154`
- `JobRepository.java:156-167`
- `JobRepository.java:191-241`

Điều này giúp phần lớn màn hình public fallback bằng DB vẫn an toàn hơn ES.

## Findings

### 1. Critical: startup/cron sync không dùng cùng chuẩn public-eligibility với runtime sync

Runtime sync dùng `JobServiceImpl.isJobPubliclyAvailable(job)`:

- `job.status == ACTIVE`
- chưa quá hạn
- company `APPROVED`
- company `ACTIVE`

Điểm chứng:

- `JobServiceImpl.java:818-826`
- `JobServiceImpl.java:1496-1530`

Nhưng startup/cron sync lại dùng:

- `shouldIndexJob(job)` chỉ check `job != null`, `status == ACTIVE`, `company != null`
- `toJobDocument(...).jobSearchable` chỉ check company `APPROVED + ACTIVE`, không check expiry

Điểm chứng:

- `ElasticsearchDataInitializer.java:388-392`
- `ElasticsearchDataInitializer.java:401-427`

Hệ quả production:

- job `ACTIVE` nhưng đã hết hạn vẫn có thể còn nằm trong `jobs_es` sau startup/cron
- startup/cron không phải cơ chế repair đúng nghĩa nếu runtime sync bị miss
- public index không còn là “single source of searchable truth”

### 2. High: cron repair không sửa được nhiều drift vì `contentHash` quá hẹp

`ElasticsearchDataInitializer.buildSearchText(job)` hiện chỉ hash:

- `title`
- `jobCategory.displayName`
- `state`

Điểm chứng:

- `ElasticsearchDataInitializer.java:381-386`
- `ElasticsearchDataInitializer.java:163-170`
- `ElasticsearchDataInitializer.java:395-426`

Trong khi document ES thực tế còn phụ thuộc vào:

- `description`
- `qualifications`
- `minimumQualifications`
- `responsibilities`
- `employmentType`
- `experienceLevel`
- `education`
- `city`
- `company.verificationStatus`
- `company.operationalStatus`
- `expiryDate`

Hệ quả production:

- runtime sync fail một lần là cron có thể không backfill lại
- đổi mô tả/yêu cầu công việc nhưng cron vẫn tưởng “unchanged”
- đổi trạng thái moderation/company cũng có thể không được cron repair đúng nếu runtime miss

### 3. High: full sync có side effect nghiệp vụ, có thể đẩy job cũ vào daily digest

Trong `ElasticsearchDataInitializer.toJobDocument(...)`, nếu `matchTitle(job)` thì publish lại `JobCreatedEvent`.

Điểm chứng:

- `ElasticsearchDataInitializer.java:394-400`
- `JobNotificationServiceImpl.java:38-61`
- `DailyDigestScheduler.java:63-171`

Điều này nghĩa là:

- reindex startup
- cron force sync
- repair sync

đều có thể đánh dấu lại job cũ vào `NewlyPostedJob`, sau đó luồng digest xem đó là job mới đăng.

Theo chuẩn production, reindex/search repair **không được phát sinh side effect domain event** như tạo “newly posted” hay email candidate.

### 4. High: public job detail đang cho xem job expired, và còn tăng views trước khi validate

`JobController.getJobById(...)` gọi:

1. `jobService.getJobById(id)`
2. trong service này tăng `views` và save luôn
3. sau đó mới `validateJobAccess(job)`

Điểm chứng:

- `JobController.java:117-139`
- `JobServiceImpl.java:133-144`

Ngoài ra `validateJobAccess(job)` chỉ cho public pass khi:

- `job.status == ACTIVE`
- `companyAccessPolicyService.isJobPubliclyAvailable(job)`

Nhưng `CompanyAccessPolicyService.isJobPubliclyAvailable(job)` chỉ check company status, không check expiry.

Điểm chứng:

- `JobController.java:685-710`
- `CompanyAccessPolicyServiceImpl.java:56-64`

Hệ quả production:

- candidate vẫn có thể xem detail job đã hết hạn nếu job chưa bị chuyển status
- views bị tăng cả với request bị từ chối sau đó
- metrics job detail sai

### 5. Medium: search candidate dùng ES để lấy ID rồi mới lọc DB, làm kết quả và total có thể sai lệch

Luồng search candidate hiện là:

1. query ES
2. lấy list ID
3. `jobRepository.findAllById(ids)`
4. filter lại bằng `isJobPubliclyAvailable(...)`
5. trả `PageImpl<>(ljobs, pageable, ljobs.size())`

Điểm chứng:

- `JobServiceImpl.java:716-745`
- `JobESServiceImpl.java:95-197`
- `JobESServiceImpl.java:330-529`

Hệ quả:

- nếu index bị stale thì ES có thể trả nhiều ID nhưng DB filter rớt bớt
- số lượng item trả về nhỏ hơn page size bất thường
- `total` không dùng total hits thật của ES mà dùng `ljobs.size()`
- pagination/search analytics sẽ lệch

### 6. High: verification state machine chưa chuẩn production, cho phép `APPROVED -> REJECTED/NEEDS_ADDITIONAL_INFO`

Backend admin hiện không chặn chuyển trạng thái review từ request đã `APPROVED` sang:

- `REJECTED`
- `NEEDS_ADDITIONAL_INFO`

Điểm chứng:

- `AdminCompanyVerificationServiceImpl.java:212-242`

Admin UI cũng đang mở nút `Từ chối` và `Yêu cầu bổ sung` ngay cả khi record đã `APPROVED`, chỉ khóa nút `Approve`.

Điểm chứng:

- `VerificationDetailPage.tsx:75-79`
- `VerificationDetailPage.tsx:145-177`

Trong khi phía company lại không thể submit verification mới khi company đã `APPROVED`:

- `CompanyVerificationServiceImpl.validateSubmissionAllowed(...)`

Điểm chứng:

- `CompanyVerificationServiceImpl.java:131-138`

Hệ quả nghiệp vụ:

- state machine không nhất quán
- approved không còn là trạng thái kết thúc review
- moderation và verification bị trộn lẫn
- sync ES vẫn chạy, nhưng đang chạy trên một workflow chưa chuẩn production

## Đánh giá theo từng tình huống user yêu cầu

### App startup

Có reindex qua `ElasticsearchDataInitializer implements CommandLineRunner`.

Điểm đúng:

- có chạy startup sync nếu `APP_ES_SYNC_JOBS_ENABLED=true`

Điểm chưa production:

- không dùng đúng public-eligibility
- có thể phát domain event ngoài ý muốn
- không phải repair job đáng tin cậy

Điểm chứng:

- `CareerGraphApplication.java:9-17`
- `ElasticsearchDataInitializer.java:84-91`

### Cron job

Có cron chạy qua `ElasticsearchSyncScheduler`.

Điểm đúng:

- có anti-overlap bằng `AtomicBoolean`
- có config batch size

Điểm chưa production:

- gọi lại chính logic sync chưa chuẩn ở `ElasticsearchDataInitializer.syncNow(...)`

Điểm chứng:

- `ElasticsearchSyncScheduler.java:25-54`

### Thay đổi trạng thái job, expiry date, đóng/mở job

Runtime sync hiện có.

Đánh giá:

- `publish/activate/deactivate/delete/update settings/update full job`: có sync
- riêng case “job tự hết hạn theo thời gian” chưa có processor độc lập, đang phụ thuộc vào query-time filtering và startup/cron repair chưa chuẩn

### Company chưa approve

Khi company ở `NOT_SUBMITTED`, `PENDING_REVIEW`, `REJECTED`, `NEEDS_ADDITIONAL_INFO`:

- runtime sync sẽ set `jobSearchable=false`
- DB public queries sẽ không trả job

Điểm đúng:

- public query bằng DB an toàn
- ES query candidate có filter `jobSearchable=true`, nên thường không lộ

Điểm chưa production:

- document public không bị xóa dứt điểm khỏi public index ở runtime nếu job vẫn `ACTIVE`

### Block/unblock company

Khi block/unblock:

- backend có sync lại toàn bộ job company
- runtime sync phản ánh ngay `jobSearchable`

Điểm đúng:

- case này đã được wire tương đối đầy đủ

Điểm chưa production:

- vẫn là “ẩn bằng cờ” nhiều hơn “loại khỏi public index”

## Đánh giá query search và job detail

### Search jobs

Public search candidate dùng ES rồi lấy DB theo ID:

- ưu điểm: tận dụng semantic/hybrid search
- nhược điểm: phụ thuộc index sạch

Kết luận:

- nếu sửa đúng public-index contract thì hướng này chấp nhận được
- nếu chưa sửa contract, search có thể thiếu ổn định về paging và consistency

### Personalized / popular / similar

- `personalized ES`: dùng ES rồi filter DB lại
- `popular`: dùng DB public query, an toàn hơn
- `similar`: dùng DB public query, an toàn hơn

Điểm chứng:

- `JobServiceImpl.java:455-500`
- `JobServiceImpl.java:570-595`

### Job detail

Đây là điểm chưa production rõ nhất của public read path:

- expired job vẫn có thể xem
- view count tăng trước khi auth/visibility check xong

## Kết luận cuối

### Câu hỏi: hiện tại ES có được reindex lại ở các case quan trọng không?

Có, ở runtime thì phần lớn case quan trọng đã được wire:

- create/update/publish/activate/deactivate/delete job
- company approve/reject/needs info
- company block/unblock
- company submit/resubmit verification

### Câu hỏi: hệ thống đã chuẩn production chưa?

Chưa.

Lý do chính:

1. startup/cron repair logic không cùng chuẩn với runtime public contract
2. full sync có side effect domain event
3. expired job handling chưa tách thành processor nghiệp vụ rõ ràng
4. public job detail chưa chặn expired đúng chuẩn
5. verification state machine chưa chốt theo hướng production

## Hướng xử lý tiếp theo

Chi tiết đề xuất kiến trúc và roadmap nằm ở:

- [production-sync-solution-proposal.md](./production-sync-solution-proposal.md)
- [company-verification-production-policy.md](./company-verification-production-policy.md)
