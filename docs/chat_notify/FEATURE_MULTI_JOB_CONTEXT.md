# FEATURE: Multi-Job Context Messaging
> **Bổ sung cho Phase 1 BE + Phase 3 HR FE + Phase 4 Candidate FE**  
> Giải quyết: 1 candidate ứng tuyển nhiều job cùng công ty → chat cần context rõ ràng.

---

## 🎯 Quyết định kiến trúc

**KHÔNG tạo thread riêng cho mỗi job.**  
**Giữ 1 thread/cặp HR-Candidate, thêm Job Context Layer.**

### Lý do
| Nếu tách thread per-job | Nếu giữ 1 thread + context |
|------------------------|--------------------------|
| HR có 3 thread với cùng 1 người → rối inbox | Inbox gọn, 1 người = 1 conversation |
| Không thể nói chuyện chung | Vẫn có thể nhắn tin "chung" không liên quan job cụ thể |
| Miss context khi cùng 1 topic liên quan 2 job | Có thể filter xem riêng từng job |
| Notification rối: "3 tin nhắn mới" từ cùng 1 người | 1 thread = 1 notification entry |

### Model cuối cùng
- `message_threads`: 1 record per (hr_user_id, candidate_user_id) — **giữ nguyên**
- `messages`: thêm `job_context_id` (nullable) — **tin nhắn có thể gắn tag job**
- UI: Job Switcher trên input + Filter bar + Job Divider trong chat + Notification banner

---

## 📋 BƯỚC 1 — Database (thêm vào migration hiện có)

```sql
-- Thêm job context vào messages
ALTER TABLE messages 
    ADD COLUMN IF NOT EXISTS job_context_id UUID REFERENCES jobs(id);

CREATE INDEX idx_messages_job_context ON messages(thread_id, job_context_id);

-- Thêm job context vào thread để biết jobs liên quan
-- (Denormalized để tránh query phức tạp khi hiện chips trong inbox)
-- Không cần bảng mới: query từ applications hoặc từ messages
```

> Agent ghi chú: `job_context_id` nullable — tin nhắn "Chung" (không liên quan job cụ thể) để null. Không bắt buộc gắn tag.

---

## 📋 BƯỚC 2 — Cập nhật BE

### 2a. Entity & DTO

```java
// Cập nhật Message.java — thêm:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "job_context_id")
private Job jobContext; // nullable

// Cập nhật MessageDto — thêm:
public record MessageDto(
    // ... existing fields ...
    JobContextDto jobContext  // nullable
) {}

public record JobContextDto(
    UUID jobId,
    String jobTitle,
    String jobStatus  // trạng thái application của candidate với job này
) {}
```

### 2b. Cập nhật SendMessageRequest

```java
public record SendMessageRequest(
    @NotBlank String content,
    MessageContentType contentType,
    String fileUrl,
    String fileName,
    Long fileSize,
    UUID jobContextId  // ← THÊM, nullable
) {}
```

### 2c. Cập nhật MessageService.sendMessage()

```java
public MessageDto sendMessage(UUID threadId, UUID senderId, SendMessageRequest request) {
    // ... existing validation ...
    
    message.setContent(request.content());
    message.setContentType(request.contentType() != null ? request.contentType() : MessageContentType.TEXT);
    
    // Gắn job context nếu có
    if (request.jobContextId() != null) {
        // Validate: job này phải liên quan đến thread (candidate đã apply job này)
        boolean jobBelongsToThread = applicationRepository.existsByJobIdAndCandidateId(
            request.jobContextId(),
            thread.getCandidateUser().getId()
        );
        if (!jobBelongsToThread) {
            throw new BusinessException("Job không thuộc cuộc trò chuyện này");
        }
        message.setJobContext(jobRepository.getReferenceById(request.jobContextId()));
    }
    
    // ... rest of existing logic ...
}
```

### 2d. API mới: lấy danh sách jobs liên quan thread

```java
// GET /api/v1/messages/threads/{threadId}/jobs
// Trả về: danh sách jobs candidate đã apply mà cả HR này phụ trách
// Dùng để render Job Switcher + Filter bar

@GetMapping("/threads/{threadId}/jobs")
public ResponseEntity<List<ThreadJobDto>> getThreadJobs(@PathVariable UUID threadId) {
    return ResponseEntity.ok(messageService.getThreadJobs(threadId, getCurrentUserId()));
}

// ThreadJobDto:
public record ThreadJobDto(
    UUID jobId,
    String jobTitle,
    String jobStatus,          // PENDING / REVIEWING / INTERVIEW / etc.
    long unreadCount,          // tin chưa đọc về job này
    Instant lastMessageAt,     // tin nhắn gần nhất về job này
    boolean hasMessages        // đã có tin nhắn về job này chưa
) {}
```

```java
// MessageService.getThreadJobs():
public List<ThreadJobDto> getThreadJobs(UUID threadId, UUID requesterId) {
    validateThreadAccess(threadId, requesterId);
    
    MessageThread thread = threadRepository.findById(threadId).orElseThrow();
    UUID candidateId = thread.getCandidateUser().getId();
    UUID hrId = thread.getHrUser().getId();
    
    // Lấy tất cả applications của candidate với các jobs của HR này
    // (hoặc jobs của công ty HR này thuộc về)
    return applicationRepository.findByCandidateIdAndHrId(candidateId, hrId)
        .stream()
        .map(app -> {
            long unread = messageRepository.countUnreadByThreadAndJob(
                threadId, app.getJob().getId(), requesterId);
            Instant lastMsgAt = messageRepository.findLastMessageTimeByThreadAndJob(
                threadId, app.getJob().getId()).orElse(null);
            
            return new ThreadJobDto(
                app.getJob().getId(),
                app.getJob().getTitle(),
                app.getStatus().name(),
                unread,
                lastMsgAt,
                lastMsgAt != null
            );
        })
        .sorted(Comparator.comparing(ThreadJobDto::lastMessageAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
        .collect(Collectors.toList());
}
```

### 2e. Cập nhật getMessages() — filter theo job

```java
// GET /api/v1/messages/threads/{threadId}/messages?jobId=xxx (optional)
// Nếu có jobId → filter chỉ tin nhắn của job đó
// Nếu không có jobId → trả tất cả (kèm job context info)

@GetMapping("/threads/{threadId}/messages")
public ResponseEntity<Page<MessageDto>> getMessages(
    @PathVariable UUID threadId,
    @RequestParam(required = false) UUID jobId,  // ← THÊM filter
    Pageable pageable) {
    return ResponseEntity.ok(messageService.getMessages(threadId, getCurrentUserId(), jobId, pageable));
}
```

### 2f. Cập nhật ThreadSummaryDto — thêm jobs info

```java
public record ThreadSummaryDto(
    UUID threadId,
    UserSummaryDto otherUser,
    // XÓA: ApplicationSummaryDto application (1 application cũ)
    // THÊM:
    List<ThreadJobDto> jobs,        // tất cả jobs liên quan
    ThreadJobDto primaryJob,        // job gần nhất có tin nhắn (hoặc job apply đầu tiên)
    String lastMessagePreview,
    Instant lastMessageAt,
    long unreadCount,
    boolean isOnline,
    boolean isArchived,
    boolean isBlocked
) {}
```

---

## 📋 BƯỚC 3 — HR FE (React TypeScript)

### 3a. Cập nhật Types

```typescript
// Thêm vào messaging.types.ts:

export interface ThreadJob {
  jobId: string;
  jobTitle: string;
  jobStatus: string;
  unreadCount: number;
  lastMessageAt: string | null;
  hasMessages: boolean;
}

// Cập nhật Message — thêm:
export interface Message {
  // ... existing ...
  jobContext?: {
    jobId: string;
    jobTitle: string;
    jobStatus: string;
  } | null;
}

// Cập nhật ThreadSummary:
export interface ThreadSummary {
  // ... existing ...
  jobs: ThreadJob[];       // ← thay thế application
  primaryJob: ThreadJob | null;
  // XÓA: application field
}
```

### 3b. Job Switcher trong MessageInput area

```tsx
// JobContextSelector.tsx
// Thanh nhỏ ngay trên input, cho HR/candidate chọn đang nhắn về job nào

interface Props {
  jobs: ThreadJob[];
  selectedJobId: string | null;  // null = "Chung"
  onSelect: (jobId: string | null) => void;
}

export const JobContextSelector: React.FC<Props> = ({ jobs, selectedJobId, onSelect }) => {
  return (
    <div className="job-ctx-selector">
      <span className="ctx-label">Về job:</span>
      <div className="ctx-options">
        {jobs.map(job => (
          <button
            key={job.jobId}
            className={`ctx-chip ${selectedJobId === job.jobId ? 'active' : ''}`}
            onClick={() => onSelect(selectedJobId === job.jobId ? null : job.jobId)}
            title={job.jobTitle}
          >
            {/* Truncate dài quá 18 ký tự */}
            {job.jobTitle.length > 18 ? job.jobTitle.slice(0, 16) + '…' : job.jobTitle}
            {job.unreadCount > 0 && (
              <span className="ctx-unread">{job.unreadCount}</span>
            )}
          </button>
        ))}
        <button
          className={`ctx-chip ${selectedJobId === null ? 'active-general' : ''}`}
          onClick={() => onSelect(null)}
        >
          Chung
        </button>
      </div>
    </div>
  );
};
```

**CSS:**
```css
.job-ctx-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: var(--color-background-secondary);
  border-top: 0.5px solid var(--color-border-tertiary);
  overflow-x: auto;
  /* Hide scrollbar nhưng vẫn scroll được */
  scrollbar-width: none;
}
.job-ctx-selector::-webkit-scrollbar { display: none; }

.ctx-chip {
  font-size: 11px;
  padding: 3px 9px;
  border-radius: 10px;
  border: 0.5px solid var(--color-border-secondary);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
  background: var(--color-background-primary);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}
.ctx-chip.active {
  background: var(--primary-color, #185FA5);
  color: #fff;
  border-color: transparent;
}
.ctx-chip.active-general {
  background: var(--color-background-tertiary);
  color: var(--color-text-primary);
}
.ctx-unread {
  background: #ef4444;
  color: #fff;
  font-size: 9px;
  border-radius: 6px;
  padding: 0 4px;
  min-width: 14px;
  text-align: center;
}

/* Input placeholder thay đổi theo context */
/* Trong MessageInput.tsx: */
/* placeholder={selectedJobId 
    ? `Nhắn về ${selectedJobTitle}...` 
    : 'Nhập tin nhắn...'} */
```

### 3c. Filter Bar trong ChatWindow header

```tsx
// JobFilterBar.tsx
// Tabs để lọc xem tin nhắn của job nào

interface Props {
  jobs: ThreadJob[];
  activeFilter: string | null;  // null = tất cả
  onFilter: (jobId: string | null) => void;
}

export const JobFilterBar: React.FC<Props> = ({ jobs, activeFilter, onFilter }) => {
  // Chỉ hiện nếu có >= 2 jobs
  if (jobs.length < 2) return null;

  return (
    <div className="job-filter-bar">
      <button
        className={`filter-tab ${activeFilter === null ? 'active' : ''}`}
        onClick={() => onFilter(null)}
      >
        Tất cả
        {jobs.reduce((sum, j) => sum + j.unreadCount, 0) > 0 && (
          <span className="filter-badge">
            {jobs.reduce((sum, j) => sum + j.unreadCount, 0)}
          </span>
        )}
      </button>
      {jobs.map(job => (
        <button
          key={job.jobId}
          className={`filter-tab ${activeFilter === job.jobId ? 'active' : ''}`}
          onClick={() => onFilter(job.jobId)}
        >
          {job.jobTitle.length > 15 ? job.jobTitle.slice(0, 13) + '…' : job.jobTitle}
          {job.unreadCount > 0 && (
            <span className="filter-badge">{job.unreadCount}</span>
          )}
        </button>
      ))}
    </div>
  );
};
```

```css
.job-filter-bar {
  display: flex;
  gap: 0;
  padding: 0 12px;
  border-bottom: 0.5px solid var(--color-border-tertiary);
  background: var(--color-background-secondary);
  overflow-x: auto;
  scrollbar-width: none;
}
.filter-tab {
  font-size: 12px;
  padding: 8px 12px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--color-text-secondary);
  border-bottom: 2px solid transparent;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: all 0.15s;
}
.filter-tab.active {
  color: var(--color-text-primary);
  border-bottom-color: var(--primary-color, #185FA5);
  font-weight: 500;
}
.filter-badge {
  background: #ef4444;
  color: #fff;
  font-size: 9px;
  border-radius: 6px;
  padding: 1px 4px;
}
```

### 3d. Job Divider trong Message List

```tsx
// JobDivider.tsx — separator giữa các group tin nhắn theo job

interface Props {
  jobTitle: string | null;  // null = "Tin nhắn chung"
  date?: string;
}

export const JobDivider: React.FC<Props> = ({ jobTitle, date }) => (
  <div className="job-divider">
    <div className="jd-line" />
    <span className="jd-label">
      {jobTitle ?? 'Tin nhắn chung'}
      {date && ` · ${date}`}
    </span>
    <div className="jd-line" />
  </div>
);
```

```css
.job-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 14px 0 8px;
}
.jd-line {
  flex: 1;
  height: 0.5px;
  background: var(--color-border-tertiary);
}
.jd-label {
  font-size: 10px;
  color: var(--color-text-tertiary);
  white-space: nowrap;
  padding: 0 4px;
}
```

### 3e. Message bubble — hiển thị job tag nhỏ

```tsx
// Trong MessageBubble.tsx, dưới content:
{message.jobContext && (
  <div className="msg-job-tag">
    <span
      className="job-tag-dot"
      style={{ background: getJobColor(message.jobContext.jobId) }}
    />
    {message.jobContext.jobTitle}
  </div>
)}
```

```css
.msg-job-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  margin-top: 4px;
  opacity: 0.65;
}
.job-tag-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex-shrink: 0;
}
```

**Hàm getJobColor — assign màu nhất quán per job:**
```typescript
const JOB_COLORS = ['#378ADD', '#534AB7', '#1D9E75', '#D85A30', '#D4537E'];

// Dùng index để assign màu (stable per job trong session)
const jobColorCache = new Map<string, string>();
let colorIndex = 0;

export function getJobColor(jobId: string): string {
  if (!jobColorCache.has(jobId)) {
    jobColorCache.set(jobId, JOB_COLORS[colorIndex % JOB_COLORS.length]);
    colorIndex++;
  }
  return jobColorCache.get(jobId)!;
}
```

### 3f. Logic group messages theo job trong ChatWindow

```typescript
// Trong ChatWindow.tsx, trước khi render messages list:
// Group consecutive messages by jobContextId để chèn JobDivider

interface MessageGroup {
  jobId: string | null;
  jobTitle: string | null;
  firstDate: string;
  messages: Message[];
}

function groupMessagesByJob(messages: Message[]): MessageGroup[] {
  const groups: MessageGroup[] = [];
  
  for (const msg of messages) {
    const jobId = msg.jobContext?.jobId ?? null;
    const last = groups[groups.length - 1];
    
    // Tạo group mới nếu job context thay đổi
    if (!last || last.jobId !== jobId) {
      groups.push({
        jobId,
        jobTitle: msg.jobContext?.jobTitle ?? null,
        firstDate: msg.createdAt,
        messages: [msg],
      });
    } else {
      last.messages.push(msg);
    }
  }
  
  return groups;
}

// Render:
// {groupMessagesByJob(filteredMessages).map((group, i) => (
//   <React.Fragment key={`group-${i}`}>
//     <JobDivider
//       jobTitle={group.jobTitle}
//       date={formatRelativeDate(group.firstDate)}
//     />
//     {group.messages.map(msg => (
//       <MessageBubble key={msg.id} message={msg} ... />
//     ))}
//   </React.Fragment>
// ))}
```

### 3g. Cập nhật ThreadItem trong Inbox

```tsx
// Thay thế single application badge bằng job chips

// ThreadItem.tsx:
<div className="thread-job-chips">
  {thread.jobs.slice(0, 2).map(job => (
    <span
      key={job.jobId}
      className={`job-chip ${job.unreadCount > 0 ? 'has-unread' : ''}`}
      title={job.jobTitle}
    >
      {job.jobTitle.length > 14 ? job.jobTitle.slice(0, 12) + '…' : job.jobTitle}
      {job.unreadCount > 0 && <span className="chip-dot" />}
    </span>
  ))}
  {thread.jobs.length > 2 && (
    <span className="job-chip more">+{thread.jobs.length - 2}</span>
  )}
</div>
```

```css
.thread-job-chips {
  display: flex;
  gap: 4px;
  flex-wrap: nowrap;
  overflow: hidden;
  margin-top: 4px;
  padding-left: 44px; /* align với text */
}
.job-chip {
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 10px;
  background: var(--color-background-secondary);
  color: var(--color-text-secondary);
  border: 0.5px solid var(--color-border-tertiary);
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 4px;
}
.job-chip.has-unread {
  background: #E6F1FB;
  color: #185FA5;
  border-color: #85B7EB;
  font-weight: 500;
}
.chip-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #ef4444;
}
.job-chip.more {
  background: var(--color-background-tertiary);
  color: var(--color-text-tertiary);
}
```

### 3h. Cập nhật sendMessage handler

```typescript
// Trong ChatWindow.tsx hoặc MessageInput.tsx:
const [selectedJobId, setSelectedJobId] = useState<string | null>(
  // Default: job gần nhất có tin nhắn, hoặc job đầu tiên apply
  threadJobs[0]?.jobId ?? null
);

const handleSend = async (content: string) => {
  // Optimistic message với jobContext
  const tempMessage: Message = {
    id: `temp-${Date.now()}`,
    content,
    sender: currentUser,
    createdAt: new Date().toISOString(),
    jobContext: selectedJobId
      ? { jobId: selectedJobId, jobTitle: getJobTitle(selectedJobId), jobStatus: '' }
      : null,
    // ...
  };
  
  dispatch(addMessage({ threadId, message: tempMessage }));
  
  try {
    const { data } = await messagingApi.sendMessage(threadId, {
      content,
      contentType: 'TEXT',
      jobContextId: selectedJobId ?? undefined,  // ← truyền jobContextId
    });
    
    dispatch(replaceMessage({ threadId, tempId: tempMessage.id, message: data }));
    broadcastNewMessage(threadId, data);
  } catch (err) {
    dispatch(markMessageFailed({ threadId, tempId: tempMessage.id }));
  }
};
```

---

## 📋 BƯỚC 4 — Candidate FE (React JavaScript)

### 4a. Job Context Banner

```jsx
// Trong ChatWindow của candidate:
// Hiển thị banner sticky ở trên cùng chat, có nút "Chuyển job"

const JobContextBanner = ({ jobs, selectedJobId, onSwitch }) => {
  const currentJob = jobs.find(j => j.jobId === selectedJobId);
  
  return (
    <div className="job-ctx-banner">
      <div className="jcb-icon">
        <BriefcaseIcon size={12} />
      </div>
      <div className="jcb-text">
        <div className="jcb-title">
          {currentJob ? currentJob.jobTitle : 'Tin nhắn chung'}
        </div>
        <div className="jcb-sub">
          {currentJob 
            ? `Trạng thái: ${getStatusLabel(currentJob.jobStatus)}`
            : 'Không gắn với vị trí cụ thể'
          }
        </div>
      </div>
      {jobs.length > 1 && (
        <button className="jcb-switch" onClick={onSwitch}>
          Chuyển job ›
        </button>
      )}
    </div>
  );
};
```

### 4b. Job Switch Modal (khi candidate có nhiều job)

```jsx
// JobSwitchModal.jsx — modal đơn giản liệt kê jobs

const JobSwitchModal = ({ jobs, selectedJobId, onSelect, onClose }) => (
  <div className="modal-overlay" onClick={onClose}>
    <div className="modal-sheet" onClick={e => e.stopPropagation()}>
      <div className="modal-header">
        <span>Chọn vị trí ứng tuyển</span>
        <button onClick={onClose}>×</button>
      </div>
      <div className="modal-list">
        <div
          className={`job-option ${selectedJobId === null ? 'active' : ''}`}
          onClick={() => { onSelect(null); onClose(); }}
        >
          <div className="jo-icon general">
            <ChatIcon size={16} />
          </div>
          <div>
            <div className="jo-title">Tin nhắn chung</div>
            <div className="jo-sub">Không liên quan vị trí cụ thể</div>
          </div>
        </div>
        
        {jobs.map(job => (
          <div
            key={job.jobId}
            className={`job-option ${selectedJobId === job.jobId ? 'active' : ''}`}
            onClick={() => { onSelect(job.jobId); onClose(); }}
          >
            <div className="jo-icon">
              <BriefcaseIcon size={16} />
            </div>
            <div style={{ flex: 1 }}>
              <div className="jo-title">{job.jobTitle}</div>
              <div className="jo-sub">
                <StatusBadge status={job.jobStatus} />
                {job.lastMessageAt && (
                  <span> · {formatRelative(job.lastMessageAt)}</span>
                )}
              </div>
            </div>
            {job.unreadCount > 0 && (
              <span className="jo-unread">{job.unreadCount}</span>
            )}
            {selectedJobId === job.jobId && (
              <CheckIcon size={14} color="var(--primary)" />
            )}
          </div>
        ))}
      </div>
    </div>
  </div>
);
```

```css
/* Bottom sheet trên mobile */
.modal-overlay {
  position: absolute; /* không dùng fixed */
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: flex-end;
  border-radius: inherit;
}
.modal-sheet {
  width: 100%;
  background: var(--color-background-primary);
  border-radius: 16px 16px 0 0;
  padding: 0 0 16px;
  max-height: 70%;
  overflow-y: auto;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  font-weight: 500;
  border-bottom: 0.5px solid var(--color-border-tertiary);
}
.job-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
}
.job-option:hover { background: var(--color-background-secondary); }
.job-option.active { background: #E6F1FB; }
.jo-title { font-size: 14px; font-weight: 500; }
.jo-sub { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
.jo-unread {
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  border-radius: 8px;
  padding: 1px 6px;
}
```

### 4c. Notification banner khi có tin nhắn về job khác

```jsx
// Trong ChatWindow, khi đang xem job A mà có tin nhắn về job B:

{otherJobsWithUnread.map(job => (
  <div key={job.jobId} className="other-job-banner">
    <span className="ojb-dot" />
    <span className="ojb-text">
      Có tin nhắn mới về <strong>{job.jobTitle}</strong>
    </span>
    <button
      className="ojb-switch"
      onClick={() => setSelectedJobId(job.jobId)}
    >
      Xem ngay
    </button>
  </div>
))}
```

```css
.other-job-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #EEEDFE;
  border-radius: var(--border-radius-md);
  margin: 4px 12px;
}
.ojb-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #534AB7;
  flex-shrink: 0;
}
.ojb-text { font-size: 12px; color: #3C3489; flex: 1; }
.ojb-switch {
  font-size: 11px;
  color: #534AB7;
  font-weight: 500;
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
}
```

---

## 📋 BƯỚC 5 — Cập nhật Socket Layer

```javascript
// Trong chat.js — cập nhật relay "new-message":
// Thêm jobContext vào payload để FE cập nhật filter badge đúng

socket.on("new-message", ({ threadId, message }) => {
  if (!threadId || !message) return;
  if (!socket.data.threads?.has(threadId)) return;
  
  // message đã chứa jobContext từ BE response, relay nguyên
  socket.to(`thread:${threadId}`).emit("new-message", {
    threadId,
    message, // { ..., jobContext: { jobId, jobTitle, jobStatus } | null }
  });
});
```

---

## ✅ QA Checklist — Multi-job Context

### Core
- [ ] Candidate apply Job A → HR mở chat → Job A hiện trong JobContextSelector
- [ ] Candidate apply thêm Job B → reload chat → cả A và B hiện trong selector
- [ ] HR chọn Job A → gửi tin → message bubble hiện tag "Job A"
- [ ] HR chọn "Chung" → gửi tin → không có job tag trên bubble
- [ ] Candidate nhận tin → thấy đúng job tag trên bubble

### Filter & Divider
- [ ] JobFilterBar chỉ hiện khi có >= 2 jobs
- [ ] Click filter "Job A" → chỉ thấy tin về Job A
- [ ] Click filter "Tất cả" → thấy tất cả tin, có JobDivider chia nhóm
- [ ] JobDivider hiện đúng job title và ngày
- [ ] Khi filter Job A → unread count chỉ đếm tin Job A chưa đọc

### Candidate UX
- [ ] Job Context Banner hiện đúng job đang active
- [ ] Click "Chuyển job" → modal/bottom sheet hiện danh sách jobs
- [ ] Select job khác trong modal → banner cập nhật
- [ ] Có tin nhắn về Job B trong khi đang xem Job A → notification banner hiện
- [ ] Click "Xem ngay" trên banner → switch sang Job B

### Thread Inbox
- [ ] Thread item hiện chips cho tất cả jobs (max 2 + "+N" nếu nhiều hơn)
- [ ] Chip highlight nếu job đó có unread

### Edge Cases
- [ ] Job đã bị xóa/closed → tin nhắn cũ vẫn hiện tag đúng (không crash)
- [ ] Gửi tin về job mà candidate chưa apply → BE trả 400
- [ ] 1 candidate, 5 jobs cùng 1 công ty → selector scroll được, không vỡ layout
- [ ] Filter bar 5 tabs → horizontal scroll mượt
