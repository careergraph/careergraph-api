# Sync Job and Verification System Description - 2026-06-22

## Purpose

Tài liệu này mô tả nhanh hệ thống hiện tại cho 2 trục:

- sync job public sang Elasticsearch
- xác thực company và tác động của nó lên public job visibility

## Domain flow hiện tại

### 1. Verification lifecycle

- HR submit hoặc resubmit hồ sơ xác thực company.
- Admin review hồ sơ và ra quyết định.
- Company status thay đổi kéo theo sync lại toàn bộ job của company.

Source chính:

- `careergraph-api/src/main/java/com/hcmute/careergraph/controllers/CompanyVerificationController.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/controllers/CompanyVerificationHistoryController.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/controllers/AdminCompanyController.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/CompanyVerificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/CompanyVerificationMapperSupport.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/persistence/models/CompanyVerificationRequest.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/persistence/models/CompanyVerificationDocument.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/persistence/dtos/request/CompanyVerificationRequests.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/persistence/dtos/response/CompanyVerificationResponses.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/repositories/CompanyVerificationRequestRepository.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/repositories/CompanyVerificationDocumentRepository.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/enums/company/CompanyVerificationStatus.java`

### 2. Public job eligibility

Public availability hiện được ghép từ:

- `job.status`
- `job.expiryDate`
- `company.verificationStatus`
- `company.operationalStatus`

Source chính:

- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/CompanyAccessPolicyServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/CompanyAccessPolicyService.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/controllers/JobController.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/repositories/JobRepository.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/repositories/CompanyRepository.java`

### 3. Runtime Elasticsearch sync

Runtime sync hiện chạy chủ yếu từ mutation path:

- create/update/publish/delete/update settings job
- submit/resubmit verification
- approve/reject/request additional info
- block/unblock company

Source chính:

- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/CompanyVerificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobESServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/repositories/JobESRepository.java`

### 4. Startup / cron repair sync

Repair hiện nằm ở:

- startup initializer
- scheduled sync
- internal manual trigger

Source chính:

- `careergraph-api/src/main/java/com/hcmute/careergraph/config/app/ElasticsearchDataInitializer.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/config/app/ElasticsearchSyncScheduler.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/controllers/InternalElasticsearchSyncController.java`

### 5. Side effects liên quan notification / digest

Reindex hiện có liên quan tới domain side effects của job notification.

Source chính:

- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobNotificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/schedule/DailyDigestScheduler.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/persistence/models/NewlyPostedJob.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/persistence/models/JobNotificationQueue.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/persistence/models/JobNotificationHistory.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/repositories/NewlyPostedJobRepository.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/repositories/JobNotificationQueueRepository.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/repositories/JobNotificationHistoryRepository.java`

## UI / integration surfaces liên quan

### HR

- `careergraph-hr/src/pages/CompanyVerification/CompanyVerificationPage.tsx`
- `careergraph-hr/src/services/companyVerificationService.ts`

### Admin

- `careergraph-admin/src/features/company-verification/pages/VerificationQueuePage.tsx`
- `careergraph-admin/src/features/company-verification/pages/VerificationDetailPage.tsx`
- `careergraph-admin/src/features/companies/pages/CompanyListPage.tsx`
- `careergraph-admin/src/features/companies/pages/CompanyDetailPage.tsx`
- `careergraph-admin/src/features/dashboard/pages/DashboardPage.tsx`
- `careergraph-admin/src/features/company-verification/api/companyVerificationApi.ts`
- `careergraph-admin/src/features/companies/api/adminCompanyApi.ts`

## Current design gaps

1. Runtime sync và startup/cron sync chưa dùng cùng một public contract.
2. Verification lifecycle trước đây cho phép mutate ngược sau khi đã kết luận.
3. Job detail public chưa đồng nhất hoàn toàn với list/search public rules.
4. Reindex path còn dính side effect nghiệp vụ.
5. Chưa có processor riêng cho expired jobs.

## Recommended reading order for future work

1. `production-sync-solution-proposal.md`
2. `company-verification-production-policy.md`
3. `job-elasticsearch-sync-review.md`
4. Các source file ở nhóm verification
5. Các source file ở nhóm runtime sync
6. Các source file ở nhóm startup/cron repair
