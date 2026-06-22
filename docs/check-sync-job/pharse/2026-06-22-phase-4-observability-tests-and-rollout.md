# Phase 4 - Observability, Tests, and Rollout

## Goal

Đưa thay đổi vào trạng thái có thể rollout production an toàn.

## Deliverables

- thêm test cho policy guard và public contract
- thêm log/metric đủ đọc khi sync fail hoặc drift
- chuẩn bị rollout checklist và rollback notes
- review lại admin + HR UX sau khi backend rule đổi

## Main files

- `careergraph-api/src/test/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImplTest.java`
- các test mới quanh `JobServiceImpl` / `JobController`
- `careergraph-admin/src/features/dashboard/pages/DashboardPage.tsx`
- `careergraph-admin/src/features/companies/pages/CompanyListPage.tsx`
- `careergraph-hr/src/pages/CompanyVerification/CompanyVerificationPage.tsx`

## Acceptance

- có ít nhất test policy cho verification lifecycle
- có cách đọc được drift hoặc sync failure từ log/report
- có checklist deploy cho production
