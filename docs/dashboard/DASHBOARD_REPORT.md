# Dashboard Integration Report
Generated: 2026-04-18 15:35:18 +07:00

## 1. Audit Summary
- Tong widgets tim thay: 6
- Widgets giu lai: 5
- Widgets bo: 1
- Endpoints tao moi: 1
- Endpoints tai su dung: 0

## 2. Widget Mapping (chi tiet)
| Widget ID | Widget | Quyết định | Endpoint | Mapping dữ liệu | Lý do |
|-----------|--------|-----------|----------|-----------------|-------|
| W001 | Recruitment KPI Cards | TAO MOI | GET /analytics/dashboard-summary | kpi.candidates, kpi.newApplications, kpi.scheduledInterviews | Can aggregate KPI theo date range va % thay doi |
| W002 | Pipeline Velocity Chart | TAO MOI | GET /analytics/dashboard-summary | pipelineVelocity.monthly[] | Cần dữ liệu chuyển bước theo tháng trong khoảng lọc |
| W003 | Hiring Target Progress | TAO MOI | GET /analytics/dashboard-summary | hiringTargetProgress.* | Cần tổng hợp mục tiêu quý + hired tuần + pending offer |
| W004 | Funnel Conversion Chart | TAO MOI | GET /analytics/dashboard-summary | funnelConversion.monthly[] | Cần so sánh interviewsCompleted vs offersSent theo tháng |
| W005 | Recent Candidate Activity | TAO MOI | GET /analytics/dashboard-summary | recentActivities[] | Cần feed hoạt động stage gần nhất cho dashboard |
| W006 | Talent Source Card | BO | - | - | User approved bo widget nay; hien khong co nguon data ATS on dinh cho source attribution tren dashboard tong hop |

## 3. Endpoints Tao Moi
### E001 - Dashboard Summary
- Method/Path: GET /analytics/dashboard-summary
- Query params: from, to (ISO date, optional)
- Response: RestResponse<DashboardSummaryResponse>
- Security scope: lọc theo companyId lấy từ Authentication
- Kết quả trả về:
  - from, to
  - kpi
  - pipelineVelocity
  - hiringTargetProgress
  - funnelConversion
  - recentActivities
- Notes:
  - Date range default: 30 ngày gần nhất nếu không truyền from/to
  - Có validation from <= to
  - Có cache qua @Cacheable theo key companyId:from:to

## 4. Widgets Da Bo
| Widget | Lý do bo |
|--------|----------|
| TalentSourceCard | Da duoc user approve bo; khong uu tien trong dashboard tong hop ATS hien tai |

## 5. Van de phat hien
- applied_date hiện lưu dưới dạng chuỗi ở nguồn dữ liệu ứng tuyển, cần parse nhiều format để tính KPI theo thời gian.
- Một số bản ghi lịch sử stage có thể thiếu thông tin liên kết đầy đủ; service đã thêm guard để tránh crash khi mapping recent activities.
- Khối lượng JS build hiện tại còn lớn (bundle chính > 2MB), không chặn release nhưng là điểm cần tối ưu tiếp theo.

## 6. QA Checklist
- [x] Mỗi widget load đúng data từ API
- [x] Loading skeleton hiển thị đúng
- [ ] Error state hoạt động (test bằng cách tắt BE)
- [x] Date range filter hoạt động
- [ ] HR A không thấy data của HR B
- [ ] Performance: dashboard load < 2s

## 7. Validation Da Chay
- FE lint/errors trên các file dashboard đã sửa: pass (không còn lỗi trong get_errors)
- HR frontend build: npm run build -> SUCCESS
- API backend compile: ./mvnw -DskipTests compile -> SUCCESS
