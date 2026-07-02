Bạn hãy đóng vai trò là senior dev 15 + năm kinh nghiệm và nhà thiết kê hệ thống chuẩn production hãy thực hiện. Bạn hãy đọc src để đúng ngữ cảnh. 

Bây giờ các logic hiện tại đã oke rồi, bây giờ tôi muốn có 1 trường hợp là AI sẽ lọc bị sai xót nên tôi muốn là HR có thể xem lại hồ sơ ứng viên và có thể chuyển về hồ sơ về sàng lọc hoặc là ứng tuyển gì đó thì pipline tuyển dụng có thể thay đổi. nói cách khác là vẫn cho HR thực hiện trở về stage( mà phải phù hợp chủân prodution).  Còn HR thì sẽ nhấn từ chối ứng viên thì không thể roll back thôi.  Và thêm popup xác nhận khi HR nhấn từ chối ứng viên nữa. 



Hãy sửa ít nhất có thể chỉ sửa những cái lỗi hiện tại chuẩn logic như hiện tại đang làm
Bạn hãy đóng vai trò là senior test để kiểm tra lại các chức năng đã hoạt động đúng chủân chưa. 
Và đóng vai trò là khách hàng khó tính kiểm tra lại UI UX thuận tiện chưa production chaư
và viết báo cáo vào folder bằng md có thể chia cấp folder con nữa để quản lý
careergraph-api/docs/reports
careergraph-hr/docs/reports
careergraph-client/docs/reports
careergraph-rtc/docs/reports
Đặt tên file có ngày và thể hiện file nó chứa nội dung gì hay fix gì cái nào không ảnh hưởng thì không cần viết report(chỉ viết khi có code thay đổi)



Bạn hãy đóng vai trò là senior dev 15 + năm kinh nghiệm và nhà thiết kê hệ thống chuẩn production hãy thực hiện. Bạn hãy đọc src để đúng ngữ cảnh. 

Hãy đọc src toàn bộ src code liên quan là lên plan nên chia bao nhiêu pharse để thực hiện chức năng sync job và xác thực có mô tả yêu cầu trong : careergraph-api/docs/check-sync-job
Các pharse lưu vào : careergraph-api/docs/check-sync-job/pharse
Đồng thời viết file md mô tả hiện thống là đường dẫn các file liên quan đến chức năng này và lưu ở: careergraph-api/docs/check-sync-job/description 

Đặt tên file theo tưng pharse đồng thời cung cấp cho tôi master prompt và nên đính kèm những file nào để tôi có thể đến từ file và kèm theo prompt và kèm file để tiếp tục thực hiện 