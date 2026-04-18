# PROMPT GUIDE — Dashboard Integration & Responsive Redesign

---

## ═══════════════════════════════════════
## TASK A — Dashboard: Kết nối API thực
## ═══════════════════════════════════════

### Files attach
- `DASHBOARD_API_INTEGRATION.md`
- **Toàn bộ source Spring Boot** (zip hoặc paste các Controller files)
- **Toàn bộ source HR FE** (zip hoặc paste Dashboard-related files)

### Prompt — Chạy 1 lần duy nhất

```
Bạn là senior developer 15+ năm kinh nghiệm, đồng thời là business analyst 
có kinh nghiệm với hệ thống ATS (Applicant Tracking System).

Đọc file DASHBOARD_API_INTEGRATION.md để hiểu nhiệm vụ.

BƯỚC 1 — AUDIT (quan trọng nhất, làm kỹ):
Đọc toàn bộ source HR FE, tìm và liệt kê MỌI widget/metric/chart 
đang dùng dữ liệu hardcode. Đọc toàn bộ Spring Boot Controller files, 
liệt kê MỌI endpoint đang có.

BƯỚC 2 — MAPPING:
Tạo bảng mapping widget → endpoint. Với mỗi widget, quyết định:
- MAP: endpoint đã có, dùng luôn
- MAP (transform): endpoint có, cần xử lý data ở FE
- TẠO MỚI: data có trong DB, cần thêm endpoint
- BỎ: widget không hợp lý với hệ thống tuyển dụng

Trình bày bảng mapping và CHỜ tôi approve trước khi implement.

BƯỚC 3 (sau khi tôi approve) — IMPLEMENT:
- Chỉ tạo endpoint thực sự cần thiết, không duplicate
- Xóa toàn bộ hardcode trong FE, thay bằng API call
- Thêm skeleton loading, error state, empty state cho mỗi widget

BƯỚC 4 — XUẤT BÁO CÁO:
Tạo file DASHBOARD_REPORT.md theo template trong file hướng dẫn.

Bắt đầu với Bước 1, đừng code gì trước khi có mapping table.
```

---

## ═══════════════════════════════════════
## TASK B — Responsive: Lập kế hoạch (chạy trước)
## ═══════════════════════════════════════

### Mục đích
Agent đọc source → tự chia phase → xuất kế hoạch.
**Không implement gì.** Chỉ lập kế hoạch.

### Files attach — HR FE
- `RESPONSIVE_PHASE_PLANNER.md`
- Toàn bộ source HR FE (zip)

### Prompt — HR FE Phase Planning

```
Bạn là senior frontend developer 15+ năm kinh nghiệm, 
chuyên về responsive design và large-scale React applications.

Đọc file RESPONSIVE_PHASE_PLANNER.md để hiểu nhiệm vụ.

NHIỆM VỤ: Chỉ lập kế hoạch, KHÔNG implement bất cứ thứ gì.

Đọc toàn bộ source HR FE và thực hiện:

1. Xác định tech stack CSS (Tailwind? CSS Modules? Breakpoints hiện tại?)
2. Scan tất cả Pages — liệt kê đầy đủ với priority và độ phức tạp
3. Scan tất cả Shared Components ảnh hưởng nhiều pages
4. Chia phase hợp lý theo tiêu chí trong file hướng dẫn
5. Với mỗi phase: mô tả scope, ước tính thời gian, liệt kê file
6. Tạo prompt file riêng cho từng phase (RESPONSIVE_PHASE_1.md, PHASE_2.md...)

Xuất file RESPONSIVE_PHASE_PLAN_HR.md đầy đủ theo template.
Sau đó tạo các file RESPONSIVE_PHASE_N_HR.md cho từng phase.

Trình bày kế hoạch để tôi review trước.
```

### Files attach — Client FE
- `RESPONSIVE_PHASE_PLANNER.md`  
- Toàn bộ source Client FE (zip)

### Prompt — Client FE Phase Planning

```
Bạn là senior frontend developer 15+ năm kinh nghiệm.

Nhiệm vụ GIỐNG HỆT prompt HR FE ở trên, nhưng áp dụng cho 
source Client FE (React JS, không phải TypeScript).

Lưu ý bổ sung cho Client FE:
- Client thường dùng mobile nhiều hơn HR → mobile-first priority cao hơn
- Cần bottom navigation trên mobile nếu hiện tại chưa có
- Touch UX quan trọng hơn (button size, swipe gestures)

Xuất file RESPONSIVE_PHASE_PLAN_CLIENT.md và các RESPONSIVE_PHASE_N_CLIENT.md.
```

---

## ═══════════════════════════════════════
## TASK C — Responsive: Thực thi từng Phase
## (Dùng sau khi đã có Phase Plan và đã approve)
## ═══════════════════════════════════════

### Cách dùng

Sau khi agent xuất `RESPONSIVE_PHASE_PLAN_HR.md` và các `RESPONSIVE_PHASE_N_HR.md`,
bạn review kế hoạch, approve, rồi chạy từng phase theo prompt sau:

### Prompt template — Thực thi Phase N (HR FE)

```
Bạn là senior frontend developer 15+ năm kinh nghiệm responsive design.

Attach files:
- RESPONSIVE_PHASE_N_HR.md  ← file kế hoạch phase này
- [Các file source trong scope của phase này]

Đọc file RESPONSIVE_PHASE_N_HR.md, implement ĐÚNG những gì được liệt kê.

Yêu cầu bắt buộc:
- Breakpoints: mobile < 768px | tablet 768–1023px | desktop ≥ 1024px
- Không có horizontal scroll ngoài ý muốn trên mobile
- Touch targets tối thiểu 44px
- Không break layout desktop đang hoạt động
- Test từng breakpoint sau khi implement mỗi component

Quan trọng: KHÔNG implement bất cứ thứ gì ngoài scope của phase này.
Nếu phát hiện vấn đề thuộc phase khác, ghi chú lại nhưng không fix.

Sau khi implement xong phase này:
1. Chạy QA checklist trong file
2. Báo cáo: component nào done, component nào có vấn đề cần chú ý
3. List những gì đã thay đổi (file nào, thay đổi gì)
```

### Prompt template — Thực thi Phase N (Client FE)

```
[Giống hệt trên nhưng đổi file sang RESPONSIVE_PHASE_N_CLIENT.md]

Lưu ý bổ sung cho Client:
- Đây là React JavaScript (không TypeScript)
- Ưu tiên mobile experience: touch targets lớn, transitions mượt
- Nếu phase này bao gồm navigation: implement bottom tab bar cho mobile
```

---

## 💡 Tips workflow hiệu quả

**Cho Dashboard task:**
```
Sau khi agent xuất mapping table, trước khi approve:
- Kiểm tra xem có widget nào bị bỏ mà bạn muốn giữ không
- Kiểm tra xem endpoint "TẠO MỚI" có thực sự cần không
- Approve từng nhóm hoặc approve toàn bộ
```

**Cho Responsive task:**
```
Sau khi có Phase Plan:
- Kiểm tra thứ tự phase có hợp lý không
- Có thể gộp hoặc tách phase nếu muốn
- Approve rồi chạy phase 1 trước, merge xong mới chạy phase 2
```

**Nếu agent implement sai scope:**
```
Dừng lại. Phase này chỉ bao gồm [liệt kê files]. 
Revert những thay đổi ngoài scope và tiếp tục đúng scope.
```

**Nếu agent hỏi quá nhiều trước khi làm:**
```
Đừng hỏi, đọc source code và tự quyết định. 
Ghi assumption của bạn vào đầu file báo cáo/kế hoạch.
Tôi sẽ review và điều chỉnh sau.
```
