# 02 — Thiết kế Kiến trúc & Kỹ thuật

---

## 1. Database Schema (thêm mới)

```sql
-- Bảng log trạng thái từng lần reminder đã gửi
CREATE TABLE interview_reminder_log (
    id              BIGSERIAL PRIMARY KEY,
    interview_id    BIGINT NOT NULL REFERENCES interviews(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    reminder_type   VARCHAR(10) NOT NULL,  -- 'T30', 'T15', 'IMMINENT'
    status          VARCHAR(20) NOT NULL DEFAULT 'SENT',
                    -- SENT | DELIVERED | DISMISSED | EXPIRED | SNOOZED
    sent_at         TIMESTAMP NOT NULL DEFAULT now(),
    delivered_at    TIMESTAMP,
    dismissed_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_reminder_log_interview ON interview_reminder_log(interview_id);
CREATE INDEX idx_reminder_log_user ON interview_reminder_log(user_id, status);

-- Thêm cột vào bảng interviews nếu chưa có
ALTER TABLE interviews
    ADD COLUMN IF NOT EXISTS room_link VARCHAR(500),
    ADD COLUMN IF NOT EXISTS reminder_sent_30 BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS reminder_sent_15 BOOLEAN DEFAULT FALSE;
```

---

## 2. Redis Key Design

```
# Dedup guard — ngăn gửi 2 lần
reminder:dedup:{interviewId}:T30    TTL = 2h
reminder:dedup:{interviewId}:T15    TTL = 1h

# Track user online status (nếu chưa có)
user:online:{userId}                TTL = 30s (refresh bởi heartbeat)

# Pending reminders cho user offline
reminder:pending:{userId}           Type: LIST, TTL = 24h
```

---

## 3. Spring Boot API — Endpoints mới

```
GET  /api/v1/interviews/today
     → Lấy danh sách interview hôm nay của user (SCHEDULED)
     → Response: List<InterviewReminderDTO>

GET  /api/v1/interviews/pending-reminders
     → Lấy reminders pending khi user vừa online
     → Response: List<PendingReminderDTO>

POST /api/v1/interviews/{id}/reminder-ack
     → FE báo đã nhận/dismiss reminder
     → Body: { type: "T15", action: "DISMISSED" | "JOINED" | "SNOOZED" }

POST /api/v1/interviews/{id}/snooze
     → Snooze reminder thêm 5 phút
     → Chỉ cho phép 1 lần / interview / type
```

---

## 4. DTOs (Java)

```java
// InterviewReminderDTO.java
public record InterviewReminderDTO(
    Long interviewId,
    Long candidateId,
    Long hrId,
    String jobTitle,
    String companyName,
    LocalDateTime scheduledAt,
    String roomLink,
    String status
) {}

// PendingReminderDTO.java
public record PendingReminderDTO(
    Long interviewId,
    String reminderType,   // T30 | T15 | IMMINENT
    LocalDateTime scheduledAt,
    String roomLink,
    boolean isMissed       // scheduled_at đã qua
) {}

// ReminderSocketPayload.java (để emit qua RTC)
public record ReminderSocketPayload(
    String event,          // "interview.reminder"
    Long interviewId,
    String jobTitle,
    String companyName,
    LocalDateTime scheduledAt,
    String roomLink,
    int minutesBefore,     // 30 | 15 | 0
    String reminderType    // T30 | T15 | IMMINENT
) {}
```

---

## 5. Scheduler Service (Spring Boot)

```java
@Service
@RequiredArgsConstructor
public class InterviewReminderScheduler {

    private final InterviewRepository interviewRepo;
    private final ReminderLogRepository reminderLogRepo;
    private final RedisTemplate<String, String> redis;
    private final RtcEventPublisher rtcPublisher;  // gọi sang RTC service

    // Chạy mỗi 1 phút
    @Scheduled(fixedDelay = 60_000)
    public void processReminders() {
        LocalDateTime now = LocalDateTime.now();

        // Query T-30: scheduled_at trong [now+29, now+31]
        List<Interview> t30 = interviewRepo
            .findScheduledBetween(now.plusMinutes(29), now.plusMinutes(31));
        t30.forEach(i -> sendReminder(i, "T30", 30));

        // Query T-15: scheduled_at trong [now+14, now+16]
        List<Interview> t15 = interviewRepo
            .findScheduledBetween(now.plusMinutes(14), now.plusMinutes(16));
        t15.forEach(i -> sendReminder(i, "T15", 15));
    }

    private void sendReminder(Interview interview, String type, int minutesBefore) {
        String deupKey = "reminder:dedup:" + interview.getId() + ":" + type;
        
        // Dedup check — atomic set với NX
        Boolean isNew = redis.opsForValue()
            .setIfAbsent(deupKey, "1", Duration.ofHours(2));
        if (Boolean.FALSE.equals(isNew)) return; // Đã gửi rồi

        // Emit tới ứng viên
        emitToUser(interview.getCandidateId(), interview, type, minutesBefore);
        // Emit tới HR
        emitToUser(interview.getHrId(), interview, type, minutesBefore);

        // Log
        saveLog(interview, type);
    }

    private void emitToUser(Long userId, Interview i, String type, int min) {
        var payload = new ReminderSocketPayload(
            "interview.reminder",
            i.getId(), i.getJobTitle(), i.getCompanyName(),
            i.getScheduledAt(), i.getRoomLink(), min, type
        );
        rtcPublisher.emitToUser(userId, payload);
    }
}
```

---

## 6. RTC Service — Socket Events mới

### Thêm vào RTC Service (Node.js / Spring WebSocket)

```javascript
// Node.js Socket.IO example
// Nhận từ Spring Boot API publish qua Redis Pub/Sub hoặc HTTP call

redisSubscriber.subscribe('interview:reminder', (message) => {
    const { userId, payload } = JSON.parse(message);
    const socketRoom = `user:${userId}`;
    
    // Kiểm tra user có online không
    if (io.sockets.adapter.rooms.has(socketRoom)) {
        io.to(socketRoom).emit('interview.reminder', payload);
        // Cập nhật delivered status
        updateDelivered(payload.interviewId, userId, payload.reminderType);
    } else {
        // User offline → lưu pending vào Redis list
        redis.lpush(`reminder:pending:${userId}`, JSON.stringify(payload));
        redis.expire(`reminder:pending:${userId}`, 86400); // 24h
    }
});

// Khi user kết nối socket
socket.on('connect', async () => {
    const userId = socket.handshake.auth.userId;
    socket.join(`user:${userId}`);
    
    // Flush pending reminders
    const pending = await redis.lrange(`reminder:pending:${userId}`, 0, -1);
    if (pending.length > 0) {
        await redis.del(`reminder:pending:${userId}`);
        pending.forEach(p => socket.emit('interview.reminder', JSON.parse(p)));
    }
});
```

---

## 7. Frontend — Event Handling

```typescript
// reminderService.ts

interface ReminderPayload {
    interviewId: number;
    jobTitle: string;
    companyName: string;
    scheduledAt: string;
    roomLink: string;
    minutesBefore: number;
    reminderType: 'T30' | 'T15' | 'IMMINENT';
}

class ReminderService {
    private timeoutIds: Map<string, ReturnType<typeof setTimeout>> = new Map();
    private popupQueue: ReminderPayload[] = [];
    private isShowingPopup = false;

    // Gọi khi đăng nhập / page load
    async initTodayReminders() {
        const interviews = await api.get('/interviews/today');
        interviews.forEach(iv => this.scheduleLocalReminders(iv));
        
        // Pending reminders (khi vừa online lại)
        const pending = await api.get('/interviews/pending-reminders');
        pending.forEach(r => this.enqueuePopup(r));
    }

    scheduleLocalReminders(interview: InterviewReminderDTO) {
        const remainMs = new Date(interview.scheduledAt).getTime() - Date.now();
        const id = interview.interviewId;

        if (remainMs > 30 * 60 * 1000) {
            this.setReminder(`${id}-T30`, remainMs - 30 * 60 * 1000, interview, 'T30');
        }
        if (remainMs > 15 * 60 * 1000) {
            this.setReminder(`${id}-T15`, remainMs - 15 * 60 * 1000, interview, 'T15');
        } else if (remainMs > 0) {
            this.enqueuePopup({ ...interview, minutesBefore: 0, reminderType: 'IMMINENT' });
        }
    }

    private setReminder(key: string, delayMs: number, interview: any, type: string) {
        const tid = setTimeout(() => {
            this.enqueuePopup({ ...interview, reminderType: type });
        }, delayMs);
        this.timeoutIds.set(key, tid);
    }

    // Được gọi khi nhận socket event
    onSocketReminder(payload: ReminderPayload) {
        // Clear local timeout tương ứng để tránh duplicate
        const key = `${payload.interviewId}-${payload.reminderType}`;
        if (this.timeoutIds.has(key)) {
            clearTimeout(this.timeoutIds.get(key)!);
            this.timeoutIds.delete(key);
        }
        this.enqueuePopup(payload);
    }

    // Hủy lịch khi interview bị cancel
    cancelReminders(interviewId: number) {
        ['T30', 'T15'].forEach(type => {
            const key = `${interviewId}-${type}`;
            if (this.timeoutIds.has(key)) {
                clearTimeout(this.timeoutIds.get(key)!);
                this.timeoutIds.delete(key);
            }
        });
        popupStore.dismissByInterviewId(interviewId);
    }

    private enqueuePopup(payload: ReminderPayload) {
        this.popupQueue.push(payload);
        if (!this.isShowingPopup) this.showNext();
    }

    private showNext() {
        if (this.popupQueue.length === 0) {
            this.isShowingPopup = false;
            return;
        }
        this.isShowingPopup = true;
        const next = this.popupQueue.shift()!;
        popupStore.show(next);
    }
}

export const reminderService = new ReminderService();
```

---

## 8. Socket Events Summary

| Event | Direction | Khi nào |
|-------|-----------|---------|
| `interview.reminder` | Server → Client | T-30, T-15, IMMINENT |
| `interview.scheduled.imminent` | Server → Client | Lên lịch khi còn < 30p |
| `interview.cancelled` | Server → Client | Huỷ lịch |
| `interview.rescheduled` | Server → Client | Đổi giờ |
| `reminder.ack` | Client → Server | User dismiss/join/snooze |
