# Candidate Index Sync - HR Search

## Muc tieu nghiep vu

HR tim ung vien phu hop nhat voi nhu cau tuyen dung.

- Neu HR chua nhap keyword, backend dung cac job ACTIVE cua company lam ngu canh tim kiem.
- Neu HR nhap keyword, backend dung keyword do lam ngu canh tim kiem.
- Candidate chi xuat hien trong HR search khi `isOpenToWork = true` va co tin hieu nghe nghiep du de tim kiem.

## Nguon du lieu Candidate Index

Candidate Elasticsearch document gom hai nhom tin hieu rieng:

- Intent/profile: `desiredPosition`, `currentJobTitle`, `summary`, `skills`, `industries`, `locations`, `workTypes`, salary, education, experience.
- Shared CV: chi CV/resume ACTIVE duoc candidate bat `shareToFindJob = true`.

Quyet dinh thiet ke production:

- HR search khong dung tat ca CV da upload. Neu du lieu cu co nhieu CV cung `shareToFindJob = true`, backend lay CV moi nhat.
- CV raw text chi cat toi 1000 ky tu de giam nhieu.
- `cvKeywords` duoc uu tien hon `resumeText` khi search/embedding.
- Tieu chi tim viec co the null. Neu candidate khong co tieu chi nhung co CV shared da extract text/keywords, candidate van co the duoc index.
- Neu candidate khong co intent va CV shared chua extract xong, document chua duoc index. Sau khi extraction listener hoan tat, event se sync lai.

## Dieu kien index/xoa

`CandidateESService.syncCandidate()` va `syncAllCandidates()` dung chung rule:

- `isOpenToWork = false`: xoa document khoi Elasticsearch.
- `isOpenToWork = true` va co profile intent: index document.
- `isOpenToWork = true`, khong co profile intent, nhung co CV shared co `cvKeywords` hoac `resumeText`: index document.
- Khong co tin hieu nghe nghiep nao: xoa document de tranh ket qua rong/nhieu.

## Cac event dong bo

- Candidate cap nhat thong tin/profile/job criteria/skills/job search status: publish `CandidateUpdatedEvent`.
- Upload CV: `ResumeFilePersistedEvent` extract text/keywords, sau do publish `CandidateUpdatedEvent.RESUME_UPDATED`.
- Toggle CV share: publish `CandidateUpdatedEvent.RESUME_VISIBILITY_CHANGED`.
- Toggle CV share khong tai lai Cloudinary. ES chi lay du lieu CV da luu trong DB; neu CV chua co text thi dung fallback metadata ten file.
- Delete CV: set file `DELETED`, tat `shareToFindJob`, publish `CandidateUpdatedEvent.RESUME_DELETED`.

Tat ca event sau commit deu goi `CandidateESService.syncCandidate(candidateId)` de dam bao cung mot rule index/xoa.

## CV chua extract text

Upload CV moi uu tien extract text truc tiep tu file local/temp ngay trong flow upload. Cach nay tranh loi moi truong khong resolve duoc `res.cloudinary.com` khi worker tai nguoc file tu Cloudinary.

Neu CV da upload tu truoc va chua co text:

- Khi bat `shareToFindJob`, backend khong tai nguoc Cloudinary de extract lai trong synchronous user flow.
- Trong luc CV chua co extracted text/keywords, Candidate index dung tam metadata ten file CV (`fileName`, `originalFileName`) lam tin hieu yeu de ung vien khong bi xoa khoi ES hoan toan.
- De match tot keyword nhu `java developer` hoac `full stack`, CV can extract thanh cong hoac ten file CV phai chua cac keyword do.

## Backfill/initial sync

`CandidateESService.syncAllCandidates()` quet toan bo Candidate DB:

- Candidate hop le se duoc index/update.
- Candidate khong con hop le se bi xoa khoi ES neu document cu con ton tai.
- So luong tra ve chi dem candidate thuc su duoc index.

Internal sync `POST /internal/elasticsearch/sync?target=candidates` da duoc noi ve service nay de tranh lech logic giua startup/backfill va event runtime.

## HR search flow

Endpoint: `POST /candidates/suggestion/search`

- `keyword` rong/null: `searchCandidatesForCompany(companyId, filter, pageable)` lay toi 20 active jobs cua company, build query tu title + top qualifications.
- `keyword` co gia tri: `hybridSearchCandidates(keyword, filter, pageable)`.
- Ca hai flow deu filter `isOpenToWork = true`.
- Ranking dung hybrid BM25 + vector embedding, boost cao cho intent (`desiredPosition`, `currentJobTitle`, `skills`), sau do moi den `cvKeywords`, cuoi cung `resumeText`.

## HR result actions

Candidate search response tra them metadata cua CV dang duoc candidate bat chia se:

- `resumeFileId`: id file CV/resume dang shared.
- `resumeFileName`: ten file hien thi cho HR.
- `resumeUrl`: URL mo CV truc tiep.
- `profileUrl`: deep link den man hinh ho so ung vien trong HR app.

Frontend HR dung cac field nay de hien:

- Nut `Xem CV`: mo `resumeUrl` tren tab moi.
- Nut `Xem ho so`: dieu huong den `profileUrl` va chon dung candidate qua query `candidateId`.
- Nut `Chat`: goi `POST /messages/threads` voi `candidateId`, sau do dieu huong den `/messages?thread={threadId}`.

## Messaging draft visibility

Khi HR bam `Chat`, backend chi tao/lay `MessageThread`, chua tao `Message` va chua gui notification.

Quyet dinh production:

- HR duoc thay thread nhap de soan tin dau tien.
- Candidate inbox va unread count chi lay thread co `lastMessageAt IS NOT NULL`.
- Vi vay neu HR mo thread nhung chua nhan tin, candidate khong thay cuoc tro chuyen trong trang quan ly tin nhan.
- Khi HR gui message dau tien, `sendMessage()` cap nhat `lastMessageAt`, `lastMessagePreview`, xoa deletion/archive state va goi notification nhu flow hien tai.

## Diem can luu y tiep theo

- Nen co monitoring cho so luong candidate indexed va so luong bi skipped do chua co intent/CV.
- Nen co retry/outbox neu async event sync ES fail nhieu lan.
- Neu sau nay cho phep nhieu CV shared, nen tao nested `sharedResumes[]` co type/weight thay vi ghep text vao mot field.
