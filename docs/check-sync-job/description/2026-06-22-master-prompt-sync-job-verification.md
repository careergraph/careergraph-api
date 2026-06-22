# Master Prompt for Continuing Sync Job + Verification Work - 2026-06-22

## Master prompt

```md
Bạn hãy đóng vai trò là senior backend engineer, system designer và production reviewer cho dự án CareerGraph.

Mục tiêu:
- Chuẩn hóa public job eligibility và đồng bộ job Elasticsearch theo đúng production contract.
- Chuẩn hóa verification lifecycle của company để không làm sai audit trail và không gây drift cho public search.
- Giữ startup/cron/manual repair chỉ là cơ chế repair, không tạo side effect nghiệp vụ.
- Đảm bảo admin UI, HR flow và backend rule đồng nhất.

Yêu cầu làm việc:
- Đọc kỹ các file đính kèm trước khi sửa.
- Không giả định ngoài source đang có.
- Ưu tiên thay đổi nhỏ, rõ, có test hoặc cách verify cụ thể.
- Nếu có trade-off, ghi rõ theo góc nhìn production.
- Nếu chưa đủ context, tiếp tục đọc source liên quan thay vì trả lời chung chung.

Kết quả cần có:
- code changes nếu phase hiện tại yêu cầu implement lưu ở: careergraph-api/docs/check-sync-job/reports
- báo cáo md ngắn mô tả:
  - vấn đề
  - root cause
  - thay đổi đã làm
  - cách verify
  - rủi ro còn lại
```

## Attach files for all phases

- `careergraph-api/docs/check-sync-job/production-sync-solution-proposal.md`
- `careergraph-api/docs/check-sync-job/company-verification-production-policy.md`
- `careergraph-api/docs/check-sync-job/job-elasticsearch-sync-review.md`
- `careergraph-api/docs/check-sync-job/description/2026-06-22-sync-job-verification-system-description.md`

## Attach files by phase

### Phase 1

- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/CompanyVerificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/enums/company/CompanyVerificationStatus.java`
- `careergraph-admin/src/features/company-verification/pages/VerificationDetailPage.tsx`
- `careergraph-admin/src/features/companies/pages/CompanyDetailPage.tsx`

### Phase 2

- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/CompanyAccessPolicyServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/controllers/JobController.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/repositories/JobRepository.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobESServiceImpl.java`

### Phase 3

- `careergraph-api/src/main/java/com/hcmute/careergraph/config/app/ElasticsearchDataInitializer.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/config/app/ElasticsearchSyncScheduler.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/controllers/InternalElasticsearchSyncController.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/JobNotificationServiceImpl.java`
- `careergraph-api/src/main/java/com/hcmute/careergraph/schedule/DailyDigestScheduler.java`

### Phase 4

- `careergraph-api/src/test/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImplTest.java`
- `careergraph-admin/src/features/dashboard/pages/DashboardPage.tsx`
- `careergraph-admin/src/features/companies/pages/CompanyListPage.tsx`
- `careergraph-hr/src/pages/CompanyVerification/CompanyVerificationPage.tsx`

## Recommended execution order

1. Chốt policy và guard backend trước.
2. Chuẩn hóa runtime public contract.
3. Sửa startup/cron/manual repair cho cùng contract.
4. Bổ sung observability, tests, rollout notes.
