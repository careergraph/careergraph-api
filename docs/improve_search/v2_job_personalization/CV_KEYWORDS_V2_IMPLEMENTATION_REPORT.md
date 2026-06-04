# V2 - CV Keywords Extraction & Personalized Job Search

## Tổng quan

Phiên bản V2 cải thiện chất lượng personalized job search bằng cách:
1. **Tách biệt intent vs CV evidence** — Profile signals (desiredPosition, skills, industries) được ưu tiên hơn CV text
2. **Extract keywords thay vì dump raw text** — Giảm từ 4000 chars xuống ~300 chars keywords có trọng tâm
3. **Bỏ shareToFindJob filter cho personalization** — CV nào active nhất sẽ được dùng (shareToFindJob chỉ dành HR)
4. **Configurable strategy** — Env var chọn HEURISTIC/GEMINI/HYBRID

---

## Kiến trúc

```
┌─────────────────┐     ┌──────────────────────┐
│  Resume Upload  │────▶│  Text Extraction     │
│  (File entity)  │     │  (PDFBox/POI)        │
└─────────────────┘     └──────────┬───────────┘
                                   │ 
                    ┌──────────────▼───────────────┐
                    │  CvKeywordsExtractionService  │
                    │  Strategy: ENV variable       │
                    ├──────────────────────────────-┤
                    │  HEURISTIC: instant, local    │
                    │  GEMINI: async, AI service    │
                    │  HYBRID: heuristic + gemini   │
                    └──────────────┬───────────────┘
                                   │ saves
                    ┌──────────────▼───────────────┐
                    │  File.cvKeywordsJson (TEXT)   │
                    │  {"searchKeywords":"...",     │
                    │   "jobTitle":"...",           │
                    │   "skills":[...]}            │
                    └──────────────┬───────────────┘
                                   │ reads
                    ┌──────────────▼───────────────┐
                    │  CandidateSearchTextBuilder   │
                    │  .buildProfile(candidate)     │
                    │  → CandidateSearchProfile     │
                    │    ├─ intentText (BM25 high)  │
                    │    ├─ cvKeywords (BM25 low)   │
                    │    └─ embeddingText (KNN)     │
                    └──────────────────────────────-┘
```

---

## Env Variables

| Variable | Default | Mô tả |
|----------|---------|--------|
| `CV_KEYWORDS_STRATEGY` | `HYBRID` | `HEURISTIC` / `GEMINI` / `HYBRID` |
| `CV_KEYWORDS_MAX_CHARS` | `300` | Giới hạn ký tự cho searchKeywords output |

---

## Files đã thay đổi

### Python (careergraph-ai)
| File | Thay đổi |
|------|----------|
| `models/cv_keywords_models.py` | **NEW** — Pydantic request/response models |
| `services/cv_service.py` | Thêm `extract_cv_keywords()` method (Gemini prompt) |
| `routes/cv_routes.py` | Thêm `POST /api/v1/extract-cv-keywords` endpoint |

### Java (careergraph-api)
| File | Thay đổi |
|------|----------|
| `application.yml` | Thêm `application.cv-keywords.strategy` + `max-keywords-chars` |
| `helper/CvKeywordsHeuristicExtractor.java` | **NEW** — Rules-based keyword extraction |
| `persistence/models/File.java` | Thêm `cvKeywordsJson` column (TEXT) |
| `persistence/dtos/response/CandidateSearchProfile.java` | **NEW** — Structured search profile DTO |
| `repositories/FileRepository.java` | Thêm method không filter `shareToFindJob` |
| `services/FastAPIClientService.java` | Thêm `extractCvKeywords()` interface method |
| `services/impl/FastAPIClientServiceImpl.java` | Implement `extractCvKeywords()` |
| `services/impl/CvKeywordsExtractionService.java` | **NEW** — Orchestrates extraction strategy |
| `services/CandidateSearchTextBuilder.java` | Thêm `buildProfile()` method trả về structured DTO |
| `services/impl/JobServiceImpl.java` | `getJobsPersonalizedES()` + `searchEmbed()` dùng V2 profile |
| `services/impl/JobRecommendationServiceImpl.java` | `genKeyword()` dùng V2 profile |
| `services/impl/ResumeTextExtractionServiceImpl.java` | Trigger keyword extraction after text extraction |

---

## Chiến lược hoạt động

### HEURISTIC (default nếu cần zero-latency)
- Xử lý local, instant (~1ms)
- Ưu tiên sections: Objective > Skills > Experience
- Detect job titles, tech keywords, deduplicate
- Chất lượng: 70-80% so với Gemini

### GEMINI (best quality)
- Gọi `gemini-2.5-flash-lite` qua FastAPI, ~2-5s
- Structured output: jobTitle, skills, industries, searchKeywords
- Temperature = 0.4 cho deterministic output
- Retry 2 lần, timeout 30s

### HYBRID (recommended)
- Lưu heuristic ngay (instant availability cho search)
- Gemini @Async override khi sẵn sàng (better quality sau 2-5s)
- Best of both: không blocking + high quality

---

## Bug fixes

1. **shareToFindJob wrongly gating personalization**: Dùng `findFirstByOwnerIdAndStatusAndFileTypeInOrderByCreatedDateDesc` thay vì `...ShareToFindJobTrue...`
2. **4000 chars raw CV diluting BM25**: Giảm xuống ~300 chars keywords có trọng tâm
3. **No signal weighting**: Intent signals (desiredPosition, skills) ưu tiên hơn CV evidence

---

## DB Migration cần chạy

```sql
ALTER TABLE files ADD COLUMN cv_keywords_json TEXT;
```

---

## Test checklist

- [ ] Upload CV → verify `cv_keywords_json` được populate (check DB)
- [ ] Set `CV_KEYWORDS_STRATEGY=HEURISTIC` → verify chỉ heuristic chạy
- [ ] Set `CV_KEYWORDS_STRATEGY=GEMINI` → verify Gemini API được gọi
- [ ] Set `CV_KEYWORDS_STRATEGY=HYBRID` → verify cả hai chạy (heuristic first, gemini override)
- [ ] Personalized job search → verify kết quả relevance tốt hơn V1
- [ ] Candidate không có CV → fallback về intent-only search
- [ ] Candidate không có intent + không có CV → fallback về anonymous jobs
- [ ] `searchEmbed` với blank keyword + logged-in → dùng embeddingText từ profile
- [ ] Daily digest `genKeyword()` → dùng embeddingText
