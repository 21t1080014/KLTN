// Biến thể sản phẩm + thao tác hàng loạt + import/export CSV cho trang /admin/products.
// Dùng các hàm dùng chung của product.js: showToast, loadProducts, currentPage.
const VARIANT_API = productId => `/admin/products/api/${productId}/variants`;
let variantsProductId = null;
let variantsProductImages = [];
let statusOptions = ['DRAFT', 'ACTIVE', 'INACTIVE', 'DISCONTINUED'];

$(document).ready(function() {
	fetch('/admin/products/api/form-options').then(r => r.json()).then(j => {
		if (j.responseCode === 1 && j.data.statuses) statusOptions = j.data.statuses;
		const opts = statusOptions.map(s => `<option value="${s}">${s}</option>`).join('');
		$('#bulkStatus').html(opts);
		$('#variantStatus').html(opts);
	});

	$('#chkAllProducts').on('change', function() {
		$('.chk-product').prop('checked', this.checked);
	});

	$('#variantForm').on('submit', async function(e) {
		e.preventDefault();
		const id = $('#variantId').val();
		const body = new URLSearchParams();
		body.append('variantName', $('#variantName').val());
		body.append('color', $('#variantColor').val());
		body.append('strapOption', $('#variantStrap').val());
		body.append('caseSize', $('#variantSize').val());
		body.append('price', $('#variantPrice').val());
		body.append('status', $('#variantStatus').val());
		body.append('lowStockThreshold', $('#variantThreshold').val());
		let url = VARIANT_API(variantsProductId), method = 'POST';
		if (id) {
			url += '/' + id;
			method = 'PUT';
		} else {
			body.append('sku', $('#variantSku').val());
			body.append('initialQuantity', $('#variantInitialQty').val() || 0);
		}
		const res = await fetch(url, { method, headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body });
		const json = await res.json();
		if (json.responseCode !== 1) return showToast(json.responseMsg, 'danger');
		// lưu ảnh riêng của biến thể
		const variantId = json.data.variantId;
		const imgBody = new URLSearchParams();
		$('.variant-img-chk:checked').each(function() { imgBody.append('imageIds', this.value); });
		await fetch(`${VARIANT_API(variantsProductId)}/${variantId}/images`, {
			method: 'PUT', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: imgBody
		});
		showToast('Đã lưu biến thể', 'success');
		resetVariantForm();
		loadVariants();
		loadProducts(currentPage);
	});
});

async function openVariantsModal(productId) {
	variantsProductId = productId;
	const res = await fetch(`/admin/products/api/${productId}`);
	const json = await res.json();
	if (json.responseCode !== 1) return showToast(json.responseMsg, 'danger');
	$('#variantsProductName').text(json.data.name);
	variantsProductImages = json.data.images || [];
	resetVariantForm();
	await loadVariants();
	$('#variantsModal').modal('show');
}

async function loadVariants() {
	const res = await fetch(VARIANT_API(variantsProductId));
	const json = await res.json();
	if (json.responseCode !== 1) return showToast(json.responseMsg, 'danger');
	const rows = json.data.map(v => `
		<tr>
			<td>${v.sku}${v.default ? ' <span class="badge">mặc định</span>' : ''}</td>
			<td>${v.variantName}</td><td>${v.color || ''}</td><td>${v.strapOption || ''}</td><td>${v.caseSize || ''}</td>
			<td>${Number(v.price).toLocaleString('vi-VN')}đ</td><td>${v.status}</td>
			<td>${v.quantity} / ${v.reservedQuantity} / ${v.availableQuantity}</td>
			<td>
				<button class="btn btn-sm btn-warning" onclick='editVariant(${JSON.stringify(v)})'>Sửa</button>
				${v.default || v.status !== 'ACTIVE' ? '' : `<button class="btn btn-sm btn-outline-primary" onclick="makeDefault(${v.variantId})">Đặt mặc định</button>`}
				${v.default || document.body.dataset.role !== 'admin' ? '' : `<button class="btn btn-sm btn-danger" onclick="deleteVariant(${v.variantId})">Xóa</button>`}
			</td>
		</tr>`).join('');
	$('#variantsBody').html(rows);
}

function renderVariantImages(selectedIds) {
	$('#variantImages').html(variantsProductImages.map(img => `
		<label class="form-check-label" style="cursor:pointer">
			<input type="checkbox" class="form-check-input variant-img-chk" value="${img.imageId}" ${selectedIds.includes(img.imageId) ? 'checked' : ''}>
			<img src="/admin/products/api/img/${img.url}" style="width:60px;height:60px;object-fit:contain" alt="">
		</label>`).join('') || '<span class="text-muted">Sản phẩm chưa có ảnh</span>');
}

function editVariant(v) {
	$('#variantFormTitle').text('Sửa biến thể ' + v.sku);
	$('#variantId').val(v.variantId);
	$('#variantSku').val(v.sku).prop('disabled', true);
	$('#variantName').val(v.variantName);
	$('#variantColor').val(v.color || '');
	$('#variantStrap').val(v.strapOption || '');
	$('#variantSize').val(v.caseSize || '');
	$('#variantPrice').val(v.price);
	$('#variantStatus').val(v.status);
	$('#variantThreshold').val(v.lowStockThreshold);
	$('#variantInitialWrap').hide();
	renderVariantImages(v.imageIds || []);
}

function resetVariantForm() {
	$('#variantFormTitle').text('Thêm biến thể');
	$('#variantForm')[0].reset();
	$('#variantId').val('');
	$('#variantSku').prop('disabled', false);
	$('#variantStatus').val('ACTIVE');
	$('#variantThreshold').val(5);
	$('#variantInitialQty').val(0);
	$('#variantInitialWrap').show();
	renderVariantImages([]);
}

async function makeDefault(variantId) {
	const res = await fetch(`${VARIANT_API(variantsProductId)}/${variantId}/default`, { method: 'PUT' });
	const json = await res.json();
	showToast(json.responseMsg, json.responseCode === 1 ? 'success' : 'danger');
	loadVariants();
	loadProducts(currentPage);
}

async function deleteVariant(variantId) {
	if (!confirm('Xóa biến thể này?')) return;
	const res = await fetch(`${VARIANT_API(variantsProductId)}/${variantId}`, { method: 'DELETE' });
	const json = await res.json();
	showToast(json.responseMsg, json.responseCode === 1 ? 'success' : 'danger');
	loadVariants();
	loadProducts(currentPage);
}

function selectedProductIds() {
	return $('.chk-product:checked').map(function() { return this.value; }).get();
}

async function postBulk(url, params) {
	const ids = selectedProductIds();
	if (!ids.length) return showToast('Chưa chọn sản phẩm nào', 'danger');
	const body = new URLSearchParams(params);
	ids.forEach(id => body.append('productIds', id));
	const res = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body });
	const json = await res.json();
	if (json.responseCode !== 1 && json.data && json.data.errors) {
		showToast(json.responseMsg + ' — ' + json.data.errors.slice(0, 3).join('; '), 'danger');
	} else {
		showToast(json.responseMsg, json.responseCode === 1 ? 'success' : 'danger');
	}
	if (json.responseCode === 1) loadProducts(currentPage);
}

function bulkApplyStatus() {
	postBulk('/admin/products/api/bulk-status', { status: $('#bulkStatus').val() });
}

function bulkApplyPrice() {
	const value = $('#bulkPriceValue').val();
	if (value === '') return showToast('Nhập giá trị', 'danger');
	postBulk('/admin/products/api/bulk-price', { mode: $('#bulkPriceMode').val(), value });
}

async function importCsv() {
	const file = $('#importFile')[0].files[0];
	if (!file) return showToast('Chọn file CSV trước', 'danger');
	const fd = new FormData();
	fd.append('file', file);
	const res = await fetch('/admin/products/api/import', { method: 'POST', body: fd });
	const json = await res.json();
	if (json.responseCode !== 1 && json.data && json.data.errors) {
		showToast(json.responseMsg + ' — ' + json.data.errors.slice(0, 3).join('; '), 'danger');
	} else {
		showToast(json.responseMsg, json.responseCode === 1 ? 'success' : 'danger');
	}
	if (json.responseCode === 1) loadProducts(currentPage);
}
