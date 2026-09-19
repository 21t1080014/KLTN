# 00 — Audit hiện trạng & Gap so với chuẩn thương mại

## Chuẩn bị
- Xác nhận project: `D:\DuAnCN\CNV\KLTN`, Spring Boot 3.4.4, Maven, MySQL, Thymeleaf.
- Backup DB trước khi bắt đầu: `db-backups/shopdongho_backup_20260919_012249.sql`
  (lệnh: `mysqldump -u root -p --routines --triggers --events shopdongho > ...`).
  File này **không commit git** (đã thêm `db-backups/` vào `.gitignore` vì có thể chứa dữ liệu thật).
- Git branch riêng cho toàn bộ task: `feature/admin-commerce-overhaul` (tách từ `master`, sau khi đã
  commit dứt điểm 2 việc trước đó — audit bảo mật/cấu trúc và redesign Tailwind — vào `master`).
- Dữ liệu dev hiện tại (tham khảo để đánh giá rủi ro migrate): 20 sản phẩm, 9 đơn hàng, 9 user
  (2 admin, 2 product_staff, 5 customer — chưa có support_staff/warehouse_staff nào để test).

## Hiện trạng Entity chính
- **ProductEntity**: 1 sản phẩm = 1 giá (`price`), 1 dòng tồn kho (`InventoryEntity` quan hệ 1-1),
  không có biến thể (màu/dây/size), không có trạng thái sống (draft/active/inactive/discontinued) —
  chỉ có `ProductCondition` (tình trạng máy: Mới 100%/Like New/Đã qua sử dụng, khác nghĩa "trạng thái
  đăng bán"). `product_id` là UUID string.
- **InventoryEntity**: chỉ có `quantity`, không có ngưỡng cảnh báo tồn thấp, không có "reserved"
  (số lượng đang giữ cho đơn pending), không có lịch sử thay đổi (ai/khi nào/lý do).
- **PurchaseEntriesEntity**: có ghi nhận nhập hàng (product, quantity, importPrice, importedAt, note)
  — đây là lịch sử NHẬP, nhưng không có lịch sử XUẤT/điều chỉnh tồn kho, và nhập hàng không tự động
  cộng vào `InventoryEntity.quantity` (cần xác nhận lại ở module Product).
- **BrandEntity / WatchTypeEntity / CaseMaterialEntity / StrapMaterialEntity / GlassMaterialEntity**:
  đều là danh mục 1 cấp (chỉ `id` + `name`), không có danh mục đa cấp (category tree). `WatchTypeEntity`
  thực ra đang đóng vai trò "loại máy" (dữ liệu hiện có: Cơ/Automatic, Điện Tử, Pin/Quartz) — **đã đáp
  ứng đúng yêu cầu "loại máy"**, không phải gap như checklist gốc lo ngại (tên field gây hiểu nhầm).
- **ProductImageEntity**: đã có nhiều ảnh/sản phẩm + `sortOrder` — đáp ứng yêu cầu ảnh đa dạng, NHƯNG
  ảnh gắn theo `product_id` (không có khái niệm biến thể nên không thể có "ảnh riêng theo biến thể").
- **PromotionEntity + PromotionProductEntity**: đã có khuyến mãi theo thời gian (`startAt`/`endAt`),
  áp cho nhiều sản phẩm qua bảng n-n `promotion_products` — đáp ứng yêu cầu "giá gốc + giá khuyến mãi
  + thời gian áp dụng" ở mức sản phẩm (không phải biến thể).
- **OrderEntity**: `orderStatus` (pending/processing/shipping/completed/canceled) và `paymentStatus`
  (pending/paid/failed) là enum cột riêng, **không có validation chuyển trạng thái** (service hiện tại
  set thẳng bất kỳ status nào, có thể nhảy cóc PENDING→DELIVERED). Không có lý do hủy, không có người
  hủy, không có bảng lịch sử thay đổi trạng thái đơn.
- **UserEntity**: đã có `status` (ACTIVE/LOCKED/PENDING) — khóa tài khoản (soft) **đã có sẵn cơ chế**,
  chỉ chưa có màn quản lý khách hàng riêng (hiện `AdminUserController` gộp chung tất cả role vào 1 danh
  sách, không tách "khách hàng" ra xem lịch sử mua/tổng chi tiêu).
- **Dashboard hiện tại** (`DashboardServiceImpl`): tổng đơn, tổng doanh thu (all-time, không theo mốc
  thời gian), tổng tồn kho, số đơn theo từng status. Không có: doanh thu theo ngày/tuần/tháng, sản phẩm
  bán chạy, cảnh báo tồn thấp.

## Danh sách Controller/chức năng admin hiện có
| Controller | Route gốc | Chức năng |
|---|---|---|
| `DashboardController` | `/admin/dashboard` | Xem dashboard tổng quan |
| `ProductController` | `/admin/products` | CRUD sản phẩm (không biến thể), upload nhiều ảnh, search/filter/phân trang |
| `BrandController` | `/admin/brands` (thiếu `/` đầu, xem ghi chú bảo mật cũ) | CRUD thương hiệu + logo |
| `CaseMaterialController`, `StrapMaterialController`, `GlassMaterialController`, `WatchTypeController` | `/admin/categories/*` | CRUD danh mục 1 cấp (tên) |
| `InventoryController` | `/admin/inventory` | Xem/sửa số lượng tồn, xem tồn thấp (`/api/low-stock`), search theo tên+ngày+giá |
| `PurchaseEntryController` | `/admin/purchases` | Tạo/sửa/xóa phiếu nhập hàng |
| `PromotionController` | `/admin/promotions` | CRUD khuyến mãi + gán/gỡ sản phẩm vào khuyến mãi |
| `OrderController` | `/admin/orders` | Xem danh sách đơn, sửa status đơn + status thanh toán (không validate chuyển trạng thái), xóa đơn (chỉ khi pending+pending) |
| `VoucherController` | `/admin/voucher` | CRUD voucher (mã, % / số tiền giảm, đơn tối thiểu, thời hạn, usage limit) |
| `AdminUserController` | `/admin/users` | CRUD người dùng (mọi role trộn chung), đổi role/status/mật khẩu |
| `LoginAdminController` | `/admin/login` | Đăng nhập/đăng xuất admin |

## Cơ chế phân quyền hiện tại (`RoleInterceptor`)
Áp dụng cho toàn bộ `/admin/**` (trừ `/admin/login`), dựa trên session attribute `roleName`:

| Path prefix | Role được phép |
|---|---|
| `/admin/products` | admin, product_staff |
| `/admin/orders` | admin, support_staff |
| `/admin/inventory` | admin, warehouse_staff |
| `/admin/purchases` | admin, warehouse_staff |
| `/admin/users`, `/admin/brands`, `/admin/categories`, `/admin/promotions`, `/admin/voucher`, `/admin/reports` | admin only |
| `/admin/dashboard` | **bất kỳ role đã đăng nhập** (đã ghi nhận ở audit bảo mật trước, chưa sửa vì cần hỏi trước — xem file audit bảo mật cũ) |

→ Về cơ bản đã khớp với yêu cầu "product_staff chỉ đụng sản phẩm/tồn kho..." NGOẠI TRỪ: `product_staff`
hiện KHÔNG có quyền vào `/admin/inventory` (chỉ warehouse_staff+admin), và `warehouse_staff` không có
quyền vào `/admin/products`. Theo yêu cầu gốc "product_staff chỉ được đụng module sản phẩm/tồn kho" —
đây là 1 GAP cần sửa ở Phần 2.5 (rà soát lại phân quyền).

## GAP tổng hợp theo checklist (Phần 1), đã sắp ưu tiên

### 1.1 Sản phẩm — ưu tiên CAO
- [GAP-1] Không có biến thể (model/màu/dây/size), mỗi biến thể SKU/giá/tồn riêng.
- [GAP-2] Không có trạng thái sống sản phẩm (draft/active/inactive/discontinued).
- [GAP-3] Tồn kho: thiếu ngưỡng cảnh báo (đã có API low-stock nhưng ngưỡng hard-code trong query, cần
  xác nhận), thiếu "reserved quantity" cho đơn pending, thiếu lịch sử thay đổi tồn kho có người/thời điểm.
- [GAP-4] Không có import/export CSV, không có bulk update giá/trạng thái.
- Đã có (không phải gap): nhiều ảnh + sort order, giá gốc + khuyến mãi theo thời gian, danh mục
  thương hiệu/loại máy 1 cấp.

### 1.2 Đơn hàng — ưu tiên CAO
- [GAP-5] Không validate luồng chuyển trạng thái đơn (cho phép nhảy cóc).
- [GAP-6] Không có lý do hủy đơn, không ghi người hủy (khách/admin).
- [GAP-7] Không liên kết hủy đơn đã thanh toán với luồng hoàn tiền (paymentStatus không tự chuyển
  sang trạng thái hoàn tiền).
- [GAP-8] Không có audit trail lịch sử thay đổi trạng thái đơn.

### 1.3 Khách hàng — ưu tiên TRUNG BÌNH
- [GAP-9] Không có màn riêng xem lịch sử mua hàng theo từng khách (hiện phải tự tra `/admin/orders`
  lọc theo user, không tiện).
- Đã có (không phải gap): khóa/mở tài khoản soft qua `UserStatus.LOCKED` — chỉ cần UI rõ hơn.
- [GAP-10] Không có phân loại khách theo tổng chi tiêu.

### 1.4 Báo cáo/Dashboard — ưu tiên THẤP (làm cuối, phụ thuộc module trên)
- [GAP-11] Doanh thu chỉ có tổng all-time, không theo ngày/tuần/tháng.
- [GAP-12] Không có top sản phẩm bán chạy.
- [GAP-13] Cảnh báo tồn thấp đã có ở module Inventory nhưng chưa lên Dashboard.

### 1.5 Phân quyền — ưu tiên CAO (làm ngay sau khi các module core xong)
- [GAP-14] `product_staff` thiếu quyền `/admin/inventory` theo đúng mô tả gốc "sản phẩm/tồn kho".
- [GAP-15] `/admin/dashboard` mở cho mọi role (đã ghi nhận từ trước, cần quyết định lại khi có thêm
  dữ liệu nhạy cảm hơn ở dashboard mới).
- [GAP-16] Endpoint mới ở các module sau (biến thể, lịch sử tồn kho, audit trail đơn hàng) phải được
  đưa vào `RoleInterceptor` ngay khi tạo, không để lọt.

## Kế hoạch triển khai (bám theo Phần 2 của yêu cầu)
1. Category/brand — nền tảng (đánh giá: hiện đã tương đối ổn, chỉ cần bổ sung nếu cần cho biến thể)
2. Product + variant + tồn kho (GAP-1, GAP-2, GAP-3)
3. Order flow + trạng thái đơn (GAP-5, GAP-6, GAP-7, GAP-8)
4. Customer management (GAP-9, GAP-10)
5. Rà soát phân quyền (GAP-14, GAP-15, GAP-16)
6. Report/dashboard (GAP-11, GAP-12, GAP-13)

Từng module sẽ có file riêng `docs/changes/<n>-<ten-module>.md` theo format bắt buộc, kèm backup DB
trước mỗi lần ALTER và commit git riêng sau khi module chạy ổn.
