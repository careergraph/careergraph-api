# Phase 2 Prompt Template - Runtime Sync and Access Consistency

## Purpose

Template này dùng để thực thi `Phase 2`.

Mục tiêu của phase này:

- đồng nhất runtime sync
- đồng nhất public access rule cho detail/search/apply

## Prompt template

```md
Hãy dùng master prompt đính kèm làm khung làm việc chung.

Bây giờ chỉ thực hiện Phase 2: Runtime Sync and Access Consistency.
Không làm sang Phase 3 hoặc Phase 4.

Trước khi sửa code, hãy đọc:
- master prompt
- system description
- phase 2 doc
- report hoặc kết quả implement của Phase 1
- các source files đính kèm

Yêu cầu quan trọng:
- kế thừa toàn bộ policy đã chốt ở Phase 1
- nếu thấy code hiện tại mâu thuẫn với policy của Phase 1, nêu rõ trước khi sửa
- chỉ sửa các phần thuộc runtime path
- chưa làm startup/cron repair sâu của Phase 3

Mục tiêu Phase 2:
- runtime sync dùng một public contract duy nhất
- job không còn public thì bị xóa khỏi public ES index
- job detail public validate đúng rule trước khi side effect metrics/views xảy ra

Kết quả cần có:
- code changes cho runtime sync và access validation
- report md cho Phase 2 gồm:
  - vấn đề
  - root cause
  - thay đổi đã làm
  - cách verify
  - rủi ro còn lại

Acceptance criteria:
- company mất approved hoặc bị blocked thì runtime path gỡ job khỏi ES
- job expired không còn được xử lý như public detail
- views không tăng cho request public bị từ chối
```

## Files to attach

- `careergraph-api/docs/check-sync-job/description/2026-06-22-master-prompt-sync-job-verification.md`
- `careergraph-api/docs/check-sync-job/description/2026-06-22-sync-job-verification-system-description.md`
- `careergraph-api/docs/check-sync-job/pharse/2026-06-22-phase-2-runtime-sync-and-access-consistency.md`
- `careergraph-api/docs/check-sync-job/pharse-report/phase-1-report.md`
- `careergraph-api/docs/check-sync-job/pharse/2026-06-22-phase-1-policy-and-public-contract.md`

## Source files to prioritize

- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/controllers/JobController.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/CompanyAccessPolicyServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/repositories/JobRepository.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobESServiceImpl.java`

## Expected output name suggestion

- `careergraph-api/docs/check-sync-job/pharse-report/phase-2-report.md`

