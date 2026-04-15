# PHASE 3 — ADDENDUM: Advanced Messaging UI (HR FE)
> **Đây là file bổ sung cho `03_PHASE3_HR_FE_CHAT.md`**  
> Implement TRƯỚC KHI hoặc SAU Phase 3 gốc đều được.  
> Agent đọc Phase 3 gốc và file addendum BE (`01b`) trước khi code.

---

## 🎯 4 Tính năng UI cần thêm vào HR FE

1. **Unsend** — gỡ tin nhắn trong 60 giây
2. **Delete Thread** — xóa hội thoại phía mình
3. **Archive/Unarchive** — lưu trữ hội thoại
4. **Block/Unblock** — chặn candidate

---

## 📋 BƯỚC 1 — Cập nhật API Layer

```typescript
// Thêm vào messagingApi.ts:

// Gỡ tin nhắn (có time window 60s)
unsendMessage: (messageId: string) =>
  axiosInstance.delete(`/api/v1/messages/${messageId}/unsend`),

// Xóa hội thoại phía mình
deleteThread: (threadId: string) =>
  axiosInstance.delete(`/api/v1/messages/threads/${threadId}`),

// Archive / Unarchive
archiveThread: (threadId: string) =>
  axiosInstance.post(`/api/v1/messages/threads/${threadId}/archive`),

unarchiveThread: (threadId: string) =>
  axiosInstance.post(`/api/v1/messages/threads/${threadId}/unarchive`),

// Lấy archived threads
getArchivedThreads: (page = 0, size = 20) =>
  axiosInstance.get('/api/v1/messages/threads', { params: { archived: true, page, size } }),
```

```typescript
// Tạo blockApi.ts:
export const blockApi = {
  blockUser: (userId: string, reason?: string) =>
    axiosInstance.post(`/api/v1/users/${userId}/block`, { reason }),
  
  unblockUser: (userId: string) =>
    axiosInstance.delete(`/api/v1/users/${userId}/block`),
  
  getBlockedUsers: () =>
    axiosInstance.get<BlockedUserDto[]>('/api/v1/users/blocked'),
};
```

---

## 📋 BƯỚC 2 — Types bổ sung

```typescript
// Thêm vào messaging.types.ts:

export interface BlockedUserDto {
  userId: string;
  fullName: string;
  email: string;
  avatarUrl?: string;
  blockedAt: string;
  reason?: string;
}

// Cập nhật ThreadSummary:
export interface ThreadSummary {
  // ... existing fields ...
  isArchived: boolean;    // ← MỚI
  isBlocked: boolean;     // ← MỚI
}

// Unsend countdown state per message:
export interface MessageWithMeta extends Message {
  canUnsend: boolean;       // còn trong 60s window không
  secondsUntilUnsendExpiry: number; // đếm ngược
}
```

---

## 📋 BƯỚC 3 — UX: Unsend Message

### Countdown timer per message

```typescript
// useUnsendCountdown.ts
// Hook tính toán còn bao nhiêu giây để unsend

const UNSEND_WINDOW = 60; // seconds, sync với BE

export function useUnsendCountdown(createdAt: string): {
  canUnsend: boolean;
  secondsLeft: number;
} {
  const [secondsLeft, setSecondsLeft] = useState(() => {
    const elapsed = Math.floor((Date.now() - new Date(createdAt).getTime()) / 1000);
    return Math.max(0, UNSEND_WINDOW - elapsed);
  });
  
  useEffect(() => {
    if (secondsLeft <= 0) return;
    
    const interval = setInterval(() => {
      setSecondsLeft(prev => {
        if (prev <= 1) {
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    
    return () => clearInterval(interval);
  }, []);
  
  return { canUnsend: secondsLeft > 0, secondsLeft };
}
```

### MessageBubble — thêm context menu cho tin của mình

```tsx
// Cập nhật MessageBubble.tsx

const MessageBubble: React.FC<{ message: Message; isOwn: boolean }> = ({ message, isOwn }) => {
  const [showMenu, setShowMenu] = useState(false);
  const { canUnsend, secondsLeft } = useUnsendCountdown(message.createdAt);
  
  if (message.deleted) {
    return (
      <div className={`bubble bubble-deleted ${isOwn ? 'own' : 'other'}`}>
        <RetractedIcon size={12} />
        <span>Tin nhắn đã được gỡ</span>
      </div>
    );
  }
  
  return (
    <div className={`bubble-wrapper ${isOwn ? 'own' : 'other'}`}>
      <div className={`bubble ${isOwn ? 'bubble-own' : 'bubble-other'}`}>
        {message.content}
      </div>
      
      {/* Hover actions — chỉ hiện khi hover */}
      {isOwn && (
        <div className="bubble-actions">
          <button
            className="action-btn"
            onClick={() => setShowMenu(!showMenu)}
            aria-label="Tùy chọn tin nhắn"
          >
            <MoreHorizontalIcon size={14} />
          </button>
          
          {showMenu && (
            <div className="bubble-menu">
              {canUnsend ? (
                <button onClick={() => handleUnsend(message.id)} className="menu-item danger">
                  <UndoIcon size={14} />
                  Gỡ tin nhắn
                  <span className="countdown">{secondsLeft}s</span>
                </button>
              ) : (
                <div className="menu-item disabled">
                  <UndoIcon size={14} />
                  Không thể gỡ
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
```

**CSS cho unsend countdown:**
```css
/* Countdown đổi màu khi gần hết */
.countdown {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
  transition: color 0.3s;
}

/* < 10 giây: đỏ để tạo urgency */
.countdown.urgent { color: #ef4444; }

.bubble-actions {
  opacity: 0;
  transition: opacity 0.15s;
}
.bubble-wrapper:hover .bubble-actions {
  opacity: 1;
}
```

### Handler unsend

```typescript
const handleUnsend = async (messageId: string) => {
  // Optimistic: ẩn ngay
  dispatch(markMessageDeleted({ threadId, messageId }));
  
  try {
    await messagingApi.unsendMessage(messageId);
    // Broadcast socket để đối phương cũng thấy ngay
    broadcastDeleted(threadId, messageId);
  } catch (error) {
    // Rollback nếu fail (vd: quá 60s)
    dispatch(restoreMessage({ threadId, messageId }));
    
    if (error.response?.status === 400) {
      showToast('Chỉ có thể gỡ tin nhắn trong vòng 60 giây sau khi gửi', 'error');
    }
  }
};
```

---

## 📋 BƯỚC 4 — UX: Delete Thread & Archive

### Thread context menu trong InboxSidebar

```tsx
// Cập nhật ThreadItem.tsx — thêm ... menu ở phải

const ThreadItem: React.FC<ThreadItemProps> = ({ thread, isSelected, onClick }) => {
  const [showMenu, setShowMenu] = useState(false);
  
  return (
    <div className={`thread-item ${isSelected ? 'selected' : ''}`}>
      {/* Main clickable area */}
      <div className="thread-main" onClick={onClick}>
        {/* ... existing content: avatar, name, preview ... */}
      </div>
      
      {/* ... button — hiện khi hover thread-item */}
      <div className="thread-actions">
        <button
          className="thread-menu-btn"
          onClick={(e) => { e.stopPropagation(); setShowMenu(!showMenu); }}
          aria-label="Tùy chọn hội thoại"
        >
          <MoreVerticalIcon size={16} />
        </button>
        
        {showMenu && (
          <ThreadContextMenu
            thread={thread}
            onClose={() => setShowMenu(false)}
          />
        )}
      </div>
    </div>
  );
};
```

### `ThreadContextMenu.tsx`

```tsx
const ThreadContextMenu: React.FC<{
  thread: ThreadSummary;
  onClose: () => void;
}> = ({ thread, onClose }) => {
  
  const handleArchive = async () => {
    onClose();
    if (thread.isArchived) {
      await messagingApi.unarchiveThread(thread.threadId);
      dispatch(updateThread({ threadId: thread.threadId, changes: { isArchived: false } }));
      showToast('Đã bỏ lưu trữ hội thoại');
    } else {
      await messagingApi.archiveThread(thread.threadId);
      dispatch(removeThreadFromInbox(thread.threadId)); // ẩn khỏi inbox chính
      showToast('Đã lưu trữ hội thoại');
    }
  };
  
  const handleDelete = async () => {
    onClose();
    // Confirm dialog trước khi xóa
    const confirmed = await showConfirmDialog({
      title: 'Xóa hội thoại',
      message: 'Hội thoại sẽ bị xóa khỏi danh sách của bạn. Ứng viên vẫn có thể xem tin nhắn.',
      confirmText: 'Xóa',
      cancelText: 'Hủy',
      variant: 'danger',
    });
    
    if (confirmed) {
      await messagingApi.deleteThread(thread.threadId);
      dispatch(removeThreadFromInbox(thread.threadId));
      showToast('Đã xóa hội thoại');
    }
  };
  
  const handleBlock = async () => {
    onClose();
    if (thread.isBlocked) {
      // Unblock
      const confirmed = await showConfirmDialog({
        title: 'Bỏ chặn ứng viên',
        message: `${thread.otherUser.firstName} sẽ có thể gửi tin nhắn cho bạn.`,
        confirmText: 'Bỏ chặn',
      });
      if (confirmed) {
        await blockApi.unblockUser(thread.otherUser.id);
        dispatch(updateThread({ threadId: thread.threadId, changes: { isBlocked: false } }));
        showToast('Đã bỏ chặn ứng viên');
      }
    } else {
      // Block với optional reason
      setShowBlockDialog(true);
    }
  };
  
  return (
    <div className="context-menu" role="menu">
      <button className="menu-item" onClick={handleArchive}>
        {thread.isArchived ? <UnarchiveIcon size={16} /> : <ArchiveIcon size={16} />}
        {thread.isArchived ? 'Bỏ lưu trữ' : 'Lưu trữ hội thoại'}
      </button>
      
      <button className="menu-item" onClick={handleDelete}>
        <Trash2Icon size={16} />
        Xóa hội thoại
      </button>
      
      <div className="menu-divider" />
      
      <button className="menu-item danger" onClick={handleBlock}>
        {thread.isBlocked ? <UnblockIcon size={16} /> : <BlockIcon size={16} />}
        {thread.isBlocked ? 'Bỏ chặn ứng viên' : 'Chặn ứng viên'}
      </button>
    </div>
  );
};
```

---

## 📋 BƯỚC 5 — UX: Block với Dialog

```tsx
// BlockDialog.tsx — HR nhập lý do block (optional)

const BlockDialog: React.FC<{
  candidate: UserSummary;
  onConfirm: (reason?: string) => void;
  onCancel: () => void;
}> = ({ candidate, onConfirm, onCancel }) => {
  const [reason, setReason] = useState('');
  
  return (
    <Dialog title={`Chặn ${candidate.firstName}?`}>
      <p className="dialog-description">
        Sau khi chặn, <strong>{candidate.firstName}</strong> sẽ không thể gửi tin 
        nhắn cho bạn. Bạn có thể bỏ chặn bất cứ lúc nào.
      </p>
      
      <label className="dialog-label">
        Lý do (không bắt buộc)
        <select
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          className="dialog-select"
        >
          <option value="">Chọn lý do...</option>
          <option value="SPAM">Gửi tin nhắn spam</option>
          <option value="INAPPROPRIATE">Nội dung không phù hợp</option>
          <option value="HARASSMENT">Quấy rối</option>
          <option value="OTHER">Lý do khác</option>
        </select>
      </label>
      
      <div className="dialog-actions">
        <button className="btn-secondary" onClick={onCancel}>Hủy</button>
        <button className="btn-danger" onClick={() => onConfirm(reason || undefined)}>
          Chặn ứng viên
        </button>
      </div>
    </Dialog>
  );
};
```

---

## 📋 BƯỚC 6 — Tab Archive trong Inbox

```tsx
// Cập nhật MessagesPage.tsx — thêm tabs

type InboxTab = 'all' | 'archived';

const MessagesPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<InboxTab>('all');
  const [selectedThreadId, setSelectedThreadId] = useState<string | null>(null);
  
  return (
    <div className="messages-page">
      <div className="inbox-sidebar">
        {/* Tab switcher */}
        <div className="inbox-tabs">
          <button
            className={`tab ${activeTab === 'all' ? 'active' : ''}`}
            onClick={() => setActiveTab('all')}
          >
            Hộp thư
            {totalUnread > 0 && <span className="tab-badge">{totalUnread}</span>}
          </button>
          <button
            className={`tab ${activeTab === 'archived' ? 'active' : ''}`}
            onClick={() => setActiveTab('archived')}
          >
            <ArchiveIcon size={14} />
            Đã lưu trữ
          </button>
        </div>
        
        {/* Thread list — truyền archived prop */}
        <InboxSidebar
          archived={activeTab === 'archived'}
          selectedThreadId={selectedThreadId}
          onSelectThread={setSelectedThreadId}
        />
      </div>
      
      {/* Chat panel — giữ nguyên */}
      {selectedThreadId ? (
        <ChatWindow threadId={selectedThreadId} />
      ) : (
        <EmptyChat />
      )}
    </div>
  );
};
```

---

## 📋 BƯỚC 7 — Chat Header khi thread bị block

```tsx
// Trong ChatWindow.tsx — thêm blocked banner

{thread.isBlocked && (
  <div className="blocked-banner">
    <ShieldIcon size={16} />
    <span>Bạn đã chặn ứng viên này. Họ không thể gửi tin nhắn cho bạn.</span>
    <button
      className="unblock-link"
      onClick={handleUnblock}
    >
      Bỏ chặn
    </button>
  </div>
)}

{/* MessageInput — disable khi bị block */}
<MessageInput
  disabled={thread.isBlocked}
  placeholder={thread.isBlocked ? 'Bỏ chặn để tiếp tục nhắn tin' : 'Nhập tin nhắn...'}
/>
```

---

## 📋 BƯỚC 8 — Trang quản lý Block (Settings)

```tsx
// pages/BlockedUsersPage.tsx — /settings/blocked-users (optional nhưng professional)

const BlockedUsersPage: React.FC = () => {
  const [blockedUsers, setBlockedUsers] = useState<BlockedUserDto[]>([]);
  
  useEffect(() => {
    blockApi.getBlockedUsers().then(({ data }) => setBlockedUsers(data));
  }, []);
  
  return (
    <SettingsLayout title="Danh sách đã chặn">
      {blockedUsers.length === 0 ? (
        <EmptyState 
          icon={<ShieldCheckIcon />}
          message="Bạn chưa chặn ứng viên nào"
        />
      ) : (
        <div className="blocked-list">
          {blockedUsers.map(user => (
            <div key={user.userId} className="blocked-item">
              <Avatar src={user.avatarUrl} name={user.fullName} size={40} />
              <div className="blocked-info">
                <span className="blocked-name">{user.fullName}</span>
                <span className="blocked-email">{user.email}</span>
                {user.reason && (
                  <span className="blocked-reason">Lý do: {user.reason}</span>
                )}
                <span className="blocked-since">
                  Chặn từ {formatDate(user.blockedAt)}
                </span>
              </div>
              <button
                className="btn-outline-danger btn-sm"
                onClick={() => handleUnblock(user.userId)}
              >
                Bỏ chặn
              </button>
            </div>
          ))}
        </div>
      )}
    </SettingsLayout>
  );
};
```

---

## 📋 BƯỚC 9 — Socket Events bổ sung

Sau khi HR block/unblock, cần notify socket để candidate nhận thông báo realtime:

```typescript
// Trong useChatSocket.ts — thêm listener:
socket.on('blocked-by-hr', ({ threadId }: { threadId: string }) => {
  // Candidate FE nhận event này khi bị HR block
  dispatch(setThreadBlocked({ threadId }));
  showToast('Bạn đã bị chặn bởi nhà tuyển dụng này', 'warning');
});

socket.on('unblocked-by-hr', ({ threadId }: { threadId: string }) => {
  dispatch(setThreadUnblocked({ threadId }));
});
```

Phía Node.js socket server — thêm helper để push event qua thread:

```javascript
// Trong chat.js — thêm 2 hàm export:
function notifyBlocked(threadId, candidateUserId) {
  chat.to(`thread:${threadId}`).emit('blocked-by-hr', { threadId });
}

function notifyUnblocked(threadId, candidateUserId) {
  chat.to(`thread:${threadId}`).emit('unblocked-by-hr', { threadId });
}
```

---

## ✅ QA Checklist — Phase 3 Addendum

### Unsend
- [ ] Tin vừa gửi → hover → hiện nút "Gỡ tin nhắn" với countdown
- [ ] Countdown đếm ngược đúng, chuyển đỏ khi < 10s
- [ ] Sau 60s → nút biến thành "Không thể gỡ" (disabled)
- [ ] Click gỡ → optimistic ẩn ngay, đối phương thấy "Tin nhắn đã được gỡ"
- [ ] Gỡ tin của người khác → không hiện nút
- [ ] Nếu backend trả 400 (quá giờ) → toast error + tin hiện lại

### Delete Thread
- [ ] ... menu → "Xóa hội thoại" → confirm dialog hiện đúng wording
- [ ] Confirm → thread biến khỏi inbox HR
- [ ] Candidate mở app → vẫn thấy thread bình thường
- [ ] HR vào `/messages` → không thấy thread đã xóa
- [ ] Nếu candidate gửi tin sau khi HR xóa → thread xuất hiện lại trong inbox HR

### Archive
- [ ] ... menu → "Lưu trữ" → thread biến khỏi inbox chính
- [ ] Tab "Đã lưu trữ" hiện thread vừa archive
- [ ] Vào tab Archive → click thread → chat bình thường
- [ ] Trong tab Archive → ... menu → "Bỏ lưu trữ" → về inbox chính
- [ ] Unread badge trong inbox chính không đếm archived threads

### Block
- [ ] ... menu → "Chặn ứng viên" → dialog hiện với dropdown lý do
- [ ] Confirm block → banner "đã chặn" hiện trong ChatWindow
- [ ] Message input disabled khi bị block
- [ ] HR thử gửi tin bằng cách hack (API call trực tiếp) → 403
- [ ] Candidate FE nhận socket event "blocked-by-hr"
- [ ] ... menu sau khi block → hiện "Bỏ chặn ứng viên"
- [ ] Bỏ chặn → banner biến mất, input enable lại
- [ ] Trang /settings/blocked-users hiện danh sách đúng
