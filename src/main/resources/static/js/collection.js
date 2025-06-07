import { renderProductCard, currency } from './product-utils.js';
import { initSearchSuggestions } from './search-suggestions.js';
import { updateCartCount, showCart } from './cart-utils.js';
let currentPage = 0;
window.showCart = showCart;
document.addEventListener("DOMContentLoaded", () => {
	updateCartCount();
	showCart();
	initSearchSuggestions();
	initQuickView();
});
const priceRanges = [
	{ label: 'Dưới 1.000.000', min: 0, max: 1000000 },
	{ label: '1.000.000 – 3.000.000', min: 1000000, max: 3000000 },
	{ label: '3.000.000 – 5.000.000', min: 3000000, max: 5000000 },
	{ label: 'Trên 5.000.000', min: 5000000, max: Number.MAX_SAFE_INTEGER }
];
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
                <img src="/api/img/${img.url}" class="d-block w-100" alt="">
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
							.removeClass('btn-dark');
					} else {
						$('#qv-add-to-cart')
							.prop('disabled', false)
							.text('Thêm vào giỏ')
							.addClass('btn-dark')
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
$(document).on('click', '.add-to-cart-btn', function(e) {
	e.preventDefault();
	const productId = $(this).data('id');
	const name = $(this).closest('.product-card').find('.card-title').text();
	const price = $(this).closest('.product-card').find('.text-danger').text();
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
$(document).ready(function() {
	loadSearchResults(currentPage);
	// 1. Load danh sách thương hiệu
	$.get('/api/brand-name', function(res) {
		if (res.responseCode === 1) {
			let html = '';
			res.data.brandsName.forEach(brand => {
				html += `
					<div class="form-check">
						<input class="form-check-input brand-checkbox" 
							   type="checkbox" 
							   value="${brand.name}" 
							   id="brand-${brand.brandId}">
						<label class="form-check-label" for="brand-${brand.brandId}">
							${brand.name}
						</label>
					</div>
				`;
			});
			$('#brand-filter').html(html);
		}
	});

	// 2. Load danh sách giới tính
	$.get('/api/gender', function(res) {
		if (res.responseCode === 1) {
			let html = '';
			res.data.genders.forEach(gender => {
				html += `
					<div class="form-check">
						<input class="form-check-input gender-checkbox" 
							   type="checkbox" 
							   value="${gender.code}" 
							   id="gender-${gender.code}">
						<label class="form-check-label" for="gender-${gender.code}">
							${gender.display}
						</label>
					</div>
				`;
			});
			$('#gender-filter').html(html);
		}
	});
	// 2. Load danh sách phân khúc
	$.get('/api/segment', function(res) {
		if (res.responseCode === 1) {
			let html = '';
			res.data.segments.forEach(seg => {
				html += `
	        <div class="form-check">
	          <input class="form-check-input segment-checkbox"
	                 type="checkbox"
	                 value="${seg.code}"
	                 id="segment-${seg.code}">
	          <label class="form-check-label" for="segment-${seg.code}">
	            ${seg.display}
	          </label>
	        </div>
	      `;
			});
			$('#segment-filter').html(html);
		}
	});

	let priceHtml = '';
	priceRanges.forEach((r, idx) => {
		priceHtml += `
				<div class="form-check">
					<input class="form-check-input price-checkbox"
						   type="checkbox"
						   value="${idx}"
						   id="price-${idx}">
					<label class="form-check-label" for="price-${idx}">
						${r.label}
					</label>
				</div>
			`;
	});
	$('#price-filter').html(priceHtml);
	// 3. Toggle chung cho mọi filter section
	$(document).on('click', '.toggle-filter', function() {
		const $header = $(this);
		const $body = $header.closest('.filter-section').find('.form-check-container');
		const $icon = $header.find('.toggle-icon');
		$body.slideToggle(200, function() {
			$icon.text($body.is(':visible') ? '–' : '+');
		});
	});
	$(document).on('change', '.price-checkbox', function() {
		// uncheck những ô không phải chính nó
		$('.price-checkbox').not(this).prop('checked', false);

		// sau đó gọi filter + render tag
		filterProducts();
		renderSelectedTags();
	});
	// 4. Khi checkbox brand hoặc gender thay đổi -> gọi filter
	$(document).on('change', '.brand-checkbox, .gender-checkbox, .segment-checkbox', function() {
		filterProducts();
		renderSelectedTags();
	});
	// Khi thay đổi sort
	$(document).on('change', '#sort', function() {
		filterProducts();
	});

	// 5. Load sản phẩm ban đầu
	$(document).on('click', '#pagination .pagination-btn', function(e) {
		e.preventDefault();
		const page = parseInt($(this).data('page'));
		if (!isNaN(page) && page >= 0) {
			loadSearchResults(page);
		}
	});
});


// Hàm lấy tất cả sản phẩm
function loadAllProducts() {
	$.get('/api/collections/all', function(res) {
		if (res.responseCode === 1) {
			renderProducts(res.data.products);
		}
	});
}
function loadSearchResults(page) {
	currentPage = page;
	filterProducts();     // filterProducts sẽ đọc currentPage khi gọi API
}
// Hàm filter và gọi API
function filterProducts() {
	const selectedBrands = $('.brand-checkbox:checked').map(function() {
		return $(this).val();
	}).get();

	const selectedGenders = $('.gender-checkbox:checked').map(function() {
		return $(this).val();
	}).get();
	const selectedSegments = $('.segment-checkbox:checked').map(function() {
		return $(this).val();
	}).get();
	const sortVal = $('#sort').val();
	let sortBy = null, sortDir = null;
	if (sortVal === 'Giá tăng dần') {
		sortBy = 'price';
		sortDir = 'asc';
	} else if (sortVal === 'Giá giảm dần') {
		sortBy = 'price';
		sortDir = 'desc';
	} else if (sortVal === 'Mới nhất') {
		sortBy = 'createdDate';  // hoặc trường ngày tạo của bạn
		sortDir = 'desc';
	}
	const idxs = $('.price-checkbox:checked').map((i, e) => parseInt(e.value)).get();
	const priceMins = [], priceMaxs = [];
	idxs.forEach(i => {
		const r = priceRanges[i];
		priceMins.push(r.min);
		priceMaxs.push(r.max === Number.MAX_SAFE_INTEGER ? null : r.max);
	});
	$.ajax({
		url: '/api/collections/all',
		method: 'GET',
		data: {
			brandNames: selectedBrands,
			genders: selectedGenders,
			segments: selectedSegments,
			sortBy: sortBy,
			sortDir: sortDir,
			priceMin: priceMins,
			priceMax: priceMaxs,
			page: currentPage
		},
		traditional: true,
		success: function(res) {
			if (res.responseCode === 1) {
				renderProducts(res.data.products || res.data);
				renderPagination(res.data.pagination);
			}
		}
	});
}

// Hàm render danh sách sản phẩm
function renderProducts(products) {
	const $container = $('#newestProducts').empty();
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
			$container.append(renderProductCard(product));
		});
	} else {
		$container.html('<p>Không có sản phẩm phù hợp.</p>');
	}
}
function renderPagination(pagination) {
	const cur = pagination.currentPage;
	const total = pagination.totalPages;
	const $ul = $('#pagination').empty();
	if (total <= 1) return;
	const pages = [0];
	if (total <= 7) {
		for (let i = 1; i < total - 1; i++) pages.push(i);
	} else if (cur <= 3) {
		pages.push(1, 2, 3, '...');
	} else if (cur >= total - 4) {
		pages.push('...', total - 4, total - 3, total - 2);
	} else {
		pages.push('...', cur - 1, cur, cur + 1, '...');
	}
	pages.push(total - 1);

	// First, Prev
	$ul.append(
		`<li class="page-item ${cur === 0 ? 'disabled' : ''}">
       <a href="#" class="page-link pagination-btn" data-page="0">First</a>
     </li>
     <li class="page-item ${cur === 0 ? 'disabled' : ''}">
       <a href="#" class="page-link pagination-btn" data-page="${cur - 1}">Previous</a>
     </li>`
	);

	pages.forEach(p => {
		if (p === '...') {
			$ul.append(`<li class="page-item disabled"><span class="page-link">…</span></li>`);
		} else {
			$ul.append(
				`<li class="page-item ${p === cur ? 'active' : ''}">
           <a href="#" class="page-link pagination-btn" data-page="${p}">${p + 1}</a>
         </li>`
			);
		}
	});

	// Next, Last
	$ul.append(
		`<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
       <a href="#" class="page-link pagination-btn" data-page="${cur + 1}">Next</a>
     </li>
     <li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
       <a href="#" class="page-link pagination-btn" data-page="${total - 1}">Last</a>
     </li>`
	);
}
// Hàm render các tag đã chọn (brand + gender)
function renderSelectedTags() {
	const $container = $('#selected-tags').empty();

	$('.brand-checkbox:checked').each(function() {
		const val = $(this).val();
		const label = $(this).next('label').text();
		$container.append(`
			<span class="badge bg-dark text-white px-3 py-2 remove-tag"
			      data-type="brand"
			      data-value="${val}">
				${label} ✕
			</span>
		`);
	});

	$('.gender-checkbox:checked').each(function() {
		const val = $(this).val();
		const label = $(this).next('label').text();
		$container.append(`
			<span class="badge bg-info text-white px-3 py-2 remove-tag"
			      data-type="gender"
			      data-value="${val}">
				${label} ✕
			</span>
		`);
	});
	$('.segment-checkbox:checked').each(function() {
		const val = $(this).val();
		const label = $(this).next('label').text();
		$container.append(`
	    <span class="badge bg-warning text-white px-3 py-2 remove-tag"
	          data-type="segment"
	          data-value="${val}">
	      ${label} ✕
	    </span>
	  `);
	});
	$('.price-checkbox:checked').each(function() {
		const val = $(this).val();
		const label = $(this).next('label').text();
		$container.append(`
		    <span class="badge bg-warning text-white px-3 py-2 remove-tag"
		          data-type="price"
		          data-value="${val}">
		      ${label} ✕
		    </span>
		  `);
	});
	if ($container.children().length > 0) {
		$container.append(`
			<span class="badge bg-secondary px-3 py-2" id="clearAllTags">Xóa hết ✕</span>
		`);
	}
}

// Xóa từng tag
$(document).on('click', '.remove-tag', function() {
	const type = $(this).data('type');
	const val = $(this).data('value');
	if (type === 'brand') {
		$(`.brand-checkbox[value="${val}"]`).prop('checked', false);
	} else if (type === 'gender') {
		$(`.gender-checkbox[value="${val}"]`).prop('checked', false);
	}
	else if (type === 'segment') {
		$(`.segment-checkbox[value="${val}"]`).prop('checked', false);
	}
	else if (type === 'price') {
		$(`.price-checkbox[value="${val}"]`).prop('checked', false);
	}
	filterProducts();
	renderSelectedTags();
});

// Xóa hết
$(document).on('click', '#clearAllTags', function() {
	$('.brand-checkbox, .gender-checkbox, .price-checkbox, .segment-checkbox').prop('checked', false);
	loadAllProducts();
	renderSelectedTags();
});
