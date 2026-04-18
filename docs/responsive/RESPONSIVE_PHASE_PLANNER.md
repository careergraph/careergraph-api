# TASK: Responsive Redesign — Phase Planner
> Agent đọc toàn bộ source → tự chia phase → xuất kế hoạch → user approve → thực thi từng phase

---

## 🎯 Nhiệm vụ của agent trong file này

**Không implement gì cả.** Chỉ:
1. Đọc và audit toàn bộ source
2. Lập danh sách tất cả pages/components cần responsive
3. Tự chia phase hợp lý
4. Xuất file `RESPONSIVE_PHASE_PLAN.md` để user review và approve

---

## 📋 BƯỚC 1 — Đọc source và lập inventory

### 1a. Xác định tech stack

Đọc và ghi chú:
- CSS approach: Tailwind? CSS Modules? Styled-components? SASS? Plain CSS?
- Breakpoints hiện tại đang định nghĩa ở đâu? (tailwind.config, variables.css, theme file?)
- Breakpoints hiện tại là gì? (sm/md/lg/xl hay custom px values?)
- Có design system / component library không? (Ant Design, MUI, Shadcn?)
- Layout system: Flex? Grid? Mixed?
- Có responsive utility class nào đang dùng không?

### 1b. Scan tất cả Pages

Tìm tất cả file `*Page.tsx`, `*View.tsx`, route definitions. Với mỗi page:

```
Page: /dashboard → DashboardPage.tsx
  - Layout: fixed sidebar + main content
  - Responsive hiện tại: không có
  - Vấn đề: sidebar không collapse trên mobile
  - Priority: HIGH (landing page)

Page: /jobs → JobsPage.tsx
  - Layout: table/list view
  - Responsive hiện tại: table scroll ngang
  - Vấn đề: cột quá nhiều trên mobile
  - Priority: HIGH

Page: /messages → MessagesPage.tsx  
  - Layout: 2 cột sidebar + chat
  - Responsive hiện tại: không có
  - Priority: HIGH
```

**Priority rules:**
- `HIGH`: pages user truy cập hàng ngày (dashboard, jobs, applications, messages)
- `MEDIUM`: pages quan trọng nhưng ít hơn (profile, settings, reports)
- `LOW`: pages admin hiếm dùng (user management, system config)

### 1c. Scan Global Layout Components

Tìm: `Layout.tsx`, `Sidebar.tsx`, `Navbar.tsx`, `Header.tsx`, `AppShell.tsx` (hoặc tên tương đương)

Đây là **foundation** — phải làm trước nhất vì ảnh hưởng toàn bộ app.

### 1d. Scan Shared Components

Tìm components dùng ở nhiều nơi: `Table`, `Modal`, `Form`, `Card`, `Button`, `DataGrid`, `Dropdown`.

Những component này có vấn đề responsive → ảnh hưởng nhiều pages cùng lúc.

### 1e. Đánh giá độ phức tạp mỗi item

```
Nhẹ (0.5 ngày): chỉ thêm breakpoint class, 1-2 thay đổi CSS
Vừa (1 ngày): cần restructure layout, thay đổi flex/grid direction  
Nặng (2+ ngày): cần thiết kế lại hoàn toàn, thêm mobile-specific UI (hamburger, bottom nav, drawer)
```

---

## 📋 BƯỚC 2 — Tiêu chí chia phase

Agent tự chia phase theo nguyên tắc sau:

**Phase 1 luôn là Foundation** — global layout, sidebar, navbar. Mọi phase sau phụ thuộc vào đây.

**Nhóm theo dependency**:
- Nếu Page A dùng Component X → Component X phải được fix trước hoặc cùng lúc với Page A
- Shared components (Table, Form) nên fix sớm để các page dùng lại

**Nhóm theo traffic/priority**:
- HIGH priority pages → phase đầu
- MEDIUM → phase giữa  
- LOW → phase cuối hoặc skip nếu không cần thiết

**Giới hạn mỗi phase**: 
- Không quá 5 pages/component sets lớn per phase
- Không quá 3 ngày làm việc per phase (để dễ review)

**Tiêu chí "Done" cho mỗi phase**:
- Breakpoints: mobile (320–767px), tablet (768–1023px), desktop (1024px+)
- Không có horizontal scroll trên mobile (trừ table data intentional)
- Touch targets min 44px
- Navigation accessible trên mobile
- Text readable không cần zoom

---

## 📋 BƯỚC 3 — Format file xuất ra

Agent xuất file `RESPONSIVE_PHASE_PLAN.md` với cấu trúc sau:

```markdown
# Responsive Redesign — Phase Plan
Source: [tên project / đường dẫn]
Audited: [timestamp]
Total pages: N | Total components: N | Estimated total: N ngày

## Tech Stack Summary
- CSS: [Tailwind / CSS Modules / ...]
- Breakpoints hiện tại: [danh sách]
- Component library: [nếu có]
- Breakpoints đề xuất cho dự án: { mobile: 768, tablet: 1024, desktop: 1280 }

## Inventory — Danh sách đầy đủ

### Pages (N tổng)
| # | Page | Route | File | Vấn đề chính | Priority | Độ phức tạp |
|---|------|-------|------|-------------|----------|------------|
| 1 | Dashboard | /dashboard | DashboardPage.tsx | Sidebar không collapse | HIGH | Nặng |
| 2 | ... | | | | | |

### Shared Components (N tổng)
| # | Component | Dùng ở | Vấn đề | Độ phức tạp |
|---|-----------|--------|--------|------------|

---

## Phase Plan

### Phase 1 — Foundation & Global Layout
Thời gian ước tính: X ngày
Files: Layout.tsx, Sidebar.tsx, Navbar.tsx, [...]
Mô tả: [những gì cần làm]
Prompt file: RESPONSIVE_PHASE_1.md ← agent sẽ tạo file này

### Phase 2 — [Tên nhóm]
...

---

## Quyết định BỎ QUA (nếu có)
| Item | Lý do skip |
|------|-----------|
| AdminSystemPage | Chỉ dùng trên desktop, không có mobile use case |
```

---

## 📋 BƯỚC 4 — Tạo Prompt file cho mỗi Phase

Với mỗi phase trong kế hoạch, agent tạo 1 file `RESPONSIVE_PHASE_N.md` riêng.

**Template cho mỗi phase file:**

```markdown
# Responsive Phase N — [Tên Phase]
> Đọc file này SAU KHI Phase N-1 đã hoàn thành và merge.
> Không implement gì ngoài scope của phase này.

## Files trong scope
[Danh sách file cụ thể]

## Breakpoints áp dụng
- Mobile: < 768px
- Tablet: 768px – 1023px  
- Desktop: ≥ 1024px

## Yêu cầu từng component/page

### [ComponentName]
Vấn đề hiện tại: [mô tả]
Yêu cầu mobile: [mô tả hành vi mong muốn]
Yêu cầu tablet: [nếu khác desktop]
Lưu ý đặc biệt: [nếu có]

## QA Checklist Phase N
- [ ] [item 1]
- [ ] [item 2]

## KHÔNG làm trong phase này
[Liệt kê rõ những gì thuộc phase sau để agent không bị scope creep]
```
