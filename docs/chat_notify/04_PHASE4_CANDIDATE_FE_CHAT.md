# PHASE 4 — Candidate Frontend: Inbox & Chat UI
> **Service:** FE Candidate (React JavaScript)  
> **Prerequisites:** Phase 1, 2, 3 hoàn thành. Đọc toàn bộ source FE Candidate để hiểu pattern.

---

## 🎯 Mục tiêu Phase này

Candidate hiện tại chưa có trang quản lý tin nhắn. Phase này xây dựng:
1. **Trang Inbox** (`/messages`) — danh sách cuộc trò chuyện với HR
2. **Chat view** — giao diện nhắn tin đầy đủ
3. **Floating chat button** (optional) — truy cập nhanh từ trang profile job

Candidate **chỉ nhắn tin với HR** của các công ty đã/đang ứng tuyển.

---

## 📋 BƯỚC 1 — Đọc hiểu dự án (BẮT BUỘC)

```
Agent đọc và ghi chú:
1. JavaScript hay JSX? Có TypeScript không? (dự án dùng .js hay .jsx?)
2. State management: Redux? Zustand? Context?
3. Router: v5 hay v6?
4. API client pattern: axios? fetch? SWR? React Query?
5. UI: có dùng component library không? hay custom CSS/Tailwind?
6. Trang profile/dashboard của candidate hiện có gì?
7. Auth token lấy từ đâu?
8. Có notification bell chưa?
9. Layout: có sidebar navigation không?
```

---

## 📋 BƯỚC 2 — Phân tích UX cho Candidate

### Entry points vào chat:
1. **Navigation**: thêm "Tin nhắn" vào sidebar/navbar với unread badge
2. **Trang Jobs Applied**: bên cạnh mỗi job đã nộp → nút "Nhắn tin với HR"
3. **Notification**: click notification "Bạn có tin nhắn mới" → mở thread

### Điểm khác biệt so với HR FE:
- Candidate chỉ thấy threads của mình (không có trang quản lý nhiều HR)
- Candidate không tạo thread trước (HR thường là người tạo từ kanban)
  → Nhưng candidate cũng có thể nhắn tin trước nếu muốn hỏi về job
- Candidate xem được job context của thread (đang ứng tuyển job nào)

---

## 📋 BƯỚC 3 — Folder Structure (JavaScript)

```
src/
├── features/
│   ├── messaging/
│   │   ├── api/
│   │   │   └── messagingApi.js
│   │   ├── hooks/
│   │   │   ├── useThreads.js
│   │   │   ├── useChatSocket.js
│   │   │   └── useMessages.js
│   │   ├── context/
│   │   │   └── MessagingContext.js   ← nếu không có Redux
│   │   ├── components/
│   │   │   ├── InboxList.jsx
│   │   │   ├── ThreadCard.jsx        ← khác HR: hiển thị company/HR info
│   │   │   ├── ChatWindow.jsx
│   │   │   ├── MessageBubble.jsx
│   │   │   ├── MessageInput.jsx
│   │   │   ├── TypingIndicator.jsx
│   │   │   └── EmptyInbox.jsx
│   │   └── pages/
│   │       └── MessagesPage.jsx
│   └── notifications/
│       ├── hooks/
│       │   └── useNotifications.js
│       └── components/
│           ├── NotificationBell.jsx
│           └── NotificationList.jsx
```

---

## 📋 BƯỚC 4 — API Layer (JavaScript)

```javascript
// messagingApi.js — Reuse logic tương tự HR nhưng không cần TypeScript

import { axiosInstance } from '@/api/axiosInstance'; // hoặc tương đương

export const messagingApi = {
  
  getThreads: (page = 0, size = 20) =>
    axiosInstance.get('/api/v1/messages/threads', {
      params: { page, size }
    }),
  
  // Candidate tạo thread khi muốn hỏi HR về job
  getOrCreateThread: (hrUserId, applicationId) =>
    axiosInstance.post('/api/v1/messages/threads', {
      hrUserId,       // BE sẽ xác định HR từ application nếu có
      applicationId,
    }),
  
  getMessages: (threadId, page = 0, size = 30) =>
    axiosInstance.get(`/api/v1/messages/threads/${threadId}/messages`, {
      params: { page, size, sort: 'createdAt,asc' }
    }),
  
  sendMessage: (threadId, content) =>
    axiosInstance.post(`/api/v1/messages/threads/${threadId}/messages`, {
      content,
      contentType: 'TEXT',
    }),
  
  markAsRead: (threadId) =>
    axiosInstance.post(`/api/v1/messages/threads/${threadId}/read`),
  
  deleteMessage: (messageId) =>
    axiosInstance.delete(`/api/v1/messages/${messageId}`),
  
  getUnreadCount: () =>
    axiosInstance.get('/api/v1/messages/unread-count'),
};
```

---

## 📋 BƯỚC 5 — Socket Hook (JavaScript)

```javascript
// useChatSocket.js — Tương tự HR nhưng JavaScript thuần

import { useEffect, useRef, useCallback } from 'react';
import { io } from 'socket.io-client';

const SOCKET_URL = process.env.REACT_APP_SOCKET_URL || 'http://localhost:4000';

export function useChatSocket({ token, onNewMessage, onTypingStart, onTypingStop,
                                onUserOnline, onUserOffline, onMessagesRead, onMessageDeleted }) {
  const socketRef = useRef(null);
  
  useEffect(() => {
    if (!token) return;
    
    const socket = io(`${SOCKET_URL}/chat`, {
      auth: { token },
      reconnection: true,
      reconnectionDelay: 1000,
    });
    
    socketRef.current = socket;
    
    socket.on('new-message', onNewMessage);
    socket.on('typing-start', onTypingStart);
    socket.on('typing-stop', onTypingStop);
    socket.on('user-online', onUserOnline);
    socket.on('user-offline', onUserOffline);
    socket.on('messages-read', onMessagesRead);
    socket.on('message-deleted', onMessageDeleted);
    
    return () => socket.disconnect();
  }, [token]); // eslint-disable-line react-hooks/exhaustive-deps
  
  const joinThread = useCallback((threadId) => {
    socketRef.current?.emit('join-thread', threadId);
  }, []);
  
  const leaveThread = useCallback((threadId) => {
    socketRef.current?.emit('leave-thread', threadId);
  }, []);
  
  const emitTypingStart = useCallback((threadId) => {
    socketRef.current?.emit('typing-start', { threadId });
  }, []);
  
  const emitTypingStop = useCallback((threadId) => {
    socketRef.current?.emit('typing-stop', { threadId });
  }, []);
  
  const broadcastMessage = useCallback((threadId, message) => {
    socketRef.current?.emit('new-message', { threadId, message });
  }, []);
  
  const broadcastRead = useCallback((threadId, lastReadMessageId) => {
    socketRef.current?.emit('messages-read', { threadId, lastReadMessageId });
  }, []);
  
  return { joinThread, leaveThread, emitTypingStart, emitTypingStop, broadcastMessage, broadcastRead };
}
```

---

## 📋 BƯỚC 6 — Components

### `MessagesPage.jsx`

```jsx
// Layout giống HR nhưng context khác:
// - "Các cuộc trò chuyện của bạn với nhà tuyển dụng"
// - Hiển thị company logo/name thay vì candidate info
// - Thread item: avatar HR, tên HR, tên công ty, tên job, preview

const MessagesPage = () => {
  const [selectedThreadId, setSelectedThreadId] = useState(null);
  
  // Sync URL: /messages?thread=xxx
  
  return (
    <div className="candidate-messages-page">
      <InboxList 
        selectedThreadId={selectedThreadId}
        onSelectThread={setSelectedThreadId}
      />
      <div className="chat-panel">
        {selectedThreadId 
          ? <ChatWindow threadId={selectedThreadId} />
          : <EmptyInbox />
        }
      </div>
    </div>
  );
};
```

### `ThreadCard.jsx` — Candidate thấy HR info

```jsx
// Khác HR: hiển thị thông tin của HR + công ty
// Layout:
// [Avatar HR] [Tên HR] [Tên công ty]
//             [Job title đã ứng tuyển]
//             [Last message preview]           [Thời gian]
//                                          [Unread badge]
//
// Status chip nhỏ: trạng thái ứng tuyển (nếu có applicationId)
// VD: "Đang xem xét" | "Phỏng vấn" | etc.
```

### `EmptyInbox.jsx`

```jsx
// Khi candidate chưa có cuộc trò chuyện nào:
// Illustration đẹp + text:
// "Bạn chưa có tin nhắn nào"
// "Khi nhà tuyển dụng nhắn tin cho bạn, chúng sẽ hiển thị ở đây"
// Button: "Xem việc làm đang ứng tuyển" → link đến trang applications
```

### `ChatWindow.jsx` — Shared logic với HR

```jsx
// Phần lớn giống HR ChatWindow
// Điểm khác:
// - Header: hiển thị tên HR + tên công ty (không phải candidate)
// - Có thể thêm link "Xem chi tiết job" trong header
// - Compact context info bar: "Cuộc trò chuyện về [Job Title] tại [Company]"
```

---

## 📋 BƯỚC 7 — Thêm Entry Points

### Trong Navigation (Navbar/Sidebar):
```jsx
// Tìm file navigation của candidate FE và thêm:
<NavLink to="/messages" className={/* active styles */}>
  <MessageIcon />
  <span>Tin nhắn</span>
  {unreadCount > 0 && (
    <span className="badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
  )}
</NavLink>
```

### Trong trang "Việc làm đã ứng tuyển":
```jsx
// Tìm component hiển thị danh sách applications của candidate
// Thêm button "Nhắn tin với HR" cho mỗi application:
<button 
  onClick={() => handleOpenChat(application.hrUserId, application.id)}
  className="chat-btn"
>
  <MessageIcon size={16} />
  Nhắn tin với HR
</button>

// Handler:
const handleOpenChat = async (hrUserId, applicationId) => {
  const { data } = await messagingApi.getOrCreateThread(hrUserId, applicationId);
  navigate(`/messages?thread=${data.threadId}`);
};
```

---

## 📋 BƯỚC 8 — Messaging Context (nếu không dùng Redux)

```jsx
// MessagingContext.js — global state cho messaging

const MessagingContext = createContext(null);

export const MessagingProvider = ({ children }) => {
  const { token } = useAuth(); // lấy token từ auth context
  const [threads, setThreads] = useState([]);
  const [messages, setMessages] = useState({}); // { threadId: Message[] }
  const [typingUsers, setTypingUsers] = useState({}); // { threadId: userId[] }
  const [onlineUsers, setOnlineUsers] = useState({}); // { userId: bool }
  const [totalUnread, setTotalUnread] = useState(0);
  
  const socketHandlers = {
    onNewMessage: ({ threadId, message }) => {
      setMessages(prev => ({
        ...prev,
        [threadId]: [...(prev[threadId] || []), message],
      }));
      // Cập nhật thread preview
      setThreads(prev => prev.map(t => 
        t.threadId === threadId 
          ? { ...t, lastMessagePreview: message.content, lastMessageAt: message.createdAt, unreadCount: t.unreadCount + 1 }
          : t
      ));
    },
    onTypingStart: ({ threadId, userId }) => {
      setTypingUsers(prev => ({
        ...prev,
        [threadId]: [...new Set([...(prev[threadId] || []), userId])],
      }));
    },
    onTypingStop: ({ threadId, userId }) => {
      setTypingUsers(prev => ({
        ...prev,
        [threadId]: (prev[threadId] || []).filter(id => id !== userId),
      }));
    },
    onUserOnline: ({ userId }) => setOnlineUsers(prev => ({ ...prev, [userId]: true })),
    onUserOffline: ({ userId }) => setOnlineUsers(prev => ({ ...prev, [userId]: false })),
    onMessagesRead: ({ threadId, lastReadMessageId }) => {
      // Mark messages as read
    },
    onMessageDeleted: ({ threadId, messageId }) => {
      setMessages(prev => ({
        ...prev,
        [threadId]: (prev[threadId] || []).map(m =>
          m.id === messageId ? { ...m, deleted: true, content: '' } : m
        ),
      }));
    },
  };
  
  const socketActions = useChatSocket({ token, ...socketHandlers });
  
  const value = {
    threads, setThreads,
    messages, setMessages,
    typingUsers,
    onlineUsers,
    totalUnread, setTotalUnread,
    ...socketActions,
  };
  
  return (
    <MessagingContext.Provider value={value}>
      {children}
    </MessagingContext.Provider>
  );
};

export const useMessaging = () => {
  const ctx = useContext(MessagingContext);
  if (!ctx) throw new Error('useMessaging must be used within MessagingProvider');
  return ctx;
};
```

---

## 📋 BƯỚC 9 — UI Requirements cho Candidate

### Phong cách thiết kế:
- Thân thiện hơn HR (candidate là user cuối, cần UX dễ dùng hơn)
- Mobile-first (candidate thường dùng điện thoại)
- Màu sắc theo brand của dự án candidate FE

### Mobile UX (BẮT BUỘC):
- Default view: thread list (full screen)
- Click thread: slide sang chat (full screen)
- Back button rõ ràng
- Bottom navigation (nếu dự án có)
- Touch targets đủ lớn (min 44px)

### Context job trong chat:
```jsx
// Sticky bar nhỏ ở trên cùng ChatWindow:
<div className="job-context-bar">
  <CompanyLogo src={company.logoUrl} size={24} />
  <span>Về vị trí <strong>{jobTitle}</strong></span>
  <span className={`status-chip status-${applicationStatus.toLowerCase()}`}>
    {applicationStatusLabel}
  </span>
  <Link to={`/jobs/${jobId}`}>Xem chi tiết →</Link>
</div>
```

---

## ✅ QA CHECKLIST — Phase 4

### Functional
- [ ] Candidate vào `/messages` thấy danh sách threads
- [ ] Thread list có company name, HR name, job title, preview
- [ ] Click thread → chat mở đúng
- [ ] Candidate nhận tin nhắn realtime khi HR gửi
- [ ] Candidate gửi tin → HR nhận realtime
- [ ] Typing indicator hoạt động 2 chiều
- [ ] Unread badge trên navigation cập nhật realtime
- [ ] Read receipt hoạt động
- [ ] "Nhắn tin với HR" từ trang applications → tạo/mở thread đúng
- [ ] Job context bar hiển thị đúng info

### Mobile
- [ ] Thread list → click → chat chuyển đúng
- [ ] Back button trong chat → quay về thread list
- [ ] Keyboard mobile không che input
- [ ] Touch targets đủ lớn

### Cross-service Integration
- [ ] HR gửi → Candidate nhận (test end-to-end)
- [ ] Candidate gửi → HR nhận ở cả `/messages` và tab kanban
- [ ] Unread count sync đúng giữa 2 phía

---

## 🔧 Addendum — Layout Parity With HR (2026-04-15)

### Bắt buộc đồng nhất với HR
- Outgoing message luôn bên phải.
- Incoming message luôn bên trái.
- Không tách riêng behavior bubble/read/unread/typing nếu không có lý do nghiệp vụ.

### Name/Avatar fallback rules
- Display name ưu tiên full name.
- Nếu thiếu full name, fallback email local-part.
- Nếu vẫn thiếu dữ liệu:
  - HR side: hiển thị "HR".
  - Candidate side: hiển thị label phân biệt theo user, không dùng text chung cho nhiều thread.
- Avatar fallback dùng 1 ký tự đầu của display name (hoặc "HR" khi profile HR trống).

---

## 📤 Output của Phase 4

1. `/messages` route hoạt động cho candidate
2. Chat 2 chiều HR ↔ Candidate hoạt động realtime
3. Entry point từ trang applications
4. Badge unread trên navigation
5. Mobile responsive
