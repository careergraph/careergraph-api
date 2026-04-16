# PHASE 1 — Backend Foundation: Database Schema & REST APIs
> **Service:** Spring Boot Backend  
> **Prerequisites:** Đọc toàn bộ source code Spring Boot để hiểu: package structure, naming conventions, existing entities (User, Application, Job), security config, JWT filter, exception handling pattern.

---

## 🎯 Mục tiêu Phase này

Xây dựng toàn bộ nền tảng backend cho hệ thống nhắn tin:
- Database schema hoàn chỉnh
- JPA Entities + Repositories
- Service layer với business logic đầy đủ
- REST APIs cho FE consume
- Không làm vỡ bất kỳ API nào hiện có

---

## 📋 BƯỚC 1 — Đọc hiểu dự án (BẮT BUỘC trước khi code)

```
Agent phải đọc và ghi chú:
1. Cấu trúc package (vd: com.careergraph.*)
2. Base entity có dùng @MappedSuperclass không? Có createdAt, updatedAt tự động không?
3. User entity: trường nào là ID (UUID hay Long?), có role field không?
4. Application entity: các status hiện tại là gì (enum)?
5. Security config: JWT filter hoạt động ra sao, có SecurityUtils để lấy current user không?
6. Exception handling: có GlobalExceptionHandler không? Pattern trả về lỗi là gì?
7. DTO pattern: có dùng MapStruct không? Hay convert thủ công?
8. Database: PostgreSQL, đang dùng Flyway hay Liquibase migration không?
9. Tên bảng convention: snake_case hay camelCase?
```

---

## 📋 BƯỚC 2 — Database Schema

Tạo migration file (Flyway: `V{next_version}__add_messaging_system.sql` hoặc Liquibase tương đương).

### Bảng `message_threads`
```sql
CREATE TABLE message_threads (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hr_user_id      UUID NOT NULL REFERENCES users(id),
    candidate_user_id UUID NOT NULL REFERENCES users(id),
    application_id  UUID REFERENCES applications(id), -- nullable: thread có thể tạo độc lập
    last_message_at TIMESTAMPTZ,
    last_message_preview VARCHAR(255), -- preview 100 ký tự đầu
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Mỗi cặp HR-Candidate chỉ có 1 thread (hoặc per application nếu business cần)
    UNIQUE (hr_user_id, candidate_user_id)
    -- Nếu cần per-application: UNIQUE (hr_user_id, candidate_user_id, application_id)
);

CREATE INDEX idx_thread_hr ON message_threads(hr_user_id);
CREATE INDEX idx_thread_candidate ON message_threads(candidate_user_id);
CREATE INDEX idx_thread_last_msg ON message_threads(last_message_at DESC);
```

> ⚠️ **Agent quyết định:** Nếu business logic yêu cầu mỗi job application có thread riêng thì dùng UNIQUE với application_id. Nếu HR và candidate chỉ có 1 cuộc hội thoại chung thì UNIQUE (hr_user_id, candidate_user_id). Đọc code hiện tại để quyết định.

### Bảng `messages`
```sql
CREATE TABLE messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id   UUID NOT NULL REFERENCES message_threads(id) ON DELETE CASCADE,
    sender_id   UUID NOT NULL REFERENCES users(id),
    content     TEXT NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'TEXT', -- TEXT | IMAGE | FILE
    file_url    VARCHAR(500),  -- nếu content_type != TEXT
    file_name   VARCHAR(255),
    file_size   BIGINT,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_thread ON messages(thread_id, created_at DESC);
CREATE INDEX idx_messages_sender ON messages(sender_id);
```

### Bảng `message_reads`
```sql
-- Track ai đã đọc đến message nào trong thread
CREATE TABLE message_reads (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id   UUID NOT NULL REFERENCES message_threads(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    last_read_message_id UUID REFERENCES messages(id),
    last_read_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    UNIQUE (thread_id, user_id)
);

CREATE INDEX idx_reads_user ON message_reads(user_id);
```

### Bảng `notifications`
```sql
CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id    UUID NOT NULL REFERENCES users(id),
    type            VARCHAR(50) NOT NULL,
    -- Types: NEW_MESSAGE, APPLICATION_STATUS_CHANGED, NEW_APPLICATION,
    --        APPLICATION_VIEWED, APPLICATION_SHORTLISTED, APPLICATION_REJECTED
    title           VARCHAR(255) NOT NULL,
    body            VARCHAR(500) NOT NULL,
    data            JSONB,  -- metadata: {threadId, applicationId, jobId, messageId, ...}
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_recipient ON notifications(recipient_id, created_at DESC);
CREATE INDEX idx_notif_unread ON notifications(recipient_id, is_read) WHERE is_read = FALSE;
```

### Bảng `notification_preferences` (optional nhưng professional)
```sql
CREATE TABLE notification_preferences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    type            VARCHAR(50) NOT NULL,
    in_app_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled   BOOLEAN NOT NULL DEFAULT FALSE, -- phase sau
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    UNIQUE (user_id, type)
);
```

---

## 📋 BƯỚC 3 — JPA Entities

Tạo entities theo đúng package convention của dự án. Tham khảo existing entities để biết:
- `@MappedSuperclass` base class
- Audit fields pattern
- UUID vs Long ID

### `MessageThread.java`
```java
@Entity
@Table(name = "message_threads")
public class MessageThread extends BaseEntity { // hoặc dùng pattern của dự án
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hr_user_id", nullable = false)
    private User hrUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_user_id", nullable = false)
    private User candidateUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application; // nullable
    
    @Column(name = "last_message_at")
    private Instant lastMessageAt;
    
    @Column(name = "last_message_preview", length = 255)
    private String lastMessagePreview;
    
    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL)
    @OrderBy("createdAt DESC")
    private List<Message> messages = new ArrayList<>();
    
    // Getters/Setters/Builder
}
```

### `Message.java`
```java
@Entity
@Table(name = "messages")
public class Message extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private MessageThread thread;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private MessageContentType contentType = MessageContentType.TEXT;
    
    @Column(name = "file_url", length = 500)
    private String fileUrl;
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "is_deleted")
    private boolean deleted = false;
    
    @Column(name = "deleted_at")
    private Instant deletedAt;
    
    // Getters/Setters
}

public enum MessageContentType { TEXT, IMAGE, FILE }
```

### `MessageRead.java`
```java
@Entity
@Table(name = "message_reads")
public class MessageRead {
    @Id @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    private MessageThread thread;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private Message lastReadMessage;
    
    private Instant lastReadAt;
}
```

### `Notification.java`
```java
@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient;
    
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, length = 500)
    private String body;
    
    @Type(JsonBinaryType.class) // dùng hypersistence-utils hoặc tương đương
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;
    
    private boolean isRead = false;
    private Instant readAt;
    private Instant createdAt;
}

public enum NotificationType {
    NEW_MESSAGE,
    APPLICATION_STATUS_CHANGED,
    NEW_APPLICATION,
    APPLICATION_VIEWED,
    APPLICATION_SHORTLISTED,
    APPLICATION_REJECTED,
    APPLICATION_INTERVIEW_SCHEDULED
}
```

---

## 📋 BƯỚC 4 — Repositories

```java
// MessageThreadRepository
public interface MessageThreadRepository extends JpaRepository<MessageThread, UUID> {
    
    // Lấy tất cả threads của HR, sắp xếp theo tin nhắn mới nhất
    @Query("""
        SELECT t FROM MessageThread t
        WHERE t.hrUser.id = :userId
        ORDER BY t.lastMessageAt DESC NULLS LAST
        """)
    Page<MessageThread> findByHrUserId(UUID userId, Pageable pageable);
    
    @Query("""
        SELECT t FROM MessageThread t
        WHERE t.candidateUser.id = :userId
        ORDER BY t.lastMessageAt DESC NULLS LAST
        """)
    Page<MessageThread> findByCandidateUserId(UUID userId, Pageable pageable);
    
    Optional<MessageThread> findByHrUserIdAndCandidateUserId(UUID hrId, UUID candidateId);
    
    // Tìm hoặc tạo thread
    @Query("""
        SELECT t FROM MessageThread t
        WHERE t.hrUser.id = :hrId AND t.candidateUser.id = :candidateId
        """)
    Optional<MessageThread> findThread(UUID hrId, UUID candidateId);
}

// MessageRepository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    @Query("""
        SELECT m FROM Message m
        WHERE m.thread.id = :threadId AND m.deleted = false
        ORDER BY m.createdAt ASC
        """)
    Page<Message> findByThreadId(UUID threadId, Pageable pageable);
    
    // Đếm tin chưa đọc trong thread cho user
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.thread.id = :threadId
          AND m.sender.id != :userId
          AND m.deleted = false
          AND m.createdAt > (
              SELECT COALESCE(mr.lastReadAt, '1970-01-01') FROM MessageRead mr
              WHERE mr.thread.id = :threadId AND mr.user.id = :userId
          )
        """)
    long countUnreadInThread(UUID threadId, UUID userId);
    
    // Đếm tổng unread của user
    @Query("""
        SELECT COUNT(m) FROM Message m
        JOIN m.thread t
        WHERE m.sender.id != :userId
          AND m.deleted = false
          AND (t.hrUser.id = :userId OR t.candidateUser.id = :userId)
          AND m.createdAt > (
              SELECT COALESCE(mr.lastReadAt, '1970-01-01') FROM MessageRead mr
              WHERE mr.thread.id = t.id AND mr.user.id = :userId
          )
        """)
    long countTotalUnread(UUID userId);
}

// NotificationRepository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);
    
    long countByRecipientIdAndIsReadFalse(UUID recipientId);
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.recipient.id = :userId AND n.isRead = false")
    int markAllAsRead(UUID userId, Instant now);
}
```

---

## 📋 BƯỚC 5 — DTOs

### Request DTOs
```java
public record SendMessageRequest(
    @NotBlank String content,
    MessageContentType contentType,
    String fileUrl,
    String fileName,
    Long fileSize
) {}

public record CreateThreadRequest(
    @NotNull UUID candidateId,   // HR tạo thread với candidate
    UUID applicationId           // optional
) {}
```

### Response DTOs
```java
// Thread summary cho inbox list
public record ThreadSummaryDto(
    UUID threadId,
    UserSummaryDto otherUser,         // HR thấy candidate info, candidate thấy HR info
    ApplicationSummaryDto application, // nullable
    String lastMessagePreview,
    Instant lastMessageAt,
    long unreadCount,
    boolean isOnline                   // từ socket layer (set sau)
) {}

// Chi tiết message
public record MessageDto(
    UUID id,
    UUID threadId,
    UserSummaryDto sender,
    String content,
    MessageContentType contentType,
    String fileUrl,
    String fileName,
    Long fileSize,
    boolean deleted,
    Instant createdAt,
    boolean isRead,     // người nhận đã đọc chưa
    Instant readAt
) {}

// Notification
public record NotificationDto(
    UUID id,
    NotificationType type,
    String title,
    String body,
    Map<String, Object> data,
    boolean isRead,
    Instant createdAt
) {}

public record NotificationPageDto(
    List<NotificationDto> notifications,
    long totalUnread,
    boolean hasMore
) {}
```

---

## 📋 BƯỚC 6 — Service Layer

### `MessageService.java`
```java
@Service
@Transactional
public class MessageService {
    
    // Lấy hoặc tạo thread giữa HR và candidate
    public ThreadSummaryDto getOrCreateThread(UUID currentUserId, UUID otherUserId, UUID applicationId);
    
    // Lấy danh sách threads của user (inbox)
    public Page<ThreadSummaryDto> getThreads(UUID userId, Pageable pageable);
    
    // Lấy messages trong thread (với pagination)
    public Page<MessageDto> getMessages(UUID threadId, UUID currentUserId, Pageable pageable);
    
    // Gửi tin nhắn
    public MessageDto sendMessage(UUID threadId, UUID senderId, SendMessageRequest request);
    
    // Đánh dấu đã đọc
    public void markThreadAsRead(UUID threadId, UUID userId);
    
    // Thu hồi tin nhắn (soft delete)
    public void deleteMessage(UUID messageId, UUID requesterId);
    
    // Đếm tổng unread
    public long getTotalUnread(UUID userId);
    
    // Kiểm tra quyền truy cập thread
    private void validateThreadAccess(UUID threadId, UUID userId);
}
```

**Business rules bắt buộc:**
- Chỉ HR hoặc candidate trong thread mới đọc được
- Chỉ sender mới xóa được tin của mình
- Khi gửi message: cập nhật `last_message_at` và `last_message_preview` của thread
- `last_message_preview` = 100 ký tự đầu của content, nếu là file thì "[Tệp đính kèm]"
- Pagination: default 30 messages/page, cursor-based sẽ tốt hơn offset

### `NotificationService.java`
```java
@Service
@Transactional
public class NotificationService {
    
    // Tạo notification và push realtime (gọi socket service)
    public Notification createNotification(UUID recipientId, NotificationType type, 
                                           String title, String body, Map<String, Object> data);
    
    // Batch tạo notification (vd: 1 job có nhiều candidate)
    public void createBulkNotifications(List<UUID> recipientIds, NotificationType type,
                                        String title, String body, Map<String, Object> data);
    
    // Lấy danh sách notifications
    public NotificationPageDto getNotifications(UUID userId, int page, int size);
    
    // Đánh dấu 1 notification đã đọc
    public void markAsRead(UUID notificationId, UUID userId);
    
    // Đánh dấu tất cả đã đọc
    public void markAllAsRead(UUID userId);
    
    // Đếm unread
    public long getUnreadCount(UUID userId);
    
    // Hook vào Application status change (gọi từ ApplicationService hiện tại)
    public void onApplicationStatusChanged(Application application, ApplicationStatus oldStatus, 
                                           ApplicationStatus newStatus, User changedBy);
    
    // Hook khi có ứng viên apply job mới
    public void onNewApplication(Application application);
    
    // Hook khi có tin nhắn mới
    public void onNewMessage(Message message, MessageThread thread);
}
```

---

## 📋 BƯỚC 7 — REST Controllers

### `MessageController.java`
```java
@RestController
@RequestMapping("/api/v1/messages")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {
    
    // GET /api/v1/messages/threads?page=0&size=20
    // Lấy danh sách threads (inbox) của current user
    @GetMapping("/threads")
    public ResponseEntity<Page<ThreadSummaryDto>> getThreads(Pageable pageable);
    
    // POST /api/v1/messages/threads
    // Tạo/lấy thread với user khác
    @PostMapping("/threads")
    public ResponseEntity<ThreadSummaryDto> getOrCreateThread(@Valid @RequestBody CreateThreadRequest request);
    
    // GET /api/v1/messages/threads/{threadId}
    // Lấy chi tiết thread
    @GetMapping("/threads/{threadId}")
    public ResponseEntity<ThreadSummaryDto> getThread(@PathVariable UUID threadId);
    
    // GET /api/v1/messages/threads/{threadId}/messages?page=0&size=30
    // Lấy lịch sử tin nhắn (sắp xếp ASC theo thời gian)
    @GetMapping("/threads/{threadId}/messages")
    public ResponseEntity<Page<MessageDto>> getMessages(@PathVariable UUID threadId, Pageable pageable);
    
    // POST /api/v1/messages/threads/{threadId}/messages
    // Gửi tin nhắn
    @PostMapping("/threads/{threadId}/messages")
    public ResponseEntity<MessageDto> sendMessage(@PathVariable UUID threadId,
                                                   @Valid @RequestBody SendMessageRequest request);
    
    // POST /api/v1/messages/threads/{threadId}/read
    // Đánh dấu thread đã đọc
    @PostMapping("/threads/{threadId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID threadId);
    
    // DELETE /api/v1/messages/{messageId}
    // Thu hồi tin nhắn
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId);
    
    // GET /api/v1/messages/unread-count
    // Tổng số tin chưa đọc (cho badge)
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount();
}
```

### `NotificationController.java`
```java
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    
    // GET /api/v1/notifications?page=0&size=20
    @GetMapping
    public ResponseEntity<NotificationPageDto> getNotifications(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size);
    
    // POST /api/v1/notifications/{id}/read
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id);
    
    // POST /api/v1/notifications/read-all
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead();
    
    // GET /api/v1/notifications/unread-count
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount();
}
```

---

## 📋 BƯỚC 8 — Hook vào existing ApplicationService

> Agent phải tìm file `ApplicationService.java` (hoặc tên tương đương) và thêm hook:

```java
// Trong method đổi status của application:
@Transactional
public Application updateStatus(UUID applicationId, ApplicationStatus newStatus, UUID hrUserId) {
    Application app = findById(applicationId);
    ApplicationStatus oldStatus = app.getStatus();
    
    app.setStatus(newStatus);
    Application saved = applicationRepository.save(app);
    
    // ← THÊM HOOK NÀY (inject NotificationService)
    notificationService.onApplicationStatusChanged(saved, oldStatus, newStatus, getCurrentUser());
    
    return saved;
}

// Trong method tạo application mới:
public Application createApplication(CreateApplicationRequest request) {
    // ... existing logic ...
    Application saved = applicationRepository.save(application);
    
    // ← THÊM HOOK NÀY
    notificationService.onNewApplication(saved);
    
    return saved;
}
```

---

## ✅ QA CHECKLIST — Phase 1

Agent đóng vai tester, chạy các test sau trước khi kết thúc phase:

### API Tests (dùng Postman/curl hoặc viết integration tests)

- [ ] `POST /threads` → tạo thread mới thành công
- [ ] `POST /threads` với cùng HR+candidate → trả về thread cũ (idempotent)
- [ ] `GET /threads` → trả đúng danh sách, có unreadCount
- [ ] `POST /threads/{id}/messages` → lưu message, cập nhật lastMessageAt
- [ ] `GET /threads/{id}/messages` → pagination đúng, sort ASC
- [ ] `POST /threads/{id}/read` → cập nhật MessageRead
- [ ] `GET /unread-count` → đếm đúng
- [ ] `DELETE /{messageId}` bởi non-sender → 403 Forbidden
- [ ] `GET /threads/{id}` bởi user không trong thread → 403 Forbidden
- [ ] Message content rỗng → 400 Bad Request
- [ ] Notification được tạo khi application status thay đổi
- [ ] Notification được tạo khi có application mới

### Edge Cases
- [ ] Thread với application đã bị xóa → handle gracefully
- [ ] Message content có emoji, ký tự Unicode → lưu đúng
- [ ] Concurrent requests tạo thread → không tạo duplicate (xử lý UniqueConstraintException)

---

## 📤 Output của Phase 1

Bàn giao:
1. Migration SQL file đã chạy thành công
2. Tất cả Entities, Repositories, Services, Controllers
3. Tất cả APIs test pass
4. Document ngắn về API endpoints để Phase 3, 4 dùng
