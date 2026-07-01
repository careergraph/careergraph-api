# Báo cáo thay đổi chưa commit - careergraph-api

## Phạm vi rà soát
- Thời điểm rà soát: 2026-07-01
- Repo: `careergraph-api`
- Các file thay đổi:
  - `src/main/java/com/hcmute/careergraph/controllers/JobController.java`
  - `src/main/java/com/hcmute/careergraph/mapper/JobMapper.java`
  - `src/main/java/com/hcmute/careergraph/persistence/dtos/response/JobResponse.java`
  - `src/main/java/com/hcmute/careergraph/services/impl/InterviewServiceImpl.java`

## Tóm tắt nghiệp vụ cho khách hàng
Đợt thay đổi này tập trung vào hai nghiệp vụ chính:

1. Cải thiện hiển thị trạng thái ứng tuyển trong chi tiết việc làm.
Hệ thống tách riêng các trạng thái:
- ứng viên đã từng ứng tuyển,
- hồ sơ hiện có đang bị chặn ứng tuyển lại,
- trạng thái đã lưu việc làm.

Điều này giúp giao diện phía ứng viên hiển thị đúng hơn tình trạng hồ sơ thay vì chỉ có một cờ `isApplied`.

2. Tự động hoàn tất phỏng vấn online sau khi nhà tuyển dụng gửi feedback.
Khi feedback được lưu thành công, backend sẽ đồng bộ:
- trạng thái buổi phỏng vấn sang `COMPLETED`,
- trạng thái tham gia phòng phỏng vấn của ứng viên sang hoàn tất,
- thời điểm rời phòng nếu trước đó chưa được ghi nhận.

## Ảnh hưởng tới nghiệp vụ đang có
- Ảnh hưởng tích cực đến nghiệp vụ ứng tuyển lại: frontend có thể phân biệt "đã từng ứng tuyển" và "đang bị chặn ứng tuyển lại".
- Ảnh hưởng tích cực đến nghiệp vụ phỏng vấn online: giảm tình trạng feedback đã gửi nhưng trạng thái phòng/phỏng vấn vẫn chưa hoàn tất.
- Có thay đổi contract API ở `JobResponse`: thêm `hasApplied` và `reapplyBlocked`. Các frontend consumer cần đồng bộ để tận dụng đầy đủ dữ liệu mới.

## Đánh giá rủi ro
- Không thấy xung đột nghiệp vụ rõ ràng trong phần backend này.
- Rủi ro chính là tương thích dữ liệu với frontend cũ nếu nơi gọi API chưa xử lý 2 trường mới.

## Khuyến nghị commit
- Có thể tách 2 commit:
  - Nhóm 1: đồng bộ trạng thái job apply/reapply.
  - Nhóm 2: tự động hoàn tất interview online sau feedback.

## Gợi ý lệnh commit
```bash
git add src/main/java/com/hcmute/careergraph/controllers/JobController.java src/main/java/com/hcmute/careergraph/mapper/JobMapper.java src/main/java/com/hcmute/careergraph/persistence/dtos/response/JobResponse.java docs/reports/working-tree/2026-07-01-uncommitted-change-review.md
git commit -m "api refine job apply status"
```

```bash
git add src/main/java/com/hcmute/careergraph/services/impl/InterviewServiceImpl.java
git commit -m "api sync interview completion after feedback"
```
