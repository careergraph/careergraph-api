# Báo cáo tách branding mail OTP giữa HR và ứng viên

Ngày: `2026-07-02`
Phạm vi: `careergraph-api`

## Vấn đề

Mail OTP backend đang dùng chung một format mặc định theo ngữ cảnh ứng viên:

1. Lời chào mặc định là `Ứng viên`
2. Subject mặc định là `CareerGraph | Mã xác thực OTP`
3. Khi HR đăng ký, đăng nhập chưa xác thực, quên mật khẩu, đổi email hoặc đổi mật khẩu, mail vẫn mang branding của ứng viên

Điều này làm trải nghiệm chưa đúng production vì người dùng HR nhận mail mang ngữ cảnh sai.

## Cách sửa

1. Mở rộng `MailService.sendOtp(...)` để nhận thêm:
   - `recipientLabel`
   - `platformName`
2. Giữ method cũ để tương thích, nhưng backend auth sẽ chủ động truyền ngữ cảnh đúng.
3. `AuthServiceImpl` hiện tự phân loại:
   - `Role.HR` -> `Nhà tuyển dụng` + `CareerGraph HR`
   - `Role.USER` -> `Ứng viên` + `CareerGraph`
4. Áp dụng cho các luồng OTP:
   - đăng ký
   - resend OTP
   - login khi email chưa xác thực
   - quên mật khẩu
   - đổi email
   - đổi mật khẩu

## Kết quả

1. Ứng viên tiếp tục nhận mail đúng ngữ cảnh `CareerGraph`
2. HR nhận mail đúng ngữ cảnh `CareerGraph HR`
3. Không cần tách thêm template mới, nên thay đổi nhỏ và rủi ro thấp

## Xác minh

Đã compile backend thành công bằng:

```bash
./mvnw -q -DskipTests compile
```
