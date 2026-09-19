# 02 — Product + biến thể + tồn kho

## Thay đổi
Sản phẩm giờ có **nhiều biến thể** (model/màu/dây/size), mỗi biến thể có SKU, giá, tồn kho và ngưỡng cảnh báo
riêng; sản phẩm có **trạng thái vòng đời** (DRAFT/ACTIVE/INACTIVE/DISCONTINUED) và chỉ ACTIVE mới hiện/bán ở
storefront. Tồn kho tách rõ **tồn thực / đang giữ (reserved) / bán được**, mọi biến động đều ghi vào
**lịch sử kho** (ai, khi nào, trước/sau). Thêm **import/export CSV** và **cập nhật hàng loạt** giá/trạng thái.

## Lý do
Giải quyết GAP-1, GAP-2, GAP-3, GAP-4 trong `00-audit.md`. Ghi chú đính chính audit: phiếu nhập
(`PurchaseEntriesServiceImpl`) **đã** cộng tồn kho từ trước (audit ghi nhầm là không); phần thiếu thật sự là
lịch sử biến động — nay đã có.

## Thay đổi Database
Hibernate (`ddl-auto=update`) tạo cấu trúc mới khi khởi động; dữ liệu cũ được chuyển bởi
`config/VariantBackfillRunner` (idempotent, chạy mỗi lần khởi động, chạy lại không đổi gì). SQL tương đương:
```sql
CREATE TABLE product_variants (
  variant_id BIGINT AUTO_INCREMENT PRIMARY KEY, product_id VARCHAR(250) NOT NULL,
  sku VARCHAR(60) NOT NULL UNIQUE, variant_name VARCHAR(150) NOT NULL,
  color VARCHAR(50), strap_option VARCHAR(50), case_size VARCHAR(50),
  price DECIMAL(12,2) NOT NULL, is_default BIT NOT NULL,
  status ENUM('ACTIVE','DISCONTINUED','DRAFT','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES products(product_id));
CREATE TABLE inventory_movements (
  movement_id BIGINT AUTO_INCREMENT PRIMARY KEY, variant_id BIGINT, product_id VARCHAR(250),
  movement_type ENUM('INITIAL','PURCHASE_IN','PURCHASE_REVERSAL','ADJUSTMENT','ORDER_RESERVE',
    'ORDER_RELEASE','ORDER_SHIPPED','ORDER_RETURN') NOT NULL,
  quantity_change INT NOT NULL, quantity_before INT NOT NULL, quantity_after INT NOT NULL,
  reference VARCHAR(100), note VARCHAR(255), created_by VARCHAR(100), created_at DATETIME(6) NOT NULL,
  INDEX idx_movement_variant (variant_id), INDEX idx_movement_product (product_id));
ALTER TABLE products ADD COLUMN status ENUM('ACTIVE','DISCONTINUED','DRAFT','INACTIVE') NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE inventories ADD COLUMN variant_id BIGINT NULL, ADD COLUMN reserved_quantity INT NOT NULL DEFAULT 0,
  ADD COLUMN low_stock_threshold INT NOT NULL DEFAULT 5,
  ADD CONSTRAINT fk_inventory_variant FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id);
ALTER TABLE product_images ADD COLUMN variant_id BIGINT NULL,
  ADD CONSTRAINT fk_image_variant FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id);
-- Runner: bỏ unique index ux_inventory_product (1 sản phẩm nhiều dòng tồn kho), thay bằng idx_inventories_product
```
Migrate dữ liệu cũ (kết quả trên DB dev, đã kiểm tra bằng SQL sau khi chạy): 20 sản phẩm → 20 biến thể mặc định
(sku/giá = của sản phẩm), 20 dòng tồn kho (2 dòng cũ được gắn vào biến thể mặc định, 18 dòng mới với tồn 0),
2 dòng lịch sử `INITIAL` cho số dư đầu kỳ; toàn bộ 20 sản phẩm giữ trạng thái ACTIVE (không sản phẩm nào bị ẩn);
không có lệch sku/giá giữa sản phẩm và biến thể mặc định; đủ khóa ngoại.

**Hai điều chỉnh dữ liệu cần lưu ý**
1. **Collation**: DB mặc định `utf8mb4_unicode_ci` khiến bảng Hibernate tạo mới khác các bảng cũ
   (`utf8mb4_0900_ai_ci`) → JOIN theo `product_id` lỗi "Illegal mix of collations" và khóa ngoại không tạo được.
   Đã chạy: `ALTER DATABASE shopdongho COLLATE utf8mb4_0900_ai_ci;` và
   `ALTER TABLE categories|product_variants|inventory_movements CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`.
   **Môi trường khác phải chạy đúng các lệnh này TRƯỚC lần chạy đầu với code mới** (hoặc tạo DB mới với collation này).
2. **Chuẩn hoá SKU**: 4 SKU cũ dính khoảng trắng đầu/cuối (vd `"Casio AQ-230A-7DMQ "`) đã được `trim` (cả sản phẩm
   và biến thể mặc định) để tra cứu/import theo SKU khớp; tạo/sửa sản phẩm từ nay tự trim.

- Backup trước module: `db-backups/shopdongho_before_module2_20260919_105448.sql`
  (`mysqldump -u root -p --routines --triggers --events shopdongho > ...`).
- **Rollback**: restore file backup trên (khuyến nghị). Rollback thủ công: bỏ cột/bảng mới
  (`DROP TABLE inventory_movements, product_variants` sau khi `DROP FOREIGN KEY fk_inventory_variant, fk_image_variant`
  và `DROP COLUMN variant_id/reserved_quantity/low_stock_threshold` ở `inventories`, `variant_id` ở `product_images`,
  `status` ở `products`) rồi `ALTER TABLE inventories DROP INDEX idx_inventories_product, ADD UNIQUE KEY ux_inventory_product (product_id);`.
- **Cột deprecated (chưa xóa)**: `products.price` và `products.sku` vẫn được giữ và luôn đồng bộ với biến thể mặc
  định vì storefront/giỏ hàng/đơn hàng hiện đọc từ đó. Chỉ xóa sau khi storefront chuyển hẳn sang biến thể.
- **Rủi ro `ddl-auto=update`**: chỉ thêm, không sửa/xóa cột cũ, nên an toàn cho thay đổi này; nhưng không kiểm soát
  được thứ tự/lịch sử migration và lệ thuộc collation mặc định của DB (như sự cố trên). Đề xuất chuyển Flyway cho
  các module lớn tiếp theo — cần bạn duyệt trước, chưa tự làm.

## File đã thay đổi
- Mới: `enums/ProductStatus`, `enums/MovementType`, `entity/ProductVariantEntity`, `entity/InventoryMovementEntity`,
  `repository/ProductVariantRepository`, `repository/InventoryMovementRepository`, `dto/VariantDto`,
  `service/{StockService,ProductVariantService,ProductBulkService}` + `impl`, `common/CurrentActor`,
  `config/VariantBackfillRunner`, `controller/{ProductVariantController,ProductBulkController}`,
  `static/js/product-variants.js`, test `ProductVariantInventoryTest`, `ProductBulkTest`.
- Sửa: `ProductEntity` (+`status`, +`availableQuantity` bằng `@Formula`, bỏ quan hệ 1-1 `inventory`),
  `InventoryEntity` (+variant, reserved, ngưỡng; product thành nhiều-1), `ProductImageEntity` (+variant),
  `InventoryRepository` (default-variant, tồn thấp theo ngưỡng riêng), `ProductRepository` (mọi query storefront lọc
  `ACTIVE`), `ProductService/Impl` (status, tạo biến thể mặc định+tồn kho khi tạo sản phẩm, đồng bộ giá/sku khi sửa,
  chặn xóa sản phẩm đã có trong đơn), `ProductCustomerServiceImpl` (chỉ ACTIVE, số lượng = bán được),
  `InventoryServiceImpl/Controller/Service/Dto` (theo biến thể, reserved, lịch sử, sửa lỗi lọc ngày dùng cột không
  tồn tại), `PurchaseEntriesServiceImpl` (mọi thay đổi kho qua `StockService`), `ProductController/Dto`,
  `LoginAdminController` (lưu `username` vào session — header admin trước đây hiển thị trống),
  `templates/admin/products.html`, `templates/admin/inventory.html`, `static/js/product.js`, `static/js/inventory.js`.
- Phân quyền: endpoint mới nằm dưới `/admin/products` và `/admin/inventory` nên dùng chung rule sẵn có
  (product_staff/admin và warehouse_staff/admin) — chưa thay đổi ma trận quyền (để Module 5).

## Quy tắc nghiệp vụ đã cài đặt
- Mỗi sản phẩm luôn có đúng 1 biến thể mặc định; giá + SKU sản phẩm = của biến thể mặc định (đồng bộ 2 chiều
  qua form sản phẩm, form biến thể, import CSV, đổi giá hàng loạt).
- SKU biến thể duy nhất toàn hệ thống và không trùng SKU sản phẩm khác. Không xóa biến thể mặc định; không xóa biến
  thể còn tồn/đang giữ; biến thể mặc định phải ACTIVE.
- Tồn thực không được xuống thấp hơn số đang giữ; không âm. Mọi thay đổi ghi lịch sử với `created_by` lấy từ session.
- Chỉ ACTIVE hiển thị ở storefront (trang chủ, khuyến mãi, nổi bật, tìm kiếm, gợi ý, chi tiết, danh sách).
- Import CSV (cột `sku` + `price`/`status`/`quantity`) là **tất cả hoặc không gì cả**: có 1 dòng sai thì không dòng
  nào được áp dụng và trả danh sách lỗi theo số dòng. Đổi giá hàng loạt (PERCENT/AMOUNT/SET) từ chối cả lô nếu có giá ≤ 0.
- Sản phẩm đã có trong đơn hàng không xóa được (gợi ý dùng DISCONTINUED).

## Cách test
1. Tự động: `mvn clean test` → **18/18 pass**. Trong đó `ProductVariantInventoryTest` (6: tạo sản phẩm sinh biến thể
   mặc định+tồn kho, trạng thái quyết định hiển thị storefront, vòng đời biến thể + lịch sử + tồn thấp + ràng buộc xóa,
   reserved chặn giảm tồn và trừ vào số bán được, phiếu nhập cộng kho + ghi lịch sử, trang render) và
   `ProductBulkTest` (4: export, import cập nhật giá/trạng thái/tồn theo SKU, import all-or-nothing, đổi giá %/trạng
   thái hàng loạt + từ chối giá âm). Các test chạy trong transaction và rollback.
2. Thủ công (admin): Sản phẩm → nút "Biến thể" (biểu tượng layers) để thêm/sửa/đặt mặc định/xóa biến thể, gán ảnh
   riêng; đổi Status trong form sản phẩm; tick nhiều sản phẩm → đổi trạng thái/giá; Export CSV, sửa file rồi Import.
   Tồn kho → thấy từng biến thể kèm giữ/bán được/ngưỡng, nút Sửa (kiểm kê + ghi chú), Lịch sử.

## Việc còn thiếu
- **Storefront chưa cho khách chọn biến thể**: vẫn bán biến thể mặc định (giỏ hàng/checkout làm việc ở mức sản phẩm).
  Biến thể phụ hiện chỉ quản lý ở admin (cần thiết kế lại trang chi tiết + giỏ hàng + `order_items.variant_id`).
- Cơ chế **giữ hàng khi tạo đơn / xuất kho khi giao / nhả khi hủy** đã có sẵn ở `StockService` (reserve/release/ship/restock)
  nhưng chưa được nối vào luồng đơn hàng — làm ở Module 3. Hiện `reserved_quantity` luôn 0.
- Phiếu nhập hàng vẫn ở mức sản phẩm (cộng vào biến thể mặc định), chưa chọn biến thể.
- UI admin ở mức dùng được (chưa trau chuốt); chưa import CSV tạo sản phẩm mới (chỉ cập nhật theo SKU đã có).
