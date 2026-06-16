# CV Template Roadmap And Agent Prompts

## Current priority

Ưu tiên trước mắt đã được triển khai:

1. Chọn template CV ngay từ trang chi tiết công việc.
2. Lấy tất cả CV `ACTIVE` đã upload của ứng viên làm context cho AI suggestion.

Tài liệu này mô tả các phase tiếp theo để mở rộng hệ thống template CV theo hướng enterprise, đẹp hơn, đa dạng hơn, và dễ giao việc cho agent.

## Design direction

### Mục tiêu sản phẩm

- Template phải đủ đẹp để người dùng cảm thấy “đáng dùng”.
- Template phải đủ sạch để ATS không hỏng.
- Có khả năng map template theo nhóm ngành:
  - tech
  - business
  - creative
  - fresher
  - executive
- Về sau cần phân biệt:
  - visual template
  - semantic content strategy
  - export behavior

### Nguồn tham khảo nên nghiên cứu

- Resume.io
- Reactive Resume
- FlowCV
- Canva Resume Templates
- Novoresume
- Overleaf CV templates
- Figma Community:
  - `resume template`
  - `ats resume`
  - `cv builder`

Lưu ý: chỉ tham khảo layout, visual hierarchy, UX flow. Không copy nguyên mẫu thương mại.

## Recommended phases

### Phase 1. Solidify current flow

Mục tiêu:

- hardening flow `JobDetail -> select template -> fetch AI suggestion -> CVBuilder`
- thêm kiểm thử và giảm risk production

Việc nên làm:

1. Redis rate limit atomic bằng increment + expire.
2. Integration test cho `POST /jobs/{jobId}/cv-suggestion`.
3. Frontend E2E hoặc component test cho modal chọn template.
4. Xử lý loading/error state tốt hơn trong builder nếu AI chậm.

Files cần đọc:

- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/FastAPIClientServiceImpl.java`
- `careergraph-client/src/sections/JobDetail/CtaBanner.jsx`
- `careergraph-client/src/sections/CVBuilder/components/TemplateSelectionModal.jsx`
- `careergraph-client/src/pages/CVBuilder.jsx`

Prompt gợi ý cho agent:

```md
Bạn là senior fullstack engineer và tester. Hãy hardening tính năng tạo CV từ Job Detail:

1. Đổi rate limit cv-suggestion sang atomic Redis increment.
2. Viết test backend cho endpoint cv-suggestion.
3. Viết test frontend cho modal chọn template và flow chuyển sang CVBuilder.
4. Không phá flow đang có.

Bắt buộc:
- Đọc trước các file sau:
  - careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java
  - careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/FastAPIClientServiceImpl.java
  - careergraph-client/src/sections/JobDetail/CtaBanner.jsx
  - careergraph-client/src/sections/CVBuilder/components/TemplateSelectionModal.jsx
  - careergraph-client/src/pages/CVBuilder.jsx
- Sau khi làm xong phải chạy test/build tối đa có thể.
- Sau khi làm xong bắt buộc phải viết lại báo cáo markdown mới trong careergraph-api/docs/advan_feature_CV.
```

### Phase 2. Template metadata and template recommendation

Mục tiêu:

- gắn metadata mạnh hơn cho từng template để gợi ý thông minh theo job

Nên thêm metadata:

- `industries`
- `seniority`
- `layoutType`
- `atsScoreHint`
- `supportsPhoto`
- `supportsTwoColumn`
- `recommendedFor`

Ứng dụng:

- modal template có thể highlight:
  - “Phù hợp job này”
  - “ATS-friendly”
  - “Tốt cho fresher”

Files cần đọc:

- `careergraph-client/src/data/templatesConfig.js`
- `careergraph-client/src/sections/CVBuilder/components/TemplateSelectionModal.jsx`
- `careergraph-client/src/pages/JobDetail.jsx`

Prompt gợi ý:

```md
Hãy mở rộng hệ thống template CV theo hướng metadata-driven:

1. Bổ sung metadata cho templatesConfig.
2. Từ job detail, highlight template nào phù hợp với job đang xem.
3. Thiết kế UI badge/recommendation tinh gọn, không rối.

Bắt buộc:
- Giữ tương thích ngược với các template hiện có.
- Không hardcode logic recommendation trong nhiều nơi.
- Sau khi làm xong phải viết lại báo cáo markdown mới trong careergraph-api/docs/advan_feature_CV.
```

### Phase 3. Diverse visual systems

Mục tiêu:

- tăng độ đa dạng thật sự giữa các template
- tránh cảm giác “cùng một layout đổi màu”

Nhóm template nên có:

1. ATS Minimal
2. Modern Tech
3. Executive Premium
4. Creative Portfolio-lite
5. Graduate / Fresher
6. Academic / Research

Nguyên tắc visual:

- mỗi nhóm có typography khác nhau
- hierarchy khác nhau
- spacing system khác nhau
- màu chủ đạo có lý do, không random
- mobile preview và PDF phải ổn

Files cần đọc:

- `careergraph-client/src/sections/CVBuilder/templates/`
- `careergraph-client/src/sections/CVBuilder/components/PdfPreview.jsx`
- `careergraph-client/src/data/templatesConfig.js`

Prompt gợi ý:

```md
Bạn là senior frontend engineer và product designer.
Hãy tạo thêm 2-3 template CV mới có visual language thực sự khác biệt, không chỉ đổi màu.

Yêu cầu:
- Ưu tiên tính đọc tốt trong PDF.
- Ít nhất 1 template tối ưu ATS.
- Ít nhất 1 template cho tech và 1 template cho business/executive.
- Cập nhật templatesConfig và preview đồng bộ.

Bắt buộc:
- Mỗi template phải có mô tả use case rõ ràng.
- Sau khi làm xong phải viết lại báo cáo markdown mới trong careergraph-api/docs/advan_feature_CV.
```

### Phase 4. Template architecture refactor

Mục tiêu:

- tách content model khỏi render model
- dễ scale số lượng template

Hướng làm:

1. Chuẩn hóa `CVDocumentModel`.
2. Thêm adapter layer:
   - AI suggestion -> CVDocumentModel
   - user profile -> CVDocumentModel
   - template render -> CVDocumentModel
3. Tách rendering concerns:
   - typography
   - colors
   - layout slots
   - section blocks

Prompt gợi ý:

```md
Hãy refactor kiến trúc CV Builder để scale nhiều template hơn:

1. Chuẩn hóa document model trung gian.
2. Tách adapter dữ liệu và template renderer.
3. Giữ backward compatibility với flow build-cv hiện tại.

Bắt buộc:
- Trình bày rõ migration path.
- Sau khi làm xong phải viết lại báo cáo markdown mới trong careergraph-api/docs/advan_feature_CV.
```

### Phase 5. AI-assisted template-content pairing

Mục tiêu:

- AI không chỉ sinh content, mà còn gợi ý template phù hợp

Ý tưởng:

- AI trả thêm:
  - `recommendedTemplateIds`
  - `reasoningSummary`
  - `contentTone`
- UI modal ưu tiên hiển thị top 3 template phù hợp nhất

Prompt gợi ý:

```md
Hãy mở rộng flow cv-suggestion để AI có thể gợi ý template phù hợp nhất cho job và hồ sơ ứng viên.

Yêu cầu:
- Không phá response cũ.
- Nếu AI không trả recommendation thì fallback sang rules-based recommendation.
- UI modal hiển thị top recommendation rõ ràng.

Bắt buộc:
- Có fallback strategy.
- Sau khi làm xong phải viết lại báo cáo markdown mới trong careergraph-api/docs/advan_feature_CV.
```

## Suggested execution order

1. Phase 1
2. Phase 2
3. Phase 3
4. Phase 4
5. Phase 5

## Final note for future agents

Mỗi lần triển khai phase mới, bắt buộc:

1. Đọc báo cáo implementation gần nhất trong thư mục này.
2. Đọc các file code liên quan trước khi sửa.
3. Sau khi hoàn thành phải viết lại báo cáo markdown mới:
   - đã làm gì
   - file nào đổi
   - test/build nào đã chạy
   - rủi ro còn lại
