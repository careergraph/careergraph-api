# Admin Production Audit - Verification UI and Candidate Search Access

## Phạm vi

- `careergraph-admin`:
  - dashboard quản trị
  - danh sách yêu cầu xác thực
  - chi tiết yêu cầu xác thực
  - danh sách doanh nghiệp
- `careergraph-api`:
  - policy truy cập tính năng tìm kiếm ứng viên của công ty
  - controller search ứng viên

## Kết quả chính

- đã chuẩn hóa lại nhiều text tĩnh theo văn phong tiếng Việt formal, ngắn gọn và phù hợp hơn với màn hình quản trị production
- đã cải thiện responsive cho:
  - danh sách yêu cầu xác thực
  - danh sách doanh nghiệp
  - bộ lọc admin ở hai màn trên
- đã khóa backend để công ty chưa `APPROVED` hoặc không `ACTIVE` không thể sử dụng tính năng tìm kiếm ứng viên

## Các thay đổi đã thực hiện

### 1. Admin copy/UI

- thay các label mang tính nội bộ hoặc hơi "demo" như:
  - `Sự cố chính sách` -> `Cảnh báo tuân thủ`
  - `Điểm tích hợp hàng đợi` -> `Danh sách chờ xử lý`
  - `Điểm tích hợp chế tài` -> `Điều phối doanh nghiệp`
- cập nhật lại mô tả ở dashboard, queue page, detail page và company list để:
  - rõ nghiệp vụ hơn
  - tránh wording quá máy móc
  - bám ngữ cảnh vận hành và xét duyệt

### 2. Responsive

- chuyển hai màn table nặng dữ liệu sang mô hình:
  - desktop/tablet lớn: bảng
  - mobile: card list
- giữ nguyên pagination và action chính trên mobile để không mất chức năng
- bộ lọc được đổi sang dạng control bar nhiều hàng, co giãn tốt hơn ở breakpoint nhỏ

### 3. Candidate search access policy

- trước khi sửa, endpoint tìm kiếm ứng viên mới chỉ lấy `companyId` từ session rồi search
- sau khi sửa, backend bắt buộc:
  - `company.verificationStatus = APPROVED`
  - `company.operationalStatus = ACTIVE`
- nếu chưa đủ điều kiện, request bị chặn tại controller bằng policy service

## Verify đã chạy

### Backend

Lệnh:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH ./mvnw -Dtest=CompanyAccessPolicyServiceImplTest,CandidateSuggestionControllerTest test
```

Kết quả:

- `BUILD SUCCESS`
- `Tests run: 7, Failures: 0, Errors: 0`

### Admin

Lệnh:

```bash
npm run build
```

Kết quả:

- build thành công
- có cảnh báo môi trường Node hiện tại là `18.20.8`, trong khi Vite khuyến nghị `20.19+` hoặc `22.12+`
- dù vậy artifact vẫn được build thành công ở phiên hiện tại

## Đánh giá như một senior test

- rule production quan trọng nhất cho candidate search đã được khóa ở backend, phù hợp hơn nhiều so với chỉ chặn ở UI
- các màn admin chính đã dễ đọc hơn và ít mang cảm giác "tool nội bộ tạm thời"
- responsive đã đủ dùng ở mức production nội bộ trên mobile/tablet

## Đánh giá như người dùng khó tính

- phần filter trước đây hơi phẳng và thiếu thứ bậc; bản mới đã rõ khối thao tác hơn
- mobile trước đây phụ thuộc scroll ngang bảng; bản mới dễ dùng hơn đáng kể
- vẫn còn cơ hội cải thiện thêm ở các badge/trạng thái nếu muốn đồng bộ toàn admin app về cùng một hệ ngôn ngữ

## Residual risks

- hiện mới rà soát sâu các màn admin liên quan verification/doanh nghiệp, chưa phải toàn bộ `careergraph-admin`
- cảnh báo chunk size và warning version Node chưa chặn build, nhưng nên được dọn ở phase tối ưu frontend riêng
