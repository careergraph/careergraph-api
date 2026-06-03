# Phương án 1: Gemini Extract CV Keywords

## 1. Tổng quan

Dùng LLM (Gemini 2.5 Flash Lite) để extract keywords có cấu trúc từ CV text.
Spring Boot gọi async đến AI service sau khi extract raw text thành công.

**Khi nào gọi:** 1 lần duy nhất sau khi `ResumeTextExtractionServiceImpl` lưu `resume_extracted_text` thành công.

**Lưu kết quả:** Column `file.cv_keywords_json` (TEXT/JSON) — không cần gọi lại trừ khi CV thay đổi.

---

## 2. Kiến trúc

```
Upload CV
  → Extract text (PDFBox/POI) → lưu resume_extracted_text
  → Publish ResumeTextExtractedEvent
  → Async listener gọi AI service
  → POST /api/v1/extract-cv-keywords { resumeText: "..." }
  → Gemini extract → structured JSON
  → Lưu file.cv_keywords_json
  → Publish CandidateUpdatedEvent(RESUME_KEYWORDS_EXTRACTED)
  → Re-index / CandidateSearchTextBuilder dùng keywords
```

---

## 3. API Contract

### Request (Spring Boot → AI Service)

```http
POST /api/v1/extract-cv-keywords
Content-Type: application/json
```

```json
{
  "resumeText": "Nguyễn Văn A\nJava Backend Developer\n3 năm kinh nghiệm...",
  "maxKeywordsChars": 300
}
```

### Response (AI Service → Spring Boot)

```json
{
  "jobTitle": "Java Backend Developer",
  "skills": ["Java", "Spring Boot", "PostgreSQL", "Docker", "Microservices", "REST API"],
  "industries": ["Công nghệ thông tin", "Phần mềm"],
  "experienceYears": 3,
  "educationLevel": "Đại học",
  "topCompanies": ["FPT Software", "Viettel"],
  "summary": "Backend developer 3 năm kinh nghiệm Java Spring Boot, triển khai microservices, PostgreSQL",
  "searchKeywords": "Java Backend Developer Spring Boot Microservices PostgreSQL Docker REST API"
}
```

Field `searchKeywords` (~200-300 chars) là output chính dùng cho `CandidateSearchTextBuilder`.

---

## 4. Implementation AI Service (Python)

### Model: `models/cv_keywords_models.py`

```python
from typing import Optional, List
from pydantic import BaseModel, Field, ConfigDict


class ExtractCvKeywordsRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    resume_text: str = Field(..., alias="resumeText", min_length=10)
    max_keywords_chars: int = Field(default=300, alias="maxKeywordsChars")


class ExtractCvKeywordsResponse(BaseModel):
    job_title: Optional[str] = Field(default=None, alias="jobTitle")
    skills: List[str] = Field(default_factory=list)
    industries: List[str] = Field(default_factory=list)
    experience_years: Optional[int] = Field(default=None, alias="experienceYears")
    education_level: Optional[str] = Field(default=None, alias="educationLevel")
    top_companies: List[str] = Field(default_factory=list, alias="topCompanies")
    summary: Optional[str] = Field(default=None)
    search_keywords: str = Field(default="", alias="searchKeywords")
```

### Service: `services/cv_service.py` (thêm method)

```python
async def extract_cv_keywords(self, resume_text: str, max_chars: int = 300) -> str:
    """
    Extract structured keywords từ CV text bằng Gemini.
    Trả về JSON string.
    """
    # Truncate input nếu quá dài (Gemini context window an toàn)
    truncated = resume_text[:8000] if len(resume_text) > 8000 else resume_text

    prompt = f"""Bạn là chuyên gia phân tích CV. Từ nội dung CV bên dưới, extract thông tin quan trọng.

--- NỘI DUNG CV ---
{truncated}

--- YÊU CẦU ---
Extract và trả về DUY NHẤT một JSON hợp lệ (không markdown, không giải thích):
{{
  "jobTitle": "<vị trí/chức danh chính trong CV, 1 dòng>",
  "skills": ["<top 5-8 skills kỹ thuật/chuyên môn chính>"],
  "industries": ["<1-2 ngành nghề liên quan>"],
  "experienceYears": <số năm kinh nghiệm, integer hoặc null>,
  "educationLevel": "<Đại học/Cao đẳng/Thạc sĩ/Tiến sĩ hoặc null>",
  "topCompanies": ["<1-3 công ty gần nhất đã làm>"],
  "summary": "<mô tả ngắn 1-2 câu về profile ứng viên, tiếng Việt>",
  "searchKeywords": "<chuỗi keywords quan trọng nhất, tối đa {max_chars} ký tự, ngăn cách bằng dấu cách>"
}}

Lưu ý:
- searchKeywords phải chứa: job title + top skills + industry keywords
- Không bao gồm thông tin cá nhân (tên, email, SĐT, địa chỉ)
- Ưu tiên từ khóa liên quan nghề nghiệp, bỏ qua noise
- Nếu CV song ngữ, ưu tiên tiếng Anh cho skills, tiếng Việt cho industry
"""
    messages = [HumanMessage(content=prompt)]
    response = await self.llm.ainvoke(messages)
    return self._clean_json_string(response.content)
```

### Route: `routes/cv_routes.py` (thêm endpoint)

```python
@router.post("/api/v1/extract-cv-keywords")
async def extract_cv_keywords(body: ExtractCvKeywordsRequest):
    """
    Extract structured keywords từ CV text.
    Spring Boot gọi async sau khi extract raw text thành công.
    """
    try:
        logger.info("Received extract-cv-keywords request, text length=%d", len(body.resume_text))
        service = get_cv_service()
        result_json_str = await service.extract_cv_keywords(
            resume_text=body.resume_text,
            max_chars=body.max_keywords_chars
        )
        return Response(content=result_json_str, media_type="application/json")
    except Exception as e:
        logger.error(f"Error in extract_cv_keywords: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))
```

---

## 5. Implementation Spring Boot (Java)

### DB Schema: Thêm column vào `file`

```sql
ALTER TABLE file ADD COLUMN cv_keywords_json TEXT;
```

### Entity: `File.java`

```java
@Column(name = "cv_keywords_json", columnDefinition = "TEXT")
private String cvKeywordsJson;
```

### FastAPIClientService: Thêm method

```java
String extractCvKeywords(String jsonBody);
```

### FastAPIClientServiceImpl: Thêm implementation

```java
@Override
public String extractCvKeywords(String jsonBody) {
    try {
        log.info("Calling FastAPI extract-cv-keywords endpoint");
        String response = webClient.post()
                .uri(FAST_API_URL + "/api/v1/extract-cv-keywords")
                .header("Content-Type", "application/json")
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(2)))
                .block();
        if (response == null) {
            throw new RuntimeException("FastAPI returned null response");
        }
        return response;
    } catch (Exception ex) {
        log.error("ERROR: FastAPI extract-cv-keywords failed - {}", ex.getMessage());
        return null; // Graceful fallback: không có keywords thì dùng heuristic
    }
}
```

### Listener: Gọi sau khi extract text thành công

```java
// Trong ResumeTextExtractionServiceImpl hoặc listener riêng
@Async
public void extractKeywordsFromResume(String fileId) {
    File file = fileRepository.findById(fileId).orElse(null);
    if (file == null || !StringUtils.hasText(file.getResumeExtractedText())) return;

    String requestBody = """
        {"resumeText": %s, "maxKeywordsChars": 300}
        """.formatted(objectMapper.writeValueAsString(file.getResumeExtractedText()));

    String response = fastAPIClientService.extractCvKeywords(requestBody);
    if (response != null) {
        file.setCvKeywordsJson(response);
        fileRepository.save(file);
        // Publish event để CandidateSearchTextBuilder có thể dùng
        eventPublisher.publishEvent(new CandidateUpdatedEvent(file.getOwnerId(), "RESUME_KEYWORDS_EXTRACTED"));
    }
}
```

### CandidateSearchTextBuilder: Dùng keywords

```java
// Thay vì:
// .map(File::getResumeExtractedText)
// .map(this::resumeSnippet)  // 4000 chars raw

// Dùng:
private String resolveResumeKeywords(File file) {
    // Ưu tiên AI-extracted keywords
    if (StringUtils.hasText(file.getCvKeywordsJson())) {
        try {
            JsonNode node = objectMapper.readTree(file.getCvKeywordsJson());
            String searchKeywords = node.path("searchKeywords").asText("");
            if (!searchKeywords.isBlank()) {
                return searchKeywords; // ~300 chars, clean, chính xác
            }
        } catch (Exception ignored) {}
    }
    // Fallback: heuristic extract (xem Phương án 2)
    return heuristicExtract(file.getResumeExtractedText(), 300);
}
```

---

## 6. Ưu điểm

| # | Ưu điểm |
|---|---|
| 1 | Chính xác 95%+ — LLM hiểu ngữ cảnh, CV viết tự do, song ngữ |
| 2 | Structured output — có thể dùng `skills[]` riêng cho matching, `jobTitle` riêng cho intent |
| 3 | Multilingual — tiếng Việt/Anh đều xử lý tốt |
| 4 | Ít maintenance — prompt duy nhất, không cần update rules |
| 5 | Đã có infrastructure — Gemini key, WebClient, pattern sẵn |
| 6 | Output ngắn (~300 chars) — BM25 search hiệu quả |

## 7. Nhược điểm

| # | Nhược điểm | Mitigation |
|---|---|---|
| 1 | Latency 1-3s | Gọi async, chỉ 1 lần khi upload |
| 2 | Cost ~$0.001-0.003/CV | Rẻ cho flash-lite; cache kết quả |
| 3 | Dependency external service | Fallback heuristic nếu AI fail |
| 4 | Rate limit Gemini | Queue + retry; batch nếu cần |
| 5 | AI hallucination | Validate JSON schema; reject nếu invalid |

## 8. Khi nào dùng Gemini vs Heuristic

```
Upload CV
  → Extract text thành công
  → Gọi Gemini extract (async)
  → Thành công? → Lưu cv_keywords_json → dùng searchKeywords
  → Thất bại?  → Fallback heuristic (lấy 300 chars đầu + detect skills)
```

**Luôn có fallback.** Gemini extract là "best effort enhancement", heuristic là baseline.

---

## 9. Ví dụ output thực tế

### Input CV (raw extract):
```
NGUYỄN VĂN A
Java Backend Developer
Email: nguyenvana@gmail.com | SĐT: 0912345678
LinkedIn: linkedin.com/in/nguyenvana

MỤC TIÊU NGHỀ NGHIỆP
Tìm kiếm vị trí Senior Java Developer tại công ty công nghệ, 
phát triển hệ thống backend quy mô lớn.

KINH NGHIỆM LÀM VIỆC
FPT Software (2022 - 2025) - Java Developer
- Phát triển microservices với Spring Boot, Spring Cloud
- Thiết kế RESTful API phục vụ 1M+ users
- PostgreSQL, Redis, Kafka, Docker, Kubernetes

Viettel Solutions (2020 - 2022) - Junior Developer  
- Phát triển module quản lý thuê bao
- Java, Hibernate, Oracle DB

HỌC VẤN
Đại học Bách Khoa TP.HCM - Khoa học Máy tính (2016-2020)

KỸ NĂNG
Java, Spring Boot, Spring Cloud, Microservices, PostgreSQL, 
Redis, Kafka, Docker, Kubernetes, CI/CD, Git, REST API
```

### Output Gemini:
```json
{
  "jobTitle": "Senior Java Backend Developer",
  "skills": ["Java", "Spring Boot", "Spring Cloud", "Microservices", "PostgreSQL", "Redis", "Kafka", "Docker"],
  "industries": ["Công nghệ thông tin", "Phần mềm"],
  "experienceYears": 5,
  "educationLevel": "Đại học",
  "topCompanies": ["FPT Software", "Viettel Solutions"],
  "summary": "Java Backend Developer 5 năm kinh nghiệm, chuyên microservices Spring Boot quy mô lớn",
  "searchKeywords": "Senior Java Backend Developer Spring Boot Microservices PostgreSQL Redis Kafka Docker Kubernetes REST API Công nghệ thông tin"
}
```

**`searchKeywords` = 156 chars** — ngắn gọn, chính xác, dùng ngay cho BM25.
