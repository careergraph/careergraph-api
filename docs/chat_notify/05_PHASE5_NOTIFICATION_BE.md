# PHASE 5 — Notification System: Backend & Event Hooks
> **Service:** Spring Boot Backend + Node.js RTC (mở rộng)  
> **Prerequisites:** Phase 1–4 hoàn thành. `NotificationService` đã có skeleton từ Phase 1, Phase này implement đầy đủ.

---

## 🎯 Mục tiêu Phase này

Xây dựng hệ thống thông báo hoàn chỉnh:

### Notifications cho Candidate (nhận)
| Trigger | Notification |
|---------|-------------|
| HR gửi tin nhắn | "HR [Tên] vừa nhắn tin cho bạn" |
| HR xem hồ sơ | "[Công ty] đã xem hồ sơ của bạn cho vị trí [Job]" |
| HR chuyển sang "Đang xem xét" | "Hồ sơ của bạn đang được xem xét tại [Công ty]" |
| HR chuyển sang "Phỏng vấn" | "🎉 Chúc mừng! Bạn đã được mời phỏng vấn tại [Công ty]" |
| HR từ chối | "Hồ sơ của bạn chưa phù hợp với vị trí [Job] tại [Công ty]" |
| HR chuyển sang bất kỳ status nào | Customize theo từng status |

### Notifications cho HR (nhận)
| Trigger | Notification |
|---------|-------------|
| Candidate nộp đơn | "Ứng viên mới [Tên] đã ứng tuyển vị trí [Job]" |
| Candidate gửi tin nhắn | "[Tên Candidate] vừa nhắn tin cho bạn" |
| (Tổng hợp) | "Có [N] ứng viên mới ứng tuyển [Job] hôm nay" |

---

## 📋 BƯỚC 1 — Implement NotificationService đầy đủ

```java
@Service
@Slf4j
@Transactional
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final SocketNotificationPusher socketPusher; // từ Phase 2
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    
    // ── CREATE NOTIFICATION ─────────────────────────────────
    
    public Notification createAndPush(UUID recipientId, NotificationType type,
                                      String title, String body, Map<String, Object> data) {
        // 1. Lưu vào DB
        Notification notification = new Notification();
        notification.setRecipient(userRepository.getReferenceById(recipientId));
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setData(data);
        notification.setCreatedAt(Instant.now());
        
        Notification saved = notificationRepository.save(notification);
        
        // 2. Push realtime qua socket (async, không block)
        CompletableFuture.runAsync(() -> {
            try {
                socketPusher.pushToUser(recipientId, toDto(saved));
                
                // Cập nhật unread count
                long unreadNotifs = notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
                long unreadMessages = messageRepository.countTotalUnread(recipientId);
                socketPusher.pushUnreadCounts(recipientId, unreadMessages, unreadNotifs);
            } catch (Exception e) {
                log.warn("Failed to push notification socket: {}", e.getMessage());
            }
        });
        
        return saved;
    }
    
    // ── APPLICATION STATUS CHANGED ──────────────────────────
    
    public void onApplicationStatusChanged(Application application, 
                                           ApplicationStatus oldStatus, 
                                           ApplicationStatus newStatus,
                                           User changedBy) {
        UUID candidateId = application.getCandidate().getId();
        String jobTitle = application.getJob().getTitle();
        String companyName = application.getJob().getCompany().getName();
        UUID applicationId = application.getId();
        UUID jobId = application.getJob().getId();
        
        Map<String, Object> data = Map.of(
            "applicationId", applicationId.toString(),
            "jobId", jobId.toString(),
            "jobTitle", jobTitle,
            "companyName", companyName,
            "oldStatus", oldStatus.name(),
            "newStatus", newStatus.name(),
            "navigateTo", "/applications/" + applicationId
        );
        
        String title;
        String body;
        
        switch (newStatus) {
            case VIEWED:
                title = companyName + " đã xem hồ sơ của bạn";
                body = "Hồ sơ của bạn cho vị trí " + jobTitle + " đã được xem";
                break;
            case REVIEWING:
                title = "Hồ sơ đang được xem xét";
                body = companyName + " đang xem xét hồ sơ của bạn cho vị trí " + jobTitle;
                break;
            case SHORTLISTED:
                title = "🎉 Bạn đã lọt vào danh sách ngắn!";
                body = companyName + " đã chọn bạn vào danh sách ứng viên tiềm năng cho " + jobTitle;
                break;
            case INTERVIEW_SCHEDULED:
                title = "🎊 Bạn được mời phỏng vấn!";
                body = companyName + " muốn phỏng vấn bạn cho vị trí " + jobTitle;
                break;
            case REJECTED:
                title = "Thông báo về hồ sơ ứng tuyển";
                body = "Hồ sơ của bạn cho " + jobTitle + " tại " + companyName + " chưa phù hợp lúc này";
                break;
            case ACCEPTED:
                title = "🎉 Chúc mừng! Bạn đã được nhận!";
                body = companyName + " đã chấp nhận hồ sơ của bạn cho vị trí " + jobTitle;
                break;
            default:
                title = "Cập nhật hồ sơ ứng tuyển";
                body = "Hồ sơ của bạn tại " + companyName + " đã được cập nhật";
        }
        
        createAndPush(candidateId, NotificationType.APPLICATION_STATUS_CHANGED, title, body, data);
    }
    
    // ── NEW APPLICATION ─────────────────────────────────────
    
    public void onNewApplication(Application application) {
        // Notify HR của job đó
        UUID hrUserId = application.getJob().getCreatedBy().getId();
        String candidateName = application.getCandidate().getFullName();
        String jobTitle = application.getJob().getTitle();
        UUID jobId = application.getJob().getId();
        UUID applicationId = application.getId();
        
        Map<String, Object> data = Map.of(
            "applicationId", applicationId.toString(),
            "jobId", jobId.toString(),
            "jobTitle", jobTitle,
            "candidateName", candidateName,
            "navigateTo", "/jobs/" + jobId + "/applications"
        );
        
        createAndPush(
            hrUserId,
            NotificationType.NEW_APPLICATION,
            "Ứng viên mới ứng tuyển",
            candidateName + " vừa ứng tuyển vị trí " + jobTitle,
            data
        );
        
        // Check: nếu có nhiều application cùng job trong 1 giờ → batch notification
        // (Advanced feature - implement sau nếu cần)
    }
    
    // ── NEW MESSAGE ─────────────────────────────────────────
    
    public void onNewMessage(Message message, MessageThread thread) {
        UUID senderId = message.getSender().getId();
        UUID hrId = thread.getHrUser().getId();
        UUID candidateId = thread.getCandidateUser().getId();
        
        // Recipient là người kia
        UUID recipientId = senderId.equals(hrId) ? candidateId : hrId;
        
        String senderName = message.getSender().getFullName();
        String preview = message.getContentType() == MessageContentType.TEXT
            ? truncate(message.getContent(), 60)
            : "[Tệp đính kèm]";
        
        Map<String, Object> data = Map.of(
            "threadId", thread.getId().toString(),
            "messageId", message.getId().toString(),
            "senderId", senderId.toString(),
            "navigateTo", "/messages?thread=" + thread.getId()
        );
        
        createAndPush(
            recipientId,
            NotificationType.NEW_MESSAGE,
            senderName + " vừa nhắn tin",
            preview,
            data
        );
    }
    
    // ── QUERY METHODS ────────────────────────────────────────
    
    @Transactional(readOnly = true)
    public NotificationPageDto getNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository
            .findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        
        long unreadCount = notificationRepository.countByRecipientIdAndIsReadFalse(userId);
        
        return new NotificationPageDto(
            notifications.getContent().stream().map(this::toDto).collect(Collectors.toList()),
            unreadCount,
            notifications.hasNext()
        );
    }
    
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification n = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        if (!n.getRecipient().getId().equals(userId)) {
            throw new AccessDeniedException("Cannot mark others' notifications");
        }
        
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(Instant.now());
            notificationRepository.save(n);
        }
    }
    
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId, Instant.now());
        // Push updated count
        socketPusher.pushUnreadCounts(userId, 
            messageRepository.countTotalUnread(userId), 0L);
    }
    
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }
    
    // ── HELPERS ─────────────────────────────────────────────
    
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
    
    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
            n.getId(),
            n.getType(),
            n.getTitle(),
            n.getBody(),
            n.getData(),
            n.isRead(),
            n.getCreatedAt()
        );
    }
}
```

---

## 📋 BƯỚC 2 — Hook ApplicationService (tìm file, không tạo mới)

Agent phải **tìm file ApplicationService hiện tại** trong dự án và thêm:

```java
// Inject NotificationService vào ApplicationService
@Autowired
private NotificationService notificationService;

// Tìm method updateStatus (hoặc tên tương đương):
public Application updateApplicationStatus(UUID id, ApplicationStatus status) {
    Application app = findById(id);
    ApplicationStatus oldStatus = app.getStatus();
    app.setStatus(status);
    Application saved = repository.save(app);
    
    // === THÊM ===
    notificationService.onApplicationStatusChanged(saved, oldStatus, status, getCurrentUser());
    // ===========
    
    return saved;
}

// Tìm method createApplication (hoặc tên tương đương):
public Application createApplication(CreateApplicationRequest request) {
    // ... existing logic ...
    Application saved = repository.save(newApplication);
    
    // === THÊM ===
    notificationService.onNewApplication(saved);
    // ===========
    
    return saved;
}
```

**Quan trọng:** Nếu ApplicationService có nhiều methods đổi status (approve, reject, shortlist riêng), agent phải thêm hook vào TẤT CẢ methods đó.

---

## 📋 BƯỚC 3 — Hook MessageService

```java
// Trong MessageService.sendMessage():
public MessageDto sendMessage(UUID threadId, UUID senderId, SendMessageRequest request) {
    // ... existing logic ...
    Message saved = messageRepository.save(message);
    
    // Cập nhật thread last message
    thread.setLastMessageAt(saved.getCreatedAt());
    thread.setLastMessagePreview(buildPreview(saved));
    threadRepository.save(thread);
    
    // === THÊM ===
    notificationService.onNewMessage(saved, thread);
    // ===========
    
    return toDto(saved);
}
```

---

## 📋 BƯỚC 4 — Notification Aggregation (tránh spam)

```java
// Vấn đề: nếu HR thay đổi status 5 lần trong 1 giờ → candidate nhận 5 notifications
// Giải pháp: Kiểm tra notification gần nhất cùng type+applicationId

@Transactional
public void onApplicationStatusChanged(...) {
    // Check xem đã có notification tương tự trong 5 phút gần đây chưa
    // Nếu có → update notification đó thay vì tạo mới
    Optional<Notification> recent = notificationRepository
        .findRecentSameType(recipientId, NotificationType.APPLICATION_STATUS_CHANGED, 
                            applicationId, Instant.now().minus(5, ChronoUnit.MINUTES));
    
    if (recent.isPresent()) {
        // Update existing
        Notification n = recent.get();
        n.setTitle(title);
        n.setBody(body);
        n.setData(data);
        n.setRead(false); // reset read status
        n.setCreatedAt(Instant.now());
        Notification saved = notificationRepository.save(n);
        socketPusher.pushToUser(recipientId, toDto(saved));
    } else {
        createAndPush(recipientId, type, title, body, data);
    }
}
```

Thêm query vào repository:
```java
@Query("""
    SELECT n FROM Notification n
    WHERE n.recipient.id = :recipientId
      AND n.type = :type
      AND n.createdAt >= :since
      AND CAST(n.data->>'applicationId' AS text) = CAST(:applicationId AS text)
    ORDER BY n.createdAt DESC
    LIMIT 1
    """)
Optional<Notification> findRecentSameType(UUID recipientId, NotificationType type, 
                                          UUID applicationId, Instant since);
```

---

## 📋 BƯỚC 5 — Rate Limiting cho Message Notifications

```java
// Tránh spam notification khi chat liên tục:
// Chỉ gửi notification NEW_MESSAGE nếu người nhận không online (không đang mở thread)

// Cách 1: Track online users qua Node.js (phức tạp hơn)
// Cách 2: Đơn giản hơn — check xem có notification NEW_MESSAGE chưa đọc trong 2 phút không

public void onNewMessage(Message message, MessageThread thread) {
    UUID recipientId = ...;
    
    // Nếu recipient đã có notification chưa đọc từ cùng thread trong 2 phút → skip
    boolean hasRecentUnread = notificationRepository.existsRecentUnreadMessageNotif(
        recipientId, thread.getId(), Instant.now().minus(2, ChronoUnit.MINUTES)
    );
    
    if (!hasRecentUnread) {
        createAndPush(recipientId, NotificationType.NEW_MESSAGE, title, body, data);
    } else {
        // Vẫn cập nhật unread count badge
        long unreadNotifs = notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
        long unreadMessages = messageRepository.countTotalUnread(recipientId);
        socketPusher.pushUnreadCounts(recipientId, unreadMessages, unreadNotifs);
    }
}
```

---

## 📋 BƯỚC 6 — Cấu hình application.properties

```properties
# Socket server config
socket.server.url=http://localhost:4000
socket.internal.api-key=${SOCKET_INTERNAL_API_KEY:dev-secret-change-in-prod}

# Notification config  
notification.message.cooldown-minutes=2
notification.cleanup.days-to-keep=90

# Async executor cho notification push
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=5
```

---

## 📋 BƯỚC 7 — Scheduled Cleanup (optional nhưng professional)

```java
@Component
public class NotificationCleanupJob {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Value("${notification.cleanup.days-to-keep:90}")
    private int daysToKeep;
    
    // Chạy mỗi ngày lúc 2:00 AM
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldNotifications() {
        Instant cutoff = Instant.now().minus(daysToKeep, ChronoUnit.DAYS);
        int deleted = notificationRepository.deleteByCreatedAtBeforeAndIsReadTrue(cutoff);
        log.info("Cleaned up {} old notifications", deleted);
    }
}
```

---

## ✅ QA CHECKLIST — Phase 5

### Application Status Notifications
- [ ] HR đổi status → candidate nhận notification trong DB
- [ ] Socket push hoạt động (check qua browser console)
- [ ] Mỗi status có title/body message phù hợp
- [ ] Status REJECTED → message nhẹ nhàng, không harsh
- [ ] Status INTERVIEW_SCHEDULED → message phấn khích

### Message Notifications
- [ ] HR gửi tin → candidate nhận notification
- [ ] Candidate gửi tin → HR nhận notification
- [ ] Rate limiting: 5 tin nhắn liên tiếp trong 1 phút → chỉ 1 notification mới
- [ ] Nếu notification đã đọc → tin nhắn tiếp theo tạo notification mới

### New Application Notifications
- [ ] Candidate apply job → HR nhận notification
- [ ] navigateTo đúng → click sẽ đến đúng trang
- [ ] HR không nhận notification cho job của HR khác

### Aggregation
- [ ] Đổi status 2 lần trong 5 phút → 1 notification (updated)
- [ ] Đổi status sau 5 phút → notification mới

### API
- [ ] `GET /notifications` trả đúng list, sort DESC
- [ ] `POST /notifications/{id}/read` → mark 1 cái
- [ ] `POST /notifications/read-all` → mark tất cả
- [ ] `GET /notifications/unread-count` → đúng số
- [ ] Không thể đọc notification của người khác → 403

---

## 📤 Output của Phase 5

1. `NotificationService.java` hoàn chỉnh
2. Hooks trong `ApplicationService.java` (không break existing)
3. Hooks trong `MessageService.java`
4. `NotificationCleanupJob.java`
5. Cập nhật `application.properties`
6. Test evidence (screenshot hoặc log) cho mỗi notification type
