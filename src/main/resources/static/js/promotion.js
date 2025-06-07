const API_BASE = '/admin/promotions/api';
let currentPage = 0, pageSize = 3;
let lastSearch = { keyword: '' };
let lastFilter = { discountType: '', discountValue: '' };
let selectedProductPage = 0;

// Initialize modals and bindings
$(document).ready(function() {
	// Bootstrap modals
	const $productInput = $('#productSearch');
	const $productIdHidden = $('#productId');
	const $suggestionList = $('#productSuggestionList');

	let debounceTimer;
	const promotionModalEl = $('#promotionModal')[0];
	const promotionProductModalEl = $('#promotionProductModal')[0];
	const promotionModal = new bootstrap.Modal(promotionModalEl);
	const promotionProductModal = new bootstrap.Modal(promotionProductModalEl);
	$productInput.on('input', function() {
		clearTimeout(debounceTimer);
		const keyword = $(this).val().trim();

		if (keyword.length < 1) {
			$suggestionList.hide();
			return;
		}

		debounceTimer = setTimeout(() => {
			$.get(`${API_BASE}/search-product-name`, { keyword })
				.done(res => {
					const data = res.data || [];
					$suggestionList.empty();
					if (data.length > 0) {
						data.forEach(product => {
							const $li = $(`<li class="list-group-item list-group-item-action">${product.name}</li>`);
							$li.on('click', function() {
								$productInput.val(product.name);
								$productIdHidden.val(product.id);
								$suggestionList.hide();
							});
							$suggestionList.append($li);
						});
						$suggestionList.show();
					} else {
						$suggestionList.hide();
					}
				});
		}, 300);
	});
	// Search on Enter
	$('#searchDiscount').on('input', function() { lastFilter.discountValue = $(this).val().trim(); });
	$('#searchKeyword').on('keyup', function(e) { if (e.key === 'Enter') searchPromotions(); });
	$('#discountType').on('change', function() { lastFilter.discountType = $(this).val(); });
	// Form submit
	$('#promotionForm').on('submit', function(e) {
		e.preventDefault();
		handlePromotionFormSubmit(promotionModal);
	});

	// Add product button
	$('#addProductBtn').on('click', function() {
		handleAddProduct(promotionProductModal);
	});

	// Open Add modal
	$('#addPromotionBtn').on('click', function() {
		openAddModal(promotionModal);
	});

	loadPromotions();
	window.openAddModal = openAddModal;
	window.openEditModal = openEditModal;
	// Expose global functions
	window.openEditPromotionModal = async function(id) {
		try {
			const res = await fetch(`${API_BASE}/${id}`);
			const data = await res.json();
			if (data.responseCode !== 1) {
				return showError('Promotion not found');
			}
			openEditModal(data.data, promotionModal);
		} catch (err) {
			showError('Error loading promotion: ' + err.message);
		}
	};
	//window.deletePromotion = deletePromotion;
	$('#confirmDeleteBtn').on('click', async function() {
		const promotionId = $(this).data('promotion-id');
		try {
			const res = await fetch(`${API_BASE}/${promotionId}`, { method: 'DELETE' });
			const json = await res.json();

			if (json.responseCode === 1) {
				bootstrap.Modal.getInstance(document.getElementById('deleteConfirmModal')).hide();
				loadPromotions(); // hoặc load lại danh sách tương ứng
			} else {
				showErrorToast(json.responseMsg || 'Xóa thất bại');
			}
		} catch (e) {
			console.error(e);
			showErrorToast('Lỗi khi xóa');
		}
	});
	$('#searchInPromoBtn').on('click', function() {
		const keyword = $('#productSearchInPromo').val().trim();
		if (!keyword || !window.selectedPromotionId) {
			return showError('Vui lòng nhập từ khóa và chọn khuyến mãi.');
		}
		searchProductsInPromotion(window.selectedPromotionId, keyword);
	});

	$('#productSearchInPromo').on('keypress', function(e) {
		if (e.key === 'Enter') {
			$('#searchInPromoBtn').click();
		}
	});

});

// Search promotions
function searchPromotions() {
	lastSearch.keyword = $('#searchKeyword').val().trim();
	loadPromotions(0);
}

// Load with pagination and search
async function loadPromotions(page = currentPage) {
	currentPage = page;
	const params = new URLSearchParams({
		keyword: lastSearch.keyword,
		discountType: lastFilter.discountType,
		discountValue: lastFilter.discountValue,
		page: currentPage,
		size: pageSize
	});
	const url = (lastSearch.keyword || lastFilter.discountType || lastFilter.discountValue)
		? `${API_BASE}/search?${params}`
		: `${API_BASE}?${params}`;
	try {
		const res = await fetch(url);
		const data = await res.json();
		if (data.responseCode !== 1) {
			return showError(data.message || 'Error loading promotions');
		}
		renderPromotions(data.data.promotions || data.data.promotion);
		renderPagination(data.data.pagination);
	} catch (err) {
		showError('Error fetching promotions: ' + err.message);
	}
}

// Render promotion buttons
function renderPromotions(list) {
	const container = $('#promotionButtons').empty();
	if (!list || !list.length) {
		return container.text('No promotions found');
	}

	const ul = $('<ul>', { class: 'list-group' });

	list.forEach(p => {
		const li = $('<li>', { class: 'list-group-item d-flex justify-content-between align-items-center' });

		// Xác định phần hiển thị giá trị giảm giá kèm đơn vị
		let discountDisplay = '';
		if (p.discountType === 'percent') {
			discountDisplay = `${p.discountValue}%`;
		} else if (p.discountType === 'amount') {
			discountDisplay = `${p.discountValue} đồng`;
		}

		// Tạo phần tên + discount
		const nameSpan = $('<span>', {
			html: `<strong>${p.name}</strong> — <em>${discountDisplay}</em>`,
			style: 'cursor:pointer; flex-grow:1;',
			click: () => showPromotionDetail(p.promotionId, p.name)
		});

		const editBtn = $('<button>', {
			class: 'btn btn-sm btn-outline-secondary me-2',
			html: '<i class="bi bi-pencil"></i>',
			click: () => openEditPromotionModal(p.promotionId)
		});

		const deleteBtn = $('<button>', {
			class: 'btn btn-sm btn-outline-danger',
			html: '<i class="bi bi-trash"></i>',
			click: () => confirmDelete(p.promotionId)
		});

		li.append(nameSpan, editBtn, deleteBtn);
		ul.append(li);
	});

	container.append(ul);
}



// Pagination
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
	      <a class="page-link" href="#" onclick="loadPromotions(0);return false;"><i class="bi bi-chevron-double-left"></i></a>
	    </li>
	    <li class="page-item ${cur === 0 ? 'disabled' : ''}">
	      <a class="page-link" href="#" onclick="loadPromotions(${cur - 1});return false;"><i class="bi bi-chevron-left"></i></a>
	    </li>
	  `);

	// Các mục trang hoặc ellipsis
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
	          <a class="page-link" href="#" onclick="loadPromotions(${p});return false;">${p + 1}</a>
	        </li>
	      `);
		}
	});

	// Next & Last
	ul.append(`
	    <li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
	      <a class="page-link" href="#" onclick="loadPromotions(${cur + 1});return false;"><i class="bi bi-chevron-right"></i></a>
	    </li>
	    <li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
	      <a class="page-link" href="#" onclick="loadPromotions(${total - 1});return false;"><i class="bi bi-chevron-double-right"></i></a>
	    </li>
	  `);
}

// Show detail and products
function showPromotionDetail(id, name, page = 0) {
	selectedProductPage = page;
	window.selectedPromotionId = id;
	window.selectedPromotionName = name;
	$('#selectedPromotionName').text(name);
	$('#promotionDetail').removeClass('d-none');
	$.getJSON(`${API_BASE}/${id}/products?page=${page}`)
		.done(resp => { renderProductList(resp.data.products); renderProductPagination(resp.data.pagination, id, name); })
		.fail(xhr => showError('Error fetching products: ' + xhr.statusText));
}

function renderProductList(products) {
	const list = $('#productList').empty();

	if (!products || !products.length) {
		return $('<li>', {
			class: 'list-group-item text-muted',
			text: 'No products'
		}).appendTo(list);
	}

	products.forEach(p => {
		const li = $('<li>', {
			class: 'list-group-item d-flex justify-content-between align-items-center'
		});

		// Nội dung sản phẩm
		const content = `<span><strong>${p.productName}</strong> (${p.discountType}: ${p.discountValue})</span>`;
		li.append(content);

		// Nút xóa với icon
		const deleteBtn = $('<button>', {
			class: 'btn btn-sm btn-danger',
			click: () => removeProduct(p.productId)
		}).html('<i class="bi bi-trash"></i>');

		li.append(deleteBtn);
		list.append(li);
	});
}


// Add/Edit promotion form submit
function handlePromotionFormSubmit(modal) {
	const id = $('#promotionId').val();
	const dto = {
		name: $('#name').val(),
		discountType: $('#discountType').val(),
		discountValue: $('#discountValueInput').val(),
		startAt: $('#startAt').val(),
		endAt: $('#endAt').val(),
		description: $('#description').val()
	};
	const method = id ? 'PUT' : 'POST';
	const url = id ? `${API_BASE}/${id}` : API_BASE;
	$.ajax({ url, method, contentType: 'application/json', data: JSON.stringify(dto) })
		.done(resp => {
			if (resp.responseCode === 1) {
				modal.hide(); loadPromotions(currentPage);
			} else showError(resp.message || 'Error saving');
		})
		.fail(xhr => showError('Error saving: ' + xhr.statusText));
}

// Product add
function handleAddProduct(modal) {
	const productId = $('#productId').val().trim();
	if (!productId) return showError('Nhập mã sản phẩm');
	$.ajax({ url: `${API_BASE}/${selectedPromotionId}/products/${productId}`, method: 'POST' })
		.done(resp => {
			if (resp.responseCode === 1) { modal.hide(); showPromotionDetail(selectedPromotionId, selectedPromotionName, selectedProductPage); }
			else showError(resp.message);
		})
		.fail(xhr => showError('Error adding product: ' + xhr.statusText));
}

function openAddModal() {
	// Reset form trước khi thêm mới
	document.getElementById('promotionForm').reset();
	document.getElementById('promotionId').value = '';
	document.getElementById('promotionModalLabel').textContent = 'Thêm Khuyến mãi';

	const modal = new bootstrap.Modal(document.getElementById('promotionModal'));
	modal.show();
}
function openEditModal(promotion, modal) {
	document.getElementById('promotionId').value = promotion.promotionId || '';
	document.getElementById('name').value = promotion.name || '';
	document.getElementById('discountType').value = promotion.discountType || '';
	document.getElementById('discountValueInput').value = promotion.discountValue || '';
	document.getElementById('startAt').value = promotion.startAt ? promotion.startAt.slice(0, 16) : '';
	document.getElementById('endAt').value = promotion.endAt ? promotion.endAt.slice(0, 16) : '';
	document.getElementById('description').value = promotion.description || '';
	document.getElementById('promotionModalLabel').textContent = 'Sửa Khuyến mãi';

	modal.show();
}
function renderProductPagination(pagination, promotionId, promotionName) {
	const cur = pagination.currentPage;
	const total = pagination.totalPages;
	const ul = $('#productPagination').empty();
	if (total <= 1) return;

	const pages = [];
	pages.push(0);

	if (total <= 7) {
		for (let i = 1; i < total - 1; i++) pages.push(i);
	} else {
		if (cur <= 3) {
			for (let i = 1; i <= 3; i++) pages.push(i);
			pages.push('...');
		} else if (cur >= total - 4) {
			pages.push('...');
			for (let i = total - 4; i < total - 1; i++) pages.push(i);
		} else {
			pages.push('...');
			pages.push(cur - 1, cur, cur + 1);
			pages.push('...');
		}
	}
	pages.push(total - 1);

	ul.append(`
	    <li class="page-item ${cur === 0 ? 'disabled' : ''}">
	      <a class="page-link" href="#" onclick="showPromotionDetail(${promotionId}, '${promotionName}', 0); return false;"><i class="bi bi-chevron-double-left"></i></a>
	    </li>
	    <li class="page-item ${cur === 0 ? 'disabled' : ''}">
	      <a class="page-link" href="#" onclick="showPromotionDetail(${promotionId}, '${promotionName}', ${cur - 1}); return false;"><i class="bi bi-chevron-left"></i></a>
	    </li>
	`);

	pages.forEach(p => {
		if (p === '...') {
			ul.append(`<li class="page-item disabled"><span class="page-link">…</span></li>`);
		} else {
			ul.append(`
	          <li class="page-item ${p === cur ? 'active' : ''}">
	            <a class="page-link" href="#" onclick="showPromotionDetail(${promotionId}, '${promotionName}', ${p}); return false;">${p + 1}</a>
	          </li>
	        `);
		}
	});

	ul.append(`
	    <li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
	      <a class="page-link" href="#" onclick="showPromotionDetail(${promotionId}, '${promotionName}', ${cur + 1}); return false;"><i class="bi bi-chevron-right"></i></a>
	    </li>
	    <li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
	      <a class="page-link" href="#" onclick="showPromotionDetail(${promotionId}, '${promotionName}', ${total - 1}); return false;"><i class="bi bi-chevron-double-right"></i></a>
	    </li>
	`);
}

// Remove product or promotion
function removeProduct(pid) {
	$.ajax({ url: `${API_BASE}/${selectedPromotionId}/products/${pid}`, method: 'DELETE' })
		.done(resp => {
			if (resp.responseCode === 1) {
				showPromotionDetail(selectedPromotionId, selectedPromotionName);
			} else {
				showError(resp.message || 'Xóa thất bại');
			}
		}).fail(xhr => showError('Error removing product: ' + xhr.statusText));
}

function confirmDelete(promotionId) {
	$('#confirmDeleteBtn').data('promotion-id', promotionId);
	new bootstrap.Modal(document.getElementById('deleteConfirmModal')).show();
}
function searchProductsInPromotion(promotionId, keyword, page = 0, size = 10) {
	const url = `${API_BASE}/${promotionId}/search-products?productName=${encodeURIComponent(keyword)}&page=${page}&size=${size}`;

	$.getJSON(url)
		.done(resp => {
			if (resp.responseCode !== 1) return showError(resp.responseMsg || 'Không tìm thấy sản phẩm.');

			renderProductList(resp.data.products);
			renderProductPagination(resp.data.pagination, promotionId, window.selectedPromotionName);
		})
		.fail(xhr => showError('Lỗi tìm sản phẩm: ' + xhr.statusText));
}

function showError(msg) {
	const box = $('#errorBox');
	if (!box.length) $('<div>', { id: 'errorBox', class: 'alert alert-danger', text: msg }).prependTo('.container');
	else box.text(msg).show();
	setTimeout(() => $('#errorBox').fadeOut(), 5000);
}
