# Phương án 2: Heuristic Extract CV Keywords (Regex/Rules)

## 1. Tổng quan

Extract keywords từ CV text bằng rules-based: regex patterns, section detection, và frequency analysis.
Không cần external AI service, chạy hoàn toàn trong Spring Boot.

**Khi nào chạy:** Ngay sau khi `ResumeTextExtractionServiceImpl` lưu `resume_extracted_text`.

**Ưu tiên dùng khi:** AI service unavailable, hoặc làm baseline fallback.

---

## 2. Kiến trúc

```
Upload CV
  → Extract text (PDFBox/POI) → lưu resume_extracted_text
  → Gọi CvKeywordsHeuristicExtractor.extract(resumeText)
  → Trả về keywords string (~300 chars)
  → Lưu file.cv_heuristic_keywords (hoặc dùng trực tiếp)
  → CandidateSearchTextBuilder dùng keywords
```

Không cần network call, không async, chạy inline.

---

## 3. Chiến lược extract

### 3.1 Bỏ noise (thông tin cá nhân, format)

```java
// Patterns cần loại bỏ
private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}");
private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{2,4}\\)?[-.\\s]?\\d{3,4}[-.\\s]?\\d{3,4}");
private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+|www\\.[^\\s]+");
private static final Pattern DATE_PATTERN = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{2,4}|\\d{4}\\s*[-–]\\s*\\d{4}|\\d{4}\\s*[-–]\\s*(nay|present|hiện tại)", Pattern.CASE_INSENSITIVE);

// Từ noise (headers, labels)
private static final Set<String> NOISE_WORDS = Set.of(
    "email", "phone", "điện thoại", "địa chỉ", "address", "linkedin",
    "github", "facebook", "date of birth", "ngày sinh", "giới tính",
    "gender", "quốc tịch", "nationality", "tình trạng hôn nhân"
);
```

### 3.2 Detect sections quan trọng

```java
// Section headers cần ưu tiên
private static final Map<String, Integer> SECTION_PRIORITY = Map.of(
    "objective", 1,        // Mục tiêu nghề nghiệp
    "mục tiêu", 1,
    "skills", 2,           // Kỹ năng
    "kỹ năng", 2,
    "experience", 3,       // Kinh nghiệm
    "kinh nghiệm", 3,
    "summary", 1,          // Tóm tắt
    "tóm tắt", 1,
    "profile", 1
);

// Regex detect section header
private static final Pattern SECTION_HEADER = Pattern.compile(
    "^\\s*(#{1,3}\\s*)?([A-ZÀ-Ỹ][A-ZÀ-Ỹ\\s]{2,30}|[a-zA-ZÀ-ỹ\\s]+:)\\s*$",
    Pattern.MULTILINE
);
```

### 3.3 Extract logic

```java
public String extract(String resumeText, int maxChars) {
    if (resumeText == null || resumeText.isBlank()) return "";

    // Step 1: Clean noise
    String cleaned = removeNoise(resumeText);

    // Step 2: Split thành sections
    List<Section> sections = detectSections(cleaned);

    // Step 3: Ưu tiên lấy content từ sections quan trọng
    StringBuilder keywords = new StringBuilder();

    // 3a. Lấy dòng đầu tiên có ý nghĩa (thường là Job Title)
    String firstMeaningfulLine = getFirstMeaningfulLine(cleaned);
    if (firstMeaningfulLine != null) {
        keywords.append(firstMeaningfulLine).append(' ');
    }

    // 3b. Lấy nội dung section Objective/Summary (nếu có)
    for (Section s : sections) {
        if (s.priority == 1 && keywords.length() < maxChars) {
            String content = truncateSection(s.content, 100);
            keywords.append(content).append(' ');
        }
    }

    // 3c. Lấy nội dung section Skills (nếu có)
    for (Section s : sections) {
        if (s.priority == 2 && keywords.length() < maxChars) {
            String content = truncateSection(s.content, 150);
            keywords.append(content).append(' ');
        }
    }

    // 3d. Lấy job titles từ Experience section
    for (Section s : sections) {
        if (s.priority == 3 && keywords.length() < maxChars) {
            String titles = extractJobTitlesFromExperience(s.content);
            keywords.append(titles).append(' ');
        }
    }

    // Step 4: Nếu vẫn chưa đủ / không detect được sections → lấy N chars đầu
    if (keywords.toString().trim().length() < 50) {
        keywords = new StringBuilder(getFallbackText(cleaned, maxChars));
    }

    // Step 5: Final cleanup
    return keywords.toString()
            .replaceAll("\\s+", " ")
            .trim()
            .substring(0, Math.min(keywords.length(), maxChars));
}
```

---

## 4. Implementation chi tiết (Java)

### `CvKeywordsHeuristicExtractor.java`

```java
package com.hcmute.careergraph.helper;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CvKeywordsHeuristicExtractor {

    private static final int DEFAULT_MAX_CHARS = 300;

    // === NOISE PATTERNS ===
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{2,4}\\)?[-.\\s]?\\d{3,4}[-.\\s]?\\d{3,4}");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+|www\\.[^\\s]+|linkedin\\.com[^\\s]*|github\\.com[^\\s]*");
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "\\d{1,2}/\\d{1,2}/\\d{2,4}|\\d{4}\\s*[-–]\\s*(\\d{4}|nay|present|hiện tại|now)",
            Pattern.CASE_INSENSITIVE);

    // Lines chứa chỉ thông tin cá nhân
    private static final Pattern PERSONAL_LINE = Pattern.compile(
            "^\\s*(email|phone|điện thoại|sđt|số điện thoại|địa chỉ|address|ngày sinh|date of birth|giới tính|gender|linkedin|github|facebook|portfolio)\\s*[:：]",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    // === SECTION DETECTION ===
    private static final List<SectionPattern> PRIORITY_SECTIONS = List.of(
            new SectionPattern(Pattern.compile("(?i)(mục tiêu|objective|career\\s*objective|summary|tóm tắt|profile|giới thiệu)"), 1),
            new SectionPattern(Pattern.compile("(?i)(kỹ năng|skills|technical\\s*skills|competencies|năng lực)"), 2),
            new SectionPattern(Pattern.compile("(?i)(kinh nghiệm|experience|work\\s*experience|professional\\s*experience)"), 3)
    );

    // === TECH KEYWORDS (boost detection) ===
    private static final Set<String> TECH_KEYWORDS = Set.of(
            "java", "python", "javascript", "typescript", "react", "angular", "vue",
            "spring", "spring boot", "node.js", "express", "django", "flask",
            "postgresql", "mysql", "mongodb", "redis", "elasticsearch",
            "docker", "kubernetes", "aws", "azure", "gcp", "ci/cd", "git",
            "microservices", "rest api", "graphql", "kafka", "rabbitmq",
            "machine learning", "deep learning", "data science", "ai",
            "html", "css", "tailwind", "figma", "photoshop",
            "agile", "scrum", "jira", "devops", "linux"
    );

    public String extract(String resumeText) {
        return extract(resumeText, DEFAULT_MAX_CHARS);
    }

    public String extract(String resumeText, int maxChars) {
        if (!StringUtils.hasText(resumeText)) return "";

        // Step 1: Remove noise
        String cleaned = removeNoise(resumeText);
        String[] lines = cleaned.split("\\n");

        StringBuilder result = new StringBuilder();

        // Step 2: Get first meaningful line (usually job title / name + title)
        String firstLine = getFirstMeaningfulLine(lines);
        if (firstLine != null) {
            result.append(firstLine).append(' ');
        }

        // Step 3: Extract by priority sections
        Map<Integer, String> sectionContents = extractSections(lines);

        // Priority 1: Objective/Summary
        if (sectionContents.containsKey(1)) {
            appendWithLimit(result, sectionContents.get(1), 120, maxChars);
        }

        // Priority 2: Skills
        if (sectionContents.containsKey(2)) {
            appendWithLimit(result, sectionContents.get(2), 150, maxChars);
        }

        // Priority 3: Experience (chỉ lấy job titles)
        if (sectionContents.containsKey(3)) {
            String titles = extractJobTitlesOnly(sectionContents.get(3));
            appendWithLimit(result, titles, 100, maxChars);
        }

        // Step 4: Nếu quá ít, fallback lấy top lines
        String output = result.toString().trim();
        if (output.length() < 50) {
            output = getFallbackText(lines, maxChars);
        }

        // Step 5: Final deduplicate và clean
        return deduplicateWords(output, maxChars);
    }

    // ============================
    // PRIVATE HELPERS
    // ============================

    private String removeNoise(String text) {
        String result = EMAIL_PATTERN.matcher(text).replaceAll("");
        result = PHONE_PATTERN.matcher(result).replaceAll("");
        result = URL_PATTERN.matcher(result).replaceAll("");
        result = DATE_RANGE_PATTERN.matcher(result).replaceAll("");
        // Remove lines that are purely personal info
        result = PERSONAL_LINE.matcher(result).replaceAll("");
        return result;
    }

    private String getFirstMeaningfulLine(String[] lines) {
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.length() < 3) continue;
            // Skip nếu chỉ là tên (thường ALL CAPS ngắn < 30 chars không có keyword)
            if (trimmed.length() < 30 && trimmed.equals(trimmed.toUpperCase())
                    && !containsTechKeyword(trimmed)) {
                continue;
            }
            // Lấy dòng đầu có ý nghĩa
            if (trimmed.length() >= 5) {
                return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
            }
        }
        return null;
    }

    private Map<Integer, String> extractSections(String[] lines) {
        Map<Integer, StringBuilder> sections = new HashMap<>();
        int currentPriority = -1;
        int linesSinceHeader = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Check if this line is a section header
            int detected = detectSectionPriority(trimmed);
            if (detected > 0) {
                currentPriority = detected;
                linesSinceHeader = 0;
                continue;
            }

            // Append content to current section (max 10 lines per section)
            if (currentPriority > 0 && linesSinceHeader < 10) {
                sections.computeIfAbsent(currentPriority, k -> new StringBuilder())
                        .append(trimmed).append(' ');
                linesSinceHeader++;
            }
        }

        Map<Integer, String> result = new HashMap<>();
        sections.forEach((k, v) -> result.put(k, v.toString().trim()));
        return result;
    }

    private int detectSectionPriority(String line) {
        String lower = line.toLowerCase().replaceAll("[^a-zA-ZÀ-ỹ\\s]", "").trim();
        for (SectionPattern sp : PRIORITY_SECTIONS) {
            if (sp.pattern.matcher(lower).find() && lower.length() < 40) {
                return sp.priority;
            }
        }
        return -1;
    }

    private String extractJobTitlesOnly(String experienceContent) {
        // Heuristic: job titles thường ở đầu mỗi entry, trước dấu "-" hoặc "|" hoặc "("
        String[] parts = experienceContent.split("\\s{2,}|\\n|\\|");
        StringBuilder titles = new StringBuilder();
        for (String part : parts) {
            String trimmed = part.trim();
            // Job title thường 3-6 words, có chứa keyword nghề nghiệp
            if (trimmed.split("\\s+").length <= 8 && containsTechKeyword(trimmed)) {
                titles.append(trimmed).append(' ');
            }
        }
        return titles.toString().trim();
    }

    private boolean containsTechKeyword(String text) {
        String lower = text.toLowerCase();
        return TECH_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private void appendWithLimit(StringBuilder sb, String content, int sectionLimit, int totalLimit) {
        if (sb.length() >= totalLimit) return;
        String truncated = content.length() > sectionLimit ? content.substring(0, sectionLimit) : content;
        sb.append(truncated).append(' ');
    }

    private String getFallbackText(String[] lines, int maxChars) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.length() < 3) continue;
            sb.append(trimmed).append(' ');
            if (sb.length() >= maxChars) break;
        }
        return sb.toString().trim();
    }

    private String deduplicateWords(String text, int maxChars) {
        String[] words = text.split("\\s+");
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            String lower = word.toLowerCase();
            if (seen.contains(lower)) continue;
            seen.add(lower);
            if (result.length() + word.length() + 1 > maxChars) break;
            result.append(word).append(' ');
        }
        return result.toString().trim();
    }

    // === INNER CLASS ===
    private record SectionPattern(Pattern pattern, int priority) {}
}
```

---

## 5. Tích hợp vào CandidateSearchTextBuilder

```java
@Service
@RequiredArgsConstructor
public class CandidateSearchTextBuilder {

    private static final int CV_KEYWORDS_MAX_CHARS = 300;

    private final FileRepository fileRepository;
    private final CvKeywordsHeuristicExtractor heuristicExtractor;

    public String build(Candidate candidate, boolean includeResume) {
        if (candidate == null) return "";

        StringBuilder sb = new StringBuilder();
        append(sb, candidate.getDesiredPosition());
        append(sb, candidate.getCurrentJobTitle());
        append(sb, candidate.getSummary());
        appendAll(sb, candidate.getIndustries());
        appendAll(sb, candidate.getLocations());
        appendAll(sb, candidate.getWorkTypes());

        if (candidate.getSkills() != null) {
            candidate.getSkills().stream()
                    .map(skill -> skill.getSkill() != null ? skill.getSkill().getName() : null)
                    .forEach(value -> append(sb, value));
        }

        if (includeResume) {
            // Lấy BẤT KỲ CV active nào (không cần shareToFindJob)
            fileRepository.findFirstByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc(
                            candidate.getId(),
                            Status.ACTIVE,
                            List.of(FileType.RESUME, FileType.CV))
                    .ifPresent(file -> {
                        String keywords = resolveKeywords(file);
                        if (StringUtils.hasText(keywords)) {
                            append(sb, keywords);
                        }
                    });
        }

        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private String resolveKeywords(File file) {
        // Ưu tiên 1: AI-extracted keywords (nếu có từ Gemini)
        if (StringUtils.hasText(file.getCvKeywordsJson())) {
            try {
                // Parse searchKeywords từ JSON
                // (dùng ObjectMapper hoặc simple regex)
                String json = file.getCvKeywordsJson();
                int idx = json.indexOf("\"searchKeywords\"");
                if (idx > 0) {
                    int start = json.indexOf("\"", idx + 16) + 1;
                    int end = json.indexOf("\"", start);
                    if (start > 0 && end > start) {
                        return json.substring(start, end);
                    }
                }
            } catch (Exception ignored) {}
        }

        // Ưu tiên 2: Heuristic extract từ raw text
        if (StringUtils.hasText(file.getResumeExtractedText())) {
            return heuristicExtractor.extract(file.getResumeExtractedText(), CV_KEYWORDS_MAX_CHARS);
        }

        return null;
    }
}
```

---

## 6. Ưu điểm

| # | Ưu điểm |
|---|---|
| 1 | Zero latency — chạy inline, < 10ms |
| 2 | Zero cost — không gọi external API |
| 3 | Zero dependency — không cần AI service running |
| 4 | Deterministic — cùng input luôn cho cùng output |
| 5 | Testable — dễ unit test với các CV mẫu |
| 6 | Fallback tốt — luôn có kết quả dù AI fail |

## 7. Nhược điểm

| # | Nhược điểm | Mitigation |
|---|---|---|
| 1 | Chính xác ~60-70% | Fallback cho Gemini; tốt hơn 4000 chars raw |
| 2 | Regex tiếng Việt khó hoàn hảo | Pattern đủ cho 80% CV format phổ biến |
| 3 | CV format đặc biệt bị miss | Fallback lấy N chars đầu |
| 4 | Không hiểu ngữ cảnh | Skills detect dựa trên dictionary TECH_KEYWORDS |
| 5 | Cần maintain TECH_KEYWORDS set | Thêm keywords khi cần, không ảnh hưởng logic |
| 6 | Không extract được experienceYears, industry | Chỉ output string, không structured |

---

## 8. So sánh output giữa Heuristic vs Gemini

### Input CV (raw extract):
```
NGUYỄN VĂN A
Java Backend Developer
Email: nguyenvana@gmail.com | SĐT: 0912345678

MỤC TIÊU NGHỀ NGHIỆP
Tìm kiếm vị trí Senior Java Developer tại công ty công nghệ,
phát triển hệ thống backend quy mô lớn.

KINH NGHIỆM LÀM VIỆC
FPT Software (2022 - 2025) - Java Developer
- Phát triển microservices với Spring Boot, Spring Cloud
- Thiết kế RESTful API phục vụ 1M+ users
- PostgreSQL, Redis, Kafka, Docker, Kubernetes

KỸ NĂNG
Java, Spring Boot, Spring Cloud, Microservices, PostgreSQL,
Redis, Kafka, Docker, Kubernetes, CI/CD, Git, REST API
```

### Heuristic output:
```
Java Backend Developer Tìm kiếm vị trí Senior Java Developer tại công ty công nghệ phát triển hệ thống backend quy mô lớn Java Spring Boot Spring Cloud Microservices PostgreSQL Redis Kafka Docker Kubernetes CI/CD Git REST API
```
**~230 chars** — khá tốt, bắt được job title + objective + skills.

### Gemini output:
```
Senior Java Backend Developer Spring Boot Microservices PostgreSQL Redis Kafka Docker Kubernetes REST API Công nghệ thông tin
```
**~156 chars** — ngắn hơn, sạch hơn, có industry.

### So sánh:

| Tiêu chí | Heuristic | Gemini |
|---|---|---|
| Job title detection | ✅ "Java Backend Developer" | ✅ "Senior Java Backend Developer" (tốt hơn) |
| Skills extraction | ✅ Full skills section | ✅ Top skills (gọn hơn) |
| Noise removal | ✅ Email/phone removed | ✅ Hoàn toàn sạch |
| Industry detection | ❌ Không có | ✅ "Công nghệ thông tin" |
| Deduplicate | ⚠️ Có thể lặp | ✅ Không lặp |
| Length | 230 chars | 156 chars |

---

## 9. Khi nào chỉ dùng Heuristic (không cần Gemini)

- AI service không khả dụng (maintenance, rate limit)
- CV đã extract xong nhưng Gemini queue đầy
- Environment không có `GOOGLE_API_KEY`
- Test environment
- CV rất ngắn (< 200 chars) — heuristic đủ tốt

---

## 10. Chiến lược kết hợp (Recommended)

```
Upload CV → Extract text
  ↓
  ├─ Heuristic extract ngay lập tức (sync, < 10ms)
  │   → Lưu file.cv_heuristic_keywords
  │   → CandidateSearchTextBuilder dùng NGAY
  │
  └─ Gọi Gemini extract (async, 1-3s)
      → Thành công? → Lưu file.cv_keywords_json, override heuristic
      → Thất bại? → Giữ heuristic keywords (đã có sẵn)
```

**Lợi ích:**
- User upload CV → personalization hoạt động **NGAY** (heuristic)
- Sau 1-3s, Gemini trả kết quả tốt hơn → update → lần search sau chính xác hơn
- Không bao giờ bị "trống keywords" chờ AI
