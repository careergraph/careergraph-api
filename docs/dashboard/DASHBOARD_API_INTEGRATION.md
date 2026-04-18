# TASK: Dashboard — Kết nối dữ liệu thực từ API
> **Scope:** Spring Boot BE + HR FE (React TypeScript)  
> **Nguyên tắc cốt lõi:** Đọc toàn bộ source trước, không tạo endpoint trùng, không giữ lại hardcode, view không có dữ liệu thực → bỏ hoặc ghi rõ lý do giữ.

---

## 🎯 Mục tiêu

1. Đọc toàn bộ source HR FE → lập bản đồ mọi widget/chart/metric đang hardcode
2. Đọc toàn bộ Spring Boot → lập danh sách endpoint đã có
3. Quyết định: widget nào map được endpoint nào, thiếu thì tạo mới, không hợp lý thì bỏ
4. Implement và xuất file báo cáo

---

## 📋 BƯỚC 1 — ĐỌC & AUDIT (BẮT BUỘC, không skip)

### 1a. Audit HR FE Dashboard

Agent đọc toàn bộ các file liên quan dashboard (tìm theo: `Dashboard`, `Overview`, `Stats`, `Chart`, `Metric`, `Analytics`, `Summary`).

Với mỗi widget/section, ghi vào bảng audit:

```
| Widget ID | Tên hiển thị | Dữ liệu hiện tại | Loại chart | Ghi chú |
|-----------|-------------|------------------|-----------|---------|
| W001      | Tổng ứng viên | hardcode: 1,234 | metric card | ... |
| W002      | Theo trạng thái | hardcode array | pie chart | ... |
```

### 1b. Audit Spring Boot Endpoints

Agent đọc toàn bộ `@RestController` files, lập danh sách:

```
| Endpoint | Method | Path | Response shape | Ghi chú |
|----------|--------|------|----------------|---------|
| E001 | GET | /api/v1/applications/stats | {...} | đã có |
| E002 | GET | /api/v1/jobs/count | {...} | đã có |
```

### 1c. Mapping Table

Sau khi có 2 danh sách trên, tạo bảng mapping:

```
| Widget | Quyết định | Endpoint | Lý do |
|--------|-----------|----------|-------|
| W001 - Tổng ứng viên | MAP | E001 /applications/stats | field totalCount |
| W002 - Theo trạng thái | MAP | E001 /applications/stats | field byStatus |
| W005 - Conversion rate | BỎ | - | Không có đủ data trong hệ thống |
| W008 - Revenue | BỎ | - | Hệ thống tuyển dụng không track doanh thu |
| W010 - Time to hire | TẠO MỚI | GET /api/v1/analytics/time-to-hire | Cần aggregate từ applications |
```

**Quy tắc quyết định:**
- `MAP` → endpoint đã tồn tại, data phù hợp
- `MAP (transform)` → endpoint có, cần transform/aggregate ở FE
- `TẠO MỚI` → data có trong DB nhưng chưa có endpoint
- `BỎ` → widget không hợp lý với hệ thống tuyển dụng, không có data nền

---

## 📋 BƯỚC 2 — IMPLEMENT BE (chỉ tạo endpoint thực sự thiếu)

### Template endpoint analytics (nếu cần tạo mới)

```java
@RestController
@RequestMapping("/api/v1/analytics")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    // Ví dụ — chỉ tạo nếu chưa có endpoint tương đương:
    
    // GET /api/v1/analytics/dashboard-summary
    // Gom nhiều metric vào 1 call để giảm request từ FE
    @GetMapping("/dashboard-summary")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getDashboardSummary(from, to, getCurrentUserId()));
    }
}
```

**Nguyên tắc thiết kế endpoint analytics:**
- Ưu tiên 1 endpoint gom nhiều metric (`/dashboard-summary`) thay vì nhiều call
- Luôn hỗ trợ date range filter (`from`, `to`)
- HR chỉ thấy data của jobs mình phụ trách (filter theo `createdBy` hoặc `assignedTo`)
- Cache result 5 phút (hoặc dùng `@Cacheable`) vì dashboard không cần realtime tuyệt đối

### Các metric thường gặp trong hệ thống tuyển dụng — tham khảo khi quyết định BỎ/GIỮ:

```
✅ Hợp lý (nếu có data):
- Tổng đơn ứng tuyển (tổng / hôm nay / tuần này)
- Đơn theo trạng thái (pie/donut chart)
- Đơn theo job (bar chart top N jobs)
- Đơn theo thời gian (line chart 30/60/90 ngày)
- Tổng jobs đang mở / đã đóng
- Top jobs nhiều ứng viên nhất
- Tỷ lệ pass interview (nếu track)
- Thời gian xử lý đơn (nếu có created_at và status_changed_at)

❌ Không hợp lý với tuyển dụng:
- Revenue / doanh thu (trừ khi là recruitment agency)
- Customer satisfaction score
- Product conversion rate
- Page views / traffic analytics
- Inventory metrics
```

---

## 📋 BƯỚC 3 — IMPLEMENT FE

### 3a. Tạo dashboard API client

```typescript
// src/features/dashboard/api/dashboardApi.ts
export const dashboardApi = {
  getSummary: (from?: string, to?: string) =>
    axiosInstance.get<DashboardSummaryDto>('/api/v1/analytics/dashboard-summary', {
      params: { from, to }
    }),
  // Thêm các call khác nếu cần
};
```

### 3b. Custom hook cho dashboard data

```typescript
// src/features/dashboard/hooks/useDashboardData.ts
export function useDashboardData(dateRange: DateRange) {
  const [data, setData] = useState<DashboardSummaryDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    setLoading(true);
    dashboardApi.getSummary(dateRange.from, dateRange.to)
      .then(res => setData(res.data))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [dateRange.from, dateRange.to]);
  
  return { data, loading, error };
}
```

### 3c. Yêu cầu UI khi thay hardcode

- **Loading state**: Skeleton cho mỗi widget (không dùng spinner toàn trang)
- **Error state**: Mỗi widget có error boundary riêng — 1 widget lỗi không crash toàn dashboard
- **Empty state**: Khi data = 0 hoặc chưa có data, hiện text rõ ràng (không để chart trống)
- **Date range picker**: Thêm nếu chưa có — default "30 ngày gần nhất"

---

## 📋 BƯỚC 4 — XUẤT BÁO CÁO

Sau khi implement xong, agent tạo file `DASHBOARD_REPORT.md` với nội dung:

```markdown
# Dashboard Integration Report
Generated: [timestamp]

## 1. Audit Summary
- Tổng widgets tìm thấy: N
- Widgets giữ lại: N
- Widgets bỏ: N
- Endpoints tạo mới: N
- Endpoints tái sử dụng: N

## 2. Widget Mapping (chi tiết)
[Bảng mapping đầy đủ]

## 3. Endpoints Tạo Mới
[Danh sách + lý do]

## 4. Widgets Đã Bỏ
[Danh sách + lý do cụ thể]

## 5. Vấn đề phát hiện
[Nếu có data inconsistency, missing foreign key, etc.]

## 6. QA Checklist
- [ ] Mỗi widget load đúng data từ API
- [ ] Loading skeleton hiển thị đúng
- [ ] Error state hoạt động (test bằng cách tắt BE)
- [ ] Date range filter hoạt động
- [ ] HR A không thấy data của HR B
- [ ] Performance: dashboard load < 2s
```
