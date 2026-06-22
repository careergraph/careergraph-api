# Phase 4 Report - Observability, Tests, and Rollout

## Vấn đề

- các rule quan trọng của verification lifecycle và public job contract mới được chốt ở phase trước nhưng test coverage còn mỏng
- repair/sync path đã đúng hướng hơn sau Phase 3, nhưng log chưa đủ cô đọng để nhìn nhanh drift, stale document, hoặc cleanup do expired jobs
- manual internal sync chưa có log mở đầu/kết thúc rõ ràng để support rollout và production audit
- admin/HR UX đã tương đối khớp với policy backend, nhưng vẫn cần note rõ operational implication cho team rollout

## Root cause

- test tập trung nhiều ở flow chức năng chung, chưa khóa chặt các guard production-sensitive
- observability hiện chủ yếu là log rời rạc theo từng nhánh xử lý, thiếu summary-level signal cho support/oncall
- môi trường test dùng Mockito inline mock maker mặc định, không ổn định trên máy local/CI không hỗ trợ agent attach

## Thay đổi đã làm

- bổ sung test cho verification lifecycle guard trong `AdminCompanyVerificationServiceImplTest`:
  - approve path tiếp tục xác nhận sync + notification
  - reject path tiếp tục chặn request đã kết luận
  - thêm test `requestAdditionalInfo(...)` để khóa behavior `PENDING_REVIEW -> NEEDS_ADDITIONAL_INFO`
- thêm `CompanyAccessPolicyServiceImplTest` để khóa public contract trung tâm:
  - job public hợp lệ
  - job expired bị loại
  - company chưa `APPROVED` bị loại
  - candidate application bị chặn khi job không còn public
- mở rộng `JobServiceImplTest` cho runtime sync behavior của company jobs:
  - job không còn public phải `deleteById(...)` khỏi ES
  - job còn public phải `save(...)` lại search document
- thêm `careergraph-api/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` với `mock-maker-subclass` để unit test chạy ổn định trên môi trường không hỗ trợ inline agent attach
- tăng observability ở backend:
  - `JobServiceImpl.syncCompanyJobsSearchDocuments(...)` log tổng số job, số public và non-public theo company
  - `ElasticsearchDataInitializer.syncNow(...)` log summary state gồm `totalJobs`, `eligibleJobs`, `staleDocuments`, `changedJobs`, `unchangedJobs`, `force`, `batchSize`
  - khi phát hiện stale ES docs trước sync, log warning kèm sample IDs
  - `ExpiredJobRepairService` log warning khi phát hiện expired jobs cần cleanup, kèm sample IDs
  - `InternalElasticsearchSyncController` log request parameters và target hoàn tất cho manual sync

## Test coverage đã thêm hoặc còn thiếu

Đã thêm:

- verification decision guard quan trọng nhất
- public eligibility contract ở policy service
- runtime sync contract cho `syncCompanyJobsSearchDocuments(...)`

Đã verify:

- chạy `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH ./mvnw -Dtest=AdminCompanyVerificationServiceImplTest,CompanyAccessPolicyServiceImplTest,JobServiceImplTest test`
- kết quả: `BUILD SUCCESS`, `Tests run: 12, Failures: 0, Errors: 0`

Còn thiếu:

- chưa có controller-level test cho `JobController.getJobById(...)` để khóa behavior "chỉ tăng views sau khi public access hợp lệ"
- chưa có test cho `InternalElasticsearchSyncController` response contract và target matrix
- chưa có integration test cho startup/scheduler repair path với repository thật hoặc slice test

## Observability improvements

- có log summary đủ để nhìn nhanh drift theo ba lớp:
  - company-level runtime sync
  - batch-level repair sync
  - expired artifact cleanup
- stale document detection giờ có warning riêng để support dễ phân biệt giữa:
  - sync không có thay đổi
  - sync có drift cần cleanup
  - sync fail do embedding/index issue
- manual internal sync giờ có audit trail rõ hơn ở đầu vào và đầu ra, thuận tiện cho production rollout runbook

## Review admin + HR UX implications

- `DashboardPage.tsx` và `CompanyListPage.tsx` hiện đã phản ánh đúng bối cảnh moderation/monitoring, không thấy blocker UX bắt buộc phải sửa trong phase này
- `CompanyVerificationPage.tsx` của HR vẫn phù hợp với policy Phase 1 vì resubmission chỉ diễn ra sau `REJECTED` hoặc `NEEDS_ADDITIONAL_INFO`
- residual UX note: admin cần được onboarding rõ rằng block/unblock company có thể làm public jobs biến mất/xuất hiện lại qua sync, đây là implication backend đúng chủ đích chứ không phải lỗi hiển thị

## Rollout checklist

1. Deploy bằng JDK 17+ và giữ nguyên test mock-maker config trong repo.
2. Chạy unit test mục tiêu hoặc full API test suite trên CI sau merge.
3. Deploy backend lên staging, xác nhận log summary mới xuất hiện ở:
   - company verification decision path
   - expired-job repair path
   - scheduled/manual job sync path
4. Chạy một lần `POST /internal/elasticsearch/sync?target=jobs` sau deploy để repair stale docs và expired artifacts.
5. Theo dõi 1-2 chu kỳ scheduler đầu:
   - `staleDocuments`
   - `changedJobs`
   - `pending`
   - warning của expired-job repair
6. Kiểm tra ngẫu nhiên vài case production-like:
   - company `APPROVED + ACTIVE` có job public
   - company bị `BLOCKED` làm job bị gỡ khỏi ES
   - job expired bị xóa ES doc và notification artifact
7. Thông báo admin/support rằng manual sync log giờ có request/finish markers để tiện tra cứu incident.

## Rollback notes

- rollback code an toàn vì thay đổi phase 4 chủ yếu là test + logging, không đổi business contract đã chốt ở Phase 1-3
- nếu cần rollback backend, ưu tiên rollback cả phần observability cùng version runtime sync hiện tại để tránh lệch log/runbook
- nếu deploy phase 4 xong đã chạy manual sync repair, rollback code không tự phục hồi stale ES docs cũ; khi rollback cần cân nhắc chạy lại manual sync của version đích nếu contract sync khác
- file `mockito-extensions/org.mockito.plugins.MockMaker` chỉ ảnh hưởng test runtime, không ảnh hưởng production

## Residual risks

- chưa có integration test end-to-end cho scheduler/manual sync với Elasticsearch thật
- log mới cải thiện khả năng đọc drift nhưng chưa có metric/alerting pipeline riêng; production vẫn phụ thuộc log aggregation
- chưa chạm frontend copy để giải thích rõ hơn public visibility side effect cho admin/HR, nên phần này vẫn cần rollout communication
