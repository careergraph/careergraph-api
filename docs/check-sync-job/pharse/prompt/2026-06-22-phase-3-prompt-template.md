# Phase 3 Prompt Template - Repair, Reindex, and Expiry Processing

## Purpose

Template này dùng để thực thi `Phase 3`.

Mục tiêu của phase này:

- chuẩn hóa startup/cron/manual repair
- bỏ side effect nghiệp vụ khỏi reindex
- bổ sung xử lý expired jobs

## Prompt template

```md
Hãy dùng master prompt đính kèm làm khung làm việc chung.

Bây giờ chỉ thực hiện Phase 3: Repair, Reindex, and Expiry Processing.
Không làm sang Phase 4.

Trước khi sửa code, hãy đọc:
- master prompt
- system description
- phase 3 doc
- report hoặc kết quả của Phase 1
- report hoặc kết quả của Phase 2
- các source files đính kèm

Yêu cầu quan trọng:
- kế thừa toàn bộ policy và public contract đã chốt ở các phase trước
- startup/cron/manual sync phải được coi là repair path, không phải business path
- nếu phát hiện side effect domain event trong reindex path, phải chỉ rõ

Mục tiêu Phase 3:
- startup/cron/manual repair dùng cùng public eligibility contract với runtime
- content hash đủ rộng để repair drift thật sự
- reindex không tạo side effect như job mới đăng hoặc digest side effect
- có hướng xử lý expired jobs định kỳ

Kết quả cần có:
- code changes cho repair path
- report md cho Phase 3 gồm:
  - vấn đề
  - root cause
  - thay đổi đã làm
  - cách verify
  - rủi ro còn lại
  - lưu ý rollout

Acceptance criteria:
- startup/cron không tạo lại public docs sai
- repair path không publish domain event nghiệp vụ
- có cơ chế làm sạch expired jobs rõ ràng
```

## Files to attach

- `careergraph-api/docs/check-sync-job/description/2026-06-22-master-prompt-sync-job-verification.md`
- `careergraph-api/docs/check-sync-job/description/2026-06-22-sync-job-verification-system-description.md`
- `careergraph-api/docs/check-sync-job/pharse/2026-06-22-phase-3-repair-reindex-and-expiry-processing.md`
2026-06-22-phase-2-runtime-sync-and-access-consistency.md`
- `careergraph-api/docs/check-sync-job/pharse-report/phase-1-report.md`
- `careergraph-api/docs/check-sync-job/pharse-report/phase-2-report.md`
- `careergraph-api/docs/check-sync-job/pharse/2026-06-22-phase-1-policy-and-public-contract.md`
- `careergraph-api/docs/check-sync-job/pharse/2026-06-22-phase-2-runtime-sync-and-access-consistency.md`

## Source files to prioritize

- `careergraph-api/src/main/java/com/hcmute/careergraph/config/app/ElasticsearchDataInitializer.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/config/app/ElasticsearchSyncScheduler.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/controllers/InternalElasticsearchSyncController.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobNotificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/schedule/DailyDigestScheduler.java`

## Expected output name suggestion

- `careergraph-api/docs/check-sync-job/pharse-report/phase-3-report.md`

