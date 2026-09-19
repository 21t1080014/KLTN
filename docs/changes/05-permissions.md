# 05 — Rà soát phân quyền

## Thay đổi
- **Ma trận phân quyền tập trung** `common/AdminPermissions.java` (thay chuỗi `if/else` trong `RoleInterceptor`): mỗi module có
  role được **đọc** (GET), **ghi** (POST/PUT), **xóa** (DELETE); đường dẫn không khớp module nào → **chỉ admin**
  (deny-by-default, đóng GAP-16: endpoint mới không tự động mở cho nhân viên). So khớp theo ranh giới đoạn đường dẫn
  (`/admin/productsX` không hưởng luật của `/admin/products`).

  | Module | Đọc | Ghi | Xóa | Ghi chú |
  |---|---|---|---|---|
  | `/admin/products` (gồm biến thể, CSV, bulk) | admin, product_staff | admin, product_staff | **admin** | nhân viên sản phẩm dùng trạng thái "Ngừng bán" thay vì xóa |
  | `/admin/inventory` | admin, warehouse, **product_staff** | admin, warehouse | admin, warehouse | GAP-14: product_staff **chỉ xem**; xóa dòng tồn chỉ khi tồn = 0 (service kiểm tra) |
  | `/admin/purchases` | admin, warehouse | admin, warehouse | admin, warehouse | xóa chỉ trong ngày + phải đủ tồn để đảo, có ghi lịch sử kho + audit |
  | `/admin/orders` | admin, support, warehouse | admin, support, warehouse | admin | từng bước chuyển trạng thái còn giới hạn theo role ở `OrderWorkflow` (Module 3) |
  | `/admin/customers` | admin, support | admin, support | admin | |
  | `/admin/users`, `brands`, `categories`, `promotions`, `voucher`, `reports`, `audit` | admin | admin | admin | |
  | `/admin/dashboard` | mọi nhân viên | mọi nhân viên | admin | quyết định về dữ liệu doanh thu để Module 6 (GAP-15) |

- **Thu hồi phiên ngay khi khóa/xóa/hạ quyền** (`RoleInterceptor` là `@Component`, nạp lại tài khoản từ DB mỗi request):
  tài khoản không còn ACTIVE / không còn là nhân viên → hủy session; role trong session luôn được đồng bộ theo DB
  (đổi role có hiệu lực ngay, không phải đợi hết phiên 30 phút). Không thay đổi cơ chế xác thực (vẫn session + `userLogin`).
- **Phản hồi từ chối đúng chuẩn**: gọi API mà chưa đăng nhập → `401` JSON; không đủ quyền → `403` JSON; trang HTML → chuyển
  về `/admin/login` (chưa đăng nhập) hoặc `/admin/dashboard` (đã đăng nhập nhưng không đủ quyền). Trước đây mọi trường hợp đều
  redirect sang trang đăng nhập nên `fetch().json()` của UI hỏng âm thầm.
- **Sửa 2 lỗ hổng đăng nhập nghiêm trọng**:
  1. Đăng nhập **admin** trước đây không kiểm tra trạng thái ⇒ tài khoản LOCKED/PENDING vẫn vào được. Nay chỉ ACTIVE.
  2. Đăng nhập **storefront** (`AuthController`) gọi `login()` chung ⇒ khách bị khóa vẫn đăng nhập được, và tài khoản nhân viên
     cũng lấy được phiên khách. Nay dùng `loginCustomer()` (chỉ role customer + ACTIVE).
  Thông báo sai tài khoản/mật khẩu nay dùng **thông điệp chung** ở cả hai nơi, không lộ tài khoản nào tồn tại.
- **Nhật ký thao tác** (`admin_audit_log` + trang `/admin/audit`, chỉ admin): ghi đăng nhập admin (thành công/sai/bị chặn), tạo/sửa
  (kèm đổi vai trò/trạng thái/mật khẩu — không ghi giá trị mật khẩu)/xóa người dùng, xóa sản phẩm, xóa phiếu nhập, xóa dòng tồn kho,
  xóa đơn hàng; kèm người thực hiện, role, IP. Lọc theo hành động/người thực hiện, phân trang.
- **UI theo quyền** (ẩn nút thay vì để bấm rồi 403): `<body data-role>`; tồn kho ẩn Sửa/Xóa với product_staff; nút xóa sản phẩm/biến thể
  chỉ hiện với admin; menu: product_staff thấy Tồn kho (chỉ xem), admin thấy Nhật ký thao tác.

## Lý do
Đóng GAP-14/15(một phần)/16 trong `00-audit.md` và các lỗ hổng phát hiện khi rà từng module: phiên nhân viên bị khóa vẫn dùng
được, khóa tài khoản không có tác dụng ở cả cổng admin lẫn storefront, quyền xóa dữ liệu quan trọng không phân tầng, không có
dấu vết ai làm gì với tài khoản/dữ liệu nhạy cảm, và ma trận quyền rải rác khó kiểm chứng.

## Thay đổi Database
Backup trước khi đổi: `db-backups/shopdongho_before_module5_*.sql`. Chỉ thêm 1 bảng, không sửa/xóa gì có sẵn.
- **Bảng mới `admin_audit_log`** (hibernate tự tạo): `audit_id` PK, `actor`, `actor_role`, `action`, `target_type`, `target_id`,
  `detail varchar(500)`, `ip`, `created_at`; index `idx_audit_created`, `idx_audit_actor`; không FK.
  ```sql
  CREATE TABLE admin_audit_log (audit_id bigint AUTO_INCREMENT PRIMARY KEY, action varchar(40) NOT NULL, actor varchar(100),
    actor_role varchar(30), created_at datetime(6) NOT NULL, detail varchar(500), ip varchar(64), target_id varchar(100),
    target_type varchar(40), KEY idx_audit_created (created_at), KEY idx_audit_actor (actor));
  ```
- **Rollback**: `DROP TABLE admin_audit_log;` (và gỡ các lời gọi `auditService`), hoặc restore backup.
- Không migrate dữ liệu, không có cột deprecated.

## File đã thay đổi
- Mới: `common/AdminPermissions.java`, `entity/AuditLogEntity.java`, `repository/AuditLogRepository.java`,
  `service/AuditService.java`, `service/impl/AuditServiceImpl.java`, `controller/AuditController.java`,
  `templates/admin/audit.html`, `test/.../PermissionMatrixTest.java`, `test/.../AdminAuth.java` (đăng nhập giả lập đúng cơ chế thật cho test).
- Sửa: `config/RoleInterceptor.java` (viết lại), `config/MvcConfig.java` (dùng bean interceptor),
  `controller/LoginAdminController.java`, `controller/AuthController.java`, `repository/UserRepository.java`,
  `service/impl/UserServiceImpl.java`, `ProductServiceImpl.java`, `PurchaseEntriesServiceImpl.java`, `InventoryServiceImpl.java`,
  `OrderServiceImpl.java` (gọi audit), `templates/layout/admin.html`, `templates/admin/inventory.html`,
  `static/js/inventory.js`, `product.js`, `product-variants.js`, `purchase-entry.js`, và các test cũ chuyển sang `AdminAuth`
  (không còn set thẳng `roleName`).

## Cách test
1. Tự động: `.\mvnw.cmd test -Dtest=PermissionMatrixTest` (14 test): bảng role × module × GET/POST/PUT/DELETE, ranh giới đường dẫn,
   ẩn danh 302/401, truy cập chéo module 403, product_staff xem-được-không-ghi-được tồn kho, quyền xóa theo tầng, khóa/xóa/hạ
   quyền nhân viên bị cắt ngay ở request kế tiếp (session bị hủy), đăng nhập admin/storefront chặn tài khoản khóa/chưa kích hoạt/
   sai loại, thông báo lỗi không lộ tài khoản tồn tại, audit ghi đúng và chỉ admin xem được.
2. Thủ công: đăng nhập admin ở 2 trình duyệt; tab 2 dùng tài khoản nhân viên, tab 1 khóa tài khoản đó (Người dùng) → tab 2 bấm
   bất kỳ chức năng nào sẽ bị đưa về đăng nhập. Xem `/admin/audit` thấy các thao tác vừa làm.

## Việc còn thiếu / lưu ý vận hành
- ⚠ Tài khoản `admin` (role product_staff, **LOCKED**) trước đây đăng nhập được do lỗi đã sửa; nay bị chặn đúng thiết kế. Dùng
  tài khoản admin đầy đủ (`superadmin` hoặc `admin 2`).
- **Chưa chống dò mật khẩu** (giới hạn số lần đăng nhập sai/khóa tạm theo IP+tài khoản) — audit đã ghi `LOGIN_FAILED` để làm căn cứ,
  cần thiết kế ngưỡng với bạn trước khi bật (tránh tự khóa mình).
- **CSRF** vẫn chưa bật (đổi trạng thái bằng POST/PUT chỉ dựa vào cookie phiên; `cookie.secure`/SameSite chưa cấu hình) — mục "cần hỏi" cũ.
- Phân quyền theo **dữ liệu** (vd nhân viên chỉ thấy đơn được giao cho mình) chưa có; hiện là phân quyền theo module/thao tác.
- `promotions`/`voucher`/`brands`/`categories` vẫn admin-only, không có audit khi sửa/xóa (chưa coi là dữ liệu nhạy cảm bằng tài khoản/kho).
- Dashboard vẫn mở cho mọi nhân viên (quyền vào trang), việc **ẩn doanh thu với non-admin** làm ở Module 6.
- Cache lookup tài khoản mỗi request admin (1 truy vấn theo khóa chính) chưa cần nhưng có thể thêm nếu lưu lượng admin lớn.
