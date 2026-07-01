# CareerGraph Interview Module - Production Business Specification

Ngày cập nhật: 2026-07-01

Đường dẫn tài liệu:

- `careergraph-api/docs/reports/2026-07-01-interview-module-production-business-spec.md`

## 1. Mục tiêu chức năng

Module phỏng vấn hỗ trợ HR vận hành các buổi phỏng vấn theo 2 hình thức:

- `ONLINE`
- `OFFLINE`

Tài liệu này mô tả rule nghiệp vụ đang được chốt theo hướng production cho luồng phỏng vấn online, đặc biệt với mô hình:

- `1 room / 1 job / 1 ngày`
- Nhiều ứng viên của cùng job trong cùng ngày dùng chung một room
- Nhưng tại một thời điểm chỉ có `1 HR side session + 1 active candidate` được xử lý trực tiếp trong room UI

## 2. Kiến trúc nghiệp vụ tổng quát

### 2.1. Đối tượng chính

- `Interview`
  - đại diện cho một lịch phỏng vấn của một ứng viên
  - gắn với `applicationId`, `candidateId`, `jobId`
- `InterviewRoom`
  - đại diện cho room online dùng chung cho một `job` trong một `ngày`
- `RoomParticipant`
  - đại diện cho slot của một ứng viên trong room
  - chứa `applicationId`, `candidateId`, `slotStart`, `slotEnd`, `admitStatus`, `joinedAt`, `leftAt`
- `InterviewRecording`
  - đại diện cho một bản ghi video
  - hiện đã lưu theo:
    - `interviewId`
    - `roomParticipantId` (mới bổ sung)

### 2.2. Ý nghĩa của shared room

Một room không đồng nghĩa một cuộc phỏng vấn duy nhất.

Room là không gian vận hành online trong ngày cho một job. Mỗi ứng viên vẫn có:

- interview riêng
- trạng thái riêng
- feedback riêng
- recording riêng

Nói cách khác:

- room là container vận hành
- interview là đơn vị nghiệp vụ

## 3. Rule truy cập room

### 3.1. HR

HR không bị giới hạn vào room trước giờ phỏng vấn trong cùng ngày.

Rule:

- HR có thể vào room bất kỳ lúc nào trong `đúng ngày phỏng vấn`
- Nếu room thuộc ngày trước đó thì coi là hết hiệu lực, không cho truy cập tiếp

Ý nghĩa production:

- HR có thể vào sớm để kiểm tra camera/mic
- HR có thể vào giữa ngày để xử lý nhiều ứng viên cùng room
- HR có thể quay lại room nếu đang vận hành dở

### 3.2. Candidate

Candidate chỉ được truy cập room từ `15 phút trước scheduledAt` của chính interview đó.

Rule:

- trước mốc `scheduledAt - 15 phút`: không cho vào
- từ mốc `scheduledAt - 15 phút` đến hết thời lượng phiên: được phép vào
- sau khi bị kick: candidate phải xin admit lại
- reload khi chưa bị kick: không cần admit lại

### 3.3. Room cũ

Room của ngày trước được coi là kết thúc nghiệp vụ truy cập.

## 4. Rule room online theo ngày

### 4.1. Một room cho một job trong một ngày

Nếu cùng `jobId` và cùng `interviewDate`, hệ thống tái sử dụng một `roomCode`.

Điều này có nghĩa:

- 5 ứng viên của job A trong ngày 2026-07-01 có thể cùng dùng room `RM-...`
- nhưng không có nghĩa 5 người được active cùng lúc trong UI

### 4.2. Một ứng viên active tại một thời điểm

Đây là rule rất quan trọng của phiên bản hiện tại.

Room có thể chứa nhiều ứng viên theo lịch trong ngày, nhưng tại một thời điểm:

- chỉ được phép có `1 candidate` ở trạng thái `ADMITTED / active call`
- UI hiện tại được thiết kế đúng cho mô hình `1 HR + 1 candidate active`

Rule này hiện được enforce ở:

- backend service
- RTC signaling
- HR UI

Lý do:

- tránh sai peer connection
- tránh nhầm recording
- tránh HR nhìn một UI 1-1 nhưng dữ liệu thực tế lại có 2 candidate cùng active

## 5. Rule participant trong room

### 5.1. Trạng thái participant

`RoomParticipant.admitStatus` có các giá trị chính:

- `PENDING`
- `WAITING_LOBBY`
- `ADMITTED`
- `REJECTED`
- `REMOVED`
- `COMPLETED`

Lưu ý production hiện tại:

- `ADMITTED` là trạng thái quyết định ứng viên đang giữ `active session`
- thao tác `Hoàn thành interview` không được tự hiểu là ứng viên đã rời room
- `leftAt` chỉ nên phản ánh lúc session trong room thực sự kết thúc

### 5.2. Luồng bình thường

1. Candidate truy cập room trong khoảng thời gian hợp lệ
2. Candidate gửi yêu cầu vào room
3. HR nhìn thấy join request
4. HR bấm admit
5. Participant chuyển sang `ADMITTED`
6. `joinedAt` được ghi nhận
7. Interview tương ứng có thể auto chuyển `IN_PROGRESS`

### 5.3. Reload

Nếu candidate đã được admit và chỉ reload trình duyệt:

- không cần HR admit lại
- signaling cho phép rejoin

Ý nghĩa production:

- reload/mất kết nối ngắn hạn không được coi là kết thúc phiên thật sự
- active slot vẫn được giữ tạm cho chính candidate đó
- HR không nên admit người tiếp theo chỉ vì candidate vừa F5 hoặc chập mạng ngắn hạn

### 5.4. Kick

Nếu HR kick candidate:

- participant chuyển sang `REMOVED`
- kết nối realtime bị cắt
- candidate muốn vào lại phải gửi yêu cầu mới
- HR phải admit lại

### 5.5. Leave session chủ động

Nếu candidate bấm `Rời phòng` một cách chủ động:

- active realtime session phải được giải phóng ngay
- candidate không bị đánh dấu `REMOVED`
- nếu interview chưa complete, participant quay về trạng thái `WAITING_LOBBY`
- nếu interview đã complete, participant chuyển sang `COMPLETED`
- nếu candidate muốn quay lại sau khi đã leave thật sự, candidate phải gửi yêu cầu vào lại để HR xem xét admit

Ý nghĩa production:

- phân biệt rõ `leave thật sự` với `reload`
- không để active slot bị kẹt khi HR cần chuyển sang ứng viên tiếp theo
- vẫn giữ được audit trail `joinedAt/leftAt`

## 6. Rule hoàn thành phỏng vấn

### 6.1. Online

Phỏng vấn online chỉ được `complete` khi ứng viên đó đã thực sự vào room.

Điều kiện thực thi:

- interview status thuộc nhóm cho phép complete
- `RoomParticipant.joinedAt != null` cho đúng ứng viên đó

Điểm quan trọng:

- hệ thống không chỉ dựa vào `scheduledAt`
- nếu candidate chưa từng vào room thì không được complete online

### 6.2. Early handling

HR có thể hoàn thành nghiệp vụ trong khoảng `15 phút trước giờ hẹn` nếu candidate đã vào room.

Đây là chủ đích business:

- không cần chờ đúng phút scheduled mới bấm complete
- phù hợp cho các buổi phỏng vấn diễn ra sớm một chút

### 6.3. Offline

Offline không phụ thuộc room participation.

Logic offline chủ yếu đi theo trạng thái interview và mốc thời gian.

### 6.4. Complete không đồng nghĩa out room

Đây là rule nghiệp vụ cần tách rất rõ.

Khi HR bấm `Hoàn thành`:

- chỉ `Interview.interviewStatus` chuyển sang `COMPLETED`
- room không tự đóng
- candidate không bị kick tự động
- HR vẫn có thể ở lại room để:
  - đánh giá
  - gán recording
  - kiểm tra lại thông tin sau buổi phỏng vấn

Ý nghĩa production:

- `complete interview` là chốt nghiệp vụ tuyển dụng cho ứng viên đó
- `leave room / kick / close room` là chốt phiên realtime
- đây là 2 hành động khác nhau và không được trộn

### 6.5. Ứng viên cuối cùng sau khi complete

Khi HR complete ứng viên cuối cùng:

- room vẫn phải được giữ nguyên cho tới khi HR chủ động `đóng room` hoặc `kết thúc phiên`
- không được tự out candidate hoặc tự đóng room chỉ vì interview đã completed

Lý do:

- HR có thể chưa đánh giá xong
- HR có thể vừa stop record và cần gán clip đúng ứng viên
- HR có thể cần xác nhận lại nội dung với ứng viên trước khi kết thúc hẳn session

## 7. Rule feedback

### 7.1. Rule mới đã chốt

Feedback chỉ được phép sau khi interview đã `COMPLETED`.

Điều này áp dụng cho cả:

- frontend
- backend

### 7.2. Ý nghĩa production

Rule này ngăn các lỗi vận hành sau:

- HR đánh giá khi buổi phỏng vấn chưa kết thúc
- HR đánh giá nhầm ứng viên đang active trong room chung
- dữ liệu pipeline bị cập nhật sớm trước khi operator xác nhận complete

### 7.3. Online

Online muốn feedback thì phải đi qua chuỗi:

1. candidate đã vào room
2. HR complete interview
3. sau đó mới feedback

## 8. Rule recording

### 8.1. Mục đích

Recording dùng để lưu video của buổi phỏng vấn và gắn nó về đúng ứng viên/interview.

### 8.2. Rule mới đã chốt

Recording không chỉ nên gắn theo `interviewId`.

Phiên bản hiện tại đã bổ sung thêm:

- `roomParticipantId`

Điều này giúp xác định rõ:

- clip thuộc candidate nào trong room
- clip được gán theo slot nào

### 8.3. Vì sao cần `roomParticipantId`

Trong shared room theo ngày:

- nhiều interview khác nhau cùng dùng một roomCode
- nếu chỉ lưu `interviewId`, hệ thống vẫn phụ thuộc nhiều vào chọn tay của HR

Khi có `roomParticipantId`:

- recording biết candidate/slot cụ thể
- backend có thể validate participant đó có đúng interview không
- detail page có thể hiển thị clip đã gán cho ai

### 8.4. Flow recording chuẩn

1. HR record trong room
2. HR stop record
3. video upload thành công
4. modal gán recording mở ra
5. danh sách chỉ hiển thị ứng viên đã thực sự vào room (`joinedAt != null`)
6. nếu có candidate đang `ADMITTED`, modal ưu tiên preselect candidate active đó
7. khi save:
   - gửi `interviewId`
   - gửi `roomParticipantId`
8. backend validate participant có:
   - cùng room
   - cùng application với interview được chọn
   - đã từng join room

### 8.5. Trạng thái “Bỏ qua”

Nếu HR bỏ qua bước gán candidate:

- hệ thống vẫn có thể lưu clip theo interview hiện tại
- nhưng mức độ audit kém hơn so với việc gắn đầy đủ `roomParticipantId`

Khuyến nghị vận hành:

- không nên bỏ qua gán nếu room có nhiều candidate trong ngày

### 8.6. Quan hệ giữa complete và recording

Thứ tự production an toàn là:

1. candidate đã vào room
2. HR complete interview
3. HR gán feedback
4. HR gán recording
5. candidate tự rời room hoặc HR mời rời room nếu cần chuyển sang ứng viên tiếp theo

Điểm quan trọng:

- `complete` không được làm mất context gán recording
- nếu candidate vẫn đang active trong room sau khi complete, modal recording phải vẫn ưu tiên đúng candidate đó
- chỉ khi active session thực sự kết thúc thì HR mới được admit candidate tiếp theo

### 8.7. Thứ tự vận hành khuyến nghị trong room shared

Với mô hình `1 room / 1 job / 1 ngày`, trình tự production an toàn nhất là:

1. admit candidate
2. phỏng vấn
3. complete interview
4. feedback
5. assign recording
6. candidate `leave-session` hoặc HR `kick/remove` nếu cần chuyển lượt
7. admit candidate tiếp theo

Không nên đổi lượt theo thứ tự:

- complete xong là tự giả định candidate đã out
- hoặc admit candidate B khi candidate A vẫn còn giữ active session

## 9. Rule active candidate

### 9.1. Định nghĩa

`Active candidate` là ứng viên đang ở trạng thái `ADMITTED` trong room tại thời điểm hiện tại.

### 9.2. Cách hiển thị

HR UI cần hiển thị rõ:

- ứng viên nào đang active
- ứng viên nào đã vào room nhưng không còn active
- ứng viên nào đã completed

Mục tiêu:

- HR không nhầm candidate hiện tại khi record
- HR biết rõ ai là người đang được phỏng vấn
- giảm sai thao tác trong room dùng chung

## 10. Case nghiệp vụ chính

### Case A. Một room, một candidate, phỏng vấn bình thường

1. HR vào room
2. candidate vào trong khung 15 phút
3. HR admit
4. candidate active
5. HR phỏng vấn
6. HR complete
7. HR feedback
8. HR record gán đúng candidate

Kỳ vọng:

- flow đơn giản, không ambiguity

### Case B. Một room, nhiều candidate trong ngày, xử lý tuần tự

1. candidate A vào
2. HR admit A
3. A active
4. complete A
5. feedback A
6. recording A gán A
7. A tự rời room hoặc HR mời rời room
8. candidate B vào
9. HR admit B

Kỳ vọng:

- luôn chỉ có 1 active candidate
- clip A không dính sang B
- complete A không tự giải phóng active slot nếu A vẫn còn trong room

### Case C. Candidate reload

1. candidate đã admit
2. candidate F5
3. candidate vào lại

Kỳ vọng:

- không cần HR admit lại
- peer/session reconnect được

### Case C2. Candidate leave chủ động rồi muốn quay lại

1. candidate đã admit
2. candidate bấm `Rời phòng`
3. active slot được giải phóng
4. candidate muốn quay lại thì gửi yêu cầu mới
5. HR quyết định admit lại hay không

Kỳ vọng:

- khác với reload
- HR có thể admit candidate tiếp theo ngay sau khi session cũ đã leave thật sự

### Case D. Candidate bị kick

1. candidate đang active
2. HR kick
3. candidate gửi yêu cầu lại
4. HR admit lại

Kỳ vọng:

- phải admit lại
- peer cũ bị dọn sạch

### Case E. HR cố admit người thứ hai khi đang có candidate active

Kỳ vọng production hiện tại:

- bị chặn
- UI báo rõ chỉ hỗ trợ 1 active candidate tại một thời điểm
- backend và signaling cũng chặn

## 11. Những quyết định production đã chốt

### Đã chốt

- 1 room cho 1 job trong 1 ngày
- HR vào room bất kỳ lúc nào trong ngày
- candidate chỉ vào từ 15 phút trước lịch của mình
- reload không cần admit lại
- leave-session chủ động thì giải phóng active slot và phải xin vào lại nếu muốn quay lại
- kick thì phải admit lại
- online complete chỉ khi candidate đã vào room
- complete không tự kick candidate và không tự đóng room
- feedback chỉ sau complete
- recording gắn theo `interviewId + roomParticipantId`
- một thời điểm chỉ 1 candidate active trong room

### Chưa mở rộng trong phiên bản hiện tại

- multi-candidate active đồng thời trong cùng room UI
- nhiều remote tile thực sự cho HR
- auto-segmentation recording theo từng candidate nếu HR record xuyên suốt nhiều phiên liên tiếp

## 12. Rủi ro còn lại và khuyến nghị tiếp theo

### Rủi ro còn lại

- nếu HR dùng recording theo cách “record liên tục nhiều candidate mà không tách clip”, operator vẫn có thể tạo clip khó audit
- nếu business vừa muốn `reload không cần admit lại` vừa muốn `ứng viên rời phòng là lập tức nhường slot cho người khác`, cần bổ sung rule phân biệt `mất kết nối tạm thời` với `rời phiên thật sự`
- nếu sau này business muốn 2 candidate active đồng thời, UI và signaling hiện tại sẽ không còn phù hợp

### Khuyến nghị tiếp theo

1. Thêm metadata cho recording:
   - `recordingStartedAt`
   - `recordingEndedAt`
   - `assignedBy`
   - `assignedAt`

2. Thêm audit log cho hành động room:
   - admit
   - reject
   - kick
   - complete
   - assign recording

3. Bổ sung `leave-session` hoặc `grace period reconnect` cho candidate:
   - reload/mất mạng ngắn hạn không cần admit lại
   - nhưng rời phiên thật sự phải giải phóng active slot rõ ràng

4. Nếu cần multi-remote thật sự:
   - phải redesign cả signaling lẫn HR UI
   - không nên mở rộng bằng cách vá trên kiến trúc 1-1 hiện tại

## 13. Kết luận

Phiên bản hiện tại đã được siết lại theo hướng production an toàn cho mô hình:

- shared room theo job/ngày
- nhiều candidate trong ngày
- nhưng xử lý tuần tự từng candidate

Đây là mô hình phù hợp với UI hiện tại, giảm rủi ro:

- nhầm candidate active
- complete sai thời điểm
- feedback sớm
- gán recording sai người

Nếu trong tương lai business mở rộng sang `nhiều candidate active đồng thời`, module room interview cần được xem như một pha thiết kế mới thay vì tiếp tục chỉnh nhỏ trên kiến trúc hiện tại.
