# Tổng kết đợt chỉnh sửa Admin (Module 1 → 6)

Nhánh: `feature/admin-commerce-overhaul` — mỗi module là 1 commit, đều chạy `mvn clean test` xanh trước khi commit.

## 1. Các file .md đã tạo
| File | Nội dung |
|---|---|
| `docs/changes/00-audit.md` | Audit ban đầu: hiện trạng, GAP theo checklist thương mại, thứ tự triển khai |
| `docs/changes/01-category-brand.md` | Danh mục nhiều cấp (categories) gắn sản phẩm |
| `docs/changes/02-product-variant-inventory.md` | Biến thể, trạng thái sản phẩm, tồn kho + lịch sử, CSV/bulk |
| `docs/changes/03-order-flow.md` | Máy trạng thái đơn, giữ/xuất/nhả kho, hoàn tiền, lịch sử, khách tự hủy |
| `docs/changes/04-customer-management.md` | Khách hàng: hồ sơ, hạng, khóa/mở khóa có lý do, bảo vệ tài khoản |
| `docs/changes/05-permissions.md` | Ma trận phân quyền, thu hồi phiên ngay, chặn đăng nhập tài khoản khóa, nhật ký thao tác |
| `docs/changes/06-reports-dashboard.md` | Báo cáo doanh thu ngày/tuần/tháng, top bán chạy, dashboard theo vai trò |
| `docs/changes/99-tong-ket.md` | File này |

## 2. Tổng hợp thay đổi schema (chỉ THÊM; không xóa/sửa cột cũ trừ mở rộng ENUM)
Backup trước mỗi module trong `db-backups/` (đã `.gitignore`): `before_module1..6_*.sql`. Cơ chế: `ddl-auto=update` + 2 runner idempotent.

| Module | Bảng/cột | Ghi chú |
|---|---|---|
| 1 | **Bảng** `categories` (cây cha-con); **cột** `products.category_id` (NULL) + FK | 20 sản phẩm giữ nguyên |
| 2 | **Bảng** `product_variants`, `inventory_movements`; **cột** `products.status`, `inventories.variant_id/reserved_quantity/low_stock_threshold`, `product_images.variant_id`; bỏ unique index `ux_inventory_product` (thay bằng index thường) | `VariantBackfillRunner`: 20 SP → 20 biến thể mặc định + tồn kho; `products.price/sku` **deprecated-nhưng-còn-đồng-bộ**; đổi collation DB + 3 bảng về `utf8mb4_0900_ai_ci`; trim 4 SKU |
| 3 | **Bảng** `order_status_history`; **cột** `orders.cancel_reason/cancelled_by/cancelled_by_name/cancelled_at/stock_reserved`, `order_items.variant_id` + FK; **mở rộng ENUM** `orders.order_status`, `orders.payment_status`, `orders.payment_method` (+TRANSFER) | `OrderFlowMigrationRunner`: 1 đơn `processing`→`confirmed`, 11/11 `order_items` gắn biến thể mặc định; giữ COMMENT cột; `processing` giữ lại (deprecated) |
| 4 | **Bảng** `user_status_history`; **cột** `users.lock_reason/locked_at/locked_by/internal_note` | không migrate dữ liệu |
| 5 | **Bảng** `admin_audit_log` | không migrate dữ liệu |
| 6 | (không đổi schema) | gợi ý index `orders(created_at)` khi dữ liệu lớn |

**Lưu ý khi triển khai môi trường khác**: (a) chạy các lệnh đổi collation ở `02-...md` TRƯỚC lần chạy đầu với code mới;
(b) chạy `npm run css:build` (Tailwind) khi thêm lớp CSS mới — file `output.css` đã được build lại ở Module 6;
(c) rollback: restore file backup tương ứng (SQL thủ công có trong từng file module).

## 3. Việc còn thiếu / cần bạn quyết định
**Bảo mật & vận hành (cần bạn duyệt vì đổi hành vi/hạ tầng)**
- Chuyển sang **Flyway** thay `ddl-auto=update` (đã đề xuất từ Module 1; chưa làm vì cần duyệt).
- **CSRF**, `cookie.secure`/SameSite, **giới hạn đăng nhập sai** (audit đã ghi `LOGIN_FAILED` làm căn cứ), validation dữ liệu đăng ký (email/SĐT/mật khẩu).
- **Tính giá & voucher hoàn toàn ở server**: hiện server chỉ chặn hạ giá sản phẩm và tổng tiền vượt mức; tổng sau voucher/ship do client tính; voucher chưa hoàn khi hủy đơn.

**Nghiệp vụ**
- Storefront chỉ bán **biến thể mặc định** (giỏ hàng/chi tiết chưa cho chọn biến thể); `products.price/sku` chưa thể xóa.
- Chưa tích hợp **cổng thanh toán** (xác nhận `paid` và hoàn tiền là thủ công), chưa có mã vận đơn/đơn vị vận chuyển, chưa gửi email đổi trạng thái đơn.
- Chưa có **điểm thưởng/ưu đãi theo hạng** (hạng hiện chỉ hiển thị; bảng `loyalty_points`, `user_membership` có sẵn nhưng chưa dùng).
- Phiếu nhập kho còn ở mức sản phẩm (chưa theo biến thể/lô) → chưa có giá vốn/lợi nhuận.
- Doanh thu tính theo **ngày đặt** (chưa có `delivered_at`); chưa có báo cáo theo thương hiệu/danh mục, xuất Excel/PDF.
- Khách bị khóa vẫn xem được trang cá nhân đến khi hết phiên (chỉ chặn đặt hàng); phân quyền theo dữ liệu (nhân viên chỉ thấy đơn của mình) chưa có.
- Race condition tranh hàng cuối: mới chặn bằng kiểm tra + rollback, chưa dùng khóa bi quan/`@Version` trên `inventories`.
- Promotions/voucher/brands/categories: admin-only, chưa audit khi sửa/xóa.

**Lưu ý về tài khoản**: tài khoản `admin` (role product_staff, **LOCKED**) trước đây vào được do lỗi nay đã sửa (đăng nhập không kiểm tra trạng thái);
hãy dùng tài khoản admin đầy đủ (`superadmin` / `admin 2`).

## 4. Kết quả kiểm thử cuối
`.\mvnw.cmd clean test` → **71 test, 0 lỗi, BUILD SUCCESS**
(CategoryModuleTest 4, ProductVariantInventoryTest 6, ProductBulkTest 4, OrderFlowTest 18, CustomerModuleTest 13,
PermissionMatrixTest 14, ReportModuleTest 8, PageRenderingTest 3, ShopdonghoApplicationTests 1).
