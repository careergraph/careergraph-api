# PHASE 1 — ADDENDUM: Advanced Messaging Features
> **Đây là file bổ sung cho `01_PHASE1_BE_FOUNDATION.md`**  
> Agent đọc file này SAU KHI đã hoàn thành Phase 1 gốc.  
> Mục tiêu: thêm 4 tính năng vào BE mà không phá vỡ code đã có.

---

## 🎯 4 Tính năng bổ sung

| # | Tính năng | Scope |
|---|-----------|-------|
| 1 | **Gỡ tin nhắn** (unsend) — nâng cấp soft delete đã có | BE + cả 2 FE |
| 2 | **Xóa hội thoại** (delete for me only) | BE + HR FE |
| 3 | **Lưu trữ hội thoại** (archive/unarchive) | BE + HR FE |
| 4 | **Block người dùng** (HR block candidate) | BE + HR FE |

---

## 📋 BƯỚC 1 — Database Migration (thêm vào migration mới)

Tạo file migration mới: `V{next}__add_advanced_messaging.sql`

```sql
-- 1. Thread deletions (xóa phía mình)
CREATE TABLE thread_deletions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id   UUID NOT NULL REFERENCES message_threads(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    deleted_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    UNIQUE (thread_id, user_id)
);

CREATE INDEX idx_thread_deletions_user ON thread_deletions(user_id);

-- 2. Thread archives
ALTER TABLE message_threads 
    ADD COLUMN IF NOT EXISTS archived_by_hr     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS archived_by_hr_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by_candidate     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS archived_by_candidate_at  TIMESTAMPTZ;

-- 3. Block list
CREATE TABLE user_blocks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id  UUID NOT NULL REFERENCES users(id),  -- HR
    blocked_id  UUID NOT NULL REFERENCES users(id),  -- Candidate
    reason      VARCHAR(255),  -- optional: lý do block
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    UNIQUE (blocker_id, blocked_id)
);

CREATE INDEX idx_blocks_blocker ON user_blocks(blocker_id);
CREATE INDEX idx_blocks_blocked ON user_blocks(blocked_id);

-- 4. Unsend: thêm time window check (60 giây)
-- Đã có is_deleted và deleted_at trong messages từ Phase 1
-- Thêm cột để track loại xóa:
ALTER TABLE messages 
    ADD COLUMN IF NOT EXISTS delete_type VARCHAR(20) DEFAULT NULL;
    -- Values: 'UNSEND' (gỡ với mọi người) | 'DELETE_FOR_ME' (chỉ xóa phía mình - future)
```

---

## 📋 BƯỚC 2 — JPA Entities mới

### `ThreadDeletion.java`
```java
@Entity
@Table(name = "thread_deletions")
public class ThreadDeletion {
    @Id @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    private MessageThread thread;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    private Instant deletedAt;
}
```

### `UserBlock.java`
```java
@Entity
@Table(name = "user_blocks")
public class UserBlock {
    @Id @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id")
    private User blocker;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id")
    private User blocked;
    
    private String reason;
    private Instant createdAt;
}
```

### Cập nhật `MessageThread.java` — thêm fields archive

```java
// Thêm vào class MessageThread:
@Column(name = "archived_by_hr")
private boolean archivedByHr = false;

@Column(name = "archived_by_hr_at")
private Instant archivedByHrAt;

@Column(name = "archived_by_candidate")
private boolean archivedByCandidate = false;

@Column(name = "archived_by_candidate_at")
private Instant archivedByCandidateAt;
```

### Cập nhật `Message.java` — thêm deleteType

```java
public enum MessageDeleteType { UNSEND, DELETE_FOR_ME }

// Thêm vào Message entity:
@Enumerated(EnumType.STRING)
@Column(name = "delete_type")
private MessageDeleteType deleteType;
```

---

## 📋 BƯỚC 3 — Repositories mới

```java
// ThreadDeletionRepository
public interface ThreadDeletionRepository extends JpaRepository<ThreadDeletion, UUID> {
    boolean existsByThreadIdAndUserId(UUID threadId, UUID userId);
    Optional<ThreadDeletion> findByThreadIdAndUserId(UUID threadId, UUID userId);
}

// UserBlockRepository
public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {
    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
    Optional<UserBlock> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
    List<UserBlock> findByBlockerId(UUID blockerId);
}
```

---

## 📋 BƯỚC 4 — Cập nhật MessageService

### 4a. Nâng cấp `deleteMessage()` → hỗ trợ Unsend có time window

```java
private static final long UNSEND_WINDOW_SECONDS = 60; // 60 giây để gỡ

@Transactional
public void unsendMessage(UUID messageId, UUID requesterId) {
    Message message = messageRepository.findById(messageId)
        .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
    
    // Chỉ sender mới được gỡ
    if (!message.getSender().getId().equals(requesterId)) {
        throw new AccessDeniedException("Chỉ có thể gỡ tin nhắn của bạn");
    }
    
    // Kiểm tra time window 60 giây
    long secondsElapsed = ChronoUnit.SECONDS.between(message.getCreatedAt(), Instant.now());
    if (secondsElapsed > UNSEND_WINDOW_SECONDS) {
        throw new BusinessException("Chỉ có thể gỡ tin nhắn trong vòng 60 giây sau khi gửi");
    }
    
    if (message.isDeleted()) {
        throw new BusinessException("Tin nhắn đã được gỡ trước đó");
    }
    
    message.setDeleted(true);
    message.setDeletedAt(Instant.now());
    message.setDeleteType(MessageDeleteType.UNSEND);
    message.setContent("[Tin nhắn đã được gỡ]"); // replace content
    messageRepository.save(message);
}

// ⚠️ Business rule: Nếu bỏ time window (HR yêu cầu) thì remove check đó
// Đọc yêu cầu business trước khi quyết định
```

### 4b. Xóa hội thoại (delete for me)

```java
@Transactional
public void deleteThreadForMe(UUID threadId, UUID userId) {
    // Verify user thuộc thread
    validateThreadAccess(threadId, userId);
    
    // Nếu đã xóa rồi thì bỏ qua
    if (threadDeletionRepository.existsByThreadIdAndUserId(threadId, userId)) {
        return; // idempotent
    }
    
    ThreadDeletion deletion = new ThreadDeletion();
    deletion.setThread(threadRepository.getReferenceById(threadId));
    deletion.setUser(userRepository.getReferenceById(userId));
    deletion.setDeletedAt(Instant.now());
    threadDeletionRepository.save(deletion);
    
    // Nếu CẢ 2 người đều đã xóa → xóa hẳn thread (optional, tuỳ business)
    // MessageThread thread = threadRepository.findById(threadId).orElseThrow();
    // boolean otherAlsoDeleted = checkOtherPersonDeleted(thread, userId);
    // if (otherAlsoDeleted) threadRepository.delete(thread);
}

// Khi query threads: lọc ra những thread user đã xóa
// Cập nhật findByHrUserId và findByCandidateUserId trong Repository:
@Query("""
    SELECT t FROM MessageThread t
    WHERE t.hrUser.id = :userId
      AND NOT EXISTS (
          SELECT 1 FROM ThreadDeletion td 
          WHERE td.thread.id = t.id AND td.user.id = :userId
      )
    ORDER BY t.lastMessageAt DESC NULLS LAST
    """)
Page<MessageThread> findActiveThreadsByHrUserId(UUID userId, Pageable pageable);

// Tương tự cho candidate
```

### 4c. Archive / Unarchive

```java
@Transactional
public void archiveThread(UUID threadId, UUID userId, boolean archive) {
    validateThreadAccess(threadId, userId);
    
    MessageThread thread = threadRepository.findById(threadId)
        .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
    
    boolean isHr = thread.getHrUser().getId().equals(userId);
    
    if (isHr) {
        thread.setArchivedByHr(archive);
        thread.setArchivedByHrAt(archive ? Instant.now() : null);
    } else {
        thread.setArchivedByCandidate(archive);
        thread.setArchivedByCandidateAt(archive ? Instant.now() : null);
    }
    
    threadRepository.save(thread);
}

// Query inbox: thêm filter archived
// GET /threads?archived=false (default) hoặc GET /threads?archived=true (trang archive)
@Query("""
    SELECT t FROM MessageThread t
    WHERE t.hrUser.id = :userId
      AND t.archivedByHr = :archived
      AND NOT EXISTS (
          SELECT 1 FROM ThreadDeletion td 
          WHERE td.thread.id = t.id AND td.user.id = :userId
      )
    ORDER BY t.lastMessageAt DESC NULLS LAST
    """)
Page<MessageThread> findThreadsByHrUserIdAndArchived(UUID userId, boolean archived, Pageable pageable);
```

### 4d. Block / Unblock

```java
@Transactional
public void blockUser(UUID blockerId, UUID blockedId, String reason) {
    // Chỉ HR mới được block candidate (validate role)
    User blocker = userRepository.findById(blockerId).orElseThrow();
    if (!isHrRole(blocker)) {
        throw new AccessDeniedException("Chỉ HR mới có quyền block người dùng");
    }
    
    // Idempotent: nếu đã block rồi thì bỏ qua
    if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
        return;
    }
    
    UserBlock block = new UserBlock();
    block.setBlocker(userRepository.getReferenceById(blockerId));
    block.setBlocked(userRepository.getReferenceById(blockedId));
    block.setReason(reason);
    block.setCreatedAt(Instant.now());
    userBlockRepository.save(block);
    
    // Đóng thread (không xóa, chỉ ngăn gửi thêm)
    // Thread vẫn tồn tại để HR xem lịch sử
}

@Transactional
public void unblockUser(UUID blockerId, UUID blockedId) {
    userBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
        .ifPresent(userBlockRepository::delete);
}

public boolean isBlocked(UUID blockerId, UUID blockedId) {
    return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
}

// Inject kiểm tra block vào sendMessage:
public MessageDto sendMessage(UUID threadId, UUID senderId, SendMessageRequest request) {
    MessageThread thread = threadRepository.findById(threadId).orElseThrow();
    
    // Check nếu HR đã block candidate này
    UUID hrId = thread.getHrUser().getId();
    UUID candidateId = thread.getCandidateUser().getId();
    
    if (userBlockRepository.existsByBlockerIdAndBlockedId(hrId, candidateId)) {
        throw new BusinessException("Không thể gửi tin nhắn: người dùng đã bị chặn");
    }
    
    // ... rest of existing logic
}
```

---

## 📋 BƯỚC 5 — Cập nhật DTOs

```java
// Cập nhật ThreadSummaryDto — thêm fields:
public record ThreadSummaryDto(
    UUID threadId,
    UserSummaryDto otherUser,
    ApplicationSummaryDto application,
    String lastMessagePreview,
    Instant lastMessageAt,
    long unreadCount,
    boolean isOnline,
    boolean isArchived,        // ← MỚI
    boolean isBlocked          // ← MỚI (HR xem)
) {}

// Response cho block action:
public record BlockStatusDto(
    boolean isBlocked,
    Instant blockedAt,
    String reason
) {}

// Danh sách blocked users (cho HR):
public record BlockedUserDto(
    UUID userId,
    String fullName,
    String email,
    String avatarUrl,
    Instant blockedAt,
    String reason
) {}
```

---

## 📋 BƯỚC 6 — Cập nhật REST Controller

```java
// Thêm vào MessageController:

// Gỡ tin nhắn (unsend) — nâng cấp endpoint DELETE cũ
// DELETE /api/v1/messages/{messageId}/unsend
@DeleteMapping("/{messageId}/unsend")
public ResponseEntity<Void> unsendMessage(@PathVariable UUID messageId) {
    messageService.unsendMessage(messageId, getCurrentUserId());
    return ResponseEntity.noContent().build();
}

// Xóa hội thoại (chỉ phía mình)
// DELETE /api/v1/messages/threads/{threadId}
@DeleteMapping("/threads/{threadId}")
public ResponseEntity<Void> deleteThread(@PathVariable UUID threadId) {
    messageService.deleteThreadForMe(threadId, getCurrentUserId());
    return ResponseEntity.noContent().build();
}

// Archive / Unarchive
// POST /api/v1/messages/threads/{threadId}/archive
@PostMapping("/threads/{threadId}/archive")
public ResponseEntity<Void> archiveThread(@PathVariable UUID threadId) {
    messageService.archiveThread(threadId, getCurrentUserId(), true);
    return ResponseEntity.noContent().build();
}

// POST /api/v1/messages/threads/{threadId}/unarchive
@PostMapping("/threads/{threadId}/unarchive")
public ResponseEntity<Void> unarchiveThread(@PathVariable UUID threadId) {
    messageService.archiveThread(threadId, getCurrentUserId(), false);
    return ResponseEntity.noContent().build();
}

// Lấy archived threads
// GET /api/v1/messages/threads?archived=true
// → Cập nhật endpoint getThreads hiện có, thêm @RequestParam:
@GetMapping("/threads")
public ResponseEntity<Page<ThreadSummaryDto>> getThreads(
    @RequestParam(defaultValue = "false") boolean archived,
    Pageable pageable) {
    return ResponseEntity.ok(messageService.getThreads(getCurrentUserId(), archived, pageable));
}
```

```java
// Tạo BlockController mới (hoặc thêm vào MessageController):

// POST /api/v1/users/{userId}/block
@PostMapping("/api/v1/users/{userId}/block")
@PreAuthorize("hasRole('HR')")
public ResponseEntity<Void> blockUser(
    @PathVariable UUID userId,
    @RequestBody(required = false) BlockUserRequest request) {
    String reason = request != null ? request.reason() : null;
    blockService.blockUser(getCurrentUserId(), userId, reason);
    return ResponseEntity.noContent().build();
}

// DELETE /api/v1/users/{userId}/block
@DeleteMapping("/api/v1/users/{userId}/block")
@PreAuthorize("hasRole('HR')")
public ResponseEntity<Void> unblockUser(@PathVariable UUID userId) {
    blockService.unblockUser(getCurrentUserId(), userId);
    return ResponseEntity.noContent().build();
}

// GET /api/v1/users/blocked — danh sách HR đã block
@GetMapping("/api/v1/users/blocked")
@PreAuthorize("hasRole('HR')")
public ResponseEntity<List<BlockedUserDto>> getBlockedUsers() {
    return ResponseEntity.ok(blockService.getBlockedUsers(getCurrentUserId()));
}
```

---

## ✅ QA Checklist — Addendum

- [ ] Unsend trong 60 giây → tin hiện "[Tin nhắn đã được gỡ]" cho cả 2 phía
- [ ] Unsend sau 60 giây → 400 với message rõ ràng
- [ ] Unsend tin của người khác → 403
- [ ] Xóa thread → không hiện trong inbox của mình, nhưng đối phương vẫn thấy
- [ ] Gửi tin nhắn sau khi mình đã xóa thread → thread xuất hiện lại trong inbox
- [ ] Archive thread → hiện trong tab "Đã lưu trữ", không hiện inbox chính
- [ ] Unarchive → trở lại inbox chính
- [ ] HR block candidate → candidate gửi tin → 403 hoặc error message
- [ ] HR block → HR cũng không gửi được (consistent)
- [ ] Unblock → cả 2 gửi được bình thường
- [ ] Danh sách blocked users trả đúng
- [ ] Candidate không có quyền gọi block API → 403
