# Phase 1 - Policy and Public Contract

## Goal

Chốt source of truth cho:

- verification lifecycle
- public job eligibility
- admin action policy

## Policy đã chốt

### 1. Verification lifecycle

Verification state machine của một review cycle:

```text
NOT_SUBMITTED
  -> PENDING_REVIEW

PENDING_REVIEW
  -> APPROVED
  -> REJECTED
  -> NEEDS_ADDITIONAL_INFO

REJECTED
  -> PENDING_REVIEW     (chỉ qua resubmission mới)

NEEDS_ADDITIONAL_INFO
  -> PENDING_REVIEW     (chỉ qua resubmission mới)

APPROVED
  -> terminal cho review cycle hiện tại
```

Các nguyên tắc bắt buộc:

- không mutate ngược request đã kết luận trong cùng cycle
- không cho `APPROVED -> REJECTED`
- không cho `APPROVED -> NEEDS_ADDITIONAL_INFO`
- nếu cần rà soát lại công ty đã `APPROVED`, phải dùng moderation hoặc mở cycle mới

### 2. Admin action policy

Rule cho admin:

- `approve`
- `reject`
- `request additional info`

chỉ hợp lệ khi request đang `PENDING_REVIEW`.

Khi request đã ở một trạng thái kết luận:

- UI không được gợi ý review action trái policy
- backend phải từ chối request mutate ngược
- admin chỉ còn các action moderation độc lập như `block company` hoặc `unblock company`

### 3. Public job eligibility contract

Một job được coi là public khi và chỉ khi thỏa toàn bộ:

- job tồn tại
- `job.status = ACTIVE`
- `expiryDate` chưa qua, hoặc không có expiry
- company tồn tại
- `company.verificationStatus = APPROVED`
- `company.operationalStatus = ACTIVE`

Contract này là source of truth mục tiêu cho:

- public search
- public job detail
- apply job
- runtime sync sang Elasticsearch
- startup/cron/manual repair ở các phase sau

Tên helper nên dùng nhất quán ở các phase sau:

- `isJobPubliclyAvailable(job)`

## Hiện trạng source liên quan

- backend đã khóa quyết định review về `PENDING_REVIEW` trong `AdminCompanyVerificationServiceImpl`
- HR chỉ được resubmit khi request cũ là `REJECTED` hoặc `NEEDS_ADDITIONAL_INFO`
- admin UI detail page chỉ enable action review khi request đang `PENDING_REVIEW`
- public contract ở mức hệ thống đã được chốt trong doc này để Phase 2 và Phase 3 dùng lại

## Deliverables

- khóa backend không cho mutate review request đã kết luận
- admin UI chỉ cho review khi request đang `PENDING_REVIEW`
- mô tả rõ public contract dùng cho search, detail, apply, sync

## Main files

- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/CompanyVerificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/CompanyAccessPolicyServiceImpl.java`
- `careergraph-admin/src/features/company-verification/pages/VerificationDetailPage.tsx`
- `careergraph-admin/src/features/companies/pages/CompanyDetailPage.tsx`

## Acceptance

- không thể `APPROVED -> REJECTED`
- không thể `APPROVED -> NEEDS_ADDITIONAL_INFO`
- admin UI không gợi ý sai action
- policy được ghi rõ trong docs/report

## Out of scope của Phase 1

- chưa sửa sâu runtime sync, startup sync, cron sync, repair flow
- chưa xử lý expired job processor
- chưa thêm test/observability rollout của Phase 4
