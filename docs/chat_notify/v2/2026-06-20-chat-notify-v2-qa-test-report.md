# Chat Notify V2 QA Test Report

Ngay: 2026-06-20

Loai bao cao: QA planning + acceptance report baseline

## 1. Danh gia tester senior

Phan viec nay khong nen nghiem thu trong mot lan sua tong hop ma khong chia moc.

Ly do:

- Badge message va badge bell dang co lien quan socket + polling + local optimistic state.
- Loi kiem thu nhat la loi "luc dung luc sai", rat de sot neu test qua nhanh.
- Neu deploy mot khoi lon, khi production sai se kho xac dinh loi nam o UI, store, socket hay internal push.

Ket luan QA:

- Nen test theo phase.
- Moi phase phai co acceptance criteria rieng.

## 2. Acceptance criteria theo phase

## Phase 1

Dat khi:

- HR header co `message entry point` rieng.
- Candidate header giu `message entry point` ro rang.
- Bell van hoat dong binh thuong.
- Desktop/mobile khong vo layout.

Chua dat khi:

- HR van chi co bell.
- Badge che nhau, lech position, vo header mobile.

## Phase 2

Dat khi:

- Message unread khong cong vao bell badge.
- Job/interview notification khong cong vao message badge.
- Bell mo ra khong bi tinh trang co count nhung list chua co item sau mot khoang bat thuong.

Chua dat khi:

- Co 1 message + 1 job update ma bell hien 2.
- Hoac message icon luc 2 luc 1 khi khong co user action tuong ung.

## Phase 3

Dat khi:

- Reconnect, visibility change, reload, hoac polling fallback khong tao sai lech de thay.
- Log production du de truy vet 1 su kien end-to-end.

Chua dat khi:

- Van co case chi thay thong bao sau reload.
- Khong biet event ket thuc o API, RTC hay FE.

## 3. Test case uu tien cao

### P0

- Candidate dang o home, HR gui tin nhan:
  - Message badge tang ngay
  - Bell khong tang vi message
- Candidate dang o home, job status doi:
  - Bell badge tang
  - Message badge khong tang
- HR dang o dashboard, candidate gui tin nhan:
  - HR thay message badge khong reload
- HR nhan new application:
  - Bell badge tang dung

### P1

- Mo bell sau khi co unread:
  - List hien dung item
  - Click vao item dieu huong dung
- Mo messages:
  - unread message giam dung
- Mark all read o bell:
  - chi reset bell notification
  - khong anh huong message unread

### P2

- 2 tabs cung login
- Background tab -> foreground
- Network cham / reconnect socket
- Counter > 99

## 4. Rui ro con lai can canh bao

- Neu BE van tao `NEW_MESSAGE` nhu notification item ma FE chua co rule loc ro rang, double count co the tai xuat hien.
- Neu unread count duoc day boi nhieu nguon khac nhau ma khong co source of truth ro rang, loi se xuat hien lai sau reconnect.
- Neu HR them message icon nhung khong poll/socket unread dung cach, user se thay icon nhung khong thay realtime.

## 5. Khuyen nghi tester senior

Khuyen nghi nghiem thu theo thu tu:

1. Nhat quy domain counter
2. Xac nhan UI header
3. Xac nhan socket/reconnect
4. Regression mobile

Neu khong dat buoc 1, khong nen ket luan production-ready.

## 6. Ket luan

Tu goc nhin QA, day la phan viec can chia phase va nghiem thu tung moc.

Moc co gia tri nhat can lam truoc la:

- Tach `message` khoi `bell`
- Sau do sua unread counter boundary

Chi khi 2 moc nay on dinh moi nen danh gia production-ready cho realtime notification.
