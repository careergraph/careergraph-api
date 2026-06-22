# Phase 3 Report - Repair, Reindex, and Expiry Processing

## Vấn đề

- startup/cron/manual repair path đang tự định nghĩa eligibility riêng, nên có nguy cơ drift với public contract đã chốt ở Phase 2
- reindex path đang gắn side effect nghiệp vụ qua `JobCreatedEvent`, có thể đánh dấu lại job như job mới đăng khi chỉ đang repair
- `contentHash` của repair path quá hẹp, chỉ dựa trên một phần nhỏ search text nên dễ bỏ sót drift ở field filter/visibility
- expired jobs chưa có processor riêng để làm sạch search document và notification artifact nếu runtime miss
- daily digest có thể vẫn giữ queue item cho job không còn public tại thời điểm gửi

## Root cause

- logic build Elasticsearch document bị tách đôi giữa runtime sync và initializer, không có một source of truth chung
- repair path trước đó trộn lẫn indexing concern với business notification concern
- cleanup cho expired jobs và digest queue chưa được xem như một phần của repair lifecycle

## Thay đổi đã làm

- thêm `JobSearchDocumentFactory` để gom:
  - public eligibility check dùng lại `CompanyAccessPolicyService`
  - embedding text dùng chung
  - content hash rộng hơn, phủ search relevance, filter fields và visibility-related fields
  - mapping `Job -> JobES` dùng chung cho runtime và repair
- cập nhật `JobServiceImpl.syncJobSearchDocument(...)` để runtime sync dùng cùng factory với repair path
- cập nhật `ElasticsearchDataInitializer`:
  - chỉ repair job còn public theo đúng contract trung tâm
  - xóa stale ES docs cho job không còn public
  - so sánh bằng content hash mới
  - bỏ hoàn toàn side effect publish `JobCreatedEvent`
  - bỏ cleanup phá hủy `newly_posted_jobs`, queue và history khi `force` sync
- thêm `ExpiredJobRepairService` để:
  - tìm `ACTIVE` jobs đã quá `expiryDate`
  - xóa ES docs tương ứng
  - xóa `newly_posted_jobs`
  - xóa pending notification queue items liên quan
- nối expired-job repair vào:
  - startup trước job sync
  - cron job sync
  - internal manual sync endpoint qua target `expired-jobs`, đồng thời `target=jobs` cũng chạy repair này trước
- cập nhật `DailyDigestScheduler.sendDailyDigest()` để loại queue item không còn public trước khi gửi email

## Side effect domain đã loại khỏi reindex path

- trước khi sửa, `ElasticsearchDataInitializer.toJobDocument(...)` có `publisher.publishEvent(new JobCreatedEvent(job.getId()))`
- đây là side effect nghiệp vụ không phù hợp với repair/reindex path vì có thể tạo lại marker job mới đăng trong lúc startup/cron/manual sync
- hiện tại logic này đã bị gỡ khỏi repair path; chỉ runtime create job mới còn publish event

## Cách verify

1. Tạo job `ACTIVE` nhưng company chưa `APPROVED` hoặc bị `BLOCKED`, chạy startup/cron/manual sync và xác nhận job không còn document public trong ES.
2. Sửa các field như `department`, `qualifications`, `benefits`, `location`, `expiryDate`, `company verification/operational status`, rồi chạy repair sync; xác nhận hash đổi và document được reindex.
3. Gọi `POST /internal/elasticsearch/sync?target=jobs` và xác nhận response có cả `expired-jobs` và `jobs`.
4. Đặt một job sang ngày hết hạn trong quá khứ, chạy repair path và xác nhận:
   - document ES bị xóa
   - `newly_posted_jobs` của job bị xóa
   - queue item daily digest của job bị xóa
5. Tạo queue item cho job đã expired hoặc company bị block, chạy `sendDailyDigest()` và xác nhận email không gửi cho job đó.
6. Kiểm tra log startup/cron để xác nhận expired-job repair và job sync được log riêng.

## Rủi ro còn lại

- environment hiện tại đang dùng Java 8, nên chưa compile verify được module target Java 17 trong local session này
- expired processor hiện làm sạch ES + notification artifact, nhưng chưa đổi `job.status` trong DB; đây là chủ đích để giữ contract Phase 2 dựa trên `expiryDate`, nhưng cần rollout note rõ cho team
- `DailyDigestScheduler` vẫn load job theo từng queue user-group; nếu traffic digest tăng mạnh, nên tối ưu batch fetch hoặc thêm repository bulk query ở Phase 4

## Lưu ý rollout

- deploy ở môi trường có JDK 17+ trước khi chạy build/CI
- sau deploy nên chạy một lần manual `target=jobs` để repair lại stale docs và expired artifacts
- theo dõi log của startup/cron trong 1-2 chu kỳ đầu để bắt các case dữ liệu `expiryDate` bất thường hoặc artifact cũ còn sót
