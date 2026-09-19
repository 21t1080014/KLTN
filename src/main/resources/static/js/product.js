const API_BASE = '/admin/products/api';
let currentPage = 0, pageSize = 5;
let lastSearch = { name: '' };
let selectedFiles = [];

$(document).ready(function() {
	loadProducts();

	// Search
	$("#searchForm").submit(function(e) {
		e.preventDefault();
		const name = $("#searchInput").val().trim();
		if (!name) {
			showToast("Please enter search criteria", "danger");
			return;
		}
		lastSearch.name = name;
		loadProducts(0);
	});

	$('#productForm').submit(async e => {
		e.preventDefault();
		if (!validateProductForm()) return;

		// Build FormData
		const formData = new FormData();
		const productId = $('#productForm').data('productId');
		// Append input fields
		$('#productForm').serializeArray().forEach(({ name, value }) => formData.append(name, value));

		// Append old images (filenames)
		selectedFiles
			.filter(f => !f.isNew)
			.forEach(f => formData.append('oldImages', f.fileName));

		// Append new image files
		selectedFiles
			.filter(f => f.isNew)
			.forEach(f => formData.append('images', f.file));

		const url = productId ? `${API_BASE}/${productId}` : API_BASE;
		const method = productId ? 'PUT' : 'POST';

		try {
			const resp = await fetch(url, { method, body: formData });
			const result = await resp.json();
			if (result.responseCode === 1) {
				showToast(result.responseMsg, 'success');
				$('#productModal').modal('hide');
				loadProducts();
			} else {
				showToast(result.responseMsg || 'Error occurred', 'danger');
			}
		} catch {
			showToast('Unexpected error occurred', 'danger');
		}
	});

	// Reset modal
	$('#productModal').on('hidden.bs.modal', () => {
		$('#productForm')[0].reset();
		$('#productForm').removeData('productId');
		selectedFiles = [];
		renderImagePreview();
		$('#productForm .error-message').remove();
	});

	// Handle file input
	$('#productImages').on('change', function() {
		Array.from(this.files).forEach(file => {
			selectedFiles.push({
				isNew: true,
				file,
				fileName: file.name
			});
		});
		this.value = '';
		renderImagePreview();
	});
});

function renderImagePreview() {
	const preview = $('#imagePreview').empty();
	selectedFiles.forEach((f, idx) => {
		const wrapper = $('<div>').addClass('position-relative d-inline-block me-2 mb-2');
		const img = $('<img>').addClass('img-thumbnail').css({ width: '100px', height: '100px', objectFit: 'cover' });
		if (f.isNew) {
			img.attr('src', URL.createObjectURL(f.file));
		} else {
			img.attr('src', `${API_BASE}/img/${f.fileName}`);
		}
		wrapper.append(img);
		if (idx === 0) {
			wrapper.append(
				$('<span>')
					.text('Đại diện')
					.addClass('badge bg-primary position-absolute top-0 start-0 m-1')
			);
		}
		// Delete button carries its index
		const del = $('<button>').addClass('btn btn-danger btn-sm position-absolute top-0 end-0')
			.html('<i class="bi bi-x"></i>')
			.attr('data-index', idx)
			.on('click', function() {
				const i = +$(this).attr('data-index');
				selectedFiles = selectedFiles.filter((_, j) => j !== i);
				renderImagePreview();
			});
		wrapper.append(del);
		preview.append(wrapper);
	});
}

async function loadProducts(page = currentPage) {
	currentPage = page;
	const params = new URLSearchParams({
		keyword: lastSearch.name,
		page: currentPage,
		size: pageSize
	});
	const url = lastSearch.name ? `${API_BASE}/search?${params}` : `${API_BASE}?${params}`;

	const resp = await fetch(url);
	const data = await resp.json();

	const products = data.data.products;
	const pagination = data.data.pagination;
	const totalPages = pagination ? pagination.totalPages : 0;

	if (currentPage > totalPages - 1 && totalPages > 0) {
		currentPage = totalPages - 1;
		await loadProducts(currentPage);
		return;
	}

	renderProducts(products);
	renderPagination(pagination);
}

function renderProducts(products) {
	const list = document.getElementById('productList');
	list.innerHTML = products.length
		? products.map(p => {
			const imageUrl = p.images?.find(img => img.sortOrder === 0)?.url
				? `/admin/products/api/img/${p.images.find(img => img.sortOrder === 0).url}`
				: '/img/default.png';
			return `
				<li class="list-group-item">
					<div class="d-flex justify-content-between align-items-center">
					<input type="checkbox" class="form-check-input chk-product me-2" value="${p.productId}">
					<img src="${imageUrl}" alt="${p.name}" class="me-3 thumbnail" style="width:80px;height:80px;object-fit:contain;cursor:pointer;" onclick="openImageViewer('${p.productId}')">

						<div class="flex-grow-1"><strong>${p.name}</strong>
							<span class="badge ms-2">${p.status || ''}</span><br>
							<small class="text-muted">SKU: ${p.sku} · Giá: ${Number(p.price).toLocaleString('vi-VN')}đ · Có thể bán: ${p.availableQuantity ?? 0} · ${p.variantCount || 1} biến thể${p.categoryName ? ' · ' + p.categoryName : ''}</small><br>
							<small class="product-description">${p.description || ''}</small></div>
						<div>
							<button class="btn btn-sm btn-secondary me-2" title="Biến thể" onclick="openVariantsModal('${p.productId}')"><i class="bi bi-layers"></i></button>
							<button class="btn btn-sm btn-warning me-2" onclick="openEditProductModal('${p.productId}')"><i class="bi bi-pencil"></i></button>
							${document.body.dataset.role === 'admin' ? `<button class="btn btn-sm btn-danger me-2" onclick="deleteProduct('${p.productId}')"><i class="bi bi-trash"></i></button>` : ''}
							<button class="btn btn-sm btn-info" onclick="viewProductDetails('${p.productId}')"><i class="bi bi-eye"></i></button>
						</div>
					</div>
				</li>`;
		}).join('')
		: '<li class="list-group-item text-muted">No products found.</li>';
}

function renderPagination(pagination) {
	const cur = pagination.currentPage;
	const total = pagination.totalPages;
	const ul = $('#pagination').empty();
	if (total <= 1) return;

	ul.append(`
		<li class="page-item ${cur === 0 ? 'disabled' : ''}"><a class="page-link" href="#" onclick="loadProducts(0);return false;">First</a></li>
		<li class="page-item ${cur === 0 ? 'disabled' : ''}"><a class="page-link" href="#" onclick="loadProducts(${cur - 1});return false;">Previous</a></li>`);

	for (let i = 0; i < total; i++) {
		ul.append(`<li class="page-item ${i === cur ? 'active' : ''}"><a class="page-link" href="#" onclick="loadProducts(${i});return false;">${i + 1}</a></li>`);
	}

	ul.append(`
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}"><a class="page-link" href="#" onclick="loadProducts(${cur + 1});return false;">Next</a></li>
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}"><a class="page-link" href="#" onclick="loadProducts(${total - 1});return false;">Last</a></li>`);
}

function openAddProductModal() {
	loadFormOptions().then(() => {
		$('#productForm')[0].reset();
		$('#productForm').removeData('productId');
		selectedFiles = [];
		renderImagePreview();
		$('#productModalLabel').text("Add New Product");
		$('#productModal').modal('show');
	});
}

async function openEditProductModal(id) {
	await loadFormOptions();
	try {
		const res = await fetch(`${API_BASE}/${id}`);
		const data = await res.json();
		if (data.responseCode !== 1) return showToast(data.responseMsg, "danger");
		const p = data.data;

		$('#productForm').data('productId', id);
		$('#sku').val(p.sku);
		$('#name').val(p.name);
		$('#origin').val(p.origin);
		$('#condition').val(p.condition);
		$('#warrantyPeriod').val(p.warrantyPeriod);
		$('#price').val(p.price);
		$('#gender').val(p.gender);
		$('#segment').val(p.segment);

		["brandId", "typeId", "caseMaterialId", "strapMaterialId", "glassMaterialId"].forEach(field => {
			$(`#${field} option`).filter(function() {
				return $(this).text().trim() === p[`${field.replace('Id', 'Name')}`];
			}).prop('selected', true);
		});
		$('#categoryId').val(p.categoryId || '');
		$('#status').val(p.status || 'ACTIVE');
		selectedFiles = (p.images || []).map(img => ({
			isNew: false,
			url: `${API_BASE}/img/${img.url}`,
			fileName: img.url,
			sortOrder: img.sortOrder
		}));
		renderImagePreview();

		$('#productModalLabel').text("Chỉnh sửa sản phẩm");
		$('#productModal').modal('show');
	} catch (err) {
		showToast("Error loading product", "danger");
	}
}

let productIdToDelete = null;

function deleteProduct(id) {
	productIdToDelete = id;
	$('#deleteConfirmModal').modal('show');
}

$('#confirmDeleteBtn').on('click', async function() {
	if (!productIdToDelete) return;

	try {
		const res = await fetch(`${API_BASE}/${productIdToDelete}`, { method: "DELETE" });
		const data = await res.json();

		if (data.responseCode === 1) {
			showToast("Đã xóa thành công", "success");
			loadProducts();
		} else {
			showToast(data.responseMsg || "Xóa thất bại", "danger");
		}
	} catch (err) {
		console.error(err);
		showToast("Lỗi khi xóa", "danger");
	} finally {
		$('#deleteConfirmModal').modal('hide');
		productIdToDelete = null;
	}
});

function validateProductForm() {
	let valid = true;
	$('#productForm .error-message').remove();

	const required = [
		{ selector: '#name', message: 'Product name is required.' },
		{ selector: '#sku', message: 'SKU is required.' },
		{ selector: '#price', message: 'Price is required and must be greater than zero.', condition: val => parseFloat(val) > 0 },
		{ selector: '#origin', message: 'Origin is required.' }
	];

	required.forEach(field => {
		const el = $(field.selector);
		const val = el.val().trim();
		if (!val || (field.condition && !field.condition(val))) {
			showError(el, field.message);
			valid = false;
		}
	});

	const selects = ['#brandId', '#typeId', '#condition', '#gender'];
	selects.forEach(sel => {
		if (!$(sel).val()) {
			showError($(sel), `${$(sel).attr('name') || sel} is required.`);
			valid = false;
		}
	});

	if (!$('#productForm').data('productId') && selectedFiles.length === 0) {
		showError($('#productImages'), "At least one image is required.");
		valid = false;
	}

	return valid;
}

function showError(input, message) {
	const err = $('<div>').addClass('text-danger error-message').text(message);
	input.parent().append(err);
}

function showToast(message, type = "success") {
	const toast = $('#errorToast');
	toast.removeClass("text-bg-success text-bg-danger").addClass(`text-bg-${type}`);
	toast.find('.toast-body').text(message);
	const bsToast = new bootstrap.Toast(toast[0]);
	bsToast.show();
}

async function viewProductDetails(productId) {
	try {
		const res = await fetch(`${API_BASE}/${productId}`);
		const result = await res.json();
		if (result.responseCode !== 1) {
			showToast(result.responseMsg || "Lỗi khi tải chi tiết", "danger");
			return;
		}

		const p = result.data;
		const imagesHtml = (p.images || []).map(img => `
			<img src="/admin/products/api/img/${img.url}" class="img-thumbnail me-2 mb-2" style="width:100px;height:100px;object-fit:cover;">
		`).join("");

		const html = `
			<div class="row">
				<div class="col-md-6">
					<strong>Tên sản phẩm:</strong> ${p.name}<br>
					<strong>SKU:</strong> ${p.sku}<br>
					<strong>Giá:</strong> ${p.price.toLocaleString()}<br>
					<strong>Thương hiệu:</strong> ${p.brandName}<br>
					<strong>Loại:</strong> ${p.typeName}<br>
					<strong>Xuất xứ:</strong> ${p.origin}<br>
					<strong>Trạng thái:</strong> ${p.condition}<br>
					<strong>Giới tính:</strong> ${p.gender}<br>
					<strong>Phân khúc:</strong> ${p.segment}<br>
					<strong>Bảo hành:</strong> ${p.warrantyPeriod} tháng<br>
				</div>
				<div class="col-md-6">
					<strong>Mô tả:</strong><br>
					<p>${p.description || 'Không có mô tả'}</p>
					<strong>Ảnh:</strong><br>
					<div class="d-flex flex-wrap">${imagesHtml}</div>
				</div>
			</div>
		`;

		$("#productDetailContent").html(html);
		$("#productDetailModal").modal("show");

	} catch (err) {
		console.error(err);
		showToast("Lỗi khi tải chi tiết sản phẩm", "danger");
	}
}


async function loadFormOptions() {
	try {
		const res = await fetch(`${API_BASE}/form-options`);
		const data = await res.json();
		if (data.responseCode !== 1) throw new Error();
		const opts = data.data;

		populateSelect('#brandId', opts.brands, 'brandId', 'name');
		populateSelect('#typeId', opts.types, 'typeId', 'name');
		populateSelect('#caseMaterialId', opts.caseMaterials, 'caseMaterialId', 'name');
		populateSelect('#strapMaterialId', opts.strapMaterials, 'strapMaterialId', 'name');
		populateSelect('#glassMaterialId', opts.glassMaterials, 'glassMaterialId', 'name');
		populateSelect('#categoryId', opts.categories || [], 'categoryId', 'name');
		populateEnumSelect('#status', opts.statuses || ['ACTIVE']);
		$('#status').val('ACTIVE');

		populateEnumSelect('#condition', opts.conditions);
		populateEnumSelect('#gender', opts.genders);
		populateEnumSelect('#segment', opts.segments);
	} catch {
		showToast("Error loading form data", "danger");
	}
}

function populateSelect(selector, data, valueField, textField) {
	const select = document.querySelector(selector);
	select.innerHTML = '<option value="">-- Select --</option>';
	data.forEach(item => {
		select.innerHTML += `<option value="${item[valueField]}">${item[textField]}</option>`;
	});
}

function populateEnumSelect(selector, list) {
	const select = document.querySelector(selector);
	select.innerHTML = '<option value="">-- Select --</option>';
	list.forEach(item => {
		select.innerHTML += `<option value="${item}">${item}</option>`;
	});
}
let viewerImages = [];
let viewerIndex = 0;

async function openImageViewer(productId) {
	try {
		const res = await fetch(`${API_BASE}/${productId}`);
		const result = await res.json();
		if (result.responseCode !== 1) {
			showToast("Không thể tải ảnh sản phẩm", "danger");
			return;
		}
		viewerImages = result.data.images || [];
		if (viewerImages.length === 0) {
			showToast("Sản phẩm không có ảnh", "info");
			return;
		}

		viewerIndex = 0;
		renderImageViewer();
		const modal = new bootstrap.Modal(document.getElementById("imageViewerModal"));
		modal.show();

	} catch (e) {
		showToast("Lỗi khi tải ảnh sản phẩm", "danger");
	}
}

function renderImageViewer() {
	if (!viewerImages.length) return;
	const mainImg = document.getElementById("mainProductImage");
	mainImg.src = `/admin/products/api/img/${viewerImages[viewerIndex].url}`;

	const thumbs = document.getElementById("productThumbnails");
	thumbs.innerHTML = "";

	viewerImages.forEach((img, i) => {
		const thumb = document.createElement("img");
		thumb.src = `/admin/products/api/img/${img.url}`;
		thumb.className = `img-thumbnail ${i === viewerIndex ? 'border border-primary' : ''}`;
		thumb.style = "width:80px;height:60px;object-fit:cover;cursor:pointer;";
		thumb.onclick = () => {
			viewerIndex = i;
			renderImageViewer();
		};
		thumbs.appendChild(thumb);
	});
}

function changeImage(step) {
	viewerIndex = (viewerIndex + step + viewerImages.length) % viewerImages.length;
	renderImageViewer();
}

