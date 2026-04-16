# PHASE 6 Integration QA Report

- Date: 2026-04-16
- Scope: Notification FE + Integration (HR FE TypeScript + Candidate FE JavaScript)
- Source run: careergraph-rtc/scripts/phase6-integration-test.js
- Final result: 8/8 PASS, 0 FAIL

## 1) Scenario Report (Format Chuẩn)

### Scenario 1: HR gửi tin nhắn cho Candidate
- Cách test: Dùng API gửi message từ HR, lắng nghe socket chat + notify của Candidate, sau đó mark-read notification.
- Expected: Tin nhắn realtime đến Candidate, Candidate nhận notification NEW_MESSAGE và navigateTo đúng messages?thread=...
- Actual: Candidate nhận new-message và notification NEW_MESSAGE, navigateTo=/messages?thread=29a0a4b1-922e-4e73-9629-038dd44c8c4e. Mark-read thành công.
- Status: PASS
- Fix: Không cần.

### Scenario 2: Candidate gửi tin nhắn cho HR
- Cách test: Candidate emit typing-start/stop qua chat socket, gửi message qua API, theo dõi chat + notify phía HR.
- Expected: HR thấy typing start/stop, nhận message realtime và notification NEW_MESSAGE điều hướng đúng thread.
- Actual: HR nhận đủ typing-start, typing-stop, new-message và notification NEW_MESSAGE với navigateTo đúng thread.
- Status: PASS
- Fix: Không cần.

### Scenario 3: HR đổi trạng thái ứng tuyển
- Cách test: Dùng API update stage sang INTERVIEW_SCHEDULED, quan sát notification realtime phía Candidate.
- Expected: Candidate nhận notification APPLICATION_INTERVIEW_SCHEDULED realtime, có applicationId để điều hướng.
- Actual: Candidate nhận notification type APPLICATION_INTERVIEW_SCHEDULED, title "🎉 Chúc mừng! Bạn đã được mời phỏng vấn", mapping mở /jobs/applied?applicationId=b90dd80c-b4e7-44ea-9d43-07ae274b63be.
- Status: PASS
- Fix: Không cần.

### Scenario 4: Candidate nộp đơn mới
- Cách test: Candidate apply job thuộc HR chính, theo dõi notify của HR chính và HR phụ cùng lúc.
- Expected: Chỉ HR của job X nhận NEW_APPLICATION, HR job Y không nhận. NavigateTo đúng jobs/{jobId}/applications.
- Actual: HR chính nhận NEW_APPLICATION cho job 90000000-0000-0000-0000-000000000001, HR phụ không nhận event tương ứng trong 5 giây.
- Status: PASS
- Fix: Không cần.

### Scenario 5: Read receipts và unread badges
- Cách test: Candidate rời thread, HR gửi 3 tin qua API, kiểm tra unread rồi candidate mark-read + emit messages-read.
- Expected: Unread tăng 3, candidate có NEW_MESSAGE, mở thread unread về 0 và HR nhận messages-read.
- Actual: Unread sau gửi=3, sau đọc=0. HR nhận messages-read với lastReadMessageId=f21f588a-2d06-4b40-8625-a6787eda53b3.
- Status: PASS
- Fix: Không cần.

### Scenario 6: Reconnection
- Cách test: Disconnect socket candidate, HR gửi tin lúc offline, reconnect candidate rồi fetch lại messages API.
- Expected: Reconnect tự động, không miss tin; retry flow có thể xử lý send failure.
- Actual: Candidate reconnect sau 95ms, fetch thấy message offline id=74b4edf7-bcd8-4f6a-8c28-b2c565a57028. Retry flow mô phỏng thành công.
- Status: PASS
- Fix: Không cần.

### Scenario 7: Multiple tabs
- Cách test: Mở thêm tab 2 (chat + notify) cho cùng tài khoản HR, candidate gửi tin nhắn.
- Expected: Cả 2 tab HR nhận new-message realtime và notification badge event đồng bộ.
- Actual: Tab 1 và tab 2 đều nhận cùng messageId và cùng nhận NEW_MESSAGE notification.
- Status: PASS
- Fix: Không cần.

### Scenario 8: Edge cases
- Cách test: Gửi long message 500 ký tự + emoji + URL, unsend qua API + socket event, test typing auto-stop 3s, tạo burst 100 unread rồi mark-read.
- Expected: Không lỗi với nội dung đặc biệt, unsend phát event đúng, typing tự stop, unread vượt 99 và về 0 sau đọc.
- Actual: Long message=500 ký tự, emoji/url giữ nguyên, unsend event tới HR, thread unread burst=100, sau đọc=0.
- Status: PASS
- Fix: Không cần.

## 2) Đối chiếu DoD trong 00_MASTER_OVERVIEW.md

Tham chiếu: docs/chat_notify/00_MASTER_OVERVIEW.md

1. HR có trang /messages quản lý tất cả hội thoại: ✅
2. Candidate có trang /messages xem tất cả hội thoại: ✅
3. Gửi/nhận tin nhắn realtime < 200ms latency: ❌ (chưa có benchmark latency định lượng <200ms)
4. Typing indicator hiển thị đúng: ✅
5. Read receipt cập nhật realtime: ✅
6. Unread badge cập nhật realtime: ✅
7. Lịch sử chat load đúng, infinite scroll hoạt động: ❌ (chưa có bài test UI xác nhận infinite scroll end-to-end)
8. Notification bell hiển thị đúng count: ✅
9. Click notification điều hướng đúng: ✅
10. Candidate nhận notification khi HR đổi trạng thái hồ sơ: ✅
11. HR nhận notification khi có ứng viên apply job: ✅
12. Cả 2 nhận notification khi có tin nhắn mới: ✅
13. Reconnect socket tự động khi mất kết nối: ✅
14. Không có memory leak: ❌ (chưa có soak test/profile memory)
15. Performance: FCP < 1.5s, không lag khi scroll chat: ❌ (chưa đo FCP/scroll performance)

## 3) Ghi chú môi trường test

- Để chạy được scenario cross-company, local DB đã seed thêm 1 HR phụ cho kiểm thử cách ly notification theo công ty/job:
  - Email: hr2.test@careergraph.vn
  - Company: company-test-hr2
