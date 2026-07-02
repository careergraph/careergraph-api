# Báo cáo đồng bộ mail, OTP và phỏng vấn cho ứng viên

Ngày: `2026-07-02`
Phạm vi: `careergraph-api`

## Mục tiêu

Chuẩn hóa lại mail ứng viên theo tiếng Việt, tách rõ mail cập nhật stage hồ sơ với mail lịch phỏng vấn, cấu hình domain/link qua env + YAML, và loại bỏ các chi tiết giả lập chưa đủ chuẩn production.

## Thay đổi chính

1. Việt hóa nhãn stage tuyển dụng tại `ApplicationStage` để mail và note hiển thị thống nhất theo tiếng Việt.
2. Chuẩn hóa `MailServiceImpl`:
   - Subject OTP, mail stage hồ sơ và mail phỏng vấn đều dùng tiếng Việt.
   - Bỏ hard-code `support@careergraph.com`, `https://careergraph.com/help`, logo URL giả.
   - Đọc cấu hình từ `application.web.base-url`, `application.web.help-center-url`, `application.web.company-address`, `support.email`.
3. Thêm mail phỏng vấn riêng:
   - Mail có lịch phỏng vấn mới.
   - Mail cập nhật lại lịch phỏng vấn.
   - Mail hủy lịch phỏng vấn.
   - Nếu là phỏng vấn online và có `meetingLink`, mail sẽ có nút vào phòng phỏng vấn từ domain client cấu hình qua `WEB_BASE_URL`.
4. Chỉ hiển thị “Trung tâm hỗ trợ” khi `HELP_CENTER_URL` được cấu hình thật. Nếu chưa có trang hỗ trợ bên client thì mail chỉ giữ email hỗ trợ để tránh dẫn link giả.
5. Cập nhật env example và `application.yml` để môi trường có cấu hình production rõ ràng hơn.

## Quyết định thiết kế

1. Không tạo mới trang hỗ trợ bên client trong đợt này.
Lý do: yêu cầu hiện tại ưu tiên sửa lỗi và đồng bộ logic sẵn có với thay đổi tối thiểu.

2. Không để link help center giả trong mail.
Lý do: production chuẩn hơn khi ẩn chức năng chưa tồn tại thay vì phát tán URL không dùng được.

3. Tách mail stage hồ sơ và mail phỏng vấn.
Lý do: stage `INTERVIEW` chỉ thể hiện hồ sơ đã vào giai đoạn phỏng vấn, còn lịch phỏng vấn thật phải có mail riêng kèm link/phương thức tham gia.

## QA đã kiểm tra

1. Backend compile thành công bằng `./mvnw -q -DskipTests compile`.
2. Luồng tạo interview vẫn giữ nguyên transaction hiện có, chỉ bổ sung gửi mail sau khi lưu thành công.
3. Luồng hủy interview gửi mail hủy riêng thay vì dùng chung mail stage.
4. Phỏng vấn online có link phòng dựa trên `WEB_BASE_URL` và `meetingLink`, không hard-code domain.
5. Khi chưa cấu hình `HELP_CENTER_URL`, mail không còn hiển thị link help center rỗng/giả.

## Góc nhìn production / khách hàng khó tính

1. Nội dung mail trước đây đang lẫn Anh - Việt và có logo/help center giả, đây là tín hiệu thiếu hoàn thiện production. Phần này đã được dọn lại.
2. Ứng viên cần phân biệt rõ “hồ sơ sang stage phỏng vấn” và “đã có lịch phỏng vấn cụ thể”. Sau chỉnh sửa, 2 loại thông báo này đã rõ vai trò hơn.
3. Hiện hệ thống admin chưa có bộ phận hỗ trợ ứng viên nội bộ, nên production phù hợp hơn là để email hỗ trợ một chiều, chưa cần dựng support center nếu chưa có backend/process vận hành thật.

## Rủi ro còn lại

1. Chưa có test tự động chuyên biệt cho nội dung template mail.
2. Nếu sau này muốn có support center thật cho ứng viên thì cần thêm route client, nội dung FAQ/help và quy trình tiếp nhận hỗ trợ ở backend/admin, không nên chỉ thêm link giao diện.
