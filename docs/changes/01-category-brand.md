# 01 — Category / Brand

## Thay đổi
Thêm danh mục sản phẩm **đa cấp** (cây cha-con, mỗi danh mục có thể có danh mục cha) với CRUD đầy đủ
tại `/admin/categories`, và cho phép gán 1 danh mục cho mỗi sản phẩm (tùy chọn) ngay trong form
thêm/sửa sản phẩm. Thương hiệu (Brand) và loại máy (`WatchType`) giữ nguyên vì đã đáp ứng yêu cầu.

## Lý do
Audit `00-audit.md` — checklist 1.1 yêu cầu "Danh mục đa cấp + thương hiệu + loại máy". Hiện trạng:
Brand đã có (kèm logo), `WatchType` thực chất đã là "loại máy" (Cơ/Automatic, Điện tử, Pin/Quartz),
nhưng chưa có danh mục đa cấp. Đây là nền tảng cho module Product/variant ở bước sau.

## Thay đổi Database
Do `spring.jpa.hibernate.ddl-auto=update`, Hibernate tự tạo khi app khởi động. SQL tương đương:
```sql
CREATE TABLE categories (
  category_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  parent_id INT NULL,
  description TEXT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(category_id)
);
ALTER TABLE products ADD COLUMN category_id INT NULL,
  ADD CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(category_id);
```
- Cột `products.category_id` **nullable**: 20 sản phẩm hiện có giữ nguyên (đã kiểm tra sau migrate:
  20/20 sản phẩm còn nguyên, `category_id` = NULL, chưa mất/sai dữ liệu nào). Chưa có gì để backfill.
- Backup trước khi migrate: `db-backups/shopdongho_before_module1_20260919_104124.sql`
  (lệnh: `mysqldump -u root -p --routines --triggers --events shopdongho > ...`, thư mục này đã `.gitignore`).
- **Rollback**: `ALTER TABLE products DROP FOREIGN KEY fk_products_category, DROP COLUMN category_id;
  DROP TABLE categories;` hoặc restore từ file backup ở trên.
- **Rủi ro `ddl-auto=update`**: Hibernate chỉ thêm bảng/cột, không bao giờ xóa/đổi kiểu cột cũ, nên an toàn
  cho thay đổi này; nhưng không có lịch sử migration và thứ tự chạy không kiểm soát được khi có nhiều
  môi trường. Chưa chuyển sang Flyway (thay đổi ảnh hưởng toàn bộ setup — cần bạn duyệt trước).

## File đã thay đổi
- Mới: `entity/CategoryEntity`, `repository/CategoryRepository`, `dto/CategoryDto`,
  `service/CategoryService`, `service/impl/CategoryServiceImpl`, `controller/CategoryController`,
  `templates/admin/categories/category.html`, `static/js/category.js`,
  `test/.../CategoryModuleTest`.
- Sửa: `entity/ProductEntity` (+`category`), `repository/ProductRepository` (+`countByCategory_CategoryId`),
  `dto/ProductDto` (+`categoryId`, `categoryName` dạng setter, không đổi constructor),
  `service/ProductService` + `ProductServiceImpl` (nhận `CategoryEntity`, trả danh mục trong DTO và
  `form-options`), `controller/ProductController` (param `categoryId` tùy chọn),
  `templates/admin/products.html` + `static/js/product.js` (dropdown Category),
  `templates/layout/admin.html` (link sidebar "Danh mục sản phẩm").
- Phân quyền: `/admin/categories/**` đã thuộc rule admin-only sẵn có trong `RoleInterceptor`, không cần sửa.

## Quy tắc nghiệp vụ đã cài đặt
- Tên danh mục không trùng trong cùng cấp cha.
- Không cho chọn chính nó hoặc 1 danh mục con/cháu của nó làm cha (chống vòng lặp).
- Không xóa được danh mục còn danh mục con hoặc còn sản phẩm đang gắn.

## Cách test
1. Tự động: `CategoryModuleTest` (4 test, chạy trong transaction rồi rollback): trang render, non-admin bị
   chặn, tạo/cây/trùng tên/vòng lặp/xóa có ràng buộc, `form-options` có danh mục. Kết quả: 4/4 pass.
2. Thủ công: đăng nhập admin → sidebar "Thuộc tính" → "Danh mục sản phẩm" → thêm danh mục gốc, thêm
   danh mục con chọn cha → xem cây → vào Sản phẩm → sửa 1 sản phẩm chọn Category → lưu → mở lại thấy đúng.

## Việc còn thiếu
- Storefront chưa lọc/hiển thị theo danh mục (ngoài phạm vi admin của task này).
- Sản phẩm cũ chưa được gán danh mục (không có dữ liệu để tự suy ra — admin gán dần qua form).
- Brand chưa có trạng thái ẩn/hiện; chưa có yêu cầu cụ thể nên chưa làm.
