# CV Template Modal + Multi-CV AI Context Report

Date: 2026-06-16

## Goal

Triển khai 2 thay đổi chính cho tính năng tạo CV từ trang chi tiết công việc:

1. Khi người dùng bấm `Tạo CV` ở trang `JobDetail`, hệ thống mở modal hiển thị các template CV sẵn có trong hệ thống để người dùng chọn trước.
2. Khi backend gọi AI để sinh `cv-suggestion`, hệ thống không còn chỉ lấy 1 CV upload mới nhất, mà lấy tất cả CV `ACTIVE` của ứng viên có `resumeExtractedText` hợp lệ để làm context.

## Files Changed

### Frontend

- `careergraph-client/src/sections/JobDetail/CtaBanner.jsx`
- `careergraph-client/src/sections/CVBuilder/components/TemplateSelectionModal.jsx`
- `careergraph-client/src/pages/CVBuilder.jsx`
- `careergraph-client/src/pages/CVTemplates.jsx`

### Backend

- `careergraph-api/src/main/java/com/hcmute/careergraph/repositories/FileRepository.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/FastAPIClientServiceImpl.java`
- `careergraph-api/src/test/java/com/hcmute/careergraph/services/impl/JobServiceImplTest.java`

## What Was Implemented

### 1. Template selection modal on Job Detail

- CTA `Tạo CV` không còn điều hướng cứng sang `harvard`.
- Khi bấm CTA, hệ thống mở modal chọn template.
- Modal hỗ trợ lọc `Tất cả / Miễn phí / Premium`.
- Khi người dùng chọn template:
  - Nếu chưa đăng nhập ứng viên: chuyển vào CV builder với template đã chọn, không gọi AI.
  - Nếu đã đăng nhập ứng viên: gọi API `POST /jobs/{jobId}/cv-suggestion`, sau đó chuyển vào CV builder với template đã chọn và `suggestedCv`.
- Nếu AI lỗi: vẫn chuyển vào builder với template đã chọn và fallback bằng dữ liệu hồ sơ hiện có.

### 2. Multi-CV context for AI suggestion

- Backend thêm luồng gom toàn bộ CV `ACTIVE` của candidate:
  - lọc `FileType.RESUME` và `FileType.CV`
  - sắp theo `createdDate desc`
  - bỏ CV không có text extract
  - dedupe theo `resumeContentHash` nếu có
  - chèn header phân tách từng CV theo format:
    - `[UPLOADED_CV_n | fileName | created_at=timestamp]`
- Có giới hạn tổng context:
  - property: `application.cv-suggestion.max-uploaded-context-chars`
  - default: `24000`
- Nếu không có CV usable:
  - fallback về `buildCandidateProfileText(candidate)`

### 3. Logging safety improved

- Không log full prompt chứa JD + dữ liệu ứng viên nữa.
- Chỉ log `promptLength` ở `FastAPIClientServiceImpl`.

### 4. Test coverage added

- Thêm unit test cho `JobServiceImpl`:
  - `generateCv_shouldUseAllActiveUploadedCvTextsAsPromptContext`
  - `generateCv_shouldFallbackToCandidateProfileWhenUploadedCvsHaveNoExtractedText`

## Verification

### Passed

- Backend compile:
  - `mvn -q -DskipTests compile`
- New backend unit test:
  - `mvn -q -Dtest=JobServiceImplTest test`
- Existing app smoke test:
  - `mvn -q -Dtest=CareergraphApplicationTests test`

### Could Not Fully Verify

- Frontend production build chưa chạy được trong môi trường hiện tại vì máy không có `node`, `npm`, hoặc `yarn` trong PATH tại thời điểm kiểm tra.
- Frontend đã được review logic và import path thủ công, nhưng vẫn nên chạy lại `vite build` trên máy có Node runtime để xác nhận chắc chắn.

## Production Readiness Review

### Stable enough for next development step

- Flow chọn template trước khi tạo CV đã rõ ràng và dễ mở rộng.
- AI context nhiều CV hợp lý hơn đáng kể so với chỉ lấy 1 CV đầu tiên.
- Có fallback giữ trải nghiệm không vỡ nếu AI lỗi.
- Đã tránh log prompt nhạy cảm.

### Remaining risks / debt

1. Rate limit `cv_suggestion_limit` vẫn đang dùng `get + set`, chưa atomic.
2. `CareergraphApplicationTests` vẫn pass nhưng có lỗi scheduled task `InterviewRoomScheduler` vì H2 test env thiếu bảng `INTERVIEW_ROOMS`.
3. Chưa có frontend automated test cho modal/template selection flow.
4. AI output vẫn là JSON free-form, chưa enforce schema mạnh ở tầng FastAPI model call.

## Suggested Next Actions

1. Chạy frontend build thật trên máy có Node:
   - `npm run build` hoặc `yarn build`
2. Nếu muốn production-hardening thêm:
   - đổi Redis rate limit sang atomic increment
   - thêm integration test cho endpoint `cv-suggestion`
   - thêm E2E test cho flow `JobDetail -> Modal -> CVBuilder`

## Notes For Future Agents

- Khi tiếp tục phát triển tính năng này, bắt buộc đọc ít nhất các file sau:
  - `careergraph-client/src/sections/JobDetail/CtaBanner.jsx`
  - `careergraph-client/src/sections/CVBuilder/components/TemplateSelectionModal.jsx`
  - `careergraph-client/src/pages/CVBuilder.jsx`
  - `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java`
  - `careergraph-api/src/test/java/com/hcmute/careergraph/services/impl/JobServiceImplTest.java`
- Sau khi agent hoàn thành thay đổi tiếp theo, bắt buộc phải viết lại báo cáo markdown mới trong thư mục này để lưu context cho vòng sau.
