import { renderProductCard } from './product-utils.js';
import { initSearchSuggestions } from './search-suggestions.js'
import { currency } from './product-utils.js';
import { updateCartCount } from './cart-utils.js';
import { showCart } from './cart-utils.js';
let currentPage = 0
function getQueryParam(name) {
	const urlParams = new URLSearchParams(window.location.search);
	return urlParams.get(name);
}

let keyword = getQueryParam("keyword");
const pageSize = 10;

$(document).ready(function() {
	initSearchSuggestions();
	initQuickView();
	if (keyword) {
		loadSearchResults(0); // Trang đầu tiên
	}
});
window.showCart = showCart;
document.addEventListener("DOMContentLoaded", () => {
	updateCartCount();
	showCart();
});
// 5. Quick‑View: khi nhấn nút
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
function loadSearchResults(page = currentPage) {
	$.get(`/api/product-search?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${pageSize}`, function(response) {
		if (response.responseCode === 1) {
			const products = response.data.products;
			const pagination = response.data.pagination;
			const total = response.data.total;
			const keywordDisplay = response.data.keywordss;

			// 👉 Hiển thị tóm tắt kết quả
			$('#searchSummary').text(`Tìm thấy ${total} kết quả cho từ khóa "${keywordDisplay}"`);

			const container = $('#searchResult').empty();
			if (products.length > 0) {
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
				container.html('<p>Không có kết quả phù hợp.</p>');
			}

			renderPagination(pagination);
		}
	});
}
window.loadSearchResults = loadSearchResults;

function renderPagination(pagination) {
	const cur = pagination.currentPage;
	const total = pagination.totalPages;
	const ul = $('#pagination').empty();
	if (total <= 1) return;

	// Xây danh sách các mục hiển thị (số trang hoặc '...')
	const pages = [];
	pages.push(0); // luôn luôn có trang đầu

	if (total <= 7) {
		// nếu tổng <= 7 thì hiển thị tất cả
		for (let i = 1; i < total - 1; i++) pages.push(i);
	} else {
		if (cur <= 3) {
			// gần đầu: 0,1,2,3 rồi ...
			for (let i = 1; i <= 3; i++) pages.push(i);
			pages.push('...');
		} else if (cur >= total - 4) {
			// gần cuối: ... then total-4..total-2
			pages.push('...');
			for (let i = total - 4; i < total - 1; i++) pages.push(i);
		} else {
			// giữa: ... cur-1, cur, cur+1, ...
			pages.push('...');
			pages.push(cur - 1, cur, cur + 1);
			pages.push('...');
		}
	}

	pages.push(total - 1); // luôn luôn có trang cuối

	// First & Previous
	ul.append(`
		<li class="page-item ${cur === 0 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadSearchResults(0);return false;">First</a>
		</li>
		<li class="page-item ${cur === 0 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadSearchResults(${cur - 1});return false;">Previous</a>
		</li>
	`);

	// Mục trang
	pages.forEach(p => {
		if (p === '...') {
			ul.append(`
				<li class="page-item disabled">
					<span class="page-link">…</span>
				</li>
			`);
		} else {
			ul.append(`
				<li class="page-item ${p === cur ? 'active' : ''}">
					<a class="page-link" href="#" onclick="loadSearchResults(${p});return false;">${p + 1}</a>
				</li>
			`);
		}
	});

	// Next & Last
	ul.append(`
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadSearchResults(${cur + 1});return false;">Next</a>
		</li>
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadSearchResults(${total - 1});return false;">Last</a>
		</li>
	`);
}
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
