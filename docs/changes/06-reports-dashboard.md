# 06 — Báo cáo & Dashboard

## Thay đổi
- **Định nghĩa "doanh thu" thống nhất** (trước đây dashboard cộng mọi đơn `paymentStatus='paid'`, kể cả đơn đang giao/đã hủy chờ hoàn tiền):
  doanh thu = đơn **đã giao/hoàn thành VÀ đã thanh toán**, tính theo **ngày đặt hàng**. Cùng định nghĩa với chi tiêu của khách
  (Module 4) và top bán chạy, nên các con số khớp nhau. Đơn hủy, đơn đang giao, đơn giao nhưng chưa thu tiền, đơn chờ hoàn tiền
  đều không tính.
- **Trang `/admin/reports` (chỉ admin)**: doanh thu theo **ngày / tuần (bắt đầu thứ Hai) / tháng** với khoảng ngày tùy chọn
  (mặc định 30 ngày / 12 tuần / 12 tháng), kỳ trống được điền 0 để biểu đồ liên tục, tổng doanh thu / số đơn / giá trị TB mỗi đơn,
  biểu đồ cột + bảng, **top sản phẩm bán chạy trong kỳ** (theo số lượng, kèm doanh thu), **xuất CSV danh sách đơn** trong kỳ
  (đối soát/kế toán). API: `GET /admin/reports/api/revenue`, `/api/top-products`, `/api/low-stock`, `/export/orders.csv`.
  Giới hạn kỳ: tối đa 400 ngày (theo ngày / CSV) hoặc 1100 ngày (tuần/tháng/top); ngày sai thứ tự → báo lỗi.
- **Dashboard theo vai trò** (server chỉ đưa vào JSON widget mà role được xem — không chỉ ẩn bằng CSS, nên đóng GAP-15):
  | Widget | admin | support | warehouse | product |
  |---|:-:|:-:|:-:|:-:|
  | Doanh thu hôm nay/tháng/tổng + tiền chờ hoàn | ✔ | | | |
  | Biểu đồ doanh thu 14 ngày | ✔ | | | |
  | Đơn hôm nay / tháng / tổng + đơn theo trạng thái (nhãn tiếng Việt) | ✔ | ✔ | ✔ | |
  | Tồn kho, sắp hết/hết hàng + bảng tồn thấp (GAP-13) | ✔ | | ✔ | ✔ |
  | Top bán chạy 30 ngày | ✔ (kèm doanh thu) | | | ✔ (không doanh thu) |
  | Hàng đợi việc: chờ xác nhận / chờ hoàn tiền | ✔ / — | ✔ | | |
  | Hàng đợi việc: chờ đóng gói / chờ giao | ✔ | | ✔ | |
  | Hàng đợi việc: sản phẩm nháp | ✔ | | | ✔ |
- Tồn thấp chỉ liệt kê biến thể **đang bán** (sản phẩm + biến thể ACTIVE), xếp theo còn bán được tăng dần.
- **CSV an toàn**: mọi ô văn bản đi qua `CsvUtil.cell` — escape RFC4180 và **chặn CSV/formula injection** (ô bắt đầu bằng `= + - @`
  bị thêm dấu `'` để Excel không chạy như công thức; quan trọng vì tên/email khách là dữ liệu do người ngoài nhập).
- Sửa cho **trang HTML/API export bị từ chối**: đường dẫn `/export/` được coi như API (trả 403 JSON thay vì redirect).
- **Biên dịch lại Tailwind** (`npm run css:build`): các trang admin mới từ Module 3–6 dùng thêm lớp tiện ích chưa có trong
  `output.css`; đã build lại (file `output.css` thay đổi).

## Lý do
Đóng GAP-11 (doanh thu chỉ có tổng all-time), GAP-12 (không có top bán chạy), GAP-13 (tồn thấp chưa lên dashboard), GAP-15
(dashboard mở toàn bộ số liệu cho mọi vai trò). Chủ cửa hàng cần biết doanh thu theo kỳ để ra quyết định, nhân viên chỉ cần
thấy phần việc của mình.

## Thay đổi Database
**Không có thay đổi schema** (chỉ thêm truy vấn đọc). Không cần backup/migrate riêng; đã backup tham chiếu
`db-backups/shopdongho_before_module6_*.sql`. Hiệu năng: các truy vấn nhóm theo `created_at` quét theo khoảng ngày; nếu số đơn lên
hàng trăm nghìn nên thêm index `orders(created_at)` (chưa cần cho quy mô hiện tại, để ngỏ — thêm bằng `CREATE INDEX idx_orders_created ON orders (created_at);`,
rollback `DROP INDEX idx_orders_created ON orders;`).

## File đã thay đổi
- Mới: `controller/ReportController.java`, `service/ReportService.java`, `service/impl/ReportServiceImpl.java`,
  `common/CsvUtil.java`, `templates/admin/reports.html`, `test/.../ReportModuleTest.java`.
- Sửa: `service/DashboardService.java` + `impl/DashboardServiceImpl.java` (viết lại theo vai trò),
  `controller/DashboardController.java` (truyền role), `repository/OrderRepository.java` (truy vấn doanh thu/đếm/hoàn tiền/CSV),
  `repository/OrderItemRepository.java` (top bán chạy), `repository/ProductRepository.java` (`countByStatus`),
  `config/RoleInterceptor.java` (`/export/` = API), `templates/admin/dashboard.html` (viết lại), `templates/layout/admin.html`
  (menu Báo cáo), `static/css/output.css` (build lại), `test/.../PageRenderingTest.java`.

## Cách test
1. Tự động: `.\mvnw.cmd test -Dtest=ReportModuleTest` (8 test) trên dữ liệu cố định tháng 3–4/2020: chuỗi theo ngày/tuần/tháng
   đúng số liệu và điền 0, chỉ đơn giao/hoàn thành đã thanh toán được tính, ranh giới tuần (Chủ nhật thuộc tuần trước), mặc định
   + kiểm tra khoảng ngày, xếp hạng top bán chạy bỏ qua đơn hủy/chưa trả, CSV escape + chặn formula, báo cáo chỉ admin, dashboard
   từng vai trò chỉ có đúng widget của mình (non-admin không có chữ "revenue" trong JSON), tồn thấp sắp xếp/lọc.
2. Thủ công: đăng nhập admin → Dashboard (thấy doanh thu, biểu đồ, hàng đợi) → **Báo cáo** → đổi Ngày/Tuần/Tháng, chỉnh khoảng ngày,
   bấm "Xuất đơn hàng (CSV)". Đăng nhập lần lượt các vai trò nhân viên để so sánh dashboard.

## Việc còn thiếu
- Doanh thu tính theo **ngày đặt hàng**, chưa theo ngày giao/thu tiền (chưa lưu mốc `delivered_at`; có thể lấy từ `order_status_history`
  cho đơn mới nhưng đơn cũ không có). Đơn đặt tháng này giao sang tháng sau sẽ tính vào tháng đặt.
- Chưa có **giá vốn/lợi nhuận** (phiếu nhập hiện ở mức sản phẩm, không gắn từng lô với đơn bán), chưa có báo cáo theo thương hiệu/danh mục,
  tỷ lệ hủy/hoàn, khách hàng mới vs cũ, cohort.
- Biểu đồ vẽ bằng HTML/CSS đơn giản (không dùng thư viện), chưa có tooltip nâng cao/so sánh kỳ trước.
- Báo cáo lớn chạy đồng bộ; nếu dữ liệu tăng mạnh cần index `orders(created_at)`, cache hoặc bảng tổng hợp theo ngày.
- Chưa có xuất Excel/PDF, chưa lên lịch gửi báo cáo qua email.
