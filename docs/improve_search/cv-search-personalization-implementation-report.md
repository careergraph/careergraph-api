# CV Search Personalization Implementation Report

Ngày kiểm tra: 2026-05-31

## Phạm vi đã triển khai

- Đồng bộ lifecycle CV vào CandidateES:
  - Sau khi extract CV thành công, publish `CandidateUpdatedEvent(RESUME_UPDATED)`.
  - Khi extraction fail, clear text/hash và publish `RESUME_EXTRACTION_FAILED`.
  - Khi xóa CV, soft delete, tắt `shareToFindJob`, publish `RESUME_DELETED`.
  - Khi bật/tắt CV tìm việc chính, publish `RESUME_VISIBILITY_CHANGED` sau transaction.
- Thêm idempotency guard bằng `file.resume_content_hash`.
- Thêm API:
  - `PUT /candidates/media/{fileId}/share-to-find-job`
  - Body: `{ "enabled": true | false }`
  - Response: danh sách CV active để client đồng bộ state.
- Đảm bảo chỉ một CV active/shared tại một thời điểm bằng transaction:
  - Tắt toàn bộ CV active của candidate.
  - Nếu `enabled=true`, bật CV được chọn.
- CandidateES:
  - Thêm metadata `resumeFileId`, `resumeUpdatedAt`, `resumeContentHash`.
  - `mapToES()` vẫn query DB tại thời điểm re-index để tránh stale event payload.
  - Listener luôn tôn trọng `isOpenToWork`; OFF thì xóa candidate khỏi ES.
  - Không expose raw `resumeText` trong DTO HR response.
- Personalized jobs, search blank keyword và digest:
  - Thêm `CandidateSearchTextBuilder` dùng chung.
  - Text gồm desired position, title hiện tại, summary, industries, locations, work types, skills và CV shared snippet tối đa 4000 ký tự.
  - Khi user đã nhập keyword, không concat profile/CV vào query string nữa.
  - Không dùng Elasticsearch fuzzy query cho text dài/CV để tránh `fuzzy_terms_exception` và `too_complex_to_determinize_exception`.
  - Nếu ES personalized search trả lỗi/null, fallback về latest jobs thay vì ném `NullPointerException`.
- JobES/search quality:
  - Thêm fields `qualifications`, `minimumQualifications`, `responsibilities`, `skills`.
  - Cập nhật mapping JSON.
  - BM25 job search/digest search dùng thêm JD đầy đủ.
  - Daily digest chuyển sang hybrid signal: BM25 + KNN trong tập newly posted jobs.
- Candidate UI:
  - CV card hiển thị trực tiếp trạng thái CV tìm việc chính.
  - Có CTA `Dùng CV này` / `Tắt CV chính`.
  - Optimistic update, rollback khi API lỗi.
  - Khi chưa chọn CV chính, hiển thị cảnh báo rõ ràng.
  - Xóa CV dùng `fileId` qua `/candidates/media`, đúng soft-delete flow backend.

## Kiểm tra production/enterprise

### Passed

- `mvn -q -DskipTests compile`: PASS.
- Event-driven sync không phụ thuộc Spring Security context trong async listener.
- `InternalElasticsearchSyncController` được đặt `@Profile("!test")` đồng bộ với ES initializers để test profile không boot bean phụ thuộc ES sync.
- Đã kiểm tra lỗi runtime `Cannot invoke SearchResponse.hits() because listSearch is null`: nguyên nhân là fuzzy search trên CV text dài; đã bỏ fuzzy cho query dài và thêm fallback null-safe.
- Toggle CV chính chạy trong `@Transactional`, tránh bật hai CV trong một request flow.
- CandidateES re-index lấy CV mới nhất từ DB, không dùng `fileId` trong event để quyết định nội dung index.
- HR candidate response vẫn chỉ trả profile/search fields, không trả `resumeText`.
- Backward compatibility cho frontend: API response giữ cả `shareToFindJob` và field cũ `shareToFileJob`.

### Không pass / chưa kiểm được trong môi trường hiện tại

- `mvn -q -Dtest=CareergraphApplicationTests test`: PASS sau khi tắt internal ES sync controller ở profile `test`.
- `mvn -q test`: FAIL do test hiện hữu `NotificationServiceImplTest.onApplicationStatusChanged_shouldAggregateRecentNotificationWithinWindow`.
  - Test kỳ vọng `notificationRepository.save(...)`, nhưng service skip vì đã tìm thấy recent interview notification.
  - Đây là lỗi logic/kỳ vọng ở notification test suite, không thuộc phạm vi CV search.
  - Trong log context test còn có scheduled task `InterviewRoomScheduler` đụng bảng H2 `interview_rooms` chưa tồn tại; task không làm fail targeted context test nhưng là rủi ro test isolation.
- Frontend build chưa chạy được vì máy không nhận `node`, `npm`, `yarn`.
  - `npm run build`: command not found.
  - `yarn build`: command not found.
  - Đã kiểm tra tĩnh flow API/UI, nhưng cần chạy build thực tế trên máy có Node.

## Rủi ro còn lại

- Mapping ES đã thêm field mới; môi trường đang chạy cần recreate index hoặc migration mapping phù hợp trước khi kỳ vọng search dùng field mới.
- `resume_content_hash` dựa vào `ddl-auto:update`; production chuẩn enterprise nên có migration SQL rõ ràng.
- Concurrent toggle tuyệt đối ở mức nhiều request song song nên bổ sung lock/unique partial constraint nếu DB hỗ trợ.
- HR ranking scenario B > A > C cần integration test với Elasticsearch thật hoặc Testcontainers.

## Khuyến nghị tiếp theo

1. Thêm migration SQL cho `file.resume_content_hash`.
2. Thêm reconciliation job so DB hash với ES `resumeContentHash`.
3. Sửa test profile để `mvn test` boot context sạch.
4. Thêm integration tests:
   - Extract CV publish re-index.
   - Delete CV xóa `resumeText` khỏi ES.
   - Toggle concurrent không bật hai CV.
   - Ranking B > A > C cho query `java developer`.
