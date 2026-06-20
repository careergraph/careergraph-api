# De xuat production: tach thong bao he thong va thong bao tin nhan

Ngay: 2026-06-20

## 1. Ket luan ngan

Huong phu hop production nhat la:

- `Bell notification` chi chua thong bao he thong va nghiep vu.
- `Message icon / Inbox` chi chua thong bao tin nhan chua doc.
- Khong gom badge message vao badge bell.
- Khong de HR chi co bell ma khong co message badge rieng.

Noi cach khac, UI can tach ro hai kenh:

- `Notifications`: trang thai ung tuyen, lich phong van, job workflow, system events.
- `Messages`: tin nhan chat 1-1, unread thread count, inbox actions.

## 2. Van de hien tai

Qua doc code hien tai:

- Ben candidate da co `bell` va da co unread cho `message`.
- `bell` lay `unreadCount` tu notification context.
- `message icon` lay unread rieng tu `useMessagingUnread`.
- Socket unread-counts tu backend dang day ca `notifications` va `messages`.
- Dropdown bell hien van render ca notification type `NEW_MESSAGE`.
- Ben HR hien tai header moi co bell, chua co message badge/dropdown rieng.

He qua UX:

- User thay "thong bao job" va "thong bao message" tron trong cung mot he thong nhin rat giong nhau.
- Badge ben message co luc nhay sai/tam thoi sai vi dang dong bo tu 2 nguon:
  - unread message count thuc te cua messaging
  - notification event `NEW_MESSAGE` trong notification stream
- Bell dropdown co the hien 1 muc moi nhung list noi dung chua kip refetch dong bo, tao cam giac "co 1 thong bao nhung khong thay gi".
- Ben HR thieu mot diem vao message ro rang, nen khong dat ky vong production cho he thong chat thoi gian thuc.

## 3. Danh gia production

### Phuong an A: De tat ca vao bell

Uu diem:

- Don gian ve UI.
- It component hon.

Nhuoc diem:

- Khong phan tach duoc muc do uu tien.
- Nguoi dung khong biet "co nguoi nhan tin" hay "ho so vua doi trang thai".
- Khi volume lon, bell tro thanh mot "catch-all feed", kho dung.
- Rat de gay loi badge do trung lap giua notification domain va messaging domain.

Danh gia:

- Khong phai huong production tot nhat cho he thong co chat realtime.

### Phuong an B: Tach bell va message icon

Uu diem:

- Ranh mach domain.
- Badge de hieu, de test, de monitoring.
- Dong bo voi hanh vi nguoi dung trong cac san pham production pho bien:
  - Bell = system/activity
  - Message = chat/conversation
- Giam nguy co double count va state mismatch.
- De scale sau nay: them inbox preview, typing/presence, mute thread, SLA response.

Nhuoc diem:

- Tang 1 diem hien thi tren header.
- Can thong nhat lai layout HR.

Danh gia:

- Day la huong production chuan nhat.

## 4. De xuat chot

De xuat chinh:

1. Candidate:
- Giu `message icon` voi unread message count rieng.
- Giu `bell` chi cho system/job/interview notifications.
- Loai `NEW_MESSAGE` khoi bell dropdown neu muc tieu la tranh trung lap.

2. HR:
- Bo sung `message icon` rieng tren header, ngang cap voi bell.
- `bell` cua HR chi hien:
  - new application
  - application status / AI screening
  - interview events
  - system notices
- `message icon` cua HR hien unread thread/message count va mo vao inbox.

3. Backend / event contract:
- Xem `messages` unread count la du lieu cua messaging domain.
- Xem `notifications` unread count la du lieu cua notification domain.
- Khong de cung mot su kien vua tang `message badge` vua ep user phai doc no trong `bell`, tru khi co chu dich san pham rat ro rang.

## 5. Cach xu ly thong diep NEW_MESSAGE

Co 2 huong, nhung production khuyen nghi huong 1:

### Huong 1: Khong dua NEW_MESSAGE vao bell

- Khi co tin nhan moi:
  - chi tang badge message
  - co the hien browser push notification
  - click vao message icon de vao inbox

Phu hop nhat neu chat la mot module chinh.

### Huong 2: Van luu NEW_MESSAGE trong bell, nhung khong tinh vao bell badge chinh

- Dung neu muon co "activity feed" tong hop.
- Khi do UI phai tach ro:
  - bell badge = chi system unread
  - message badge = chi message unread
  - trong bell list co the co muc lich su `NEW_MESSAGE` de audit/activity

Neu chon huong nay, tuyet doi khong duoc dung chung counter.

## 6. Nguyen nhan so dem hien tai de bi loi

Khả nang cao den tu 3 diem:

1. `NEW_MESSAGE` dang duoc xu ly nhu mot notification item trong bell list.
2. `messageUnreadCount` la mot luong state rieng, duoc sync boi socket va polling.
3. List bell va badge message khong dung chung mot quy tac domain, nen co giai doan tam thoi:
- badge message tang
- bell cung co item message
- sau do mot luong refresh/reset lai counter, nen user thay "luc 2, luc 1"

Noi ngan gon: day la loi phan chia boundary, khong chi la loi UI.

## 7. Chuan du lieu de tranh double count

Nen chot quy tac:

- `notification_unread_count`:
  - chi dem system/job/interview notifications
- `message_unread_count`:
  - chi dem unread conversations/messages

Khong co notification type nao duoc dong thoi la nguon su that cho ca hai counter.

## 8. De xuat UI/UX cu the

Header production-ready cho ca 2 role:

- `Message icon`
  - badge unread rieng
  - click mo inbox hoac message dropdown preview
- `Bell icon`
  - badge notification rieng
  - click mo notification dropdown
- Thu tu uu tien:
  - message icon dat truoc bell neu san pham uu tien giao tiep
  - hoac dat bell truoc message neu uu tien workflow

Khuyen nghi cho CareerGraph:

- HR: `Message icon` + `Bell`
- Candidate: `Message icon` + `Bell`

De dong nhat nhan thuc giua 2 he thong, khong nen de candidate co message badge ma HR thi khong.

## 9. Lo trinh thuc hien

Pha 1:

- Chot product rule: bell va message tach rieng.
- Bo sung message icon cho HR header.
- Khong cong unread message vao bell badge.

Pha 2:

- Loai `NEW_MESSAGE` khoi notification unread count.
- Neu can giu `NEW_MESSAGE` trong lich su, hien thi no o activity feed nhung khong tinh vao badge bell.

Pha 3:

- Them test cases:
  - Co 1 job notification + 1 message unread
  - Bell badge chi = 1
  - Message badge chi = 1
  - Mark read message khong anh huong bell
  - Mark read notification khong anh huong message
  - HR va candidate co hanh vi dong nhat

## 10. Ket luan cuoi

Theo chuan production, phuong an tot nhat la:

- Tach `message` ra khoi `bell`.
- Ca HR va candidate deu co `message entry point` rieng.
- Bell chi chua thong bao he thong/nghiep vu.

Day la cach giam nham lan, giam loi counter, va phu hop nhat voi mot he thong co chat realtime + workflow nhu CareerGraph.
