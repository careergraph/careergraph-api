Bạn đóng vai trò là Senior Software Architect 15 năm kinh nghiệm về Spring Boot, React, System Design và UML Sequence Diagram.

Nhiệm vụ:
Phân tích source code thực tế của project CareerGraph để vẽ UML Sequence Diagram cho chức năng:

[TÊN_CHỨC_NĂNG]

Tôi có đính kèm 2 file XML mẫu đã vẽ:
1. Đăng ký
2. Xác thực email

Hãy dùng 2 file này làm reference chính về:
- layout
- spacing
- lifeline style
- activation box
- alt block
- dashed return arrow
- destroy marker
- cách đặt label tiếng Việt
- mức độ chi tiết

Nguyên tắc bắt buộc:
- Không tự bịa flow.
- Không dùng generic flow.
- Phải đọc source code thật trước khi vẽ.
- Sequence phải phản ánh đúng logic hiện có.
- Nếu chưa xác minh được đoạn nào, thêm:
  <!-- TODO: verify actual implementation -->

Cách đọc source:
Ưu tiên đọc Backend:
- Controller
- Service
- Repository
- Entity
- DTO
- Security/JWT nếu có
- Redis nếu có
- Email/Notification nếu có
- Scheduler/Event Listener nếu có
- Exception handling

Chỉ đọc Frontend nếu chức năng có:
- modal workflow
- multi-step form
- upload file
- Kanban drag-drop
- WebSocket/WebRTC
- calendar interaction
- retry/polling/navigation logic

Quy tắc tách sequence:
Nếu một chức năng lớn có gọi lại một chức năng dùng chung, KHÔNG vẽ lại toàn bộ flow dùng chung trong diagram chính.

Ví dụ:
- Đăng ký cần xác thực email
- Quên mật khẩu cần xác thực email
- Đổi email cần xác thực email

Thì:
1. Vẽ flow chính riêng.
2. Vẽ flow dùng chung riêng.
3. Trong flow chính chỉ dùng ref/call để gọi flow dùng chung.

Ví dụ:
- register-sequence.drawio
- forgot-password-sequence.drawio
- email-verification-sequence.drawio

Trong register-sequence chỉ cần thể hiện:
ref: Xác thực email

Không duplicate toàn bộ email verification flow.

Khi nào cần tách flow riêng:
- Flow được nhiều chức năng sử dụng lại
- Flow quá dài làm diagram chính khó đọc
- Flow có business logic riêng
- Flow có alt/error riêng
- Flow có Redis/Email/Token xử lý riêng
- Flow có async processing

Các flow nên tách riêng nếu có:
- Xác thực email
- Refresh token
- Validate JWT
- Gửi notification/email
- Upload file
- AI analyze
- WebSocket/WebRTC signaling
- Payment callback nếu có

Mức độ chi tiết:
- Không quá đơn giản
- Không quá chi tiết đến từng dòng code
- Chỉ giữ business core
- Có validate chính
- Có database/cache/external service nếu source có
- Có alt block cho success/error chính

Output:
- Tạo file Draw.io XML hoàn chỉnh
- Có thể import trực tiếp vào diagrams.net
- Lưu vào:
  careergraph-api/docs/sequence

Tên file:
[TÊN_CHỨC_NĂNG_DẠNG_KEBAB_CASE]-sequence.drawio

Nếu có flow dùng chung:
- tạo thêm file riêng cho flow dùng chung
- trong flow chính dùng ref để gọi flow đó

Sau khi tạo:
- Chỉ báo tên file đã tạo
- Không giải thích dài