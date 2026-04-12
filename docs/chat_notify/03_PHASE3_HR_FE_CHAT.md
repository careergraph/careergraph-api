# PHASE 3 — HR Frontend: Inbox Management & Chat UI
> **Service:** FE HR (React TypeScript)  
> **Prerequisites:** Phase 1 & 2 hoàn thành. Đọc toàn bộ source FE HR để hiểu: routing library, state management (Redux/Zustand/Context?), UI component library, API client pattern (axios/fetch?), auth context, styling approach.

---

## 🎯 Mục tiêu Phase này

HR cần 2 điểm truy cập chat:
1. **Trang Inbox** (`/messages`) — quản lý tất cả cuộc hội thoại
2. **Chat trong kanban** — click candidate detail tab "Nhắn tin" (đã có UI, cần kết nối thực)

Tính năng:
- Inbox danh sách threads, sort theo tin mới nhất
- Unread badge
- Chat window full-featured
- Typing indicator, read receipts
- Search/filter threads (optional nhưng professional)
- Tạo thread mới từ candidate detail

---

## 📋 BƯỚC 1 — Đọc hiểu dự án (BẮT BUỘC)

```
Agent đọc và ghi chú:
1. Router: React Router v5 hay v6? (khác nhau ở useNavigate vs useHistory)
2. State management: Redux Toolkit? Zustand? Context API?
3. API client: có axiosInstance configured với baseURL + interceptors không?
4. Auth: token lấy từ đâu? (localStorage? Cookie? Context?)
5. UI library: Ant Design? Material UI? Shadcn/ui? Custom?
6. Existing chat tab UI: nằm ở file nào? Component nào?
7. Naming convention: camelCase files? PascalCase components?
8. i18n: có internationalization không? (tiếng Việt?)
9. Existing notification bell nếu có
10. WebSocket hiện tại (video call): cách connect, cách dùng trong component
```

---

## 📋 BƯỚC 2 — Architecture & Folder Structure

```
src/
├── features/
│   ├── messaging/                    ← NEW
│   │   ├── api/
│   │   │   └── messagingApi.ts       ← API calls
│   │   ├── hooks/
│   │   │   ├── useThreads.ts         ← fetch + cache threads
│   │   │   ├── useChatSocket.ts      ← socket connection
│   │   │   └── useMessages.ts        ← fetch + infinite scroll
│   │   ├── store/
│   │   │   └── messagingSlice.ts     ← Redux slice (hoặc Zustand store)
│   │   ├── components/
│   │   │   ├── InboxSidebar.tsx      ← danh sách threads
│   │   │   ├── ThreadItem.tsx        ← 1 thread trong list
│   │   │   ├── ChatWindow.tsx        ← chat view chính
│   │   │   ├── MessageBubble.tsx     ← 1 tin nhắn
│   │   │   ├── MessageInput.tsx      ← input + send button
│   │   │   ├── TypingIndicator.tsx   ← "Đang nhập..."
│   │   │   ├── ReadReceipt.tsx       ← "Đã xem"
│   │   │   └── EmptyChat.tsx         ← empty state
│   │   ├── pages/
│   │   │   └── MessagesPage.tsx      ← /messages route
│   │   └── types/
│   │       └── messaging.types.ts
│   └── notifications/                ← NEW
│       ├── hooks/
│       │   └── useNotifications.ts
│       ├── components/
│       │   ├── NotificationBell.tsx
│       │   └── NotificationDropdown.tsx
│       └── store/
│           └── notificationSlice.ts
```

---

## 📋 BƯỚC 3 — Types

```typescript
// messaging.types.ts

export type MessageContentType = 'TEXT' | 'IMAGE' | 'FILE';

export interface UserSummary {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  avatarUrl?: string;
}

export interface ThreadSummary {
  threadId: string;
  otherUser: UserSummary;
  application?: {
    id: string;
    jobTitle: string;
    status: string;
  };
  lastMessagePreview: string;
  lastMessageAt: string | null;
  unreadCount: number;
  isOnline: boolean;
}

export interface Message {
  id: string;
  threadId: string;
  sender: UserSummary;
  content: string;
  contentType: MessageContentType;
  fileUrl?: string;
  fileName?: string;
  fileSize?: number;
  deleted: boolean;
  createdAt: string;
  isRead: boolean;
  readAt?: string;
}

export interface TypingStatus {
  threadId: string;
  userId: string;
  displayName: string;
}

export interface MessagingState {
  threads: ThreadSummary[];
  threadsLoading: boolean;
  selectedThreadId: string | null;
  messages: Record<string, Message[]>; // threadId → messages
  messagesLoading: boolean;
  hasMoreMessages: Record<string, boolean>;
  typingUsers: Record<string, string[]>; // threadId → userIds
  onlineUsers: Record<string, boolean>; // userId → isOnline
  totalUnread: number;
}
```

---

## 📋 BƯỚC 4 — API Layer

```typescript
// messagingApi.ts
import { axiosInstance } from '@/api/axiosInstance'; // hoặc tên tương đương

export const messagingApi = {
  
  getThreads: (page = 0, size = 20) =>
    axiosInstance.get<PageResponse<ThreadSummary>>('/api/v1/messages/threads', {
      params: { page, size, sort: 'lastMessageAt,desc' }
    }),
  
  getOrCreateThread: (candidateId: string, applicationId?: string) =>
    axiosInstance.post<ThreadSummary>('/api/v1/messages/threads', {
      candidateId,
      applicationId,
    }),
  
  getMessages: (threadId: string, page = 0, size = 30) =>
    axiosInstance.get<PageResponse<Message>>(`/api/v1/messages/threads/${threadId}/messages`, {
      params: { page, size, sort: 'createdAt,asc' }
    }),
  
  sendMessage: (threadId: string, content: string, contentType: MessageContentType = 'TEXT') =>
    axiosInstance.post<Message>(`/api/v1/messages/threads/${threadId}/messages`, {
      content,
      contentType,
    }),
  
  markThreadAsRead: (threadId: string) =>
    axiosInstance.post(`/api/v1/messages/threads/${threadId}/read`),
  
  deleteMessage: (messageId: string) =>
    axiosInstance.delete(`/api/v1/messages/${messageId}`),
  
  getUnreadCount: () =>
    axiosInstance.get<{ count: number }>('/api/v1/messages/unread-count'),
};
```

---

## 📋 BƯỚC 5 — Socket Hook

```typescript
// useChatSocket.ts
import { useEffect, useRef, useCallback } from 'react';
import { io, Socket } from 'socket.io-client';
import { useAppDispatch } from '@/store/hooks'; // hoặc tương đương
import { 
  addMessage, updateTyping, setUserOnline, 
  markThreadRead, removeMessage 
} from '../store/messagingSlice';

const SOCKET_URL = process.env.REACT_APP_SOCKET_URL || 'http://localhost:4000';

export function useChatSocket(token: string | null) {
  const socketRef = useRef<Socket | null>(null);
  const dispatch = useAppDispatch();
  
  useEffect(() => {
    if (!token) return;
    
    const socket = io(`${SOCKET_URL}/chat`, {
      auth: { token },
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionAttempts: 5,
    });
    
    socketRef.current = socket;
    
    socket.on('connect', () => {
      console.log('[chat socket] connected');
    });
    
    socket.on('connect_error', (err) => {
      console.error('[chat socket] error:', err.message);
    });
    
    socket.on('new-message', ({ threadId, message }: { threadId: string; message: Message }) => {
      dispatch(addMessage({ threadId, message }));
      // Nếu đang xem thread này, auto-read
      // (component sẽ handle việc emit messages-read)
    });
    
    socket.on('typing-start', ({ threadId, userId, displayName }: TypingStatus) => {
      dispatch(updateTyping({ threadId, userId, isTyping: true }));
    });
    
    socket.on('typing-stop', ({ threadId, userId }: { threadId: string; userId: string }) => {
      dispatch(updateTyping({ threadId, userId, isTyping: false }));
    });
    
    socket.on('user-online', ({ userId }: { userId: string }) => {
      dispatch(setUserOnline({ userId, isOnline: true }));
    });
    
    socket.on('user-offline', ({ userId }: { userId: string }) => {
      dispatch(setUserOnline({ userId, isOnline: false }));
    });
    
    socket.on('messages-read', ({ threadId, userId, lastReadMessageId, readAt }) => {
      dispatch(markThreadRead({ threadId, userId, lastReadMessageId, readAt }));
    });
    
    socket.on('message-deleted', ({ threadId, messageId }) => {
      dispatch(removeMessage({ threadId, messageId }));
    });
    
    return () => {
      socket.disconnect();
      socketRef.current = null;
    };
  }, [token, dispatch]);
  
  const joinThread = useCallback((threadId: string) => {
    socketRef.current?.emit('join-thread', threadId);
  }, []);
  
  const leaveThread = useCallback((threadId: string) => {
    socketRef.current?.emit('leave-thread', threadId);
  }, []);
  
  const sendTypingStart = useCallback((threadId: string) => {
    socketRef.current?.emit('typing-start', { threadId });
  }, []);
  
  const sendTypingStop = useCallback((threadId: string) => {
    socketRef.current?.emit('typing-stop', { threadId });
  }, []);
  
  const broadcastNewMessage = useCallback((threadId: string, message: Message) => {
    socketRef.current?.emit('new-message', { threadId, message });
  }, []);
  
  const broadcastRead = useCallback((threadId: string, lastReadMessageId: string) => {
    socketRef.current?.emit('messages-read', { threadId, lastReadMessageId });
  }, []);
  
  const broadcastDeleted = useCallback((threadId: string, messageId: string) => {
    socketRef.current?.emit('message-deleted', { threadId, messageId });
  }, []);
  
  return {
    joinThread,
    leaveThread,
    sendTypingStart,
    sendTypingStop,
    broadcastNewMessage,
    broadcastRead,
    broadcastDeleted,
  };
}
```

---

## 📋 BƯỚC 6 — Key Components

### `MessagesPage.tsx` — Layout 2 cột (Inbox + Chat)

```typescript
// Layout: left sidebar (thread list) + right panel (chat window)
// Responsive: mobile thì full screen toggle giữa list và chat

export const MessagesPage: React.FC = () => {
  const [selectedThreadId, setSelectedThreadId] = useState<string | null>(null);
  
  // URL sync: /messages?thread=xxx
  // Để HR bookmark hoặc share link đến thread cụ thể
  
  return (
    <div className="messages-page">
      <InboxSidebar 
        selectedThreadId={selectedThreadId}
        onSelectThread={setSelectedThreadId}
      />
      {selectedThreadId ? (
        <ChatWindow threadId={selectedThreadId} />
      ) : (
        <EmptyChat message="Chọn một cuộc trò chuyện để bắt đầu" />
      )}
    </div>
  );
};
```

**CSS Layout yêu cầu:**
```css
.messages-page {
  display: flex;
  height: calc(100vh - [header height]); /* adjust theo layout dự án */
  overflow: hidden;
}

/* Sidebar: fixed width */
/* Chat: flex-grow */
/* Mobile: 100% width, toggle */
```

### `InboxSidebar.tsx` — Danh sách threads

Features bắt buộc:
- Search input để filter threads theo tên candidate hoặc job title
- Sort: mặc định theo `lastMessageAt` DESC
- Mỗi `ThreadItem` hiển thị: avatar, tên candidate, job title, preview tin cuối, thời gian, unread badge
- Skeleton loading khi đang fetch
- Empty state khi chưa có thread nào
- Infinite scroll (hoặc load more button)
- Highlight thread đang được chọn

### `ThreadItem.tsx`

```typescript
interface ThreadItemProps {
  thread: ThreadSummary;
  isSelected: boolean;
  onClick: () => void;
}

// UI elements:
// - Avatar (với online indicator dot)
// - Tên candidate (bold nếu có unread)
// - Job title (nhỏ hơn, màu nhạt)
// - Last message preview (truncate 1 line)
// - Thời gian relative (vd: "2 phút trước", "Hôm qua")
// - Unread count badge (chỉ hiện nếu > 0)
```

### `ChatWindow.tsx` — Main chat view

```typescript
// Khi mount:
// 1. joinThread(threadId)
// 2. fetchMessages(threadId) — load page cuối (tin mới nhất)
// 3. markAsRead()
// 4. Scroll xuống cuối

// Khi unmount:
// 1. leaveThread(threadId)

// Structure:
// - Header: avatar + tên + online status + button xem profile candidate
// - Messages area: infinite scroll ngược (load thêm khi scroll lên đầu)
// - Typing indicator area (hiển thị khi có người đang gõ)
// - Message input area
```

**Scroll behavior quan trọng:**
- Khi load lần đầu: scroll to bottom
- Khi nhận message mới: nếu user đang ở gần bottom → auto scroll; nếu user đang xem tin cũ → hiện "New message" banner
- Khi load more (scroll up): giữ nguyên scroll position, không nhảy

### `MessageBubble.tsx`

```typescript
// Phân biệt: tin của mình (right, màu khác) vs tin của đối phương (left)
// Hiển thị:
// - Avatar (chỉ hiện cho tin của đối phương, và chỉ hiện cho tin đầu tiên trong group)
// - Content (text hoặc file/image)
// - Timestamp (hover hoặc always visible tùy design)
// - Read receipt: chỉ hiện ở tin cuối cùng của mình
//   + "Đã gửi" (message lưu BE thành công)
//   + "Đã xem" với avatar nhỏ của người đọc + thời gian
// - Context menu (right-click hoặc ... button): "Thu hồi" cho tin của mình
// - Tin đã bị thu hồi: hiện text italic "Tin nhắn đã được thu hồi"
// - Group messages: tin liên tiếp của cùng 1 người, giảm khoảng cách, không lặp avatar
```

### `MessageInput.tsx`

```typescript
// Features:
// - Textarea tự động resize (min 1 dòng, max 5 dòng)
// - Enter gửi, Shift+Enter xuống dòng
// - Emit typing-start khi bắt đầu gõ, typing-stop khi clear hoặc gửi
// - Debounce typing events (không spam)
// - Disable khi đang gửi
// - Emoji picker (optional nhưng nice-to-have)
// - Character count (optional)
// - Optimistic UI: tin hiện ngay trước khi BE confirm
```

**Optimistic UI pattern:**
```typescript
const sendMessage = async (content: string) => {
  // 1. Tạo temp message với id tạm
  const tempMessage: Message = {
    id: `temp-${Date.now()}`,
    content,
    sender: currentUser,
    createdAt: new Date().toISOString(),
    // ...
  };
  
  // 2. Add vào store ngay (hiện lên UI)
  dispatch(addMessage({ threadId, message: tempMessage }));
  
  // 3. Gọi API
  try {
    const { data } = await messagingApi.sendMessage(threadId, content);
    
    // 4. Replace temp message bằng real message
    dispatch(replaceMessage({ threadId, tempId: tempMessage.id, message: data }));
    
    // 5. Broadcast qua socket
    broadcastNewMessage(threadId, data);
    
  } catch (error) {
    // 6. Mark temp message as failed (hiện retry button)
    dispatch(markMessageFailed({ threadId, tempId: tempMessage.id }));
  }
};
```

---

## 📋 BƯỚC 7 — Kết nối với Kanban hiện có

Agent phải tìm component `CandidateDetail` (hoặc tương đương) và tab "Nhắn tin":

```typescript
// Trong tab nhắn tin của CandidateDetail:
// Thay vì render chat UI inline phức tạp,
// chỉ cần gọi getOrCreateThread rồi render ChatWindow

const CandidateMessageTab: React.FC<{ candidateId: string; applicationId?: string }> = ({
  candidateId,
  applicationId,
}) => {
  const [threadId, setThreadId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    messagingApi.getOrCreateThread(candidateId, applicationId)
      .then(({ data }) => setThreadId(data.threadId))
      .finally(() => setLoading(false));
  }, [candidateId, applicationId]);
  
  if (loading) return <Skeleton />;
  if (!threadId) return <Error message="Không thể tạo cuộc trò chuyện" />;
  
  return <ChatWindow threadId={threadId} compact={true} />;
  // compact mode: ẩn header vì context đã rõ
};
```

---

## 📋 BƯỚC 8 — UI/UX Requirements (BẮT BUỘC)

### Visual Design
- Chat bubbles: rounded corners, soft shadow
- Sender bubble: màu primary của hệ thống (xanh hoặc màu brand)
- Receiver bubble: màu xám nhạt
- Avatar: rounded full, fallback initials
- Online indicator: chấm xanh lá nhỏ ở góc avatar
- Timestamps: dạng "2 phút trước" (dùng `date-fns` hoặc `dayjs`)
- Message groups: tin cùng người trong 5 phút → group lại, chỉ hiện 1 avatar

### Animations
- New message slide up (transform + opacity)
- Typing indicator: 3 chấm nhảy
- Thread item hover: subtle background change

### Responsiveness
- Desktop: sidebar cố định 320px
- Tablet: sidebar 260px
- Mobile (<768px): chỉ hiển thị 1 trong 2, có back button

---

## ✅ QA CHECKLIST — Phase 3

### Functional Tests
- [ ] `/messages` route accessible từ navigation
- [ ] Danh sách threads load đúng, có unread badge
- [ ] Click thread → chat window mở, scroll to bottom
- [ ] Gửi tin → hiện optimistic ngay, confirm sau khi BE trả về
- [ ] Gửi tin → đối phương nhận realtime
- [ ] Typing indicator: gõ → đối phương thấy "đang nhập..."
- [ ] Typing tự stop sau 3s không gõ
- [ ] Scroll lên → load thêm tin cũ (pagination)
- [ ] Scroll position giữ nguyên khi load more
- [ ] Thu hồi tin → hiện "Tin nhắn đã được thu hồi"
- [ ] Unread badge giảm khi mở thread
- [ ] Read receipt "Đã xem" hiện sau khi đối phương đọc
- [ ] Online/offline indicator cập nhật realtime
- [ ] Tab "Nhắn tin" trong kanban hoạt động, reuse cùng ChatWindow

### Edge Cases
- [ ] Gửi tin khi mất mạng → hiện error, retry button
- [ ] Socket disconnect → tự reconnect, không mất tin nhắn đã lưu
- [ ] Thread với 0 tin nhắn → empty state đẹp
- [ ] Tin nhắn rất dài (1000 ký tự) → wrap đúng, không vỡ layout
- [ ] Emoji trong tin nhắn → hiển thị đúng
- [ ] Nhiều threads → scroll sidebar mượt

### UI/UX
- [ ] Mobile layout đúng
- [ ] Dark/light mode (nếu có trong dự án)
- [ ] Skeleton loading đẹp
- [ ] Empty states có illustration/icon
- [ ] Animation mượt mà
