# Nhật ký redesign frontend (Tailwind — tối giản sang trọng)

File này ghi lại TOÀN BỘ thay đổi trong quá trình redesign, cập nhật liên tục theo từng trang.

## Quyết định kỹ thuật nền tảng

### 1. Bootstrap CSS vs JS
- **Giữ**: `bootstrap.bundle.min.js` (CDN) + `bootstrap-icons` — dùng cho hành vi modal, carousel,
  offcanvas, dropdown, collapse (Quick View, giỏ hàng drawer, sidebar mobile admin, dropdown user...).
  Các component này chỉ cần JS toggle class (`show`, `fade`, `active`...), không cần Bootstrap CSS
  để hoạt động về mặt hành vi.
- **Bỏ**: `bootstrap.min.css` (CDN) — không load nữa. Toàn bộ phần nhìn (spacing, màu, bố cục,
  typography) chuyển sang Tailwind utility classes + `@layer components` trong `input.css`.
- **Vì sao**: tránh xung đột reset CSS giữa 2 framework, đạt giao diện đồng bộ triệt để theo tinh
  thần tối giản. Đổi lại: phải tự viết lại CSS định vị/animation cho `.modal`, `.offcanvas`,
  `.carousel` bằng Tailwind (xem phần "Component JS-driven" dưới đây).
- Tailwind `corePlugins.preflight = false` trong `tailwind.config.js` để không reset trùng những gì
  Bootstrap JS mong đợi có sẵn (ví dụ box-sizing đã ổn định).

### 2. Cấu trúc thư mục templates/ (tổ chức lại)
- Xem mục "Cấu trúc thư mục" cập nhật dần dưới đây — mỗi lần di chuyển 1 file, ghi rõ:
  route Controller nào bị đổi return-view-string, từ gì → thành gì.

### 3. Design system
- Màu: `ivory` (nền), `charcoal`/`charcoal-soft`/`charcoal-muted` (chữ), `gold`/`gold-dark`/`gold-light`
  (accent CTA, dùng tiết chế), `line` (viền/chia khối), `sale` (giá giảm, đỏ oxblood trầm hơn đỏ tươi
  cũ `#e60023`).
- Font: `font-heading` = Playfair Display (serif, tiêu đề/giá), `font-body` = Inter (nội dung).
  Nạp qua Google Fonts `<link>` trong layout dùng chung (xem phần layout).
- Spacing: ưu tiên `py-16`/`py-24` cho section lớn, dùng thang spacing mặc định Tailwind.
- Component dùng chung khai báo tại `src/main/resources/static/css/input.css` (`@layer components`):
  `.btn-primary`, `.btn-secondary`, `.btn-icon`, `.product-card`, `.badge-sale`, `.price-current`,
  `.price-original`, `.section-heading`, `.input-field`, `.label-field`.

## Setup Tailwind
- Cài: `tailwindcss@3.4.19`, `postcss@8.5.28`, `autoprefixer@10.6.1` (devDependencies trong
  `package.json` mới tạo — trước đây project không có `package.json`).
- File nguồn: `src/main/resources/static/css/input.css`
- File output (Spring Boot serve trực tiếp, có commit git): `src/main/resources/static/css/output.css`
- `tailwind.config.js`: `content` quét `templates/**/*.html` + `static/js/**/*.js` (vì nhiều trang
  sinh HTML từ template string trong JS, ví dụ `product-utils.js`, `profileCustomer.js`).
- Script: `npm run css:dev` (watch), `npm run css:build` (minify, dùng trước khi deploy).
- `node_modules/` đã thêm vào `.gitignore`.

## Component JS-driven cần viết lại CSS bằng Tailwind (do bỏ Bootstrap CSS)
Toàn bộ đã có CSS thay thế trong `input.css` (`@layer components`), test qua server thật + MockMvc:

| Component | Dùng ở | Trạng thái |
|---|---|---|
| `.modal` (Quick View, xác nhận checkout, verify OTP, mọi modal admin CRUD) | Tất cả trang storefront + admin | ✅ xong |
| `.offcanvas` (cart drawer, sidebar mobile admin, bộ lọc nâng cao Inventory/Promotion/PurchaseEntry) | Tất cả trang storefront (trừ cart/checkout) + layout admin | ✅ xong |
| `.carousel` (Quick View ảnh, Owl Carousel trang chủ) | home/index, product/collections, product/search-shop | ✅ xong |
| `.dropdown` (user menu navbar storefront + admin) | navbar mọi trang | ✅ xong |
| `.collapse` (navbar mobile toggle, submenu sidebar admin) | navbar + sidebar admin | ✅ xong |
| `.toast` (thông báo lỗi/thành công CRUD admin) | Product/Brand/Inventory/User/3 category còn lại | ✅ xong |

## Shim Bootstrap-class cho trang Admin (chiến lược riêng)
Vì 13 trang admin có nhiều form/modal CRUD phức tạp (Product, Promotion, PurchaseEntry...), để giảm
rủi ro sửa nhầm JS, chiến lược khác với storefront: **giữ nguyên hầu hết class Bootstrap gốc trong
HTML/JS** (`.btn.btn-success`, `.form-control`, `.table`, `.card`, `.alert-danger`, `.d-flex`,
`.justify-content-between`, `.col-md-6`, v.v.) và **định nghĩa lại CSS phía dưới các class đó** bằng
Tailwind trong `input.css`. Danh sách shim đã thêm (không đổi 1 dòng JS nào cho các class này):
- Button: `.btn`, `.btn-primary/success/danger/warning/secondary/light/dark`, `.btn-outline-*`, `.btn-sm`
- Form: `.form-control`, `.form-select`, `.form-label`, `.form-check`/`.form-check-input`/`.form-check-label`
- Bảng: `.table`, `.table-bordered`, `.table-hover`, `.table-light`, `.table-responsive`
- Card/alert: `.card`, `.card-header`, `.card-body`, `.alert`, `.alert-danger`, `.alert-success`
- Grid Bootstrap tối giản: `.row`, `.col`, `.col-auto`, `.col-6/12`, `.col-md-4/5/6/7/8/12`, `.col-lg-4/6`
- Utility layout cũ: `.d-flex/none/block/grid`, `.justify-content-*`, `.align-items-*`, `.flex-column`,
  `.text-end/start`, `.w-100/50/75`, `.float-end/start`, `.position-absolute/fixed/relative`
- List/pill/badge: `.list-group`, `.list-group-item`, `.badge`, `.rounded-pill`
- Khác: `.img-fluid`, `.overlay`, `.input-group`, `.border-end`, `.border-danger`, `.bg-primary/danger/light`

→ **Lợi ích**: 12/13 trang admin gần như giữ nguyên 100% markup gốc, chỉ đổi 1 dòng
`th:replace="~{layouts/layout :: layout}"` → `th:replace="~{layout/admin :: adminLayout}"` mỗi trang,
giảm mạnh rủi ro gãy JS so với viết lại toàn bộ markup bằng tay.

## Danh sách trang — tiến độ
> ⏳ chưa làm · 🔧 đang làm · ✅ xong + đã test

### Storefront
- ✅ Layout chung (navbar/footer/head fragment) — `templates/layout/storefront.html` (head+navbar+footer),
  `templates/layout/cart-drawer.html`, `templates/layout/quick-view-modal.html` (tách từ 3 bản copy-paste).
  File cũ `templates/layouts/navbar.html`, `templates/fragments/cart-drawer.html` VẪN GIỮ (chưa xoá) vì
  các trang chưa redesign còn tham chiếu — sẽ xoá sau khi xong tất cả trang storefront.
- ✅ Trang chủ (index) — đã test qua server thật (HTML render đúng, CSS 200).
  - Di chuyển: `templates/index.html` → `templates/home/index.html`
  - Controller: `ViewCustomerController.showHome()` — `return "index"` → `return "home/index"`
  - Thêm hero banner tĩnh (không cần data mới từ Controller) theo đúng gợi ý Phần 4.2 ("banner").
  - Sửa `product-utils.js` (renderProductCard dùng chung 3 trang): đổi từ Bootstrap class sang Tailwind,
    ảnh luôn vuông (aspect-square, object-contain), nút hành động LUÔN hiện trên mobile (trước đây chỉ
    hiện khi hover, gây khó dùng trên cảm ứng — cải thiện theo audit trước).
  - **Đã phát hiện và sửa 1 rủi ro gãy JS**: `home-products.js`, `collection.js`, `search-shop.js` đều có
    dòng `.find('.text-danger').text()` để lấy giá khi bấm "Thêm vào giỏ" — class này đã đổi tên thành
    `.price-current` trong thiết kế mới. Đã sửa cả 3 file để trỏ đúng class mới (nếu không sửa, nút
    "Thêm vào giỏ" trên card sản phẩm sẽ lưu giá rỗng vào localStorage).
  - Cũng sửa 3 file trên: toggle class `btn-dark`/`btn-secondary` (Bootstrap) cho nút "Thêm vào giỏ" trong
    Quick View khi hết hàng → đổi thành `btn-primary`/`btn-secondary` (class Tailwind mới).
- ✅ Danh sách sản phẩm (collections) — đã test qua server thật.
  - Di chuyển: `templates/collections.html` → `templates/product/collections.html`
  - Controller: `ViewCustomerController.collectionPage()` — `return "collections"` → `return "product/collections"`
  - Bỏ 2 link Owl Carousel CDN không dùng (trang này chưa từng gọi `owlCarousel()`, chỉ load thư viện dư thừa).
  - Sửa `collection.js`: đổi markup checkbox filter + tag đã chọn sang Tailwind (`accent-charcoal` cho
    checkbox, class `.badge` mới thêm ở `input.css`); GIỮ NGUYÊN quan hệ sibling input→label (JS dùng
    `$(this).next('label')`) nên không đổi cấu trúc, chỉ đổi class hiển thị.
  - Thêm `.page-item/.page-link` và `.badge` vào `input.css` (@layer components) — dùng chung cho
    pagination/tag do JS sinh ở nhiều trang (collection.js, search-shop.js, orderCustomer.js...), tránh
    phải sửa logic JS ở từng nơi.
- ✅ Tìm kiếm (search-shop) — đã test qua server thật.
  - Di chuyển: `templates/search-shop.html` → `templates/product/search-shop.html`
  - Controller: `ViewCustomerController.searchPage()` — `return "search-shop"` → `return "product/search-shop"`
- ✅ Chi tiết sản phẩm (detail-shop) — đã test qua server thật.
  - Di chuyển: `templates/detail-shop.html` → `templates/product/detail-shop.html`
  - Controller: `ViewCustomerController.detailPage()` — `return "detail-shop"` → `return "product/detail-shop"`
  - **Xoá** `static/css/detail-shop.css` (không còn trang nào link tới, toàn bộ style đã chuyển sang
    Tailwind trong HTML + `.thumb/.tab/.tab-content/.table-detail` thêm vào `input.css`).
  - Thêm class dùng chung mới vào `input.css`: `.text-success`/`.text-danger` (JS
    `detail-product.js` toggle 2 class này trực tiếp qua `classList` để báo còn/hết hàng — vẫn giữ đúng
    tên class, chỉ định nghĩa lại màu theo palette mới).
  - Đổi `.price-discounted`→`.price-current` (class này chỉ dùng cho CSS, JS chọn phần tử qua `#discount-price`
    theo ID nên đổi tên class an toàn, không ảnh hưởng JS).
- ✅ Giỏ hàng (cart) — đã test qua server thật.
  - Di chuyển: `templates/cart.html` → `templates/cart/cart.html`
  - Controller: `ViewCustomerController.cartPage()` — `return "cart"` → `return "cart/cart"`
  - Sửa `cartPage.js` (render item, nút +/-/xóa) sang Tailwind — GIỮ đúng class `.btn-minus/.btn-plus/.btn-delete`
    và `[data-index]` (event delegation không đổi).
- ✅ Checkout — đã test qua server thật.
  - Di chuyển: `templates/checkout.html` → `templates/checkout/checkout.html`
  - Controller: `ViewCustomerController.checkout()` — `return "checkout"` → `return "checkout/checkout"`
  - **Phát hiện + sửa 1 bug có sẵn**: trang checkout gốc KHÔNG load `bootstrap.bundle.min.js` dù
    `checkoutPage.js` gọi `new bootstrap.Modal(...)` để hiện modal xác nhận đặt hàng/lỗi — nghĩa là modal
    này chưa từng hoạt động trước đây. Đã thêm script tag còn thiếu (khớp pattern các trang khác).
- ✅ Đăng nhập/đăng ký (auth) — đã test qua server thật.
  - Di chuyển: `templates/auth.html` → `templates/account/auth.html`
  - Controller: `ViewCustomerController.showAuthPage()` — `return "auth"` → `return "account/auth"`
  - Xoá `static/css/auth.css` (không còn trang nào link tới).
  - Thêm `.toggle-btn`/`.toggle-btn.active` vào `input.css` (JS `auth.js` toggle class "active" trực tiếp).
  - **Không** thêm `id="loginFormElement"` cho form login (theo cảnh báo ở inventory) — giữ đúng hành vi
    hiện tại: submit form login bằng HTML thường tới `/login`.
- ✅ Tài khoản (profile) — đã test qua server thật.
  - Di chuyển: `templates/profile.html` → `templates/account/profile.html`
  - Controller: `ViewCustomerController.orderPage()` — `return "profile"` → `return "account/profile"`
  - Đổi tab nav từ `.nav-link` (Bootstrap) sang `.tab`/`.tab.active` (đã có sẵn CSS từ trang chi tiết sản
    phẩm) — sửa luôn script inline trong `profile.html` cho khớp selector mới.
  - Sửa markup do JS sinh: `profileCustomer.js` (card hồ sơ/avatar/form sửa), `orderCustomer.js` (bảng chi
    tiết đơn hàng), `voucherCustomer.js` (card voucher) — toàn bộ chuyển từ Bootstrap class sang Tailwind.

### Dọn dẹp sau khi storefront hoàn tất
- Xoá fragment cũ: `templates/layouts/navbar.html`, `templates/fragments/cart-drawer.html` (đã thay bằng
  `templates/layout/storefront.html` + `templates/layout/cart-drawer.html`).
- Xoá `templates/layouts/test.html` — xác nhận không Controller nào trả về view này (file rác).
- Xoá `static/css/shop.css`, `static/css/home.css` — không còn template nào tham chiếu sau khi toàn bộ
  storefront chuyển sang Tailwind (`output.css`).
- **Toàn bộ 8 trang storefront đã test qua server thật** (`curl` từng route, kiểm tra HTTP 200/302 đúng
  như kỳ vọng, không có exception/whitelabel error trong response).

### Admin
- ✅ Layout admin — `templates/layout/admin.html` (fragment `adminLayout`, thay `layouts/layout.html`).
  Bỏ Font Awesome (chỉ dùng ở đây, không trang admin nào khác dùng) → chuẩn hoá về Bootstrap Icons
  (đã load sẵn cho storefront). Navbar + sidebar (desktop tĩnh + offcanvas mobile) giữ nguyên logic
  phân quyền theo `session.roleName` (product_staff/support_staff/warehouse_staff/admin), redirect
  script khi role không hợp lệ giữ nguyên 100%.
  - **Sửa 1 vấn đề kỹ thuật phát hiện khi đọc code**: layout gốc không load jQuery (mỗi trang admin tự
    load jQuery riêng bên trong fragment "content", đặt SAU layout — đây là kiến trúc cố ý, đã giữ
    nguyên, không thêm jQuery trùng lặp ở layout).
  - Xoá `templates/layouts/sidebar.html` — xác nhận là **file rác không được dùng** (route trỏ tới
    `/products`, `/brands`... không có `/admin` prefix, không khớp Controller nào đang tồn tại).
- ✅ Login admin — `templates/admin/login.html` (không dùng layout chung, giữ nguyên như thiết kế gốc).
- ✅ Dashboard, Products, Brands, Inventory, Promotions, Purchase Entries, Orders, Vouchers, Users,
  4 trang category (CaseMaterial/StrapMaterial/WatchGlass/WatchType) — tất cả đã di chuyển vào
  `templates/admin/` (categories vào `templates/admin/categories/`), cập nhật return-view-string ở
  10 Controller tương ứng, dùng chiến lược shim ở trên.
  - **Phát hiện + sửa 1 rủi ro gãy chức năng khi tự rà soát**: `promotion.js` gọi
    `$('#promotionDetail').removeClass('d-none')` để hiện phần chi tiết khuyến mãi — lúc đầu tôi đổi
    nhầm class ban đầu từ `d-none` sang `hidden` (Tailwind), sẽ làm phần này **không bao giờ hiện ra
    được** vì JS gỡ đúng tên class `d-none`. Đã rà soát toàn bộ `static/js/*.js` tìm mọi chỗ dùng
    `d-none` và xác nhận đây là chỗ DUY NHẤT bị ảnh hưởng, sửa lại đúng.
  - Toast lỗi ở `Product.html` gốc bị cắt khi tôi đọc file (dòng cuối) — đoán nhầm
    `data-bs-dismiss="modal"` thay vì `"toast"` lúc viết lại, đã tự phát hiện qua so sánh với pattern
    thống nhất ở các trang khác và sửa lại.

### Kiểm thử tự động (mới thêm)
- Thêm `src/test/java/.../PageRenderingTest.java` — dùng `MockMvc` render THẬT (qua Thymeleaf thật,
  không mock) toàn bộ 6 trang storefront tiêu biểu + trang login admin + cả 13 trang admin (giả lập
  session `roleName=admin` để qua được `RoleInterceptor` mà không cần tài khoản thật trong DB, vì các
  trang admin chỉ trả về "vỏ" HTML, không gọi service tầng DB khi hiển thị). **3/3 test pass** — xác
  nhận không trang nào bị lỗi cú pháp Thymeleaf (`th:replace`/`th:insert`/`th:fragment`) sau khi đổi
  toàn bộ đường dẫn template. `mvn clean test` tổng: **4/4 pass**.

## Cấu trúc thư mục — thay đổi thực tế (đầy đủ)

| File cũ | File mới | Controller.method — return string cũ → mới |
|---|---|---|
| `templates/layouts/navbar.html` + `templates/fragments/cart-drawer.html` | `templates/layout/storefront.html` (fragment `head`/`navbar`/`footer`), `templates/layout/cart-drawer.html`, `templates/layout/quick-view-modal.html` (mới, gộp 3 bản copy-paste) | — (fragment dùng chung, không qua Controller) |
| `templates/layouts/layout.html` | `templates/layout/admin.html` (fragment `adminLayout`) | — |
| `templates/layouts/sidebar.html` | *(xoá — file rác không dùng)* | — |
| `templates/layouts/test.html` | *(xoá — file rác không dùng)* | — |
| `templates/index.html` | `templates/home/index.html` | `ViewCustomerController.showHome()` — `"index"` → `"home/index"` |
| `templates/collections.html` | `templates/product/collections.html` | `ViewCustomerController.collectionPage()` — `"collections"` → `"product/collections"` |
| `templates/search-shop.html` | `templates/product/search-shop.html` | `ViewCustomerController.searchPage()` — `"search-shop"` → `"product/search-shop"` |
| `templates/detail-shop.html` | `templates/product/detail-shop.html` | `ViewCustomerController.detailPage()` — `"detail-shop"` → `"product/detail-shop"` |
| `templates/cart.html` | `templates/cart/cart.html` | `ViewCustomerController.cartPage()` — `"cart"` → `"cart/cart"` |
| `templates/checkout.html` | `templates/checkout/checkout.html` | `ViewCustomerController.checkout()` — `"checkout"` → `"checkout/checkout"` |
| `templates/auth.html` | `templates/account/auth.html` | `ViewCustomerController.showAuthPage()` — `"auth"` → `"account/auth"` |
| `templates/profile.html` | `templates/account/profile.html` | `ViewCustomerController.orderPage()` — `"profile"` → `"account/profile"` |
| `templates/pages/LoginAdmin.html` | `templates/admin/login.html` | `LoginAdminController` (3 chỗ) — `"pages/LoginAdmin"` → `"admin/login"` |
| `templates/pages/dashboard.html` | `templates/admin/dashboard.html` | `DashboardController.showDashboard()` — `"pages/dashboard"` → `"admin/dashboard"` |
| `templates/pages/Product.html` | `templates/admin/products.html` | `ProductController.showProduct()` — `"pages/Product"` → `"admin/products"` |
| `templates/pages/Brand.html` | `templates/admin/brands.html` | `BrandController.showBrand()` — `"pages/Brand"` → `"admin/brands"` |
| `templates/pages/Inventory.html` | `templates/admin/inventory.html` | `InventoryController.showInventory()` — `"pages/Inventory"` → `"admin/inventory"` |
| `templates/pages/Promotion.html` | `templates/admin/promotions.html` | `PromotionController.showPromotion()` — `"pages/Promotion"` → `"admin/promotions"` |
| `templates/pages/PurchaseEntry.html` | `templates/admin/purchase-entries.html` | `PurchaseEntryController.showPurchaseEntry()` — `"pages/PurchaseEntry"` → `"admin/purchase-entries"` |
| `templates/pages/orders.html` | `templates/admin/orders.html` | `OrderController.showOrder()` — `"pages/orders"` → `"admin/orders"` |
| `templates/pages/voucher.html` | `templates/admin/vouchers.html` | `VoucherController.showVoucher()` — `"pages/voucher"` → `"admin/vouchers"` |
| `templates/pages/AdminUser.html` | `templates/admin/users.html` | `AdminUserController.showUser()` — `"pages/AdminUser"` → `"admin/users"` |
| `templates/pages/categories/CaseMaterial.html` | `templates/admin/categories/case-material.html` | `CaseMaterialController.showCaseMaterial()` — `"pages/categories/CaseMaterial"` → `"admin/categories/case-material"` |
| `templates/pages/categories/StrapMaterial.html` | `templates/admin/categories/strap-material.html` | `StrapMaterialController.showStrapMaterial()` — `"pages/categories/StrapMaterial"` → `"admin/categories/strap-material"` |
| `templates/pages/categories/WatchGlass.html` | `templates/admin/categories/watch-glass.html` | `GlassMaterialController.showGlassMaterial()` — `"pages/categories/WatchGlass"` → `"admin/categories/watch-glass"` |
| `templates/pages/categories/WatchType.html` | `templates/admin/categories/watch-type.html` | `WatchTypeController.showWatchType()` — `"pages/categories/WatchType"` → `"admin/categories/watch-type"` |
| `static/css/shop.css`, `static/css/home.css`, `static/css/auth.css`, `static/css/detail-shop.css` | *(xoá — không còn trang nào tham chiếu)* | — |
| *(mới)* | `static/css/input.css` (nguồn Tailwind), `static/css/output.css` (build ra, có commit) | — |

**Static (css/js/images)**: không tổ chức lại thư mục con theo tính năng như đề xuất ban đầu ở Phần 3
(ví dụ tách `js/cart.js`, `js/product-filter.js` theo từng thư mục con) — quyết định giữ nguyên cấu
trúc phẳng `static/js/*.js`, `static/css/*.css` hiện tại vì các file JS vốn đã được đặt tên theo tính
năng khá rõ ràng (`cartPage.js`, `product-utils.js`, `checkoutPage.js`...), việc di chuyển thêm không
mang lại lợi ích tương xứng với rủi ro (có thể sai đường dẫn `<script src="/js/...">` ở nhiều trang).
Nếu bạn vẫn muốn tổ chức lại, đây là việc có thể làm riêng sau, ít rủi ro hơn vì static file không qua
Controller.
