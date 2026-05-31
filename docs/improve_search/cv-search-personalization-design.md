# Thiết kế tìm kiếm và cá nhân hóa dựa trên CV

## 1. Mục tiêu

Thiết kế lại luồng tìm kiếm và gợi ý việc làm để CV đã upload có thể tham gia vào:

- Cá nhân hóa việc làm cho ứng viên.
- Tìm kiếm việc làm khi ứng viên chưa nhập keyword.
- Digest job hằng ngày/hằng tuần.
- HR tìm kiếm ứng viên chính xác hơn dựa trên profile + CV.

**Nguyên tắc chính:** CV là lớp bằng chứng bổ trợ, không thay thế hoàn toàn ý định tìm việc của ứng viên. Nếu CV khớp với keyword/JD thì boost điểm. Nếu CV không khớp thì giảm ưu tiên vừa phải, không loại ứng viên/job quá sớm.

---

## 2. Hiện trạng code

Backend hiện đã có nền tảng:

- `CloudinaryServiceImpl.uploadFile()` upload CV và lưu row vào bảng `file`.
- Sau upload CV phát `ResumeFilePersistedEvent`.
- `ResumeTextExtractionEventListener` gọi `ResumeTextExtractionServiceImpl.extractAndPersistByFileId()`.
- `ResumeTextExtractionServiceImpl` trích text CV và lưu vào `file.resume_extracted_text`.
- `CandidateES` đã có field `resumeText`.
- `CandidateESServiceImpl.mapToES()` đã lấy CV mới nhất có:
  - `ownerId = candidateId`
  - `status = ACTIVE`
  - `fileType in RESUME/CV`
  - `shareToFindJob = true`
- HR search candidate đã match thêm `resumeText`.
- Candidate personalized job hiện chỉ tạo keyword từ `desiredPosition + locations + industries`.
- Daily digest hiện chỉ tạo keyword từ `desiredPosition + industry + locations`.

**Vấn đề chính:**

- Sau khi extract CV xong, chưa publish event để re-index candidate vào Elasticsearch.
- Khi xóa CV, chưa re-index để xóa `resumeText` khỏi `candidates_es`.
- Chưa có API/flow rõ ràng để bật/tắt `shareToFindJob`.
- Candidate personalized job/search job chưa tận dụng CV.
- Job index còn thiếu nhiều text quan trọng như `qualifications`, `responsibilities`, `minimumQualifications`, nên CV match job chưa đủ sâu.
- Ranking dễ bị loãng vì profile intent và CV evidence đang trộn với nhau nhưng chưa có trọng số rõ ràng.

---

## 3. Nguyên tắc enterprise production

Tách 3 nhóm tín hiệu rõ ràng:

### Intent signal
- `desiredPosition`
- `industries`
- `locations`
- `workTypes`
- `salaryExpectationMin`
- `salaryExpectationMax`
- `isOpenToWork`
- `isOpenToNotifyNewJob`

### Profile signal
- `skills`
- `currentJobTitle`
- `summary`
- `yearsOfExperience`
- `educationLevel`

### CV evidence signal
- `resumeExtractedText`
- resume embedding
- optional extracted keywords/skills (về sau)

CV chỉ nên boost khi khớp. CV không nên làm mất kết quả nếu ứng viên đang có intent rõ ràng.

---

## 4. Ranking scenario: HR tìm "java developer"

Ví dụ:

- **A:** `desiredPosition = Java Developer`, không upload CV.
- **B:** `desiredPosition = Java Developer`, CV liên quan Java.
- **C:** `desiredPosition = Java Developer`, CV không liên quan Java.

Thứ tự mong muốn:

1. **B:** intent khớp + CV evidence khớp.
2. **A:** intent khớp nhưng thiếu CV evidence.
3. **C:** intent khớp nhưng CV evidence yếu/không liên quan.

Lý do không loại C hoàn toàn: CV có thể cũ, ứng viên có thể đang chuyển hướng nghề nghiệp. Tuy nhiên C không nên đứng ngang B/A nếu query của HR là Java Developer.

**Công thức tư duy:**

```text
finalScore =
  intentScore          * 0.45
+ profileScore         * 0.30
+ resumeScore          * 0.20
+ freshness/completenessScore * 0.05
- contradictionPenalty
```

Giai đoạn đầu có thể implement bằng Elasticsearch boost thay vì custom score phức tạp.

---

## 5. Có nên lưu truncated CV vào Elasticsearch?

Có, nên lưu.

**Production rule:**

- PostgreSQL `file.resume_extracted_text` là source of truth.
- Elasticsearch `candidates_es.resumeText` là search projection.
- Chỉ index CV nếu:
  - file `ACTIVE`
  - type `RESUME` hoặc `CV`
  - `shareToFindJob = true`
  - extraction thành công
- Khi xóa CV hoặc tắt share CV:
  - không xóa candidate document nếu candidate vẫn đủ điều kiện search
  - re-index candidate để `resumeText = null` hoặc lấy CV ACTIVE/shared mới nhất khác
- Không expose raw `resumeText` ra API HR response.
- Nên có `resumeTextHash` / `contentHash` để tránh re-embed khi nội dung không đổi.

---

## 6. Data model và ES mapping đề xuất

### CandidateES

Giữ `resumeText`, bổ sung metadata nhẹ:

```text
resumeText
resumeFileId
resumeUpdatedAt
resumeContentHash
profileSearchText
```

Nếu muốn chuẩn hơn về ranking semantic (Phase sau):

```text
profileEmbedding
resumeEmbedding
combinedEmbedding
```

Giai đoạn đầu:

- Vẫn dùng 1 field `embedding`.
- Build embedding từ weighted text:
  - `desiredPosition / currentJobTitle / skills / summary` đặt trước.
  - `resumeText` snippet tối đa 4000 chars đặt sau.

Hiện `CandidateES.buildSearchText()` đã có resume snippet 4000 chars, nên việc quan trọng nhất là đảm bảo re-index đúng thời điểm.

### JobES

Mở rộng `jobs_es` để search job từ CV tốt hơn:

```text
title
description
jobCategory
employmentType
experienceLevel
education
state
city
qualifications
minimumQualifications
responsibilities
skills / requiredSkills        ← bổ sung so với bản gốc
companyName
searchText
```

> **Lưu ý:** Phải thêm `skills` hoặc `requiredSkills` vào JobES embedding text nếu schema job có field này. Đây là field match trực tiếp với CV skills của ứng viên — bỏ qua sẽ mất signal mạnh nhất.

Embedding của job không nên chỉ là:

```java
title + " " + jobCategory + " " + state
```

Nên là:

```text
title + jobCategory + description + qualifications + minimumQualifications
     + responsibilities + skills/requiredSkills + city/state
```

---

## 7. Đồng bộ dữ liệu CV → CandidateES

### 7.1 Upload CV

**Flow đề xuất:**

```text
upload CV
-> save file row
-> publish ResumeFilePersistedEvent(fileId)
-> async extract text
-> so sánh resumeContentHash mới vs hash đang lưu trong DB   ← GUARD idempotency
-> nếu hash không đổi: bỏ qua, không publish event
-> nếu hash thay đổi: lưu hash + extracted text
-> publish CandidateUpdatedEvent(candidateId, RESUME_UPDATED)
-> CandidateElasticsearchEventListener re-index candidate
```

> **Quan trọng:** `CandidateESServiceImpl.mapToES()` phải **query DB tại thời điểm re-index** để lấy CV ACTIVE/shared mới nhất — không dùng `fileId` từ event payload để quyết định CV nào được index. Điều này tránh race condition khi candidate upload nhiều CV liên tiếp: extract CV A xong sau extract CV B → ES không bị stale về CV A.

### 7.2 Xử lý extraction fail

```text
Nếu extraction fail:
- KHÔNG publish CandidateUpdatedEvent (tránh giữ lại resumeText cũ bị stale)
- Hoặc publish với flag RESUME_EXTRACTION_FAILED để listener set resumeText = null
- Không retry vô hạn: sau N lần fail, mark file.extraction_status = FAILED
- Log error + alert nếu fail rate vượt threshold (Phase 3)
```

### 7.3 Delete CV

**Flow đề xuất:**

```text
candidate delete file
-> soft delete: set file.status = DELETED
-> publish CandidateUpdatedEvent(candidateId, RESUME_DELETED)
-> re-index candidate
-> CandidateES.resolveResumeText() tự lấy CV ACTIVE/shared mới nhất khác, hoặc null
```

### 7.4 Toggle shareToFindJob

**API:**

```http
PUT /candidates/media/{fileId}/share-to-find-job
```

**Request body:**

```json
{
  "enabled": true
}
```

**Response** (trả toàn bộ list CV để frontend đồng bộ trạng thái):

```json
[
  { "id": "cv-1", "shareToFindJob": false },
  { "id": "cv-2", "shareToFindJob": true }
]
```

**Backend behavior:**

- Validate owner là candidate hiện tại.
- Chỉ cho `RESUME` / `CV`.
- **Phải chạy trong `@Transactional`** để tránh race condition 2 request bật 2 CV cùng lúc:
  1. `UPDATE file SET shareToFindJob = false WHERE ownerId = candidateId` (tắt tất cả CV khác).
  2. Set `shareToFindJob = true` cho CV được chọn.
  3. Commit transaction.
  4. Publish `CandidateUpdatedEvent(candidateId, RESUME_VISIBILITY_CHANGED)` **sau commit**.

### 7.5 Khi ứng viên xóa CV đang bật

```text
soft delete file
-> publish CandidateUpdatedEvent(candidateId, RESUME_DELETED)
-> re-index CandidateES
-> CandidateES.resumeText = null (không tự động bật CV khác)
```

**Khuyến nghị policy:** Không tự động bật CV khác sau khi xóa CV chính. An toàn hơn là để `resumeText = null`. Frontend hiển thị:

```text
Chưa chọn CV tìm việc chính
```

Candidate vẫn xuất hiện trong HR search nếu `isOpenToWork = true`, nhưng không còn boost từ CV. Personalized jobs vẫn chạy dựa trên profile/job criteria.

---

## 8. UX phía Candidate Profile: bật CV nào để dùng tìm kiếm

### Toggle cấp ứng viên (`Sidebar.jsx`)

```text
Cho phép Nhà tuyển dụng tìm bạn
```

Map với `candidate.isOpenToWork`. Đây là công tắc tổng:

- **OFF:** HR không thấy ứng viên trong search/suggestion.
- **ON:** HR có thể tìm thấy ứng viên nếu profile đủ điều kiện.

Toggle này không quyết định CV nào được dùng — chỉ quyết định ứng viên có xuất hiện trong HR search hay không.

### Toggle cấp CV (`CVCard.jsx`)

Mỗi CV nên có trạng thái rõ ràng hiển thị trực tiếp trên card (không chỉ nằm trong menu ba chấm):

```text
CV tìm việc chính
```

**Đề xuất UX:**

- Switch nhỏ hoặc radio-style toggle trực tiếp trên từng card.
- Chỉ 1 CV được bật tại một thời điểm.
- CV đang bật có badge:

```text
Đang dùng để tìm việc
```

- CV chưa bật có CTA:

```text
Dùng CV này
```

- Tooltip giải thích thêm:

```text
CV này sẽ được dùng để cá nhân hóa việc làm, email gợi ý việc làm
và giúp Nhà tuyển dụng tìm thấy bạn chính xác hơn.
```

**Frontend phải dùng optimistic update:**

- Ngay khi user click "Dùng CV này", cập nhật UI ngay (tắt badge CV cũ, bật badge CV mới) mà không chờ re-index ES.
- Nếu API trả lỗi, rollback về state cũ.

### Vì sao chỉ nên bật 1 CV?

- Tránh loãng thông tin khi ứng viên upload nhiều CV cho nhiều hướng nghề khác nhau.
- Ranking ổn định hơn, nhất là khi HR tìm theo keyword cụ thể.
- Dễ giải thích cho người dùng: hệ thống đang dùng CV nào để cá nhân hóa.
- Đồng bộ Elasticsearch đơn giản và ít stale hơn.

Ví dụ nếu ứng viên có: CV Java Backend, CV Business Analyst, CV Data Analyst — nếu index cả 3 cùng lúc, HR tìm "java developer" bị nhiễu bởi nội dung BA/Data.

---

## 9. Candidate site: Việc làm cá nhân

**Endpoint hiện tại:**

```http
GET /jobs/personalized
```

**Đề xuất thay bằng unified search text:**

```text
candidateIntentText =
  desiredPosition
  industries
  locations
  workTypes
  skills
  currentJobTitle
  summary
  shared resume text snippet (tối đa 4000 chars)
```

Sau đó gọi hybrid search:

```java
jobESService.knnSearch(candidateSearchText, filter, candidateId, pageable, PartyType.CANDIDATE)
```

**Ưu tiên:**

- Job `ACTIVE`, chưa expired.
- Match location / work type / filter.
- Boost job mới vừa phải.
- Fallback về popular/latest nếu candidate không có profile và không có CV.

---

## 10. Candidate site: Search khi chưa nhập keyword

Hiện `searchEmbed()` khi không có keyword lấy `_genKey(candidate)`.

**Đề xuất:**

```text
if query blank:
  keyword = buildCandidateSearchText(candidate, includeResume = true)
else:
  BM25 chạy trên query gốc (không append candidate data vào query string)
  Candidate data (desiredPosition, skills, currentJobTitle) dùng để boost
  bằng function_score hoặc rescore — không string concat vào query
```

> **Lưu ý quan trọng:** Không nên concat candidate data thẳng vào query string BM25 khi đã có keyword. Làm vậy sẽ inflate score không kiểm soát được và kết quả khó reproduce/debug. Dùng `function_score` hoặc KNN với candidate embedding làm query vector, BM25 là filter.

---

## 11. Daily/Weekly digest

`JobRecommendationServiceImpl.genKeyword()` nên dùng cùng builder với personalized jobs:

```text
desiredPosition
industries
locations
workTypes
skills
summary
shared resume text snippet (tối đa 4000 chars)
```

`searchRecommendJobsFromNewlyPosted()` nên chuyển từ BM25-only sang hybrid BM25 + KNN, và search trong `newlyPostedJobIds`.

---

## 12. HR site: tìm kiếm ứng viên

Hiện HR search đã match `resumeText`, nhưng cần chỉnh ranking.

**Fields BM25:**

```text
desiredPosition^10
currentJobTitle^7
skills^6
summary^3
resumeText^2
```

**Phrase prefix:**

```text
desiredPosition^10
currentJobTitle^7
skills^5
resumeText^1.5
```

**KNN embedding:**

- Candidate embedding đã gồm CV snippet.
- Boost vừa phải vì CV chỉ là evidence. Gợi ý: `0.5 – 0.8`.

**Filter bắt buộc:**

- Luôn filter `isOpenToWork = true`.
- `resumeText` chỉ có nếu CV được share.
- Không return `resumeText` trong DTO response.

---

## 13. Async listener và security context

> **Lưu ý kỹ thuật quan trọng:** `CandidateElasticsearchEventListener` chạy async ngoài request context. Spring Security context **không được propagate** sang async thread theo mặc định.

```text
CandidateElasticsearchEventListener phải:
- Chạy với service account hoặc bypass security context hoàn toàn
- Không dùng @PreAuthorize hay SecurityContextHolder bên trong async listener
- Đây là lỗi phổ biến khi migrate sang event-driven architecture
```

---

## 14. Các file cần sửa

### Backend

**`CandidateUpdatedEvent.java`**
- Thêm update types:
  - `RESUME_UPDATED`
  - `RESUME_DELETED`
  - `RESUME_VISIBILITY_CHANGED`
  - `RESUME_EXTRACTION_FAILED`
  - `PROFILE_UPDATED`
  - `SKILLS_UPDATED`

**`ResumeTextExtractionServiceImpl.java`**
- So sánh `resumeContentHash` mới vs hash trong DB trước khi publish event (idempotency guard).
- Nếu hash không đổi: bỏ qua.
- Nếu hash thay đổi: lưu hash + text → publish `CandidateUpdatedEvent(RESUME_UPDATED)`.
- Nếu fail: publish `CandidateUpdatedEvent(RESUME_EXTRACTION_FAILED)` hoặc không publish tùy policy.

**`CandidateServiceImpl.java`**
- Delete CV phải publish `CandidateUpdatedEvent(RESUME_DELETED)`.
- Thêm method `toggleShareToFindJob()` với `@Transactional` (tắt tất cả trước, bật 1).
- Update general info / skills / education / experience nên publish event nếu ảnh hưởng search.

**`CandidateESServiceImpl.java`**
- `mapToES()` phải **query DB tại thời điểm re-index** để lấy CV ACTIVE/shared mới nhất (không dùng fileId từ event).
- Chuẩn hóa `buildCandidateSearchText()`.
- Không index ứng viên vào HR search nếu `isOpenToWork = false`.

**`JobESServiceImpl.java`**
- Thêm reusable filter builder.
- Cải thiện `knnSearch`.
- Cải thiện `searchRecommendJobsFromNewlyPosted` (BM25 + KNN hybrid).
- Thêm support `description / qualifications / responsibilities / skills`.

**`JobServiceImpl.java`**
- Thay `_genKey()` bằng `CandidateSearchTextBuilder`.
- `getJobsPersonalizedES()` dùng CV/profile.
- `searchEmbed()` dùng candidate context khi query blank; dùng `function_score`/rescore khi có keyword.

**`JobES.java` + mapping JSON**
- Thêm fields: `qualifications`, `minimumQualifications`, `responsibilities`, `skills`/`requiredSkills`.

**`CandidateES.java` + mapping JSON**
- Thêm metadata nếu cần: `resumeFileId`, `resumeUpdatedAt`, `resumeContentHash`.

### Frontend

**`Sidebar.jsx`**
- Giữ toggle tổng "Cho phép Nhà tuyển dụng tìm bạn" cho `isOpenToWork`.

**`CVCard.jsx`**
- Thêm switch/radio-style toggle cấp từng CV để chọn 1 CV chính (hiển thị trực tiếp trên card, không chỉ trong menu ba chấm).
- Khi chọn một CV, gọi `PUT /candidates/media/{fileId}/share-to-find-job`.
- Dùng **optimistic update**: cập nhật UI ngay, rollback nếu API lỗi.
- Khi xóa CV đang bật, remove khỏi list và hiển thị "Chưa chọn CV tìm việc chính".

**HR site**
- Không cần đổi API lớn trong Phase 1.
- Có thể hiển thị score/match reason ở Phase sau.

---

## 15. Implementation phases

### Phase 1: Correctness & Sync

- Thêm event types (bao gồm `RESUME_EXTRACTION_FAILED`).
- Re-index candidate sau extract CV (có idempotency guard bằng content hash).
- Re-index candidate sau delete CV.
- Implement toggle `shareToFindJob` với `@Transactional`.
- `mapToES()` luôn query DB tại thời điểm re-index.
- Fix async listener không dùng security context.
- Đảm bảo ES không stale.

### Phase 2: Ranking & Search Quality

- Candidate personalized/search dùng CV.
- HR candidate search điều chỉnh boost theo công thức trọng số.
- Daily/weekly digest dùng CV.
- JobES embedding dùng JD đầy đủ hơn (thêm skills/qualifications/responsibilities).
- Search có keyword dùng `function_score`/rescore thay vì string concat.

### Phase 3: Production hardening

- Thêm content hash để tránh re-index/re-embed thừa.
- Thêm scheduled reconciliation job:
  - Scan candidates updated recently.
  - So sánh DB vs ES `contentHash`.
  - Re-index nếu lệch.
- Thêm logs/metrics:
  - Extraction success/fail rate.
  - Re-index success/fail.
  - ES search latency.
- Thêm tests:
  - Upload/extract triggers re-index.
  - Delete CV removes `resumeText` from ES.
  - B > A > C ranking scenario.
  - Toggle shareToFindJob với concurrent requests không bật 2 CV cùng lúc.

---

## 16. Kết luận

Hướng đúng là lưu CV extracted text vào PostgreSQL làm source of truth, sync phần được share vào `CandidateES.resumeText`, và dùng CV như evidence boost chứ không thay thế intent.

**Trọng tâm implement đầu tiên (Phase 1) là sync lifecycle:**

1. **Idempotency:** Content hash guard trước khi publish re-index event.
2. **Race condition:** `mapToES()` phải query DB tại thời điểm index, không dùng fileId từ event.
3. **Concurrency:** Toggle `shareToFindJob` phải chạy trong `@Transactional`.
4. **Async security:** Listener không được dùng Spring Security context.

Nếu CV extract xong nhưng ES không được re-index, cả cá nhân hóa việc làm lẫn HR search đều có thể trả kết quả stale và rất khó debug trong production.
