# Phase 1 Report - Policy and Public Contract

## Vấn đề

- verification lifecycle trước đây từng được mô tả là có nguy cơ mutate ngược sau khi đã kết luận
- public contract cho job visibility chưa được chốt rõ trong một doc phase để các phase sau dùng lại
- nếu policy chỉ nằm rải rác trong source thì backend, admin UI và các phase sync sau dễ drift

## Root cause

- source of truth đang phân tán giữa doc tổng, review notes và source code
- Phase 1 file trước đó mới dừng ở checklist ngắn, chưa mô tả state machine và public contract đủ rõ
- verification policy và public eligibility policy có liên hệ chặt, nhưng chưa được gom vào một contract chung theo phase

## Policy chốt lại

### Verification lifecycle

- `PENDING_REVIEW -> APPROVED | REJECTED | NEEDS_ADDITIONAL_INFO`
- `REJECTED -> PENDING_REVIEW` chỉ qua resubmission mới
- `NEEDS_ADDITIONAL_INFO -> PENDING_REVIEW` chỉ qua resubmission mới
- `APPROVED` là trạng thái kết thúc của cùng verification cycle

### Admin action policy

- backend chỉ cho admin ra quyết định review khi request đang `PENDING_REVIEW`
- admin UI chỉ được enable action review khi request đang `PENDING_REVIEW`
- sau khi request đã kết luận, moderation phải đi qua action riêng như `block/unblock company`

### Public job eligibility contract

Một job chỉ public khi đồng thời thỏa:

- `job.status = ACTIVE`
- `expiryDate` chưa qua hoặc không có expiry
- `company.verificationStatus = APPROVED`
- `company.operationalStatus = ACTIVE`

Contract này là chuẩn dùng lại cho:

- search
- detail
- apply
- sync

## Thay đổi đã làm

- cập nhật [2026-06-22-phase-1-policy-and-public-contract.md](/home/theron/Desktop/careergraph/careergraph-api/docs/check-sync-job/pharse/2026-06-22-phase-1-policy-and-public-contract.md:1) để chốt state machine, admin action policy và public eligibility contract
- tạo [phase-1-report.md](/home/theron/Desktop/careergraph/careergraph-api/docs/check-sync-job/pharse-report/phase-1-report.md:1) theo format báo cáo Phase 1

## Đối chiếu source hiện tại

- backend guard đã phù hợp trong [AdminCompanyVerificationServiceImpl.java](/home/theron/Desktop/careergraph/careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImpl.java:247)
- HR resubmission rule đã phù hợp trong [CompanyVerificationServiceImpl.java](/home/theron/Desktop/careergraph/careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/CompanyVerificationServiceImpl.java:67)
- admin UI chỉ cho review khi pending trong [VerificationDetailPage.tsx](/home/theron/Desktop/careergraph/careergraph-admin/src/features/company-verification/pages/VerificationDetailPage.tsx:75)

## Cách verify

1. Đọc backend guard tại `validateDecisionAllowed(...)` để xác nhận chỉ `PENDING_REVIEW` mới được approve/reject/needs-info.
2. Đọc `updateVerification(...)` để xác nhận HR chỉ resubmit từ `REJECTED` hoặc `NEEDS_ADDITIONAL_INFO`.
3. Đọc `canReviewPendingRequest` ở admin UI để xác nhận nút review chỉ khả dụng khi request đang `PENDING_REVIEW`.
4. Dùng Phase 1 policy doc làm source of truth cho các thay đổi runtime/detail/apply/sync ở Phase 2 và Phase 3.

## Rủi ro còn lại

- public contract mới được chốt ở mức policy/doc, nhưng chưa được áp đồng bộ lên toàn bộ read path và sync path trong phạm vi Phase 1
- `CompanyAccessPolicyServiceImpl.isJobPubliclyAvailable(...)` hiện chưa check `job.status` và `expiryDate`, nên Phase 2 vẫn cần chuẩn hóa runtime/read contract
- chưa có automated tests cho policy guard; phần này nên bổ sung ở Phase 4
