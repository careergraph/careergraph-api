# Phase 2 Report - Runtime Sync and Access Consistency

## Vấn đề

- runtime sync chưa dùng một public contract duy nhất để quyết định `upsert` hay `delete` document trong Elasticsearch
- helper `isJobPubliclyAvailable(...)` mới check trạng thái company, chưa phủ `job.status` và `expiryDate`
- public job detail tăng `views` trước khi xác nhận request có còn hợp lệ theo public contract hay không
- apply flow đang tự tách nhỏ rule `ACTIVE` và `expiry`, dễ drift với detail/search/sync

## Root cause

- public eligibility rule bị tách ra giữa `JobServiceImpl`, `CompanyAccessPolicyServiceImpl` và controller
- runtime sync chỉ xóa document khi `job.status != ACTIVE`, nên các trường hợp company mất `APPROVED`, bị `BLOCKED` hoặc job hết hạn có thể vẫn còn document public trong ES
- read path và side effect path chưa tách bạch, làm `views` có thể tăng cho request public đáng lẽ phải bị từ chối

## Thay đổi đã làm

- cập nhật `CompanyAccessPolicyServiceImpl.isJobPubliclyAvailable(...)` để trở thành public contract dùng chung cho runtime path:
  - `job != null`
  - `company != null`
  - `job.status = ACTIVE`
  - `expiryDate` chưa qua hoặc không có expiry
  - `company.verificationStatus = APPROVED`
  - `company.operationalStatus = ACTIVE`
- thêm `isJobExpired(Job job)` vào policy service để gom logic expiry về cùng một chỗ
- sửa `JobServiceImpl.syncJobSearchDocument(...)`:
  - nếu job không còn public thì `deleteById(...)` khỏi ES
  - chỉ `upsert` khi job còn public
  - document public được lưu với `jobSearchable = true`
- sửa `JobServiceImpl.getJobById(...)` để bỏ side effect tăng view
- thêm `JobServiceImpl.incrementJobViews(...)` và đổi `JobController.getJobById(...)` sang:
  - load job
  - validate access
  - chỉ tăng `views` sau khi request public hợp lệ
- đơn giản hóa apply flow trong `ApplicationServiceImpl` để dùng lại public contract trung tâm thay vì tách rule riêng

## Cách verify

1. Mở job public hợp lệ qua `GET /jobs/{id}` và xác nhận response thành công, `views` tăng sau request.
2. Chuyển company của job sang `BLOCKED` hoặc đổi verification khỏi `APPROVED`, rồi kích hoạt runtime sync qua mutation path hiện có; xác nhận document job bị gỡ khỏi ES.
3. Đặt `expiryDate` của job sang ngày đã qua, gọi `GET /jobs/{id}` khi là anonymous/candidate và xác nhận bị từ chối, đồng thời `views` không tăng.
4. Gọi `POST /jobs/{id}/application` với job đã expired hoặc company không còn active/approved và xác nhận request bị từ chối bởi cùng public contract.

## Rủi ro còn lại

- Phase 2 mới chuẩn hóa runtime path; startup/cron/manual repair path của Phase 3 vẫn cần dùng lại đúng contract này để tránh drift ngoài runtime mutation
- hiện chưa bổ sung automated tests cho các case expired/blocked/unapproved/public detail metrics; nên thêm ở Phase 4
- logic parse expiry hiện đang ưu tiên định dạng ngày `yyyy-MM-dd`; nếu sau này dữ liệu `expiryDate` có thêm format khác, nên chuẩn hóa model thay vì mở rộng parse rải rác
