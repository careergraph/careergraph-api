# PHASE 7 - Realtime Lifecycle Hardening (Enterprise)

## Muc tieu
- Dam bao luong realtime khong phu thuoc viec user dang mo trang tin nhan.
- Dam bao online presence khong bi sai sau khi bam nut lam moi danh sach hoi thoai.
- Dam bao socket reconnect ben vung cho production session dai.
- Khong pha vo logic cu (chat room, notify, unread counts, mark read).

## Van de goc da xac nhan
1. Chat socket lifecycle dang gan voi ChatWindow.
- Khi roi trang messages, component unmount -> khong con consume socket chat -> ngat kenh.
- He qua: khong nhan event chat/presence theo thread trong thoi gian user o trang khac.

2. Thread online status bi reset khi refresh.
- Khi goi refresh list, thread summary duoc replace bang payload REST.
- Gia tri isOnline trong payload co the stale/false va de online state vua cap nhat boi socket bi ghi de.

3. Reconnect policy qua ngan.
- reconnectionAttempts = 5 cho chat/notify khien session co the ngung reconnect sau mot so lan loi mang.

## Giai phap production-ready
### 1) Tach realtime khoi page-level va dua len app shell
- Them MessagingRealtimeBootstrap o ca HR va Candidate app shell.
- Bootstrap giu consume useChatSocket trong suot authenticated session.
- Bootstrap tu dong join cac thread da load de nhan event online/new-message/messages-read du user dang o trang nao.
- Gioi han join nen theo MAX_BACKGROUND_THREADS = 100 de tranh mo room vo han.

### 2) Thread subscription ben vung qua reconnect
- Trong useChatSocket (HR va Candidate):
  - Them subscribedThreadIds (module-level set).
  - joinThread/leaveThread cap nhat set va emit.
  - Khi socket connect lai, auto rejoin toan bo subscribedThreadIds.
  - Khi khong con consumer, clear subscribedThreadIds.

### 3) Bao toan online state khi refresh list
- Trong messaging store (HR va Candidate):
  - replaceThreads() merge lai online state voi realtime state thay vi ghi de toan bo.
  - Uu tien du lieu onlineUsers da nhan qua socket.
  - Fallback sang gia tri isOnline truoc do neu payload REST khong phan anh kip.

### 4) Tang do ben reconnect cho chat/notify
- Chat va notify hooks (HR va Candidate):
  - reconnectionDelayMax = 10000
  - reconnectionAttempts = Infinity

## Pham vi thay doi
### HR
- src/features/messaging/components/MessagingRealtimeBootstrap.tsx
- src/features/messaging/hooks/useChatSocket.ts
- src/features/messaging/store/messagingStore.ts
- src/layout/AppLayout.tsx
- src/features/notifications/hooks/useNotifySocket.ts

### Candidate
- src/features/messaging/components/MessagingRealtimeBootstrap.jsx
- src/features/messaging/hooks/useChatSocket.js
- src/features/messaging/store/messagingStore.js
- src/App.jsx
- src/features/notifications/hooks/useNotifySocket.js

### RTC test
- scripts/verify-socket-events.js

## Non-breaking guarantee
- Khong thay doi contract event socket hien co.
- Khong thay doi endpoint REST hien co.
- Khong thay doi schema DB.
- Khong doi hanh vi room interview realtime dang on dinh.

## Operational notes
- Neu can scale, nen bo sung server-side personal channel cho chat summary (ngoai thread rooms) de khong can join nhieu thread.
- Giai phap hien tai giu tuong thich nguoc va trien khai an toan tren codebase hien tai.
