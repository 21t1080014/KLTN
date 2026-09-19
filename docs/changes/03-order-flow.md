# 03 — Luồng đơn hàng + trạng thái

## Thay đổi
- **Máy trạng thái đơn hàng** (không cho nhảy cóc / đi ngược):
  `pending → confirmed → packing → shipping → delivered → completed`; nhánh phụ `canceled` (hủy trước khi giao xong)
  và `refunded` (hoàn tiền/trả hàng sau khi đã giao). `canceled`/`refunded` là trạng thái cuối.
  Trạng thái cũ `processing` được coi tương đương `confirmed` (đã migrate).
- **Trạng thái thanh toán tách riêng**: `pending → paid → refund_pending → refunded`, `failed` (thất bại/hủy trước khi trả).
  Hoàn tiền chỉ mở ra khi đơn đã `canceled/refunded`; đơn đã đóng thì không thu tiền lại.
- **Phân quyền theo từng bước** (kiểm tra ở service, không chỉ ở UI):
  | Chuyển tới | Ai được làm |
  |---|---|
  | confirmed, delivered, completed, canceled, refunded | admin, support_staff |
  | packing, shipping | admin, warehouse_staff |
  | Đổi trạng thái thanh toán | admin, support_staff |
  | Xóa đơn (chỉ đơn pending + chưa thanh toán) | admin |
  `warehouse_staff` giờ vào được trang `/admin/orders` (trước đây bị chặn) để đóng gói/giao; nút thao tác hiển thị theo quyền.
- **Lý do bắt buộc** khi hủy hoặc hoàn/trả (lưu `cancel_reason`, ai hủy CUSTOMER/ADMIN + tên, thời điểm).
- **Kho gắn với đơn** (qua `StockService`, có lịch sử `inventory_movements`):
  đặt hàng → **giữ hàng** (reserved↑, hết hàng thì từ chối cả đơn, không tạo đơn dở); `packing → shipping` → **xuất kho**
  (quantity↓, reserved↓); hủy trước khi xuất → **nhả hàng**; hủy khi đang giao/hoàn sau khi giao → **nhập lại kho**;
  xóa đơn pending → nhả hàng.
- **Tự động hóa thanh toán**: giao xong đơn COD → `paid` (đã thu tiền); hủy đơn đã `paid` → `refund_pending`;
  hủy đơn chưa trả → `failed`. Đơn thanh toán online (VNPay/CreditCard/TRANSFER) phải `paid` mới được đóng gói;
  hoàn thành đơn phải đã thu tiền.
- **Tạo đơn chắc chắn hơn** (khép một phần audit về "server tin giá client"): sản phẩm phải `ACTIVE`; đơn giá lấy theo
  server (giá niêm yết trừ khuyến mãi đang hiệu lực) — client gửi giá thấp hơn thật (> 1đ) bị từ chối; tổng tiền không
  được vượt tổng thật + phí ship tối đa (30.000₫); dòng trùng sản phẩm được gộp; `order_items.variant_id` được lưu.
- **Lịch sử xử lý** (`order_status_history`): mỗi lần tạo/đổi trạng thái/đổi thanh toán ghi ai (username + role), khi nào,
  từ-sang, ghi chú. Xem được trong màn chi tiết đơn.
- **Khách tự hủy đơn**: `POST /api/orders/{id}/cancel` (chỉ chủ đơn, chỉ khi `pending/confirmed`; đơn người khác hoặc
  đơn không tồn tại đều trả cùng một thông báo — chống IDOR). Trang "Đơn hàng của tôi" hiển thị nhãn tiếng Việt, lý do hủy
  và nút **Hủy đơn hàng**.
- **UI admin `/admin/orders`** viết lại cho dùng được: lọc theo trạng thái, phân trang, nút "Chi tiết / Xử lý" mở modal
  (khách, sản phẩm + SKU, tổng, các nút chuyển trạng thái hợp lệ theo quyền, ô lý do, nút đổi thanh toán, lịch sử).
  Escape HTML khi dựng bảng (trước đây chèn thẳng email vào innerHTML).
- Sửa lỗi có sẵn: giao diện checkout gửi `TRANSFER` (chuyển khoản) nhưng enum `PaymentMethod` không có ⇒ đặt hàng bằng
  chuyển khoản bị lỗi 400. Đã thêm `TRANSFER` vào enum + cột.
- API cũ `PUT /admin/orders/api/{id}?orderStatus=&paymentStatus=` vẫn dùng được nhưng mỗi thay đổi đều đi qua kiểm tra luồng.

## Lý do
Giải quyết các GAP về đơn hàng trong `00-audit.md`: trạng thái đặt tay tuỳ ý (nhảy cóc, ai cũng sửa được), thiếu lý do hủy,
không có hoàn tiền, đơn hàng không tác động kho (bán vượt tồn), không có lịch sử ai làm gì, khách không tự hủy được,
giá/tổng tiền tin hoàn toàn vào client. Đây là luồng đơn tối thiểu của một cửa hàng thật (xác nhận → đóng gói → giao → hoàn tất,
có hủy/hoàn kèm hoàn kho và hoàn tiền).

## Thay đổi Database
Backup trước khi đổi: `db-backups/shopdongho_before_module3_20260919_110937.sql`.

- **Bảng mới** `order_status_history` (hibernate `ddl-auto=update` tự tạo): `history_id` PK, `order_id` (KHÔNG có FK để log
  còn nguyên khi xóa đơn), `kind` (CREATED/STATUS/PAYMENT), `from_value`, `to_value`, `reason`, `changed_by`,
  `changed_by_role`, `changed_at`; index `idx_osh_order`.
- **Cột mới `orders`**: `cancel_reason varchar(255)`, `cancelled_by varchar(20)`, `cancelled_by_name varchar(100)`,
  `cancelled_at datetime`, `stock_reserved bit NOT NULL DEFAULT 0` (đơn cũ = 0 ⇒ chuyển trạng thái không đụng kho).
- **Cột mới `order_items`**: `variant_id bigint NULL` + FK `fk_order_item_variant → product_variants(variant_id)`.
- **Mở rộng ENUM** (ddl-auto=update KHÔNG tự làm; do `config/OrderFlowMigrationRunner`, idempotent, chạy sau
  `VariantBackfillRunner`, giữ nguyên COMMENT cột):
  - `orders.order_status`: `pending, processing, confirmed, packing, shipping, delivered, completed, canceled, refunded`
  - `orders.payment_status`: `pending, paid, failed, refund_pending, refunded`
  - `orders.payment_method`: thêm `TRANSFER`
- **Migrate dữ liệu** (không mất dữ liệu): 1 đơn `processing` → `confirmed`; 11/11 `order_items` cũ được gắn biến thể
  mặc định của sản phẩm (đã kiểm tra: `variant_id IS NULL` = 0).
- **SQL tương đương đã chạy (do runner thực thi)**:
  ```sql
  ALTER TABLE orders MODIFY COLUMN order_status enum('pending','processing','confirmed','packing','shipping','delivered','completed','canceled','refunded') NOT NULL DEFAULT 'pending' COMMENT 'TT đơn hàng';
  ALTER TABLE orders MODIFY COLUMN payment_status enum('pending','paid','failed','refund_pending','refunded') NOT NULL DEFAULT 'pending' COMMENT 'TT thanh toán';
  ALTER TABLE orders MODIFY COLUMN payment_method enum('COD','VNPay','CreditCard','TRANSFER') NOT NULL COMMENT 'PT thanh toán';
  UPDATE orders SET order_status='confirmed' WHERE order_status='processing';
  UPDATE order_items oi JOIN product_variants v ON v.product_id=oi.product_id AND v.is_default=1 SET oi.variant_id=v.variant_id WHERE oi.variant_id IS NULL;
  ```
- **Rollback**: restore file backup trên (khuyến nghị). Thủ công: `DROP TABLE order_status_history;`
  `ALTER TABLE order_items DROP FOREIGN KEY fk_order_item_variant, DROP COLUMN variant_id;`
  `ALTER TABLE orders DROP COLUMN cancel_reason, DROP COLUMN cancelled_by, DROP COLUMN cancelled_by_name, DROP COLUMN cancelled_at, DROP COLUMN stock_reserved;`
  và trả ENUM về giá trị cũ (chỉ được sau khi đưa các đơn có trạng thái mới về giá trị cũ, vì MySQL từ chối thu hẹp ENUM
  nếu còn dòng dùng giá trị bị bỏ). Gỡ luôn code nếu rollback DB.
- **Cột deprecated**: giá trị enum `processing` giữ lại (đánh dấu `@Deprecated`), không xóa.

## File đã thay đổi
- Mới: `common/OrderWorkflow.java` (máy trạng thái + quyền theo bước + nhãn), `common/OrderActor.java`,
  `config/OrderFlowMigrationRunner.java`, `entity/OrderStatusHistoryEntity.java`,
  `repository/OrderStatusHistoryRepository.java`, `enums/OrderHistoryKind.java`, `test/.../OrderFlowTest.java`.
- Sửa: `enums/OrderStatus.java`, `enums/PaymentStatus.java`, `enums/PaymentMethod.java`, `entity/OrderEntity.java`,
  `entity/OrderItemEntity.java`, `repository/OrderRepository.java` (lọc theo trạng thái),
  `service/OrderService.java`, `service/impl/OrderServiceImpl.java` (viết lại phần đơn hàng; voucher giữ nguyên),
  `controller/OrderController.java` (thêm `GET /api/{id}`, `PUT /api/{id}/status`, `PUT /api/{id}/payment`),
  `controller/ProductCustomerController.java` (`POST /api/orders/{id}/cancel`), `config/RoleInterceptor.java`
  (warehouse_staff vào `/admin/orders`), `config/VariantBackfillRunner.java` (`@Order(1)`),
  `templates/admin/orders.html`, `static/js/orderCustomer.js`.

## Cách test
1. Tự động: `.\mvnw.cmd test -Dtest=OrderFlowTest` (18 test, rollback sau mỗi test): giữ/nhả/xuất/nhập kho, không nhảy cóc,
   phân quyền từng bước, lý do bắt buộc, hủy khi đang giao, hoàn tiền, đơn online phải paid mới đóng gói, khách tự hủy
   (đúng chủ/sai chủ/chưa đăng nhập/đã đóng gói), đơn cũ không đụng kho, `processing`≡`confirmed`, API cũ bị chặn nhảy cóc,
   product_staff bị chặn, xóa đơn nhả hàng, giá bị hạ / tổng bị đội / hết hàng / sản phẩm ngừng bán bị từ chối.
2. Thủ công: đặt hàng ở storefront (COD) → vào Admin → Đơn hàng → Chi tiết → bấm lần lượt Xác nhận → Đóng gói → Đang giao
   → Đã giao → Hoàn thành; xem Tồn kho → Lịch sử thấy ORDER_RESERVE/ORDER_SHIPPED. Thử hủy không nhập lý do (bị chặn). Vào
   "Đơn hàng của tôi" thấy nhãn tiếng Việt và nút Hủy khi đơn còn chờ/đã xác nhận.

## Việc còn thiếu
- **Voucher chưa hoàn khi hủy đơn**, và tổng tiền sau voucher vẫn do client tính (server chỉ chặn tổng vượt mức tối đa,
  không chặn tổng thấp bất thường do voucher). Cần chuyển toàn bộ tính giá + voucher về server (đã liệt kê ở audit, chờ duyệt).
- **Chưa cổng thanh toán thật**: `paid` do nhân viên xác nhận thủ công; hoàn tiền `refund_pending → refunded` cũng thủ công.
- Chưa có mã vận đơn/đơn vị vận chuyển, chưa gửi email/thông báo đổi trạng thái cho khách.
- Storefront vẫn chỉ bán biến thể mặc định (kế thừa từ Module 2).
- Đơn cũ (trước module này) không được giữ hàng nên chuyển trạng thái không ảnh hưởng kho — cố ý, tránh sai lệch số liệu.
- Race condition khi hai khách tranh hàng cuối cùng: chặn bằng kiểm tra + rollback nhưng chưa dùng khóa bi quan/`@Version`
  trên `inventories` (nên bổ sung ở Module 5/khi tải cao).
- Quyền chi tiết (dashboard, warehouse xem đơn nào…) sẽ rà tiếp ở Module 5.
