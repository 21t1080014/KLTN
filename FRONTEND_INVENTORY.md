# Frontend Inventory — checklist trước/sau khi redesign

> File này dùng để đối chiếu khi test lại từng trang sau khi redesign (Phần 7).
> Cột "Selector JS phụ thuộc" là những class/id KHÔNG được đổi tên khi redesign,
> trừ khi sửa luôn phần JS tương ứng trong cùng lần commit.

## Nhóm STOREFRONT (khách hàng) — layout dùng `layouts/navbar.html`

### Fragment dùng chung
| Fragment | Dùng ở | Selector JS phụ thuộc |
|---|---|---|
| `layouts/navbar.html` (`th:fragment="navbar"`) | Tất cả 8 trang storefront | `#searchToggle`, `#searchBox`, `#searchInput`, `#searchSuggestions`, `#cart-count`, `#cartDrawer` (data-bs-target), `#mainNavbar` (collapse) |
| `fragments/cart-drawer.html` (`th:fragment="cartDrawer"`) | index, collections, search-shop, detail-shop, auth, profile (KHÔNG có ở cart.html, checkout.html) | `#cartDrawer`, `#cart-content`, `#cart-total` |
| Quick View Modal (`#quickViewModal`) | **Copy-paste 3 lần**: index.html, collections.html, search-shop.html (không có ở detail-shop.html, vì trang detail có UI riêng) | `#qvCarousel`, `#qv-indicators`, `#qv-carousel-inner`, `#qv-name`, `#qv-sku`, `#qv-discounted`, `#qv-original`, `#qv-description`, `#qv-qty`, `#qv-add-to-cart`, `#qv-detail-link`, `window.incrementQty/decrementQty` (quick-view-cart.js) |

### 1. index.html (`/`) — Trang chủ
- JS: `home-products.js` (module), jQuery, Bootstrap bundle, Owl Carousel
- Chức năng: carousel khuyến mãi (Owl), grid "Hàng mới về", grid "Sản phẩm nổi bật" theo brand, Quick View modal, add-to-cart, search suggestions (qua navbar)
- Selector phụ thuộc: `#promotionProducts.owl-carousel`, `#newestProducts`, `#brandList`, `#featuredProducts`, dùng chung `.product-card/.card-overlay/.add-to-cart-btn/.quick-view-btn/.card-title/.badge-sale/.out-of-stock-overlay` từ `product-utils.js:renderProductCard()`
- **Quan trọng**: `renderProductCard()` trong `product-utils.js` là nguồn DUY NHẤT sinh HTML card sản phẩm, dùng lại ở index/collections/search-shop → sửa 1 chỗ, ảnh hưởng cả 3 trang (tốt cho đồng bộ, nhưng phải test cả 3 sau khi sửa).

### 2. collections.html (`/collections`) — Danh sách sản phẩm
- JS: `collection.js`, jQuery, Bootstrap, Owl Carousel (không dùng ở đây nhưng script vẫn load)
- Chức năng: filter (phân khúc/giới tính/thương hiệu/giá — render bằng JS, toggle bằng class `.toggle-filter`/`.toggle-icon`/`data-target`), sort, pagination, product grid, quick view
- Selector phụ thuộc: `#segment-filter/#gender-filter/#brand-filter/#price-filter`, `.toggle-filter[data-target]`, `#sort`, `#selected-tags`, `#newestProducts` (grid, tên id trùng index.html nhưng khác trang), `#pagination`

### 3. search-shop.html (`/search-shop?keyword=`) — Kết quả tìm kiếm
- JS: `search-shop.js`, jQuery, Bootstrap
- Chức năng: hiển thị kết quả theo keyword, pagination, quick view
- Selector: `#searchSummary`, `#searchResult`, `#pagination`

### 4. detail-shop.html (`/detail-shop/{productId}`) — Chi tiết sản phẩm
- JS: `detail-product.js`, jQuery, Bootstrap
- Chức năng: render thumbnail + ảnh chính, tabs (chỉ 1 tab hiện tại), tăng/giảm số lượng, add-to-cart, **buy-now** (mới nối lại ở lần sửa trước), badge giảm giá
- Selector: `body[data-product-id]`, `#thumbnail-images .thumb`, `#main-image`, `.badge-sale`, `#product-name-title`, `#brand-name`, `#origin`, `#warranty`, `#gender`, `#discount-price.price-discounted`, `#original-price.price-original`, `#quantity`, `#add-to-cart-btn`, `#buy-now-btn`, `.tab[data-tab]`, `.tab-content#detail`, `#description`, `#product-detail-table`

### 5. cart.html (`/cart`) — Giỏ hàng đầy đủ
- JS: `cartPage.js`, jQuery, Bootstrap. **KHÔNG có cart-drawer fragment, KHÔNG có navbar's cart-count auto update riêng (dùng chung qua cart-utils nếu cartPage.js import)**
- Chức năng: render list từ localStorage, +/-/xóa item, tính tổng
- Selector: `#cart-items` (chứa cả `<h4>Giỏ hàng:</h4>` — PHẢI giữ khi sửa empty state), `#cart-total`, `.btn-minus/.btn-plus/.btn-delete` (event delegation qua `[data-index]`)

### 6. checkout.html (`/checkout`) — Thanh toán
- JS: `checkoutPage.js`, jQuery (KHÔNG load bootstrap bundle riêng — dựa vào script đã cache/hoặc lỗi tiềm ẩn, cần kiểm tra khi test vì modal `#confirmationModal` cần bootstrap.bundle.js để chạy `new bootstrap.Modal`)
- Chức năng: chọn phương thức vận chuyển/thanh toán, áp voucher, đặt hàng, modal xác nhận
- Selector: `body[data-logged-in][data-user-id]`, `#ship1`, `#cod/#bank`, `#codDescription/#bankDescription`, `#order-summary`, `#coupon` + nút kế bên (`.nextElementSibling`), `#subtotal`, `#shipping-fee`, `#total-amount`, `#place-order-btn`, `#confirmationModal/#confirmationModalBody/#confirmModalBtn`
- ⚠️ **Cảnh báo có sẵn (không phải do lần sửa này)**: checkout.html không có `<script src=".../bootstrap.bundle.min.js">` — nếu đây là bug tồn tại từ trước, giữ nguyên hành vi hiện tại, không tự thêm/sửa ngoài phạm vi CSS/HTML thuần.

### 7. auth.html (`/auth`) — Đăng nhập / Đăng ký
- JS: `auth.js`, `search-suggestions.js` (qua navbar), jQuery, Bootstrap
- Chức năng: toggle login/register, submit đăng ký (fetch), modal xác minh email OTP
- Selector: `#btnLogin/#btnRegister.toggle-btn`, `#loginForm/#registerForm`, form login **native submit tới `/login`** (không có id, KHÔNG phải `#loginFormElement` — auth.js check `getElementById("loginFormElement")` nhưng element này KHÔNG TỒN TẠI trong HTML hiện tại → nhánh JS fetch-login là dead code, login thật chạy qua submit HTML thường). `#registerForm form` (lấy bằng querySelector, không qua id), input `id="username/email/fullName/phone/address/rawPassword/profileImage"` (do `th:field` sinh ra, hoặc gán tay), `#verifyModal/#verifyCodeInput/#verifyFeedback`, `#btnSubmitRegister`, `window.submitVerifyCode`
- ⚠️ Không được thêm `id="loginFormElement"` cho form login — sẽ vô tình bật lại code JS fetch-login đang chết, đổi hành vi ngoài phạm vi yêu cầu.

### 8. profile.html (`/profile`) — Tài khoản
- JS: `orderCustomer.js`, `profileCustomer.js`, `voucherCustomer.js` + 1 script inline trong `profile.html` điều khiển tab
- Chức năng: 3 tab (Đơn hàng/Hồ sơ/Voucher), mỗi tab load qua AJAX và **JS tự sinh toàn bộ HTML nội dung** (không có markup tĩnh sẵn trong profile.html ngoài shell tab)
- Selector: `#accountTabs`, `#tab-orders/#tab-profile/#tab-vouchers` (nav-link, `.active`), `#section-orders/#section-profile/#section-vouchers`, `#message/#order-list/#pagination`, `#profile-content` (nội dung do `profileCustomer.js` sinh: `#avatarPreview`, `#inputAvatar`, `#btnUploadAvatar`, `#view-profile`, `#edit-profile`, `#inputFullName/#inputEmail/#inputPhone/#inputAddress`, `#btnEditProfile/#btnCancelEdit`), `#voucher-message/#voucher-list`
- **Quan trọng**: Để redesign phần nội dung 3 tab, phải sửa **template string trong .js**, không chỉ profile.html (profile.html gần như trống, chỉ là shell).

## Nhóm ADMIN (nội bộ) — layout dùng `layouts/layout.html` (sidebar)
*(Sẽ inventory chi tiết selector JS ngay trước khi redesign từng trang, theo đúng thứ tự Phần 4 mở rộng — không lặp lại công việc 2 lần)*

| Trang | Route (khớp Controller) | JS liên quan |
|---|---|---|
| layouts/layout.html + sidebar.html | layout chung admin | — |
| pages/LoginAdmin.html | /admin/login | (không dùng layout chung — xác nhận trong audit trước) |
| pages/dashboard.html | /admin/dashboard | (cần đọc) |
| pages/Product.html | /admin/products | product.js |
| pages/Brand.html | /admin/brands | brand.js |
| pages/Inventory.html | /admin/inventory | inventory.js |
| pages/Promotion.html | /admin/promotions | promotion.js |
| pages/PurchaseEntry.html | /admin/purchases | purchase-entry.js |
| pages/orders.html | /admin/orders | (cần đọc, khả năng orderAdmin chưa có tên riêng) |
| pages/voucher.html | /admin/voucher | voucher.js |
| pages/AdminUser.html | /admin/users | user.js |
| pages/categories/CaseMaterial.html | /admin/categories/... | case-material.js |
| pages/categories/StrapMaterial.html | /admin/categories/... | strap-material.js |
| pages/categories/WatchGlass.html | /admin/categories/... | watch-glass.js |
| pages/categories/WatchType.html | /admin/categories/... | watch-type.js |
| layouts/test.html | (nghi là file rác, cần xác nhận không route nào trả về "layouts/test" trước khi xóa) | — |

## Ghi chú kỹ thuật quan trọng cho cả quá trình redesign
1. Tất cả trang storefront hiện load Bootstrap 5 CSS + Bootstrap Icons qua CDN trong từng `<head>` riêng lẻ (copy-paste). Sau khi có layout fragment dùng chung, sẽ gộp về 1 nơi.
2. `product-utils.js` là single source of truth cho card sản phẩm — ưu tiên sửa ở đây.
3. Owl Carousel chỉ dùng ở index.html (khuyến mãi) — là carousel KHÁC với Bootstrap's carousel (dùng cho Quick View) → 2 lib carousel khác nhau, cần quyết định giữ Owl hay thay bằng Tailwind+JS thuần khi redesign trang chủ.
4. Nhiều nơi dùng `onclick="..."` inline (qty +/-, quick view) gắn vào `window.xxx` — không được xoá các global export này khi refactor JS.
