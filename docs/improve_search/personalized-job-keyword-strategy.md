# Chiến lược xây dựng Keyword cá nhân hóa việc làm

## 1. Bối cảnh và phạm vi

Chức năng cá nhân hóa việc làm phục vụ ứng viên ở 3 nơi:

| Nơi hiển thị | Method gọi | Mô tả |
|---|---|---|
| **Home → PersonalJobsSection** | `getJobsPersonalizedES()` → `searchJobsByNavtiveAndFuzzy()` | 6 job phù hợp nhất trên trang chủ |
| **Search page (blank keyword)** | `searchEmbed()` → `knnSearch(keyword)` | Lần đầu vào trang tìm việc chưa gõ keyword |
| **Daily Digest email** | `recommendJobsForCandidate()` → `searchRecommendJobsFromNewlyPosted()` | Gợi ý job mới qua email |

Cả 3 đều dùng `CandidateSearchTextBuilder.build(candidate, true)` để tạo keyword.

**Phân biệt rõ:**
- `shareToFindJob = true` → cho phép **HR tìm ứng viên** qua CV (chức năng HR Candidate Search).
- Cá nhân hóa việc làm cho ứng viên → dùng **MỌI CV active** mà ứng viên đã upload, không phụ thuộc `shareToFindJob`.

---

## 2. Vấn đề hiện tại

### 2.1 Sai ngữ cảnh query CV

`CandidateSearchTextBuilder` đang gọi:

```java
fileRepository.findFirstByOwnerIdAndStatusAndFileTypeInAndShareToFindJobTrueOrderByCreatedDateDesc(...)
```

Điều kiện `ShareToFindJobTrue` chỉ đúng cho HR search. Với cá nhân hóa cho ứng viên, cần dùng **bất kỳ CV active** nào.

**Hệ quả:** Ứng viên upload CV nhưng chưa bật "Cho phép tìm kiếm CV" → hệ thống KHÔNG dùng được CV để cá nhân hóa.

### 2.2 CV text quá dài (4000 chars) → loãng signal cho BM25

Khi `searchJobsByNavtiveAndFuzzy()` nhận keyword 4000+ chars:

- `minimumShouldMatch = "30%"` trên text dài → match quá dễ, kết quả loãng.
- Elasticsearch BM25 scoring bị dilute bởi noise words trong CV (tên trường, ngày tháng, thông tin liên hệ...).
- `shouldUseFuzzy()` tắt fuzzy khi > 256 chars, nhưng vấn đề chính là text dài chứ không phải fuzzy.

### 2.3 Không phân biệt trọng số Intent vs CV Evidence

Hiện tại concat tất cả vào 1 string:
```
desiredPosition + currentJobTitle + summary + industries + locations + workTypes + skills + CV_4000_chars
```

**Vấn đề:** `desiredPosition = "Data Analyst"` (10 chars) bị chìm giữa CV Java Backend (4000 chars) → kết quả trả về lẫn cả Java job lẫn Data Analyst job, không rõ ưu tiên.

### 2.4 Case chưa cập nhật Tiêu chí tìm việc

Nếu ứng viên chỉ upload CV mà chưa điền `desiredPosition`, `industries`, `locations`:
- Intent signal = rỗng
- CV signal = 4000 chars raw text
- Kết quả phụ thuộc hoàn toàn vào CV → có thể OK nhưng dễ bị noise

---

## 3. Các case cần xử lý

| Case | Intent Signal | CV Signal | Kỳ vọng |
|---|---|---|---|
| **A** | Có đầy đủ (desiredPosition, locations...) | Không có CV | Search theo intent → chính xác |
| **B** | Có đầy đủ | Có CV khớp intent | Intent + CV boost → kết quả tốt nhất |
| **C** | Có đầy đủ | Có CV KHÔNG khớp intent | Ưu tiên intent, CV boost nhẹ → tránh nhiễu |
| **D** | Trống (chưa điền tiêu chí) | Có CV | Dùng CV làm primary signal |
| **E** | Trống | Không CV | Fallback → latest jobs |
| **F** | Có đầy đủ | 2 CV khác nhau (Java + DA) | Dùng CV mới nhất, nhưng intent vẫn là primary |

---

## 4. Thiết kế giải pháp

### 4.1 Tách `CandidateSearchTextBuilder` thành 2 tầng signal

```java
public class CandidateSearchProfile {
    private String intentText;      // Từ tiêu chí tìm việc (ngắn, chính xác)
    private String cvKeywords;      // Extracted keywords từ CV (max ~300 chars)
    private String embeddingText;   // Text đầy đủ hơn cho embedding model (max ~1500 chars)
}
```

**Intent text** (high-priority, ngắn):
```
desiredPosition + currentJobTitle + industries + locations + workTypes + skills
```

**CV keywords** (medium-priority, tối đa ~300 chars):
- Không dùng raw 4000 chars
- Extract phần quan trọng nhất:
  1. Job title/objective (nếu detect được từ đầu CV)
  2. Skills section
  3. Tên công ty gần nhất + vị trí
- Fallback: lấy 300 chars đầu (thường chứa tên, vị trí, objective)

**Embedding text** (cho KNN search, chấp nhận dài hơn):
```
intentText (lặp 2 lần để tăng trọng số) + CV snippet 1500 chars
```

### 4.2 Sửa query CV: Dùng MỌI CV active (không phụ thuộc shareToFindJob)

Thêm method mới vào `FileRepository`:

```java
Optional<File> findFirstByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(
    String ownerId, Status status, List<FileType> fileTypes);
```

Dùng method này trong `CandidateSearchTextBuilder` thay vì method có `ShareToFindJobTrue`.

**Giữ nguyên** method `ShareToFindJobTrue` cho:
- `CandidateESServiceImpl.mapToES()` (ES index cho HR search)
- `CandidateElasticsearchDataInitializer` (bulk sync cho HR)

### 4.3 Chiến lược search theo case

#### Home personalized (`getJobsPersonalizedES`)

```
if (hasIntentText):
    primaryKeyword = intentText
    → searchJobsByNavtiveAndFuzzy(primaryKeyword)  // BM25, text ngắn, chính xác
    
    Nếu không đủ kết quả (< 3 jobs):
        fallback knnSearch(embeddingText)  // Hybrid search mở rộng hơn

elif (hasCvKeywords):
    → searchJobsByNavtiveAndFuzzy(cvKeywords)  // CV keywords ngắn
    
else:
    → getJobsForAnonymousUser()  // Latest jobs
```

#### Search blank keyword (`searchEmbed`)

```
if (hasIntentText AND hasCvKeywords):
    embeddingInput = intentText + " " + intentText + " " + cvKeywords
    → knnSearch(embeddingInput, filter)
    // Embedding model xử lý trọng số tự nhiên qua repetition

elif (hasIntentText):
    → knnSearch(intentText, filter)

elif (hasCvKeywords):
    → knnSearch(cvKeywords, filter)

else:
    → filterOnlySearch(filter)  // match_all + sort createdAt desc
```

#### Daily Digest (`searchRecommendJobsFromNewlyPosted`)

```
keyword = hasIntentText ? intentText : cvKeywords
embeddingInput = intentText + " " + cvKeywords (hoặc intentText nếu không có CV)
→ searchRecommendJobsFromNewlyPosted() sử dụng cả BM25(keyword) + KNN(embeddingInput)
```

### 4.4 Extract CV keywords (không dùng full text)

Heuristic đơn giản, không cần AI:

```java
public String extractCvKeywords(String resumeText, int maxChars) {
    if (resumeText == null || resumeText.isBlank()) return "";
    
    // 1. Lấy 5 dòng đầu (thường chứa objective/title)
    String[] lines = resumeText.split("\\n");
    StringBuilder result = new StringBuilder();
    
    int lineCount = 0;
    for (String line : lines) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) continue;
        // Bỏ qua dòng chỉ chứa thông tin liên hệ (email, phone, address)
        if (isContactInfo(trimmed)) continue;
        result.append(trimmed).append(' ');
        lineCount++;
        if (lineCount >= 5 || result.length() >= maxChars) break;
    }
    
    // 2. Nếu còn budget, tìm section "Skills" hoặc "Kỹ năng"
    String skillsSection = extractSkillsSection(resumeText);
    if (skillsSection != null && result.length() + skillsSection.length() <= maxChars) {
        result.append(skillsSection);
    }
    
    return result.toString().trim();
}
```

**Tại sao 300 chars?**
- BM25 multi_match với `minimumShouldMatch = "30%"` trên 50 words → cần match ~15 words → hợp lý.
- Elasticsearch scoring tốt nhất khi query ngắn và chính xác.
- Tránh `too_complex_to_determinize_exception` khi fuzzy + text dài.

### 4.5 Trọng số rõ ràng

| Signal | Vai trò | Chars tối đa | Khi nào dùng |
|---|---|---|---|
| `desiredPosition` | Strongest intent | ~100 | Luôn luôn (nếu có) |
| `currentJobTitle` | Current context | ~100 | Luôn luôn (nếu có) |
| `skills` | Technical match | ~200 | Luôn luôn (nếu có) |
| `industries + locations + workTypes` | Filter context | ~150 | Luôn luôn (nếu có) |
| `cvKeywords` (extracted) | Evidence boost | ~300 | Khi không có intent HOẶC as boost |
| `summary` | Bio context | ~200 | Chỉ cho embedding, không cho BM25 |

**Tổng intentText:** ~550 chars maximum → phù hợp BM25.
**Tổng embeddingText:** ~1500 chars maximum → phù hợp embedding model (384 dims).

---

## 5. Thay đổi cụ thể

### 5.1 Backend

| File | Thay đổi |
|---|---|
| `FileRepository.java` | Thêm `findFirstByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc()` |
| `CandidateSearchTextBuilder.java` | Refactor thành `buildProfile()` trả `CandidateSearchProfile` |
| `CandidateSearchProfile.java` (mới) | DTO chứa `intentText`, `cvKeywords`, `embeddingText` |
| `JobServiceImpl.java` | `getJobsPersonalizedES()` dùng `intentText` ưu tiên, fallback `cvKeywords` |
| `JobServiceImpl.java` | `searchEmbed()` dùng `embeddingText` cho knnSearch khi blank keyword |
| `JobRecommendationServiceImpl.java` | `genKeyword()` dùng structured profile |
| `JobESServiceImpl.java` | Không thay đổi (giữ nguyên search methods) |

### 5.2 Frontend

Không cần thay đổi frontend cho phần này. API response giữ nguyên contract.

---

## 6. Migration strategy

### Backward compatible

- `CandidateSearchTextBuilder.build(candidate, includeResume)` giữ nguyên signature cũ (return String) cho bất kỳ caller nào khác.
- Thêm method mới `buildProfile(candidate)` return `CandidateSearchProfile`.
- Caller cũ tiếp tục hoạt động, caller mới dùng structured profile.

### Không cần migration data

- Không thay đổi schema DB.
- Không thay đổi ES mapping.
- Không cần re-index.

---

## 7. Test scenarios

| # | Input | Expected Output |
|---|---|---|
| 1 | desiredPosition="Java Dev", CV=Java content | Keyword = "Java Dev ..." → Java jobs |
| 2 | desiredPosition="Data Analyst", CV=Java content | Keyword = "Data Analyst ..." → Data Analyst jobs (CV không override intent) |
| 3 | desiredPosition="", CV=Java content | Keyword = CV keywords → Java jobs (CV is primary) |
| 4 | desiredPosition="", CV="" | Fallback → latest jobs |
| 5 | desiredPosition="Java Dev", CV="" | Keyword = "Java Dev ..." → Java jobs |
| 6 | desiredPosition="Java Dev", 2 CV (Java + DA), CV mới nhất = DA | Keyword = "Java Dev ..." (intent primary, không bị DA CV override) |

---

## 8. Rủi ro và mitigation

| Rủi ro | Mitigation |
|---|---|
| `extractCvKeywords()` heuristic không chính xác | Fallback lấy N chars đầu; phase sau dùng AI extract |
| Candidate có CV nhưng text extraction fail | Đã có null-safe check, fallback intent-only |
| Embedding model input quá ngắn (chỉ intent 50 chars) | Vẫn hoạt động - embedding model xử lý short text tốt |
| BM25 `minimumShouldMatch` cần tune | Giữ 30% cho intentText (ngắn); nếu dùng cvKeywords thì 40% |

---

## 9. Ví dụ keyword output theo case

### Case B: Intent khớp CV
```
intentText = "Java Developer Công nghệ thông tin Hồ Chí Minh Full-time Java Spring Boot"
cvKeywords = "Java Developer 3 năm kinh nghiệm Spring Boot Microservices PostgreSQL"
embeddingText = "Java Developer Công nghệ thông tin Hồ Chí Minh Full-time Java Spring Boot Java Developer Công nghệ thông tin Hồ Chí Minh Full-time Java Spring Boot Java Developer 3 năm kinh nghiệm Spring Boot Microservices PostgreSQL"
```

### Case C: Intent KHÔNG khớp CV
```
intentText = "Data Analyst Công nghệ thông tin Hồ Chí Minh Full-time Python SQL Tableau"
cvKeywords = "Java Developer 3 năm kinh nghiệm Spring Boot Microservices"
embeddingText = "Data Analyst Công nghệ thông tin Python SQL Tableau Data Analyst Công nghệ thông tin Python SQL Tableau Java Developer 3 năm kinh nghiệm Spring Boot"

→ BM25 search dùng intentText = "Data Analyst ..." → kết quả Data Analyst jobs
→ CV keywords KHÔNG được dùng trong BM25 primary query (tránh nhiễu)
→ KNN embedding vẫn capture cả 2 signal nhưng intent weighted 2x
```

### Case D: Chỉ có CV, không có intent
```
intentText = ""
cvKeywords = "Java Developer 3 năm kinh nghiệm Spring Boot Microservices PostgreSQL"
embeddingText = "Java Developer 3 năm kinh nghiệm Spring Boot Microservices PostgreSQL"

→ BM25 search dùng cvKeywords → Java jobs
```
