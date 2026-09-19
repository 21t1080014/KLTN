# 04 — Quản lý khách hàng

## Thay đổi
- **Trang mới `/admin/customers`** (admin + support_staff; product/warehouse bị chặn ở `RoleInterceptor`):
  - Danh sách **chỉ khách hàng** (role `customer`), tìm theo tên / email / SĐT / tên đăng nhập, lọc theo trạng thái
    (ACTIVE/LOCKED/PENDING), sắp xếp: mới đăng ký, chi tiêu cao nhất, nhiều đơn nhất, mua gần đây, tên A-Z; phân trang.
    Mỗi dòng có **số đơn, tổng chi tiêu, hạng, đơn gần nhất**.
  - **Hồ sơ khách**: thông tin liên hệ, thống kê (tổng đơn, đã giao/hoàn thành, hủy, hoàn/trả, giá trị TB/đơn, đơn đầu/cuối),
    hạng + số tiền còn thiếu để lên hạng, 20 đơn gần nhất (link sang trang đơn hàng `/admin/orders?open=<id>`),
    lịch sử khóa/mở khóa, ghi chú nội bộ.
  - **Khóa / mở khóa / kích hoạt** tài khoản khách: khóa bắt buộc có lý do; lưu người khóa + thời điểm; mọi thay đổi ghi
    `user_status_history`.
  - **Ghi chú nội bộ** của CSKH (khách không thấy; các trường này `@JsonIgnore` nên không lọt ra API của khách).
- **Định nghĩa chi tiêu & hạng** (`common/CustomerTier`): chi tiêu = tổng đơn **đã giao/hoàn thành VÀ đã thanh toán**
  (đơn hủy/hoàn/đang giao không tính). Hạng suy ra từ chi tiêu, không lưu DB nên luôn khớp đơn hàng:
  Silver (mặc định) → Gold ≥ 20.000.000₫ → Diamond ≥ 100.000.000₫ (cấu hình ở `application.properties`:
  `shop.tier.gold-min-spend`, `shop.tier.diamond-min-spend`). Tên hạng khớp bảng `membership_levels` sẵn có.
- **Khách bị khóa không đặt hàng được** ngay cả khi session cũ còn sống: `createOrder` kiểm tra lại trạng thái trong DB.
  (Đăng nhập của khách bị khóa vốn đã bị chặn.)
- **Bảo vệ tài khoản ở trang Người dùng** (`UserServiceImpl`):
  - Không xóa được tài khoản **đã có đơn hàng** (trước đây FK RESTRICT làm nổ 500; nay báo rõ "hãy khóa thay vì xóa"); lỗi FK
    khác (đấu giá/ký gửi…) cũng trả thông báo thân thiện.
  - Không tự xóa / tự đổi vai trò / tự khóa tài khoản đang đăng nhập; không xóa, khóa hoặc hạ quyền **quản trị viên ACTIVE cuối cùng**.
  - Kiểm tra vai trò/trạng thái hợp lệ (trước đây `role` không tồn tại gây lỗi DB).
  - Đổi trạng thái tại trang này cũng ghi lịch sử + thông tin khóa như trang Khách hàng.
- **Menu admin**: thêm mục "Khách hàng" (admin + support_staff); sửa để `warehouse_staff` thấy mục "Đơn hàng"
  (bổ sung cho Module 3, vốn đã cho phép truy cập nhưng menu chưa hiện).

## Lý do
Trước đây không có khái niệm "khách hàng" riêng: chỉ có trang Người dùng (admin-only, lẫn nhân viên và khách, không có
thông tin mua hàng), CSKH không tra được lịch sử khách, khóa tài khoản không có lý do/lịch sử và không có tác dụng với
session đang mở, xóa khách có đơn gây lỗi 500, admin có thể tự khóa/xóa mình hoặc xóa quản trị viên cuối cùng.
Module này cho CSKH các thông tin tối thiểu để phục vụ/chăm sóc khách và cho admin kiểm soát rủi ro tài khoản.

## Thay đổi Database
Backup trước khi đổi: `db-backups/shopdongho_before_module4_*.sql`. Chỉ **thêm** cột/bảng (hibernate `ddl-auto=update`),
không sửa/xóa cột cũ, không cần migrate dữ liệu.

- **Cột mới `users`** (đều NULL được): `lock_reason varchar(255)`, `locked_at datetime`, `locked_by varchar(100)`,
  `internal_note varchar(1000)`.
- **Bảng mới `user_status_history`**: `history_id` PK, `user_id`, `from_status`, `to_status`, `reason`, `changed_by`,
  `changed_at`; index `idx_ush_user`; KHÔNG có FK để lịch sử còn nguyên khi tài khoản bị xóa.
- SQL tương đương:
  ```sql
  ALTER TABLE users ADD COLUMN lock_reason varchar(255) NULL, ADD COLUMN locked_at datetime(6) NULL,
                    ADD COLUMN locked_by varchar(100) NULL, ADD COLUMN internal_note varchar(1000) NULL;
  CREATE TABLE user_status_history (history_id bigint AUTO_INCREMENT PRIMARY KEY, changed_at datetime(6) NOT NULL,
    changed_by varchar(100), from_status varchar(20), reason varchar(255), to_status varchar(20) NOT NULL,
    user_id varchar(255) NOT NULL, KEY idx_ush_user (user_id));
  ```
- **Rollback**: restore backup, hoặc `DROP TABLE user_status_history; ALTER TABLE users DROP COLUMN lock_reason,
  DROP COLUMN locked_at, DROP COLUMN locked_by, DROP COLUMN internal_note;` (và gỡ code tương ứng).
- Không có cột deprecated mới. Các bảng `membership_levels`, `user_membership`, `loyalty_points` có sẵn trong DB nhưng
  chưa có entity/logic — **không đụng vào** (xem "Việc còn thiếu").

## File đã thay đổi
- Mới: `controller/CustomerAdminController.java`, `service/CustomerService.java`, `service/impl/CustomerServiceImpl.java`,
  `common/CustomerTier.java`, `dto/CustomerSummaryDto.java`, `entity/UserStatusHistoryEntity.java`,
  `repository/UserStatusHistoryRepository.java`, `repository/UserRepositoryCustom.java` + `UserRepositoryImpl.java`
  (truy vấn tổng hợp, sắp xếp theo whitelist để không ghép chuỗi từ input), `templates/admin/customers.html`,
  `test/.../CustomerModuleTest.java`.
- Sửa: `entity/UserEntity.java` (4 cột mới, `@JsonIgnore`), `repository/UserRepository.java`,
  `repository/OrderRepository.java` (`existsByUserId`, `summarizeByUser`), `service/impl/UserServiceImpl.java` (bảo vệ tài khoản),
  `service/impl/OrderServiceImpl.java` (chặn khách bị khóa đặt hàng), `config/RoleInterceptor.java`,
  `templates/layout/admin.html` (menu), `templates/admin/orders.html` (`?open=<id>`), `application.properties` (ngưỡng hạng),
  `test/.../PageRenderingTest.java`, `test/.../OrderFlowTest.java` (chọn user ACTIVE).

## Cách test
1. Tự động: `.\mvnw.cmd test -Dtest=CustomerModuleTest` (13 test): mọi kiểu sắp xếp/lọc/tìm kiếm (kể cả ký tự đặc biệt),
   số liệu chi tiêu chỉ tính đơn giao/hoàn thành đã thanh toán và lên hạng đúng ngưỡng, hồ sơ không lộ password hash,
   nhân viên không tra được qua API khách, khóa cần lý do + lịch sử + mở khóa xóa thông tin khóa, khách bị khóa không đặt hàng
   được, ghi chú giới hạn độ dài và không lộ ra API của khách, phân quyền (product/warehouse bị chặn), không xóa khách có đơn,
   không tự xóa, không xóa/khóa admin cuối cùng.
2. Thủ công: đăng nhập admin/support → menu **Khách hàng** → tìm "abc" → **Hồ sơ** → xem thống kê + link sang đơn; thử khóa không
   nhập lý do (bị chặn) rồi khóa có lý do → đăng nhập storefront bằng tài khoản đó sẽ bị từ chối; mở khóa lại.

## Việc còn thiếu
- **Chưa có chương trình điểm thưởng / ưu đãi theo hạng**: hạng hiện chỉ để hiển thị/phân loại. Bảng `loyalty_points`/`user_membership`
  chưa dùng; nếu triển khai điểm thật cần thiết kế riêng (tích điểm khi đơn hoàn thành, trừ khi hoàn/hủy, ưu đãi theo hạng).
- Session của **khách bị khóa** vẫn xem được trang cá nhân đến khi hết phiên (chỉ chặn đặt hàng); **nhân viên bị khóa** cũng
  chưa bị đá khỏi phiên đang mở — sẽ xử lý ở Module 5 (kiểm tra trạng thái ở `RoleInterceptor`).
- Chưa có xuất CSV danh sách khách, gộp tài khoản trùng, xóa/ẩn danh dữ liệu cá nhân theo yêu cầu (GDPR-like).
- Chưa có địa chỉ giao hàng nhiều địa chỉ / sổ địa chỉ (chỉ 1 chuỗi `address`).
- Validation dữ liệu đăng ký (email/SĐT hợp lệ, độ mạnh mật khẩu) vẫn chưa làm (mục "cần hỏi" trong audit).
