# Policy production cho verification và moderation của company

## Mục tiêu

Tài liệu này chốt nguyên tắc nghiệp vụ cho câu hỏi:

> Công ty đã `APPROVED` rồi thì có nên bị `REJECTED` hoặc `NEEDS_ADDITIONAL_INFO` nữa không?

Kết luận theo chuẩn production:

- **Không nên mutate ngược verification request đã `APPROVED` thành `REJECTED` hoặc `NEEDS_ADDITIONAL_INFO`.**

## Lý do nghiệp vụ

`APPROVED` phải là kết thúc của một vòng verification.

Nó có nghĩa:

- hồ sơ ở vòng review đó đã được xác nhận hợp lệ
- company được phép dùng các quyền phụ thuộc verification
- public job visibility có thể được mở ra nếu operational status cũng hợp lệ

Nếu sau này phát hiện rủi ro, sai phạm, hoặc cần rà soát lại, đó là bài toán:

- moderation/enforcement
- hoặc re-verification cycle mới

chứ không phải đổi ngược kết quả của vòng review cũ.

## State machine khuyến nghị

### Verification state

```text
NOT_SUBMITTED
  -> PENDING_REVIEW

PENDING_REVIEW
  -> APPROVED
  -> REJECTED
  -> NEEDS_ADDITIONAL_INFO

REJECTED
  -> PENDING_REVIEW     (thông qua resubmission mới)

NEEDS_ADDITIONAL_INFO
  -> PENDING_REVIEW     (thông qua resubmission mới)

APPROVED
  -> terminal cho vòng review hiện tại
```

### Moderation state

```text
ACTIVE
BLOCKED
SUSPENDED
```

Moderation là trục riêng:

- verification trả lời câu hỏi “đủ điều kiện xác thực chưa”
- moderation trả lời câu hỏi “hiện có được phép hoạt động công khai không”

## Cách xử lý đúng sau khi đã approved

### Trường hợp 1: công ty vi phạm sau khi đã approved

Xử lý đúng:

- đổi `operationalStatus` sang `BLOCKED` hoặc `SUSPENDED`
- sync lại toàn bộ job của company để gỡ khỏi public index

Không nên:

- đổi verification request cũ sang `REJECTED`

### Trường hợp 2: cần yêu cầu doanh nghiệp cập nhật lại hồ sơ pháp lý

Xử lý đúng:

- tạo một đợt `re-verification` mới
- có thể thêm cờ như `verificationRecheckRequired = true`
- hoặc mở một request verification mới với lifecycle riêng

Không nên:

- đổi request đã approved thành `NEEDS_ADDITIONAL_INFO`

### Trường hợp 3: admin phê duyệt nhầm

Xử lý đúng theo production:

- cần audit trail rõ
- có action vận hành chuyên biệt như `revoke approval` hoặc `start re-review`
- action này phải có rule riêng, audit riêng, và side effect rõ ràng

Không nên:

- dùng chung endpoint `reject` hiện tại để đảo ngược approved

## Phát hiện trên source hiện tại

Hiện backend đang cho phép admin set thẳng:

- `APPROVED -> REJECTED`
- `APPROVED -> NEEDS_ADDITIONAL_INFO`

Điểm chứng:

- `src/main/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImpl.java:212-242`

Admin UI cũng đang mở nút:

- `Từ chối`
- `Yêu cầu bổ sung`

ngay cả khi record đã `APPROVED`.

Điểm chứng:

- `src/features/company-verification/pages/VerificationDetailPage.tsx:145-177`

Trong khi phía company lại không thể submit verification mới nếu company đã `APPROVED`.

Điểm chứng:

- `src/main/java/com/hcmute/careergraph/services/impl/CompanyVerificationServiceImpl.java:131-138`

Đây là bất nhất nghiệp vụ.

## Quy tắc implement khuyến nghị

### Backend

`approve/reject/requestAdditionalInfo` chỉ được phép trên request đang:

- `PENDING_REVIEW`

Nếu request đang:

- `APPROVED`
- `REJECTED`
- `NEEDS_ADDITIONAL_INFO`

thì phải từ chối với `400 Bad Request` hoặc `409 Conflict`.

### Admin UI

Khi request đã `APPROVED`:

- ẩn hoặc disable nút `Reject`
- ẩn hoặc disable nút `Request Additional Info`
- chỉ còn action moderation như `Block company`

### Re-verification

Nếu business cần kiểm tra lại company đã approved:

- thêm luồng tạo request mới
- request mới đi vào `PENDING_REVIEW`
- request cũ giữ nguyên lịch sử `APPROVED`

## Tác động đến Elasticsearch

Policy này giúp đồng bộ ES sạch hơn vì:

- verification cycle không bị mutate ngược tùy tiện
- public visibility sau approve chủ yếu bị ảnh hưởng bởi `operationalStatus`
- enforcement event rõ ràng hơn, dễ sync hơn

Thực tế production nên là:

- `APPROVED + ACTIVE` => jobs có thể public
- `APPROVED + BLOCKED/SUSPENDED` => jobs bị gỡ khỏi public index
- `REJECTED/NEEDS_ADDITIONAL_INFO` => jobs không public

## Kết luận

Theo chuẩn production:

- `APPROVED` không nên bị đổi ngược thành `REJECTED` hoặc `NEEDS_ADDITIONAL_INFO` trên cùng verification cycle.
- Nếu company có vấn đề sau khi đã approved, xử lý bằng `BLOCKED/SUSPENDED` hoặc mở vòng `re-verification` mới.

Đây là policy nên chốt trước khi tiếp tục sửa code đồng bộ Elasticsearch, vì rule index public phụ thuộc trực tiếp vào state machine nghiệp vụ này.
