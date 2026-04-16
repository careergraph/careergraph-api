Những biến mới và ý nghĩa:

SOCKET_SERVER_URL: URL của RTC Node.js để Spring Boot gọi internal API push notification.
SOCKET_INTERNAL_API_KEY: API key xác thực giữa Spring Boot và RTC internal API.
NOTIFICATION_MESSAGE_COOLDOWN_MINUTES: chống spam NEW_MESSAGE theo thread (trong khoảng này nếu đã có notif unread thì không tạo thêm).
NOTIFICATION_AGGREGATION_WINDOW_MINUTES: gom các lần đổi trạng thái hồ sơ gần nhau thành 1 notification (update notification cũ).
NOTIFICATION_CLEANUP_DAYS_TO_KEEP: số ngày giữ notification trước khi job dọn dữ liệu chạy xóa.
Chú thích đã thêm trong application.yml:

Khối socket: giải thích vai trò URL server và API key internal.
Khối notification.message: giải thích rate limit NEW_MESSAGE.
Khối notification.aggregation: giải thích cơ chế aggregation đổi trạng thái.
Khối notification.cleanup: giải thích retention để cleanup theo lịch.
Lưu ý nhanh:

Các cảnh báo Unknown property trong editor với application.yml là cảnh báo metadata của IDE, không phải lỗi cú pháp YAML của phần mới thêm.
Nếu bạn muốn, mình có thể thêm luôn các biến này vào file .env đang dùng thật trong local để chạy ngay không cần copy