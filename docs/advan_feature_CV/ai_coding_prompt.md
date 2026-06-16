Bạn là một AI Coding Assistant có năng lực phân tích mã nguồn Java Spring Boot và dịch vụ AI (FastAPI). Nhiệm vụ của bạn là hiện thực hóa tính năng **CV Chunking** (phân mảnh CV cấu trúc) và **AI Tailor CV** (Gợi ý tối ưu CV theo tin tuyển dụng + Gap Analysis) với yêu cầu **sửa đổi mã nguồn tối thiểu, đảm bảo an toàn và không ảnh hưởng đến các logic nghiệp vụ hiện có**.

Hãy đọc kỹ toàn bộ hướng dẫn và các file mã nguồn liên quan để triển khai chính xác các bước dưới đây:

---

### Yêu Cầu 1: Triển khai CV Chunking (Cắt nhỏ thông tin CV)

#### Bước 1.1: Bổ sung trường dữ liệu lưu trữ Chunks
- **Tệp tin cần sửa:** `src/main/java/com/hcmute/careergraph/persistence/models/File.java`
- **Hành động:** Thêm thuộc tính `cvChunksJson` để ánh xạ cột `cv_chunks_json` dạng TEXT:
```java
@Column(name = "cv_chunks_json", columnDefinition = "TEXT")
private String cvChunksJson;
```
*(Hibernate ddl-auto: update sẽ tự động tạo cột này trong bảng `file` khi khởi động ứng dụng).*

#### Bước 1.2: Bổ sung hàm Chunking bằng Java
- **Tệp tin cần sửa:** `src/main/java/com/hcmute/careergraph/helper/CvKeywordsHeuristicExtractor.java`
- **Hành động:** 
  1. Thêm định nghĩa phần **Học vấn (Education)** vào danh sách `PRIORITY_SECTIONS`:
     ```java
     new SectionDef(Pattern.compile("(?i)(học vấn|bằng cấp|giáo dục|education|học tập|đại học|trường)"), 4)
     ```
  2. Viết phương thức `public String extractChunks(String resumeText)` nhận vào nội dung văn bản CV thô và trả về chuỗi JSON có cấu trúc như sau:
     ```json
     {
       "chunks": [
         { "type": "summary", "content": "Nội dung phần mục tiêu nghề nghiệp/tóm tắt", "weight": 0.8 },
         { "type": "skill", "content": "Nội dung phần kỹ năng", "weight": 1.0 },
         { "type": "experience", "content": "Nội dung phần kinh nghiệm làm việc", "weight": 0.9 },
         { "type": "education", "content": "Nội dung phần học vấn", "weight": 0.6 }
       ],
       "extracted_at": "ISO_8601_TIMESTAMP",
       "chunk_version": 1
     }
     ```
     *Lưu ý:* Thực hiện phân tích và lấy nội dung các phần bằng phương thức `extractSections(lines)` có sẵn trong class. Nếu một phần không có dữ liệu, hãy bỏ qua hoặc để trống. Sử dụng thư viện `jackson` (`ObjectMapper`) hoặc tự sinh chuỗi JSON an toàn.

#### Bước 1.3: Tích hợp Chunking khi Tải lên CV
- **Tệp tin cần sửa:** `src/main/java/com/hcmute/careergraph/services/impl/CloudinaryServiceImpl.java`
- **Hành động:**
  1. Inject thêm bean `CvKeywordsHeuristicExtractor` vào lớp.
  2. Tìm phương thức `uploadInternal(...)`. Tại khu vực xử lý lưu file CV (`if (resumeFile) { ... }`), ngay sau dòng `entity.setResumeContentHash(localExtraction.contentHash());`, thêm khối mã xử lý trích xuất chunk:
     ```java
     if (StringUtils.isNotBlank(localExtraction.text())) {
         try {
             String chunksJson = cvKeywordsHeuristicExtractor.extractChunks(localExtraction.text());
             entity.setCvChunksJson(chunksJson);
         } catch (Exception e) {
             log.warn("Failed to extract CV chunks for file: {}", e.getMessage());
         }
     }
     ```

---

### Yêu Cầu 2: Nâng cấp DTO và Tích hợp AI Tailor CV (Gap Analysis)

#### Bước 2.1: Mở rộng DTO Phản hồi
- **Tệp tin cần sửa:** `src/main/java/com/hcmute/careergraph/persistence/dtos/response/CvSuggestionResponse.java`
- **Hành động:** Thêm 4 thuộc tính mới vào lớp `CvSuggestionResponse` để nhận dữ liệu phân tích từ AI:
```java
private List<String> matchedSkills;       // Kỹ năng phù hợp giữa CV và JD
private List<String> missingSkills;       // Kỹ năng JD yêu cầu nhưng CV còn thiếu
private List<String> suggestions;         // Đề xuất/lời khuyên điều chỉnh CV
private Integer overallMatchScore;        // Điểm số phù hợp tổng quan (0 - 100)
```

#### Bước 2.2: Tích hợp logic AI và giới hạn tần suất gọi AI
- **Tệp tin cần sửa:** `src/main/java/com/hcmute/careergraph/services/impl/JobServiceImpl.java`
- **Hành động:**
  1. Inject thêm các dependency: `RedisService` và `FileRepository`.
  2. Sửa đổi phương thức `generateCv(String jobId, String candidateId)` hiện tại (được gọi từ endpoint `POST /jobs/{jobId}/cv-suggestion` trong `JobController`):
     
     **A. Kiểm soát Tần suất gọi (Rate Limiting) bằng Redis:**
     - Sử dụng key Redis: `cv_suggestion_limit:{candidateId}`.
     - Lấy bộ đếm hiện tại từ Redis. Nếu giá trị đếm `>= 10`, ném ra ngoại lệ `BadRequestException("Bạn đã vượt quá số lần tạo gợi ý CV cho phép trong ngày (tối đa 10 lần).")`.
     - Tăng bộ đếm và lưu lại vào Redis với thời gian hết hạn (TTL) là 24 giờ (86400 giây).

     **B. Lấy nguồn dữ liệu CV nguồn mới nhất của Ứng viên:**
     - Truy vấn tệp CV/Resume hoạt động mới nhất của ứng viên trong bảng `File`:
       ```java
       Optional<File> latestCv = fileRepository.findFirstByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(
           candidateId, Status.ACTIVE, List.of(FileType.RESUME, FileType.CV));
       ```
     - Nếu ứng viên **đã có CV tải lên**, lấy chuỗi văn bản trích xuất từ `latestCv.get().getResumeExtractedText()`.
     - Nếu ứng viên **chưa tải CV nào lên**, sử dụng thông tin Profile hiện tại của ứng viên (lấy qua `candidateRepository`) làm dữ liệu đầu vào CV cho Prompt.

     **C. Thiết kế Prompt gửi tới Gemini AI:**
     - Cập nhật hàm `buildCvGenerationPrompt(Job job, String cvText, Candidate candidate)` nhận chuỗi văn bản CV.
     - Viết lại câu Prompt chỉ thị cho Gemini thực hiện:
       - Đóng vai là chuyên gia tuyển dụng. So sánh chi tiết JD công việc (Title, Description, Qualifications) và CV của ứng viên.
       - Viết lại phần tóm tắt (`personal.summary`) làm nổi bật mức độ phù hợp.
       - Viết lại các `bulletPoints` kinh nghiệm tập trung vào các yêu cầu cốt lõi của JD.
       - Sắp xếp danh sách `skills` theo độ liên quan giảm dần đối với JD.
       - Phân tích và điền dữ liệu Gap Analysis: `matchedSkills` (kỹ năng khớp), `missingSkills` (kỹ năng thiếu), `suggestions` (đề xuất cải thiện), và `overallMatchScore` (điểm số từ 0 - 100).
       - Yêu cầu AI trả về kết quả định dạng JSON chuẩn khớp hoàn toàn với cấu trúc của lớp `CvSuggestionResponse` đã mở rộng.

     **D. Xử lý Lỗi & Fallback:**
     - Bọc toàn bộ quá trình gọi AI và parse JSON trong khối `try-catch`.
     - Nếu việc gọi AI gặp lỗi hoặc timeout, thực hiện bắt ngoại lệ và kích hoạt cơ chế fallback: tự tạo một đối tượng `CvSuggestionResponse` mặc định điền từ profile gốc của ứng viên (như logic thô cũ) để trả về cho client hiển thị bình thường.

---

### Yêu Cầu 3: Kiểm thử
- Đảm bảo viết Unit Test bổ sung cho hàm `extractChunks` trong lớp kiểm thử tương ứng của `CvKeywordsHeuristicExtractor`.
- Kiểm thử tích hợp chạy API `POST /jobs/{jobId}/cv-suggestion` để đảm bảo định dạng JSON trả về chứa đầy đủ các trường mới và kiểm tra rate limit hoạt động chuẩn xác.
