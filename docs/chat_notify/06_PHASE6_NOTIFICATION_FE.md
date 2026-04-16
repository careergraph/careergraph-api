# PHASE 6 — Notification Frontend + Final Integration Testing
> **Service:** FE HR (React TS) + FE Candidate (React JS)  
> **Prerequisites:** Phase 1–5 hoàn thành và đã test.  
> **Đây là phase cuối — output phải ở chuẩn production.**

---

## 🎯 Mục tiêu Phase này

1. Notification Bell + Dropdown cho cả 2 FE
2. Click notification → navigate đúng trang
3. Realtime badge update
4. Full integration test end-to-end
5. Performance & polish

---

## 📋 BƯỚC 1 — Notify Socket Hook (dùng cho cả 2 FE)

### TypeScript version (HR FE)

```typescript
// useNotifySocket.ts
import { useEffect, useRef, useCallback } from 'react';
import { io, Socket } from 'socket.io-client';

export interface NotificationPayload {
  id: string;
  type: string;
  title: string;
  body: string;
  data: Record<string, unknown>;
  isRead: boolean;
  createdAt: string;
}

export interface UnreadCounts {
  messages: number;
  notifications: number;
}

interface UseNotifySocketOptions {
  token: string | null;
  onNotification: (notification: NotificationPayload) => void;
  onUnreadCounts: (counts: UnreadCounts) => void;
}

const SOCKET_URL = process.env.REACT_APP_SOCKET_URL || 'http://localhost:4000';

export function useNotifySocket({ token, onNotification, onUnreadCounts }: UseNotifySocketOptions) {
  const socketRef = useRef<Socket | null>(null);
  
  useEffect(() => {
    if (!token) return;
    
    const socket = io(`${SOCKET_URL}/notify`, {
      auth: { token },
      reconnection: true,
      reconnectionDelay: 2000,
    });
    
    socketRef.current = socket;
    
    socket.on('connect', () => {
      console.log('[notify socket] connected');
    });
    
    socket.on('notification', (notification: NotificationPayload) => {
      onNotification(notification);
      // Browser native notification (nếu user đã grant permission)
      if ('Notification' in window && Notification.permission === 'granted') {
        new Notification(notification.title, {
          body: notification.body,
          icon: '/favicon.ico',
          tag: notification.id, // prevent duplicate
        });
      }
    });
    
    socket.on('unread-counts', (counts: UnreadCounts) => {
      onUnreadCounts(counts);
    });
    
    return () => {
      socket.disconnect();
      socketRef.current = null;
    };
  }, [token]); // eslint-disable-line
}
```

### JavaScript version (Candidate FE)

```javascript
// useNotifySocket.js — tương tự nhưng không có TypeScript types
import { useEffect, useRef } from 'react';
import { io } from 'socket.io-client';

const SOCKET_URL = process.env.REACT_APP_SOCKET_URL || 'http://localhost:4000';

export function useNotifySocket({ token, onNotification, onUnreadCounts }) {
  const socketRef = useRef(null);
  
  useEffect(() => {
    if (!token) return;
    
    const socket = io(`${SOCKET_URL}/notify`, {
      auth: { token },
      reconnection: true,
    });
    
    socketRef.current = socket;
    socket.on('notification', onNotification);
    socket.on('unread-counts', onUnreadCounts);
    
    return () => socket.disconnect();
  }, [token]); // eslint-disable-line
}
```

---

## 📋 BƯỚC 2 — Notification Store/Context

### HR FE — Redux Slice

```typescript
// notificationSlice.ts

interface NotificationState {
  notifications: NotificationPayload[];
  unreadCount: number;
  messageUnreadCount: number;
  loading: boolean;
  hasMore: boolean;
  page: number;
}

const notificationSlice = createSlice({
  name: 'notifications',
  initialState: { ... } as NotificationState,
  reducers: {
    addNotification: (state, action) => {
      // Thêm vào đầu list (mới nhất lên đầu)
      state.notifications.unshift(action.payload);
      if (!action.payload.isRead) state.unreadCount += 1;
    },
    setUnreadCounts: (state, action) => {
      state.messageUnreadCount = action.payload.messages;
      state.unreadCount = action.payload.notifications;
    },
    markOneRead: (state, action) => {
      const n = state.notifications.find(n => n.id === action.payload);
      if (n && !n.isRead) {
        n.isRead = true;
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      }
    },
    markAllRead: (state) => {
      state.notifications.forEach(n => { n.isRead = true; });
      state.unreadCount = 0;
    },
    setNotifications: (state, action) => {
      state.notifications = action.payload.notifications;
      state.unreadCount = action.payload.totalUnread;
      state.hasMore = action.payload.hasMore;
    },
  },
});
```

### Candidate FE — Context

```javascript
// NotificationContext.js
const NotificationContext = createContext(null);

export const NotificationProvider = ({ children }) => {
  const { token } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [messageUnreadCount, setMessageUnreadCount] = useState(0);
  
  // Fetch initial count khi load app
  useEffect(() => {
    if (!token) return;
    Promise.all([
      notificationApi.getUnreadCount(),
      messagingApi.getUnreadCount(),
    ]).then(([notifRes, msgRes]) => {
      setUnreadCount(notifRes.data.count);
      setMessageUnreadCount(msgRes.data.count);
    });
  }, [token]);
  
  const handleNewNotification = useCallback((notification) => {
    setNotifications(prev => [notification, ...prev]);
    setUnreadCount(prev => prev + 1);
  }, []);
  
  const handleUnreadCounts = useCallback(({ messages, notifications: notifs }) => {
    setMessageUnreadCount(messages);
    setUnreadCount(notifs);
  }, []);
  
  useNotifySocket({ token, onNotification: handleNewNotification, onUnreadCounts: handleUnreadCounts });
  
  return (
    <NotificationContext.Provider value={{
      notifications, setNotifications,
      unreadCount, setUnreadCount,
      messageUnreadCount, setMessageUnreadCount,
    }}>
      {children}
    </NotificationContext.Provider>
  );
};
```

---

## 📋 BƯỚC 3 — NotificationBell Component (dùng cho cả 2 FE)

```tsx
// NotificationBell.tsx (HR) / NotificationBell.jsx (Candidate)

const NotificationBell = () => {
  const [isOpen, setIsOpen] = useState(false);
  const { unreadCount } = useNotifications(); // hook từ store/context
  const bellRef = useRef(null);
  
  // Close khi click ngoài
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (bellRef.current && !bellRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);
  
  return (
    <div className="notification-bell" ref={bellRef}>
      <button 
        className="bell-button"
        onClick={() => setIsOpen(!isOpen)}
        aria-label={`Thông báo${unreadCount > 0 ? ` (${unreadCount} chưa đọc)` : ''}`}
      >
        <BellIcon />
        {unreadCount > 0 && (
          <span className="bell-badge" aria-hidden="true">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>
      
      {isOpen && (
        <NotificationDropdown onClose={() => setIsOpen(false)} />
      )}
    </div>
  );
};
```

---

## 📋 BƯỚC 4 — NotificationDropdown Component

```tsx
// NotificationDropdown.tsx / .jsx

const NotificationDropdown = ({ onClose }) => {
  const { notifications, unreadCount, markAllRead, loadMore, hasMore } = useNotifications();
  const navigate = useNavigate();
  
  // Load notifications khi mở
  useEffect(() => {
    notificationApi.getNotifications(0, 20).then(({ data }) => {
      setNotifications(data);
    });
  }, []);
  
  const handleClickNotification = async (notification) => {
    // 1. Mark as read
    if (!notification.isRead) {
      await notificationApi.markAsRead(notification.id);
      markOneRead(notification.id);
    }
    
    // 2. Navigate đến trang liên quan
    const navigateTo = notification.data?.navigateTo;
    if (navigateTo) {
      navigate(navigateTo);
    }
    
    // 3. Đóng dropdown
    onClose();
  };
  
  return (
    <div className="notification-dropdown">
      {/* Header */}
      <div className="dropdown-header">
        <h3>Thông báo</h3>
        {unreadCount > 0 && (
          <button onClick={markAllRead} className="mark-all-btn">
            Đánh dấu tất cả đã đọc
          </button>
        )}
      </div>
      
      {/* List */}
      <div className="notification-list">
        {notifications.length === 0 ? (
          <EmptyNotifications />
        ) : (
          notifications.map(n => (
            <NotificationItem
              key={n.id}
              notification={n}
              onClick={() => handleClickNotification(n)}
            />
          ))
        )}
        
        {hasMore && (
          <button onClick={loadMore} className="load-more-btn">
            Xem thêm
          </button>
        )}
      </div>
    </div>
  );
};
```

---

## 📋 BƯỚC 5 — NotificationItem Component

```tsx
const NotificationItem = ({ notification, onClick }) => {
  const icon = getNotificationIcon(notification.type);
  const timeAgo = formatDistanceToNow(new Date(notification.createdAt), { 
    addSuffix: true, 
    locale: vi  // date-fns tiếng Việt
  });
  
  return (
    <div 
      className={`notification-item ${!notification.isRead ? 'unread' : ''}`}
      onClick={onClick}
      role="button"
      tabIndex={0}
    >
      {/* Icon theo type */}
      <div className="notif-icon">
        {icon}
      </div>
      
      {/* Content */}
      <div className="notif-content">
        <p className="notif-title">{notification.title}</p>
        <p className="notif-body">{notification.body}</p>
        <span className="notif-time">{timeAgo}</span>
      </div>
      
      {/* Unread indicator */}
      {!notification.isRead && <div className="unread-dot" />}
    </div>
  );
};

// Icons theo type
function getNotificationIcon(type) {
  switch (type) {
    case 'NEW_MESSAGE': return <MessageIcon className="icon-blue" />;
    case 'NEW_APPLICATION': return <UserPlusIcon className="icon-green" />;
    case 'APPLICATION_STATUS_CHANGED': return <DocumentIcon className="icon-orange" />;
    case 'APPLICATION_SHORTLISTED': return <StarIcon className="icon-yellow" />;
    case 'APPLICATION_REJECTED': return <XCircleIcon className="icon-red" />;
    case 'APPLICATION_INTERVIEW_SCHEDULED': return <CalendarIcon className="icon-purple" />;
    default: return <BellIcon className="icon-gray" />;
  }
}
```

---

## 📋 BƯỚC 6 — CSS Requirements cho Notification Dropdown

```css
.notification-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 380px;
  max-height: 480px;
  background: var(--surface-color);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.12);
  border: 1px solid var(--border-color);
  overflow: hidden;
  z-index: 1000;
  
  /* Animation */
  animation: dropdown-in 0.15s ease-out;
}

@keyframes dropdown-in {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
  /* Custom scrollbar */
  scrollbar-width: thin;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--border-light);
}

.notification-item:hover { background: var(--hover-color); }
.notification-item.unread { background: var(--unread-bg-color); }

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-color);
  flex-shrink: 0;
  margin-top: 6px;
}

/* Badge */
.bell-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  background: #ef4444;
  color: white;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  /* Pulse animation khi có notification mới */
  animation: badge-pulse 0.3s ease-out;
}

@keyframes badge-pulse {
  0% { transform: scale(1.4); }
  100% { transform: scale(1); }
}

/* Mobile: full-screen dropdown */
@media (max-width: 480px) {
  .notification-dropdown {
    position: fixed;
    top: 60px; /* header height */
    right: 0;
    left: 0;
    width: 100%;
    border-radius: 0;
    max-height: calc(100vh - 60px);
  }
}
```

---

## 📋 BƯỚC 7 — Browser Push Notification Permission

```typescript
// Xin permission khi user login thành công
// Đặt trong App.tsx hoặc sau khi auth thành công

async function requestNotificationPermission() {
  if (!('Notification' in window)) return;
  
  if (Notification.permission === 'default') {
    // Chỉ xin sau khi user đã interact với app (tránh bị block)
    const permission = await Notification.requestPermission();
    console.log('[browser notif] permission:', permission);
  }
}

// Gọi sau login:
// requestNotificationPermission();
```

---

## 📋 BƯỚC 8 — Navigate Mapping

```typescript
// navigateTo từ notification.data → React Router path

// Cho HR FE:
const HR_NAVIGATE_MAP = {
  NEW_APPLICATION: (data) => `/jobs/${data.jobId}/applications`,
  NEW_MESSAGE: (data) => `/messages?thread=${data.threadId}`,
  // HR không nhận APPLICATION_STATUS_CHANGED
};

// Cho Candidate FE:
const CANDIDATE_NAVIGATE_MAP = {
  NEW_MESSAGE: (data) => `/messages?thread=${data.threadId}`,
  APPLICATION_STATUS_CHANGED: (data) => `/applications/${data.applicationId}`,
  APPLICATION_SHORTLISTED: (data) => `/applications/${data.applicationId}`,
  APPLICATION_REJECTED: (data) => `/applications/${data.applicationId}`,
  APPLICATION_INTERVIEW_SCHEDULED: (data) => `/applications/${data.applicationId}`,
};

function getNavigatePath(notification, isHR) {
  const map = isHR ? HR_NAVIGATE_MAP : CANDIDATE_NAVIGATE_MAP;
  const handler = map[notification.type];
  return handler ? handler(notification.data) : null;
}
```

---

## 📋 BƯỚC 9 — FINAL INTEGRATION TESTING

> Agent đóng vai **Senior QA / Tester khắt khe**, mở 2 browser windows (HR + Candidate) và test từng scenario:

### Scenario 1: HR gửi tin nhắn cho Candidate
```
1. HR đăng nhập, mở kanban → click candidate → tab Nhắn tin
2. HR nhập và gửi "Xin chào, chúng tôi muốn mời bạn phỏng vấn"
[Expected]
- Tin hiện ngay ở HR chat (optimistic)
- Candidate nhận tin realtime trong /messages
- Notification bell của candidate hiện badge +1
- Browser notification hiện (nếu granted)
- Candidate click notification → mở đúng thread
```

### Scenario 2: Candidate gửi tin nhắn cho HR
```
1. Candidate đăng nhập, vào /messages, chọn thread
2. Gõ "Cảm ơn! Tôi có thể hỏi thêm về..." (typing indicator test)
[Expected]
- HR thấy "Đang nhập..." khi candidate gõ
- Gõ xong → "Đang nhập..." biến mất
- HR nhận tin realtime
- HR notification bell +1
- HR click notification → mở /messages?thread=xxx
```

### Scenario 3: HR đổi trạng thái ứng tuyển
```
1. HR vào kanban, kéo card candidate từ "Mới" → "Phỏng vấn"
[Expected]
- Candidate nhận notification: "🎊 Bạn được mời phỏng vấn!"
- Bell badge +1 ngay lập tức
- Click notification → /applications/{id}
- Notification hiện đúng trong dropdown
```

### Scenario 4: Candidate nộp đơn mới
```
1. Candidate apply job X
[Expected]  
- HR của job X nhận notification: "Ứng viên [Tên] vừa ứng tuyển [Job]"
- Click notification → /jobs/{jobId}/applications
- Không có notification nào gửi đến HR của job Y
```

### Scenario 5: Read receipts & Unread badges
```
1. HR gửi 3 tin nhắn cho Candidate (candidate chưa mở)
[Expected]
- Candidate /messages: thread hiện badge "3"
- Candidate notification bell hiện badge (NEW_MESSAGE count)
- Candidate mở thread → badge về 0
- HR thấy "Đã xem" sau khi candidate đọc
- Socket emit messages-read → HR chat cập nhật
```

### Scenario 6: Reconnection
```
1. Đang chat, tắt WiFi 5 giây, bật lại
[Expected]
- Socket tự reconnect (không cần reload)
- Tin nhắn gửi trong lúc offline: hiện lỗi + retry button
- Sau khi reconnect: fetch lại messages từ API để không miss tin
```

### Scenario 7: Multiple tabs
```
1. HR mở /messages ở tab 1 và tab 2
2. Candidate gửi tin nhắn
[Expected]
- Cả 2 tab của HR nhận tin
- Unread badge trên cả 2 tab đồng bộ
```

### Scenario 8: Edge cases
```
- Gửi message 500 ký tự → hiển thị đúng, không vỡ bubble
- Gửi emoji 🎉🎊🥳 → hiển thị đúng
- Gửi chuỗi URL → hiển thị đúng (không parse thành link nếu chưa implement)
- Thu hồi tin nhắn → cả 2 phía thấy "Tin nhắn đã được thu hồi"
- Notification typing indicator → stop sau đúng 3 giây nếu không gõ tiếp
- 99+ unread badge → hiện "99+"
- 0 unread → badge ẩn hoàn toàn
```

---

## 📋 BƯỚC 10 — Performance Checklist

```
Agent phải verify:
[ ] Không có memory leak (useEffect cleanup đầy đủ)
[ ] Socket chỉ connect 1 lần (không reconnect liên tục)
[ ] Không fetch API thừa (memoization đúng)
[ ] Scroll performance: 100+ messages không lag
[ ] Bundle size: không import toàn bộ lodash/moment
[ ] Images: avatar lazy loading
[ ] API calls: không gọi trùng (race condition handling)
[ ] Pagination: không load lại từ đầu khi nhận message mới
```

---

## 📋 BƯỚC 11 — Accessibility

```
[ ] Notification bell: aria-label đúng
[ ] Notification items: role="button", tabIndex, onKeyDown (Enter/Space)
[ ] Screen reader: unread count announced
[ ] Focus management: dropdown close → focus trở về bell
[ ] Color không phải cách duy nhất để phân biệt read/unread
[ ] Chat input: label hoặc aria-label
```

---

## ✅ FINAL QA CHECKLIST — Phase 6 & Toàn dự án

### Messaging
- [ ] 2 chiều HR ↔ Candidate realtime
- [ ] Typing indicator 2 chiều
- [ ] Read receipts 2 chiều  
- [ ] Unread badges chính xác
- [ ] Infinite scroll lịch sử chat
- [ ] Thu hồi tin nhắn
- [ ] Optimistic UI với error handling
- [ ] Mobile responsive (cả 2 FE)
- [ ] HR inbox tại `/messages`
- [ ] Candidate inbox tại `/messages`
- [ ] Tab "Nhắn tin" trong kanban HR vẫn hoạt động
- [ ] Nút "Nhắn tin với HR" từ trang applications candidate

### Notifications
- [ ] Bell badge tổng (messages + notifications)
- [ ] Dropdown list đẹp, đúng icon theo type
- [ ] Click → navigate đúng trang
- [ ] Mark 1 đã đọc
- [ ] Mark all đã đọc
- [ ] Realtime push qua socket
- [ ] Browser native notification (nếu granted)
- [ ] Rate limiting: không spam notifications
- [ ] Candidate nhận đúng khi HR đổi status
- [ ] HR nhận đúng khi candidate apply

### Infrastructure
- [ ] Socket reconnect tự động
- [ ] Internal API key bảo vệ /internal endpoint
- [ ] No memory leaks
- [ ] Error boundaries ở FE
- [ ] Logging đầy đủ ở BE và Node.js

---

## 📤 Output của Phase 6 & Toàn dự án

1. `NotificationBell` + `NotificationDropdown` hoạt động ở HR FE
2. `NotificationBell` + `NotificationDropdown` hoạt động ở Candidate FE  
3. `useNotifySocket` hook ở cả 2 FE
4. All 8 integration test scenarios PASS
5. Performance verified
6. **Demo video** (optional nhưng professional): record màn hình demo tất cả tính năng chính
