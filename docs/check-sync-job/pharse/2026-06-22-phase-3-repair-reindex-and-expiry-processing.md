# Phase 3 - Repair, Reindex, and Expiry Processing

## Goal

Biến startup/cron/manual sync thành repair mechanism đúng nghĩa, không gây side effect nghiệp vụ.

## Deliverables

- `ElasticsearchDataInitializer` dùng cùng public contract với runtime
- content hash đủ rộng cho search relevance và visibility
- bỏ side effect domain event khỏi reindex path
- thêm expired-job processor riêng

## Main files

- `careergraph-api/src/main/java/com/hcmute/careergraph/config/app/ElasticsearchDataInitializer.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/config/app/ElasticsearchSyncScheduler.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/controllers/InternalElasticsearchSyncController.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobNotificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/schedule/DailyDigestScheduler.java`

## Acceptance

- startup/cron không index lại job không còn public
- repair không tạo lại `JobCreatedEvent`
- expired jobs được làm sạch định kỳ dù runtime miss
