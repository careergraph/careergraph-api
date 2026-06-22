# Phase 4 Prompt Template - Observability, Tests, and Rollout

## Purpose

Template này dùng để thực thi `Phase 4`.

Mục tiêu của phase này:

- hardening
- test coverage
- observability
- rollout checklist

## Prompt template

```md
Hãy dùng master prompt đính kèm làm khung làm việc chung.

Bây giờ chỉ thực hiện Phase 4: Observability, Tests, and Rollout.
Không quay lại mở rộng phạm vi implementation của Phase 2 hoặc Phase 3 trừ khi đó là bug blocker để test pass.

Trước khi sửa code, hãy đọc:
- master prompt
- system description
- phase 4 doc
- report hoặc kết quả của Phase 1
- report hoặc kết quả của Phase 2
- report hoặc kết quả của Phase 3
- các source files đính kèm

Yêu cầu quan trọng:
- xem các phase trước là baseline đã chốt
- nếu thấy phase trước còn hở nghiêm trọng, nêu rõ dưới dạng blocker hoặc residual risk
- tập trung vào testability, logability, deployability

Mục tiêu Phase 4:
- bổ sung test cho policy guard và public contract quan trọng
- tăng observability cho sync drift hoặc repair failure
- tạo rollout checklist và rollback notes
- review lại UX/admin implications sau các thay đổi backend

Kết quả cần có:
- test changes nếu phù hợp
- observability/log/report changes nếu phù hợp
- report md cho Phase 4 gồm:
  - test coverage đã thêm hoặc còn thiếu
  - observability improvements
  - rollout checklist
  - rollback notes
  - residual risks

Acceptance criteria:
- có test cho các rule quan trọng nhất
- có cách phát hiện sync failure hoặc drift qua log/metric/report
- có hướng rollout production rõ ràng
```

## Files to attach

- `careergraph-api/docs/check-sync-job/description/2026-06-22-master-prompt-sync-job-verification.md`
- `careergraph-api/docs/check-sync-job/description/2026-06-22-sync-job-verification-system-description.md`
- `careergraph-api/docs/check-sync-job/pharse/2026-06-22-phase-4-observability-tests-and-rollout.md`
- `careergraph-api/docs/check-sync-job/pharse-report/phase-1-report.md`
- `careergraph-api/docs/check-sync-job/pharse-report/phase-2-report.md`
- `careergraph-api/docs/check-sync-job/pharse-report/phase-3-report.md`
- `careergraph-api/docs/check-sync-job/pharse/2026-06-22-phase-1-policy-and-public-contract.md`
- `careergraph-api/docs/check-sync-job/pharse/2026-06-22-phase-2-runtime-sync-and-access-consistency.md`
- `careergraph-api/docs/check-sync-job/pharse/2026-06-22-phase-3-repair-reindex-and-expiry-processing.md`

## Source files to prioritize

- `careergraph-api/src/test/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImplTest.java`
- `các test mới quanh JobServiceImpl hoặc JobController`
- `careergraph-admin/src/features/dashboard/pages/DashboardPage.tsx`
- `careergraph-admin/src/features/companies/pages/CompanyListPage.tsx`
- `careergraph-hr/src/pages/CompanyVerification/CompanyVerificationPage.tsx`

## Expected output name suggestion

- `careergraph-api/docs/check-sync-job/pharse-report/phase-4-report.md`

