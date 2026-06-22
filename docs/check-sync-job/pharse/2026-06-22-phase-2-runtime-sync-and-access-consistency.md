# Phase 2 - Runtime Sync and Access Consistency

## Goal

Đảm bảo mọi runtime mutation dùng cùng một rule:

- nếu job không còn public thì xóa khỏi public index
- nếu job public thì upsert document

Đồng thời đồng nhất:

- job detail public
- search public
- apply job

## Deliverables

- `syncJobSearchDocument(...)` dùng `isJobPubliclyAvailable(job)` làm rule quyết định delete/upsert
- `JobController.getJobById(...)` validate trước, tăng views sau
- job detail và apply cùng bám public contract

## Main files

- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/controllers/JobController.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/CompanyAccessPolicyServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/repositories/JobRepository.java`

## Acceptance

- company mất approve hoặc bị block thì job bị gỡ khỏi ES runtime path
- job expired không còn được xem như public detail
- metrics views không tăng cho request public bị chặn
