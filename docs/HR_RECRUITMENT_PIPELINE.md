# Tùy chỉnh Pipeline Tuyển dụng (HR)

## Mục tiêu
- Mỗi công ty có pipeline riêng nhưng chỉ được dùng các trạng thái hệ thống cho phép.
- Trạng thái chỉ được bật/tắt (soft disable), không xóa hẳn để tránh mất dữ liệu ứng viên.
- Thứ tự pipeline là cấu hình hiển thị, được lưu riêng cho từng công ty.

## Flow tổng quan
### 1) Khi tạo tài khoản HR
- Hệ thống tạo Company.
- Pipeline mặc định được seed tự động dựa trên `ApplicationStage`.

### 2) Khi mở trang cấu hình pipeline
- UI gọi `GET /companies/me/recruitment-stages`.
- Nếu công ty chưa có dữ liệu (company cũ), backend tự seed pipeline mặc định và trả về danh sách.

### 3) Khi HR cập nhật pipeline
- UI gửi toàn bộ danh sách stage + thứ tự + trạng thái bật/tắt.
- Backend validate và lưu lại theo company.

### 4) Khi HR đổi stage ứng viên
- Backend kiểm tra stage đích phải đang bật trong pipeline công ty.
- Nếu tắt, trả lỗi để tránh đưa ứng viên vào trạng thái không hiển thị.

## Các điểm cần lưu ý
- Bắt buộc giữ `APPLIED` và `REJECTED` luôn bật.
- Không thể tắt một stage nếu đang có ứng viên ở stage đó.
- `OFFBOARDED` có thể bật/tắt; nếu tắt thì UI không hiển thị cột này.
- Thứ tự `OFFER_EXTENDED` và `TRIAL` được suy ra từ pipeline; nếu đổi vị trí, logic `offerBeforeTrial` sẽ tự cập nhật.
- API cập nhật yêu cầu gửi đủ tất cả stage hệ thống hỗ trợ (không gửi thiếu).
- Nếu API trả về rỗng do dữ liệu cũ, backend sẽ fallback về pipeline mặc định.

## API chính
- `GET /companies/me/recruitment-stages`
  - Trả về `stage`, `label`, `displayOrder`, `active`, `required`.
- `PUT /companies/me/recruitment-stages`
  - Body: `stages[]` đầy đủ danh sách stage, có `active` và `displayOrder`.

## Giao diện HR
- Route: `/kanbans/pipeline`.
- Cho phép bật/tắt, sắp xếp thứ tự, xem trước danh sách cột.
