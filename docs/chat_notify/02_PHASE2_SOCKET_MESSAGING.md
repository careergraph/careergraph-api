# PHASE 2 — Socket Layer: Messaging & Typing Realtime
> **Service:** Node.js RTC Server (file `server.js` đã có)  
> **Prerequisites:** Phase 1 hoàn thành. Đọc kỹ toàn bộ `server.js` hiện tại để tránh conflict.

---

## 🎯 Mục tiêu Phase này

Mở rộng RTC server để xử lý realtime cho messaging:
- Namespace `/chat` riêng biệt — không ảnh hưởng namespace video call `/rtc`
- Typing indicator
- Read receipt realtime
- Message delivery realtime
- Online presence (ai đang online)
- Notification push từ Node.js

---

## 📋 BƯỚC 1 — Đọc hiểu code hiện tại (BẮT BUỘC)

```
Agent phải đọc server.js hiện tại và ghi chú:
1. JWT decode logic → copy sang namespace mới (đừng duplicate, hãy extract thành function)
2. CORS config → áp dụng cho namespaces mới
3. ENV variables pattern → thêm SPRING_BOOT_API_URL nếu cần call back BE
4. Logging pattern → consistent với existing logs
5. Các events hiện tại để không bị conflict tên
```

---

## 📋 BƯỚC 2 — Refactor: Tách auth middleware thành shared function

```javascript
// auth.js — extract từ server.js
const jwt = require("jsonwebtoken");
const JWT_SECRET = process.env.JWT_SIGNER_KEY || "";

function verifyToken(token) {
  return jwt.verify(token, Buffer.from(JWT_SECRET, "base64"), {
    algorithms: ["HS384"],
  });
}

function socketAuthMiddleware(socket, next) {
  const token = socket.handshake.auth?.token;
  if (!token) return next(new Error("Missing token"));
  
  try {
    const decoded = verifyToken(token);
    socket.data.user = decoded;
    const role = (decoded.role || decoded.scope || "").toString().toUpperCase();
    socket.data.isHost = role.includes("HR") || role.includes("ADMIN") || role.includes("ENTERPRISE");
    socket.data.userId = decoded.sub;
    next();
  } catch {
    next(new Error("Invalid token"));
  }
}

module.exports = { verifyToken, socketAuthMiddleware };
```

---

## 📋 BƯỚC 3 — Chat Namespace `/chat`

```javascript
// chat.js — namespace riêng

const { socketAuthMiddleware } = require("./auth");

// State cho chat namespace
// onlineUsers: Map<userId, Set<socketId>> — một user có thể mở nhiều tab
const onlineUsers = new Map();

// typingUsers: Map<threadId, Map<userId, timeout>> — ai đang gõ trong thread nào
const typingUsers = new Map();

function setupChatNamespace(io) {
  const chat = io.of("/chat");
  
  // Auth middleware
  chat.use(socketAuthMiddleware);
  
  chat.on("connection", (socket) => {
    const userId = socket.data.userId;
    console.log(`[chat] connect: ${userId} (${socket.id})`);
    
    // Track online status
    if (!onlineUsers.has(userId)) onlineUsers.set(userId, new Set());
    onlineUsers.get(userId).add(socket.id);
    
    // ── JOIN THREAD ──────────────────────────────────────────
    // Client join vào room của thread để nhận events
    socket.on("join-thread", (threadId) => {
      if (!threadId || typeof threadId !== "string") return;
      
      // Validate: chỉ join thread mình có quyền (gọi BE để verify hoặc trust JWT)
      // Simple approach: trust the request, BE sẽ filter data
      socket.join(`thread:${threadId}`);
      socket.data.threads = socket.data.threads || new Set();
      socket.data.threads.add(threadId);
      
      // Notify thread members về online status
      const onlineInThread = getOnlineUsersInThread(threadId, chat);
      socket.emit("thread-online-users", { threadId, onlineUsers: onlineInThread });
      socket.to(`thread:${threadId}`).emit("user-online", { userId, threadId });
      
      console.log(`[chat] ${userId} joined thread ${threadId}`);
    });
    
    // ── LEAVE THREAD ─────────────────────────────────────────
    socket.on("leave-thread", (threadId) => {
      socket.leave(`thread:${threadId}`);
      if (socket.data.threads) socket.data.threads.delete(threadId);
      socket.to(`thread:${threadId}`).emit("user-offline", { userId, threadId });
    });
    
    // ── NEW MESSAGE (relay từ client, sau khi BE đã lưu) ─────
    // Flow: Client gửi HTTP POST → BE lưu → BE/Client emit socket event
    // Cách 1: Client emit trực tiếp sau khi nhận response từ BE (preferred cho đơn giản)
    // Cách 2: BE webhook → Node server → broadcast
    // ➡️ Dùng Cách 1: client emit "new-message" với full message data từ BE response
    socket.on("new-message", ({ threadId, message }) => {
      if (!threadId || !message) return;
      if (!socket.data.threads?.has(threadId)) return; // phải join thread trước
      
      // Relay tới tất cả members trong thread (trừ sender)
      socket.to(`thread:${threadId}`).emit("new-message", {
        threadId,
        message, // MessageDto từ BE
      });
      
      console.log(`[chat] message relay: ${userId} → thread ${threadId}`);
    });
    
    // ── TYPING INDICATOR ─────────────────────────────────────
    socket.on("typing-start", ({ threadId }) => {
      if (!threadId || !socket.data.threads?.has(threadId)) return;
      
      // Clear existing timeout
      if (!typingUsers.has(threadId)) typingUsers.set(threadId, new Map());
      const threadTyping = typingUsers.get(threadId);
      
      if (threadTyping.has(userId)) clearTimeout(threadTyping.get(userId));
      
      // Auto-stop typing sau 3 giây nếu không có event mới
      const timeout = setTimeout(() => {
        threadTyping.delete(userId);
        chat.to(`thread:${threadId}`).emit("typing-stop", { threadId, userId });
      }, 3000);
      
      threadTyping.set(userId, timeout);
      
      // Broadcast tới thread (trừ sender)
      socket.to(`thread:${threadId}`).emit("typing-start", { 
        threadId, 
        userId,
        displayName: buildDisplayName(socket.data.user)
      });
    });
    
    socket.on("typing-stop", ({ threadId }) => {
      if (!threadId) return;
      
      const threadTyping = typingUsers.get(threadId);
      if (threadTyping?.has(userId)) {
        clearTimeout(threadTyping.get(userId));
        threadTyping.delete(userId);
      }
      
      socket.to(`thread:${threadId}`).emit("typing-stop", { threadId, userId });
    });
    
    // ── READ RECEIPT ─────────────────────────────────────────
    // Client emit sau khi gọi POST /threads/{id}/read thành công
    socket.on("messages-read", ({ threadId, lastReadMessageId }) => {
      if (!threadId || !socket.data.threads?.has(threadId)) return;
      
      socket.to(`thread:${threadId}`).emit("messages-read", {
        threadId,
        userId,
        lastReadMessageId,
        readAt: new Date().toISOString(),
      });
    });
    
    // ── MESSAGE DELETED ──────────────────────────────────────
    socket.on("message-deleted", ({ threadId, messageId }) => {
      if (!threadId || !messageId) return;
      if (!socket.data.threads?.has(threadId)) return;
      
      socket.to(`thread:${threadId}`).emit("message-deleted", {
        threadId,
        messageId,
        deletedAt: new Date().toISOString(),
      });
    });
    
    // ── DISCONNECT ────────────────────────────────────────────
    socket.on("disconnect", () => {
      // Cleanup online tracking
      const userSockets = onlineUsers.get(userId);
      if (userSockets) {
        userSockets.delete(socket.id);
        if (userSockets.size === 0) {
          onlineUsers.delete(userId);
          // Notify all threads user đang ở rằng họ offline
          if (socket.data.threads) {
            for (const threadId of socket.data.threads) {
              chat.to(`thread:${threadId}`).emit("user-offline", { userId, threadId });
            }
          }
        }
      }
      
      // Cleanup typing
      if (socket.data.threads) {
        for (const threadId of socket.data.threads) {
          const threadTyping = typingUsers.get(threadId);
          if (threadTyping?.has(userId)) {
            clearTimeout(threadTyping.get(userId));
            threadTyping.delete(userId);
            chat.to(`thread:${threadId}`).emit("typing-stop", { threadId, userId });
          }
        }
      }
      
      console.log(`[chat] disconnect: ${userId}`);
    });
  });
  
  // Helper: lấy danh sách user online trong một thread
  function getOnlineUsersInThread(threadId, chatNs) {
    const room = chatNs.adapter.rooms.get(`thread:${threadId}`);
    if (!room) return [];
    
    const onlineUserIds = new Set();
    for (const socketId of room) {
      const s = chatNs.sockets.get(socketId);
      if (s) onlineUserIds.add(s.data.userId);
    }
    return Array.from(onlineUserIds);
  }
  
  return chat;
}

module.exports = { setupChatNamespace };
```

---

## 📋 BƯỚC 4 — Notification Namespace `/notify`

```javascript
// notify.js

const { socketAuthMiddleware } = require("./auth");

// notifyConnections: Map<userId, Set<socketId>>
const notifyConnections = new Map();

function setupNotifyNamespace(io) {
  const notify = io.of("/notify");
  notify.use(socketAuthMiddleware);
  
  notify.on("connection", (socket) => {
    const userId = socket.data.userId;
    
    // User join room cá nhân của mình
    socket.join(`user:${userId}`);
    
    if (!notifyConnections.has(userId)) notifyConnections.set(userId, new Set());
    notifyConnections.get(userId).add(socket.id);
    
    console.log(`[notify] connect: ${userId}`);
    
    socket.on("disconnect", () => {
      const sockets = notifyConnections.get(userId);
      if (sockets) {
        sockets.delete(socket.id);
        if (sockets.size === 0) notifyConnections.delete(userId);
      }
      console.log(`[notify] disconnect: ${userId}`);
    });
  });
  
  // ── Public API để push notification từ bên ngoài (BE gọi qua internal HTTP) ──
  // Hoặc có thể dùng Redis pub/sub nếu scale sau này
  function pushNotification(userId, notification) {
    notify.to(`user:${userId}`).emit("notification", notification);
    console.log(`[notify] push to ${userId}: ${notification.type}`);
  }
  
  function pushUnreadCount(userId, counts) {
    // counts: { messages: number, notifications: number }
    notify.to(`user:${userId}`).emit("unread-counts", counts);
  }
  
  return { namespace: notify, pushNotification, pushUnreadCount };
}

module.exports = { setupNotifyNamespace };
```

---

## 📋 BƯỚC 5 — Internal HTTP API (để BE Spring Boot push notifications)

```javascript
// internal-api.js — Express router
// Spring Boot gọi endpoint này sau khi tạo notification

const express = require("express");
const router = express.Router();
const INTERNAL_API_KEY = process.env.INTERNAL_API_KEY || "change-me-in-production";

// Middleware xác thực internal call
router.use((req, res, next) => {
  const apiKey = req.headers["x-internal-api-key"];
  if (apiKey !== INTERNAL_API_KEY) {
    return res.status(401).json({ error: "Unauthorized" });
  }
  next();
});

// POST /internal/notify — push notification đến user
// Body: { userId, notification: { id, type, title, body, data, createdAt } }
router.post("/notify", (req, res) => {
  const { userId, notification } = req.body;
  if (!userId || !notification) {
    return res.status(400).json({ error: "Missing userId or notification" });
  }
  
  // Inject notifyService (được set khi setup)
  req.app.get("notifyService").pushNotification(userId, notification);
  res.json({ ok: true });
});

// POST /internal/notify/bulk — push nhiều users
router.post("/notify/bulk", (req, res) => {
  const { userIds, notification } = req.body;
  if (!Array.isArray(userIds) || !notification) {
    return res.status(400).json({ error: "Invalid payload" });
  }
  
  const notifyService = req.app.get("notifyService");
  for (const userId of userIds) {
    notifyService.pushNotification(userId, notification);
  }
  res.json({ ok: true, pushed: userIds.length });
});

// POST /internal/unread-counts — cập nhật badge counts
router.post("/unread-counts", (req, res) => {
  const { userId, messages, notifications } = req.body;
  if (!userId) return res.status(400).json({ error: "Missing userId" });
  
  req.app.get("notifyService").pushUnreadCount(userId, { messages, notifications });
  res.json({ ok: true });
});

module.exports = router;
```

---

## 📋 BƯỚC 6 — Cập nhật `server.js` chính

```javascript
// Thêm vào server.js (đừng xóa code cũ)

const { setupChatNamespace } = require("./chat");
const { setupNotifyNamespace } = require("./notify");
const internalRouter = require("./internal-api");

// ... existing io setup ...

// Setup namespaces
setupChatNamespace(io);
const notifyService = setupNotifyNamespace(io);

// Lưu notifyService để internal-api dùng
app.set("notifyService", notifyService);

// Internal API (chỉ accessible từ localhost hoặc internal network)
app.use("/internal", express.json(), internalRouter);

// ... existing code ...
```

---

## 📋 BƯỚC 7 — Spring Boot: NotificationPushService

Thêm vào Spring Boot để gọi Node server sau khi tạo notification:

```java
@Service
public class SocketNotificationPusher {
    
    private final RestTemplate restTemplate;
    
    @Value("${socket.server.url:http://localhost:4000}")
    private String socketServerUrl;
    
    @Value("${socket.internal.api-key}")
    private String internalApiKey;
    
    public void pushToUser(UUID userId, NotificationDto notification) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = Map.of(
                "userId", userId.toString(),
                "notification", notification
            );
            
            restTemplate.postForEntity(
                socketServerUrl + "/internal/notify",
                new HttpEntity<>(body, headers),
                Void.class
            );
        } catch (Exception e) {
            // Log nhưng không fail transaction chính
            log.warn("Failed to push socket notification to {}: {}", userId, e.getMessage());
        }
    }
    
    public void pushUnreadCounts(UUID userId, long messages, long notifications) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = Map.of(
                "userId", userId.toString(),
                "messages", messages,
                "notifications", notifications
            );
            
            restTemplate.postForEntity(
                socketServerUrl + "/internal/unread-counts",
                new HttpEntity<>(body, headers),
                Void.class
            );
        } catch (Exception e) {
            log.warn("Failed to push unread counts to {}: {}", userId, e.getMessage());
        }
    }
}
```

---

## ✅ QA CHECKLIST — Phase 2

### Socket Events Test (dùng socket.io-client test script hoặc browser console)

- [ ] Connect `/chat` với valid JWT → thành công
- [ ] Connect `/chat` với invalid JWT → connection refused
- [ ] `join-thread` → nhận `thread-online-users`
- [ ] User A join thread, User B join → A nhận `user-online` event
- [ ] User A `typing-start` → User B nhận event với đúng userId
- [ ] Typing tự stop sau 3 giây nếu không có event mới
- [ ] `typing-stop` manual → B nhận event ngay
- [ ] `new-message` relay → đúng receiver nhận, sender không nhận lại
- [ ] `messages-read` → relay đúng
- [ ] Disconnect → `user-offline` broadcast đúng
- [ ] Multiple tabs: user vẫn online nếu còn 1 tab, offline khi tất cả đóng

### Internal API Test
- [ ] `POST /internal/notify` không có API key → 401
- [ ] `POST /internal/notify` đúng key → user nhận notification qua socket
- [ ] `/notify` namespace connect → user join đúng room

### Regression Test (đảm bảo video call không bị vỡ)
- [ ] Tất cả events video call hiện tại vẫn hoạt động bình thường
- [ ] Namespace isolation: chat events không leak sang rtc

---

## 📤 Output của Phase 2

1. `auth.js` — shared auth middleware
2. `chat.js` — chat namespace
3. `notify.js` — notification namespace  
4. `internal-api.js` — internal HTTP endpoints
5. `server.js` updated (không break existing code)
6. `SocketNotificationPusher.java` trong Spring Boot
7. Test script verify tất cả events
