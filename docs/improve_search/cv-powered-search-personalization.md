# Thiết kế tìm kiếm và cá nhân hóa dựa trên CV

## 1. Mục tiêu

Thiết kế lại luồng tìm kiếm và gợi ý việc làm để CV đã upload có thể tham gia vào:

- Cá nhân hóa việc làm cho ứng viên.
- Tìm kiếm việc làm khi ứng viên chưa nhập keyword.
- Digest job hằng ngày/hằng tuần.
- HR tìm kiếm ứng viên chính xác hơn dựa trên profile + CV.

Nguyên tắc chính: CV là lớp bằng chứng bổ trợ, không thay thế hoàn toàn ý định tìm việc của ứng viên. Nếu CV khớp với keyword/JD thì boost điểm. Nếu CV không khớp thì giảm ưu tiên vừa phải, không loại ứng viên/job quá sớm.

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

Vấn đề chính:

- Sau khi extract CV xong, chưa publish event để re-index candidate vào Elasticsearch.
- Khi xóa CV, chưa re-index để xóa `resumeText` khỏi `candidates_es`.
- Chưa có API/flow rõ ràng để bật/tắt `shareToFindJob`.
- Candidate personalized job/search job chưa tận dụng CV.
- Job index còn thiếu nhiều text quan trọng như `qualifications`, `responsibilities`, `minimumQualifications`, nên CV match job chưa đủ sâu.
- Ranking dễ bị loãng vì profile intent và CV evidence đang trộn với nhau nhưng chưa có trọng số rõ ràng.

## 3. Nguyên tắc enterprise production

Nên tách 3 nhóm tín hiệu:

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
- optional extracted keywords/skills về sau

CV chỉ nên boost khi khớp. CV không nên làm mất kết quả nếu ứng viên đang có intent rõ ràng.

## 4. Ranking scenario: HR tìm "java developer"

Ví dụ:

- A: `desiredPosition = Java Developer`, không upload CV.
- B: `desiredPosition = Java Developer`, CV liên quan Java.
- C: `desiredPosition = Java Developer`, CV không liên quan Java.

Thứ tự mong muốn:

1. B: intent khớp + CV evidence khớp.
2. A: intent khớp nhưng thiếu CV evidence.
3. C: intent khớp nhưng CV evidence yếu/không liên quan.

Lý do không loại C hoàn toàn: CV có thể cũ, ứng viên có thể đang chuyển hướng nghề nghiệp. Tuy nhiên C không nên đứng ngang B/A nếu query của HR là Java Developer.

Công thức tư duy:

```text
finalScore =
  intentScore * 0.45
+ profileScore * 0.30
+ resumeScore * 0.20
+ freshness/completenessScore * 0.05
- contradictionPenalty
```

Giai đoạn đầu có thể implement bằng Elasticsearch boost thay vì custom score phức tạp.

## 5. Có nên lưu truncated CV vào Elasticsearch?

Có, nên lưu.

Production rule:

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
- Nên có `resumeTextHash`/`contentHash` để tránh re-embed khi nội dung không đổi.

## 6. Data model và ES mapping đề xuất

### CandidateES

Giữ `resumeText`, có thể bổ sung metadata nhẹ:

```text
resumeText
resumeFileId
resumeUpdatedAt
resumeContentHash
profileSearchText
```

Nếu muốn chuẩn hơn về ranking semantic:

```text
profileEmbedding
resumeEmbedding
combinedEmbedding
```

Giai đoạn đầu để ít thay đổi:

- Vẫn dùng 1 field `embedding`.
- Build embedding từ weighted text:
  - `desiredPosition/currentJobTitle/skills/summary` đặt trước.
  - `resumeText` snippet tối đa 4000 chars đặt sau.

Hiện `CandidateES.buildSearchText()` đã có resume snippet 4000 chars, nên việc quan trọng nhất là đảm bảo re-index đúng thời điểm.

### JobES

Nên mở rộng `jobs_es` để search job từ CV tốt hơn:

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
companyName
searchText
```

Embedding của job không nên chỉ là:

```java
title + " " + jobCategory + " " + state
```

Nên là:

```text
title + jobCategory + description + qualifications + minimumQualifications + responsibilities + city/state
```

## 7. Đồng bộ dữ liệu CV -> CandidateES

### Upload CV

Flow đề xuất:

```text
upload CV
-> save file row
-> publish ResumeFilePersistedEvent(fileId)
-> async extract text
-> save resume_extracted_text
-> publish CandidateUpdatedEvent(candidateId, RESUME_UPDATED)
-> CandidateElasticsearchEventListener re-index candidate
```

Hiện đang thiếu bước publish `CandidateUpdatedEvent` sau khi extract text thành công hoặc khi extraction fail cần xóa text cũ.

### Delete CV

Flow đề xuất:

```text
candidate delete file
-> set file.status = DELETED
-> publish CandidateUpdatedEvent(candidateId, RESUME_DELETED)
-> re-index candidate
-> CandidateES.resolveResumeText() tự lấy CV shared ACTIVE mới nhất khác, hoặc null
```

### Toggle shareToFindJob

Cần API riêng:

```http
PUT /candidates/media/{fileId}/share-to-find-job
```

Behavior:

- Validate owner là candidate hiện tại.
- Chỉ cho `RESUME`/`CV`.
- Set `shareToFindJob`.
- Nếu bật CV này, nên tắt các CV khác để đảm bảo chỉ 1 CV chính dùng cho search.
- Publish `CandidateUpdatedEvent(candidateId, RESUME_VISIBILITY_CHANGED)`.

## 8. UX phía Candidate Profile: bật CV nào để dùng tìm kiếm

Hiện tại frontend có 2 khái niệm khác nhau, cần tách rõ để người dùng không hiểu nhầm:

### Toggle cấp ứng viên

Toggle trong `Sidebar.jsx`:

```text
Cho phép Nhà tuyển dụng tìm bạn
```

Toggle này map với `candidate.isOpenToWork`. Đây là công tắc tổng:

- OFF: HR không thấy ứng viên trong search/suggestion.
- ON: HR có thể tìm thấy ứng viên nếu profile đủ điều kiện.

Toggle này không quyết định CV nào được dùng. Nó chỉ quyết định ứng viên có xuất hiện trong HR search hay không.

### Toggle cấp CV

Trong `CVCard.jsx`, mỗi CV nên có trạng thái rõ ràng:

```text
Dùng CV này để cá nhân hóa việc làm và cho Nhà tuyển dụng tìm thấy
```

Hoặc nếu muốn ngắn hơn:

```text
CV tìm việc chính
```

Đề xuất UX:

- Không nên chỉ để trong menu ba chấm với một dòng "Cho phép tìm kiếm CV", vì hành động này quan trọng và dễ bị bỏ sót.
- Nên hiển thị trực tiếp trên từng CV một switch nhỏ hoặc radio-style toggle.
- Chỉ 1 CV được bật tại một thời điểm.
- CV đang bật nên có badge rõ:

```text
Đang dùng để tìm việc
```

- CV chưa bật có CTA rõ:

```text
Dùng CV này
```

Tooltip chỉ nên dùng để giải thích thêm, không nên là nơi duy nhất chứa ý nghĩa hành động.

Tooltip gợi ý:

```text
CV này sẽ được dùng để cá nhân hóa việc làm, email gợi ý việc làm và giúp Nhà tuyển dụng tìm thấy bạn chính xác hơn.
```

### Vì sao chỉ nên bật 1 CV?

Nên chọn 1 CV chính vì:

- Tránh loãng thông tin khi ứng viên upload nhiều CV cho nhiều hướng nghề khác nhau.
- Ranking ổn định hơn, nhất là khi HR tìm theo keyword cụ thể.
- Dễ giải thích cho người dùng: hệ thống đang dùng CV nào để cá nhân hóa.
- Đồng bộ Elasticsearch đơn giản và ít stale hơn.

Ví dụ nếu ứng viên có:

- CV Java Backend
- CV Business Analyst
- CV Data Analyst

Nếu index cả 3 CV cùng lúc, HR tìm "java developer" có thể bị nhiễu bởi nội dung BA/Data. Vì vậy production nên chọn 1 CV chính cho search/personalization.

### Khi ứng viên xóa CV đang bật

Nếu ứng viên xóa CV có `shareToFindJob = true`, backend phải xử lý:

```text
soft delete file
-> publish CandidateUpdatedEvent(candidateId, RESUME_DELETED)
-> re-index CandidateES
-> CandidateES.resumeText = null hoặc lấy CV shared ACTIVE khác nếu policy cho phép
```

Khuyến nghị policy:

- Không tự động bật CV khác sau khi xóa CV chính, trừ khi sản phẩm muốn auto fallback.
- An toàn hơn: sau khi xóa CV chính, không còn CV nào được dùng cho tìm việc.
- Frontend hiển thị trạng thái:

```text
Chưa chọn CV tìm việc chính
```

Khi re-index xong:

- `candidates_es.resumeText` phải được xóa hoặc thay bằng CV chính mới.
- Candidate vẫn có thể xuất hiện trong HR search nếu `isOpenToWork = true`, nhưng không còn boost từ CV.
- Personalized jobs vẫn chạy dựa trên profile/job criteria, chỉ mất CV evidence.

### API frontend cần gọi

`CVCard.jsx` nên gọi API thật thay vì TODO:

```http
PUT /candidates/media/{fileId}/share-to-find-job
```

Request body đề xuất:

```json
{
  "enabled": true
}
```

Response trả về danh sách CV đã cập nhật hoặc file vừa cập nhật:

```json
{
  "id": "file-id",
  "shareToFindJob": true
}
```

Tốt nhất response nên trả về toàn bộ list CV sau update để frontend dễ đồng bộ trạng thái chỉ 1 CV được bật:

```json
[
  { "id": "cv-1", "shareToFindJob": false },
  { "id": "cv-2", "shareToFindJob": true }
]
```

## 9. Candidate site: Việc làm cá nhân

Endpoint hiện tại:

```java
GET /jobs/personalized
```

Hiện đang dùng `_genKey(candidate)` rồi `searchJobsByNavtiveAndFuzzy()`.

Đề xuất thay bằng unified search text:

```text
candidateIntentText =
  desiredPosition
  industries
  locations
  workTypes
  skills
  currentJobTitle
  summary
  shared resume text snippet
```

Sau đó gọi hybrid search:

```java
jobESService.knnSearch(candidateSearchText, filter, candidateId, pageable, PartyType.CANDIDATE)
```

Ưu tiên:

- Job `ACTIVE`.
- Job chưa expired.
- Match location/work type/filter.
- Boost job mới vừa phải.
- Fallback về popular/latest nếu candidate không có profile và không có CV.

## 10. Candidate site: Search khi chưa nhập keyword

Hiện `searchEmbed()` khi không có keyword lấy `_genKey(candidate)`.

Nên đổi thành:

```text
if query blank:
  keyword = buildCandidateSearchText(candidate, includeResume = true)
else:
  keyword = query + candidate context nhẹ
```

Với query có keyword, không nên append full CV quá mạnh vì user đang có intent rõ. Nên dùng:

```text
query + desiredPosition + skills + currentJobTitle
```

Không dùng toàn bộ resume trong text query có keyword, hoặc chỉ dùng resume snippet với boost thấp ở ES.

## 11. Daily/Weekly digest

Code hiện tại tên `DailyDigestScheduler`, nhưng business có thể chạy hằng ngày hoặc hằng tuần tùy cron.

`JobRecommendationServiceImpl.genKeyword()` nên dùng cùng builder với personalized jobs:

```text
desiredPosition
industries
locations
workTypes
skills
summary
shared resume text snippet
```

`searchRecommendJobsFromNewlyPosted()` nên chuyển từ BM25-only sang hybrid BM25 + KNN, và search trong `newlyPostedJobIds`.

## 12. HR site: tìm kiếm ứng viên

Hiện HR search đã match `resumeText`, nhưng cần chỉnh ranking.

Fields đề xuất cho BM25:

```text
desiredPosition^10
currentJobTitle^7
skills^6
summary^3
resumeText^2
```

Phrase prefix:

```text
desiredPosition^10
currentJobTitle^7
skills^5
resumeText^1.5
```

KNN embedding:

- Candidate embedding đã gồm CV snippet.
- Boost vừa phải, vì CV chỉ là evidence.
- Gợi ý `0.5 - 0.8`.

Filter:

- Luôn filter `isOpenToWork = true`.
- Chỉ có `resumeText` nếu CV được share.
- Không return `resumeText` trong DTO.

## 13. Các file cần sửa

Backend:

- `CandidateUpdatedEvent.java`
  - Thêm update types:
    - `RESUME_UPDATED`
    - `RESUME_DELETED`
    - `RESUME_VISIBILITY_CHANGED`
    - `PROFILE_UPDATED`
    - `SKILLS_UPDATED`
- `ResumeTextExtractionServiceImpl.java`
  - Sau khi lưu extracted text, publish candidate re-index event.
- `CandidateServiceImpl.java`
  - Delete CV phải publish re-index event.
  - Thêm method toggle `shareToFindJob`.
  - Update general info / skills / education / experience nên publish event nếu ảnh hưởng search.
- `CandidateESServiceImpl.java`
  - Chuẩn hóa `buildCandidateSearchText`.
  - Đảm bảo resolve CV đúng rule.
  - Không index ứng viên vào HR search nếu policy yêu cầu `isOpenToWork = false`.
- `JobESServiceImpl.java`
  - Thêm reusable filter builder.
  - Cải thiện `knnSearch`.
  - Cải thiện `searchRecommendJobsFromNewlyPosted`.
  - Thêm support `description/qualifications/responsibilities`.
- `JobServiceImpl.java`
  - Thay `_genKey()` bằng `CandidateSearchTextBuilder`.
  - `getJobsPersonalizedES()` dùng CV/profile.
  - `searchEmbed()` dùng candidate context khi query blank.
- `JobES.java` + mapping JSON
  - Thêm fields JD chi tiết.
- `CandidateES.java` + mapping JSON
  - Thêm resume metadata nếu cần.

Frontend:

- Candidate site:
  - Hiển thị trạng thái CV đang dùng cho tìm việc.
  - `Sidebar.jsx` giữ toggle tổng "Cho phép Nhà tuyển dụng tìm bạn" cho `isOpenToWork`.
  - `CVCard.jsx` thêm switch/radio-style toggle cấp từng CV để chọn đúng 1 CV chính.
  - Menu ba chấm có thể giữ action phụ, nhưng trạng thái CV chính nên hiển thị trực tiếp trên card.
  - Khi chọn một CV, gọi API `share-to-find-job`; backend tắt các CV khác.
  - Khi xóa CV đang bật, frontend remove CV khỏi list và hiển thị "Chưa chọn CV tìm việc chính".
- HR site:
  - Không cần đổi API lớn trong phase đầu.
  - Có thể hiển thị score/match reason sau này.

## 14. Implementation phases

### Phase 1: Correctness & Sync

- Thêm event types.
- Re-index candidate sau extract CV.
- Re-index candidate sau delete CV.
- Thêm/tinh chỉnh toggle `shareToFindJob`.
- Đảm bảo ES không stale.

### Phase 2: Ranking & Search Quality

- Candidate personalized/search dùng CV.
- HR candidate search điều chỉnh boost.
- Daily/weekly digest dùng CV.
- JobES embedding dùng JD đầy đủ hơn.

### Phase 3: Production hardening

- Thêm content hash để tránh re-index/re-embed thừa.
- Thêm scheduled reconciliation job:
  - scan candidates updated recently
  - compare DB vs ES contentHash
  - re-index nếu lệch
- Thêm logs/metrics:
  - extraction success/fail
  - re-index success/fail
  - ES search latency
- Thêm tests:
  - upload/extract triggers re-index
  - delete CV removes `resumeText` from ES
  - B > A > C ranking scenario

## 15. Kết luận

Hướng đúng là lưu CV extracted text vào PostgreSQL làm source of truth, sync phần được share vào `CandidateES.resumeText`, và dùng CV như evidence boost chứ không thay thế intent.

Trọng tâm implement đầu tiên nên là sync lifecycle. Nếu CV extract xong nhưng ES không được re-index, cả cá nhân hóa việc làm lẫn HR search đều có thể trả kết quả stale và khó debug.
