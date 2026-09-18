import { currency } from './product-utils.js';
import { renderProductCard } from './product-utils.js';
import { initSearchSuggestions } from './search-suggestions.js'
import { updateCartCount } from './cart-utils.js';
import { showCart } from './cart-utils.js';
document.addEventListener("DOMContentLoaded", () => {
	updateCartCount();
	showCart();
});
window.showCart = showCart;
function loadBrandButtons() {
	$.get('/api/brand-name', function(response) {
		if (response.responseCode === 1) {
			const brands = response.data.brandsName;
			const container = $('#brandList').empty();

			brands.forEach((brand, index) => {
				const btn = $(`
					<button class="brand-btn" data-id="${brand.brandId}">${brand.name}</button>
				`);

				// Nếu là brand đầu tiên thì active
				if (index === 0) {
					btn.addClass('active');
					/*loadBrandName(brand.brandId);*/
					loadFeaturedProducts(brand.brandId);
				}

				container.append(btn);
			});
		}
	});
}
function loadPromotionalProducts() {
	$.get('/api/promotions/active-products', function(response) {
		if (response.responseCode === 1) {
			const products = response.data.products;
			const container = $('#promotionProducts');
			container.empty();

			if (Array.isArray(products) && products.length > 0) {
				products.forEach(p => {
					const product = {
						productId: p.productId,
						productName: p.name,
						imageUrl: p.images && p.images.length > 0
							? `/api/img/${p.images[0].url}`
							: '/img/default.png',
						originalPrice: p.price,
						quantity: p.quantity,
						discountedPrice: p.discountPrice,
						discountPercent: p.discountType === 'PERCENT' ? p.discountValue : 0
					};
					container.append(renderProductCard(product));
				});

				// Xác định số lượng items cần hiển thị
				const productCount = products.length;
				const itemsToShow = productCount < 4 ? productCount : 4;

				// Thêm class owl-carousel
				container.addClass('owl-carousel');

				// Khởi tạo Owl Carousel với cấu hình linh hoạt
				container.owlCarousel({
					items: itemsToShow,
					loop: productCount > itemsToShow,
					margin: 10,
					nav: true,
					dots: false,
					autoplay: true,
					autoplayTimeout: 5000,
					responsive: {
						0: { items: Math.min(1, itemsToShow) },
						600: { items: Math.min(2, itemsToShow) },
						1000: { items: itemsToShow }
					}
				});

			} else {
				container.html('<p>Không có sản phẩm khuyến mãi.</p>');
			}
		} else {
			$('#promotionProducts').html('<p>Lỗi tải sản phẩm khuyến mãi.</p>');
		}
	}).fail(() => {
		$('#promotionProducts').html('<p>Lỗi kết nối tới server.</p>');
	});
}



function loadFeaturedProducts(brandId) {
	$.get(`/api/featured/${brandId}`, function(response) {
		if (response.responseCode === 1) {
			// Truy cập đúng mảng sản phẩm
			const products = response.data.products;
			const container = $('#featuredProducts').empty();

			if (Array.isArray(products) && products.length > 0) {
				products.forEach(p => {
					const product = {
						productId: p.productId,
						productName: p.name,
						imageUrl: p.images && p.images.length > 0
							? `/api/img/${p.images[0].url}`
							: '/img/default.png',
						originalPrice: p.price,
						quantity: p.quantity,
						discountedPrice: p.discountPrice,
						discountPercent: p.discountType === 'PERCENT'
							? Math.round((1 - (p.discountPrice / p.price)) * 100)
							: 0
					};
					container.append(renderProductCard(product));
				});
			} else {
				container.html('<p>Chưa có sản phẩm nổi bật.</p>');
			}

		} else {
			$('#featuredProducts').text('Lỗi tải sản phẩm nổi bật');
		}
	}).fail(() => {
		$('#featuredProducts').text('Lỗi tải sản phẩm nổi bật');
	});
}
$(document).on('click', '.brand-btn', function() {
	const brandId = $(this).data('id');

	$('.brand-btn').removeClass('active');
	$(this).addClass('active');

	/*loadBrandName(brandId);*/
	loadFeaturedProducts(brandId);
});
function loadNewestProducts() {
	$.get('/api/newest', function(response) {
		if (response.responseCode === 1) {
			const products = response.data.products;
			const container = $('#newestProducts').empty();

			if (Array.isArray(products) && products.length > 0) {
				products.forEach(p => {
					const product = {
						productId: p.productId,
						productName: p.name,
						imageUrl: p.images && p.images.length > 0
							? `/api/img/${p.images[0].url}`
							: '/img/default.png',
						originalPrice: p.price,
						quantity: p.quantity,
						discountedPrice: p.discountPrice,
						discountPercent: p.discountType === 'PERCENT'
							? Math.round((1 - (p.discountPrice / p.price)) * 100)
							: 0
					};
					container.append(renderProductCard(product));
				});

				// Optional: init lại carousel nếu dùng Slick/Swiper
				// $('.product-carousel').slick({...}) hoặc Swiper init
			} else {
				container.html('<p>Không có sản phẩm mới.</p>');
			}
		} else {
			$('#newestProducts').html('<p>Lỗi tải sản phẩm mới.</p>');
		}
	}).fail(() => {
		$('#newestProducts').html('<p>Lỗi tải sản phẩm mới.</p>');
	});
}

//  Quick‑View: khi nhấn nút
function initQuickView() {
	$(document).on('click', '.quick-view-btn', function() {
		const pid = $(this).data('product-id');
		$.get(`/api/product-detail/${pid}`)
			.done(resp => {
				if (resp.responseCode === 1) {
					const p = resp.data;

					// === Hiển thị ảnh trong carousel ===
					const indicators = [];
					const slides = [];

					(p.images || []).forEach((img, idx) => {
						const active = idx === 0 ? 'active' : '';
						indicators.push(`
			  <button type="button" data-bs-target="#qvCarousel"
					  data-bs-slide-to="${idx}"
					  class="${active}" aria-label="Slide ${idx + 1}"></button>
			`);
						slides.push(`
			  <div class="carousel-item ${active}">
				<img src="/api/img/${img.url}" class="d-block w-100" alt="${p.name || ''}">
			  </div>
			`);
					});

					if (slides.length === 0) {
						slides.push(`
			  <div class="carousel-item active">
				<img src="/img/default.png" class="d-block w-100" alt="No image">
			  </div>
			`);
					}

					$('#qv-indicators').html(indicators.join(''));
					$('#qv-carousel-inner').html(slides.join(''));

					// === Badge giảm giá ===
					if (p.discountType) {
						$('#qv-badge')
							.text(`-${Math.round((1 - p.discountPrice / p.price) * 100)}%`)
							.show();
					} else $('#qv-badge').hide();

					// === Thông tin sản phẩm ===
					$('#qv-name').text(p.name);
					$('#qv-sku').text(p.sku);
					$('#qv-discounted').text(currency(p.discountPrice));
					$('#qv-original').text(
						p.discountPrice < p.price ? currency(p.price) : ''
					);
					$('#qv-description').text(p.description || '');
					$('#qv-detail-link').attr('href', `/detail-shop/${p.productId}`);
					$('#qv-qty').val(1);
					if (p.quantity === 0) {
						$('#qv-add-to-cart')
							.prop('disabled', true)
							.text('Hết hàng')
							.addClass('btn-secondary')
							.removeClass('btn-primary');
					} else {
						$('#qv-add-to-cart')
							.prop('disabled', false)
							.text('Thêm vào giỏ')
							.addClass('btn-primary')
							.removeClass('btn-secondary');
					}
					// Hiển thị modal
					new bootstrap.Modal(document.getElementById('quickViewModal')).show();
				}
			});
	});

	// tăng giảm số lượng
	window.incrementQty = () => {
		const $q = $('#qv-qty');
		$q.val(Math.max(1, +$q.val() + 1));
	};
	window.decrementQty = () => {
		const $q = $('#qv-qty');
		$q.val(Math.max(1, +$q.val() - 1));
	};
}
// Bắt sự kiện cho nút Thêm vào giỏ ở Quick-View modal
$(document).on('click', '#qv-add-to-cart', function(e) {
	e.preventDefault();

	// Lấy productId từ link detail (nó chứa đúng id)
	const href = $('#qv-detail-link').attr('href'); // dạng "/detail-shop/{id}"
	const productId = href.substring(href.lastIndexOf('/') + 1);

	// Lấy tên, giá từ modal
	const name = $('#qv-name').text().trim();
	const priceText = $('#qv-discounted').text().trim() || $('#qv-original').text().trim();
	const img = $('#qv-carousel-inner .carousel-item.active img').attr('src');
	const quantity = parseInt($('#qv-qty').val(), 10) || 1;

	// Cập nhật localStorage
	let cart = JSON.parse(localStorage.getItem("cart")) || [];
	const existing = cart.find(item => item.id === productId);
	if (existing) {
		existing.quantity += quantity;
	} else {
		cart.push({ id: productId, name, price: priceText, img, quantity });
	}
	localStorage.setItem("cart", JSON.stringify(cart));

	// Cập nhật số lượng giỏ hàng và đóng modal
	updateCartCount();
	// Nếu muốn showCart lại: showCart();
	// Đóng modal thủ công
	const modalEl = document.getElementById('confirmationModal');
	// Nếu bạn dùng confirmationModal, thay bằng:
	// const modalEl = document.getElementById('quickViewModal');
	const qvModal = bootstrap.Modal.getInstance(
		document.getElementById('quickViewModal')
	);
	if (qvModal) qvModal.hide();

	// Thông báo thành công (có thể modal hay toast)
	alert("Đã thêm vào giỏ: " + name + " (x" + quantity + ")");
});

$(document).ready(function() {
	initSearchSuggestions();
	loadBrandButtons();
	initQuickView();
	loadNewestProducts();
	loadPromotionalProducts();
	//	initSearchRedirectForm();

});
$(document).on('click', '.product-card', function(e) {
	const target = $(e.target);

	// Nếu người dùng bấm vào nút "Thêm vào giỏ" hoặc "Xem nhanh" thì không điều hướng
	if (
		target.hasClass('add-to-cart') ||
		target.hasClass('quick-view-btn') ||
		target.closest('button').length > 0
	) {
		return;
	}

	// Nếu không, chuyển hướng đến trang chi tiết
	const productId = $(this).data('product-id');
	window.location.href = `/detail-shop/${productId}`;
});
$(document).on('click', '.add-to-cart-btn', function(e) {
	e.preventDefault();
	const productId = $(this).data('id');
	const name = $(this).closest('.product-card').find('.card-title').text();
	const price = $(this).closest('.product-card').find('.price-current').text();
	const img = $(this).closest('.product-card').find('img').attr('src');

	let cart = JSON.parse(localStorage.getItem("cart")) || [];

	const existingItem = cart.find(item => item.id === productId);
	if (existingItem) {
		existingItem.quantity += 1;
	} else {
		cart.push({
			id: productId,
			name: name,
			price: price,
			img: img,
			quantity: 1
		});
	}

	localStorage.setItem("cart", JSON.stringify(cart));
	updateCartCount(); // Cập nhật số hiển thị trên icon giỏ hàng
	alert("Đã thêm sản phẩm vào giỏ hàng!");
});
