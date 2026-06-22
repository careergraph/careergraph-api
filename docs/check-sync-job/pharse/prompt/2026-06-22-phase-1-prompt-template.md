# Phase 1 Prompt Template - Policy and Public Contract

## Purpose

Template này dùng để thực thi `Phase 1`.

Mục tiêu của phase này:

- chốt policy nghiệp vụ
- chốt public job contract
- chốt guard backend và UI action policy

## Prompt template

```md
Hãy dùng master prompt đính kèm làm khung làm việc chung.

Bây giờ chỉ thực hiện Phase 1: Policy and Public Contract.
Không làm sang Phase 2, Phase 3, hoặc Phase 4.

Trước khi sửa code, hãy đọc kỹ toàn bộ các file đính kèm.
Không giả định ngoài source hiện có.

Mục tiêu Phase 1:
- chốt verification lifecycle theo chuẩn production
- chốt public job eligibility contract
- chốt admin action policy để backend và UI không mâu thuẫn nhau

Yêu cầu:
- đọc source liên quan trước khi sửa
- nếu thấy xung đột policy giữa docs và source, nêu rõ
- chỉ implement các thay đổi thuộc Phase 1
- không đụng startup/cron/reindex sâu của Phase 3 trừ khi bắt buộc để giữ compile

Kết quả cần có:
- code changes cho backend/UI nếu cần
- report md cho Phase 1 gồm:
  - vấn đề
  - root cause
  - policy chốt lại
  - thay đổi đã làm
  - cách verify
  - rủi ro còn lại

Acceptance criteria:
- không cho mutate ngược verification request đã kết luận trong cùng cycle
- admin UI không gợi ý action trái policy
- public contract được mô tả rõ để phase sau dùng lại
```

## Files to attach

- `careergraph-api/docs/check-sync-job/description/2026-06-22-master-prompt-sync-job-verification.md`
- `careergraph-api/docs/check-sync-job/description/2026-06-22-sync-job-verification-system-description.md`
- `careergraph-api/docs/check-sync-job/pharse/2026-06-22-phase-1-policy-and-public-contract.md`
- `careergraph-api/docs/check-sync-job/production-sync-solution-proposal.md`
- `careergraph-api/docs/check-sync-job/company-verification-production-policy.md`
- `careergraph-api/docs/check-sync-job/job-elasticsearch-sync-review.md`

## Source files to prioritize

- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/CompanyVerificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/enums/company/CompanyVerificationStatus.java`
- `careergraph-admin/src/features/company-verification/pages/VerificationDetailPage.tsx`
- `careergraph-admin/src/features/companies/pages/CompanyDetailPage.tsx`

## Expected output name suggestion

- `careergraph-api/docs/check-sync-job/pharse-report/phase-1-report.md`

