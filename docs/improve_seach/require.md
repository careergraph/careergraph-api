Hãy đóng vai trò là senior dev 15 + năm kinh nghiệm, 1 nhà thiết kế hệ thống chuẩn enterpirse production. Những yêu cầu nào chưa hợp lý thì đề xuất cái khác nếu chuẩn thì thực hiện
Hay kiểm tra logic lưu trữ dữ liệu Index Candidate,. 
Tôi nói lại luồng hoạt động mà tôi muốn,  HR sẽ tìm kiếm những ứng viên mà khớp nhất với mong muốn. Khi ở trang tìm kiếm ứng viên thì khi chưa nhập từ khóa tìm kiếm thì sẽ tự động lấy các job mà HR đang ứng tuyển làm key word, và khi HR nhập từ khóa tìm kiếm thì sẽ lấy đó làm keyword. 
Về phía map thông tin các nhân của ứng viên
- Sẽ có những trường hợp là ứng viên sẽ cập nhật tiêu chí tìm viêc hoặc chưa cập nhật tiêu chí tìm việc lúc này sẽ null. 
- Ứng viên sẽ upload CV(kiểm tra luồng là BE là lắng nghe event sẽ truncate intent CV, kiểm tra hiện tại là các CV mà ứng viên upload cùng với tiêu chí tìm việc sẽ là keyword để làm các nhân hóa job, và lúc đầu ứng viên chưa nhập từ khóa tìm kiếm (để tham khảo)) trường hợp là ứng viên sẽ bật CV này là dùng để cho HR tìm kiếm. Là sẽ đồng bộ lắng nghe sự kiện là ứng viên sẽ bật CV nào cho phép tìm kiếm, nếu xóa thì remove dữ liệu đi
- Hãy thực hiện sao cho chuẩn enterprise production là sẽ không bị loãng giữa CV và tieu chí tìm việc của ưng viên 
- Và đồng bộ ứng viên khi data ban đầu có, chưa đồng bộ vào elasticsearch hoặc đồng bộ thông tin và chưa đồng bộ CV mà ứng viên bật hoặc ứng viên tắt. 

- Hãy đọc src và thực hiệ sau đó cập nhật thay đổi về nghiệp vụ viết ra file md để sau này tôi có thể xem lại và lưu vào folder này careergraph-api\docs\improve_search. 
- Sau khi thực hiện xong thì tiến hành đóng vai trò là tester để tiến hành kiểm tra chức năng có hoạt động đúng không.