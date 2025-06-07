const apiUrl = '/admin/voucher/api';
let editCode = null;
let voucherModal = null;
let confirmDeleteModal = null;
let voucherToDeleteSpan = null;
let deleteVoucherCode = null;

document.addEventListener('DOMContentLoaded', () => {
	loadVouchers();
	confirmDeleteModal = new bootstrap.Modal(document.getElementById('confirmDeleteModal'));
	voucherToDeleteSpan = document.getElementById('voucherToDelete');
	document.getElementById('voucher-form').addEventListener('submit', onSubmitForm);
	document.getElementById('cancel-btn').addEventListener('click', resetForm);
	document.getElementById('confirmDeleteBtn').addEventListener('click', onConfirmDelete);
});

async function loadVouchers(page = 0) {
	const size = 10;
	const keyword = document.getElementById('searchInput').value || '';
	const res = await fetch(`${apiUrl}?page=${page}&size=${size}&keyword=${encodeURIComponent(keyword)}`);
	const result = await res.json();
	if (result.responseCode !== 1) return;

	renderVoucherTable(result.data.vouchers);
	renderPagination(result.data.pagination);
}

function renderVoucherTable(vouchers) {
	const tbody = document.getElementById('voucher-body');
	tbody.innerHTML = '';

	vouchers.forEach(v => {
		const tr = document.createElement('tr');
		tr.innerHTML = `
			<td>${v.voucherCode}</td>
			<td>${v.description || ''}</td>
			<td>${v.discountType}</td>
			<td>${v.discountValue}</td>
			<td>${v.minOrderAmount}</td>
			<td>${v.startAt}</td>
			<td>${v.endAt}</td>
			<td>${v.usageLimit}</td>
			<td>
				<button class="btn btn-sm btn-warning me-1" onclick="onEdit('${v.voucherCode}')">Sửa</button>
				<button class="btn btn-sm btn-danger" onclick="onDelete('${v.voucherCode}')">Xóa</button>
			</td>
		`;
		tbody.appendChild(tr);
	});
}

function renderPagination({ currentPage: cur, totalPages: total }) {
	const ul = document.getElementById('pagination');
	ul.innerHTML = '';
	if (total <= 1) return;

	const createPageItem = (label, page, disabled = false, active = false) => {
		const li = document.createElement('li');
		li.className = `page-item ${disabled ? 'disabled' : ''} ${active ? 'active' : ''}`;
		const a = document.createElement('a');
		a.className = 'page-link';
		a.href = '#';
		a.innerHTML = label;
		if (!disabled) {
			a.addEventListener('click', e => {
				e.preventDefault();
				loadVouchers(page);
			});
		}
		li.appendChild(a);
		return li;
	};

	const pages = [0];
	if (total <= 7) {
		for (let i = 1; i < total - 1; i++) pages.push(i);
	} else if (cur <= 3) {
		for (let i = 1; i <= 3; i++) pages.push(i);
		pages.push('...');
	} else if (cur >= total - 4) {
		pages.push('...');
		for (let i = total - 4; i < total - 1; i++) pages.push(i);
	} else {
		pages.push('...', cur - 1, cur, cur + 1, '...');
	}
	pages.push(total - 1);

	// First & Prev
	ul.appendChild(createPageItem('<i class="bi bi-chevron-double-left"></i>', 0, cur === 0));
	ul.appendChild(createPageItem('<i class="bi bi-chevron-left"></i>', cur - 1, cur === 0));

	// Page numbers
	pages.forEach(p => {
		if (p === '...') {
			const li = document.createElement('li');
			li.className = 'page-item disabled';
			li.innerHTML = `<span class="page-link">…</span>`;
			ul.appendChild(li);
		} else {
			ul.appendChild(createPageItem(p + 1, p, false, p === cur));
		}
	});

	// Next & Last
	ul.appendChild(createPageItem('<i class="bi bi-chevron-right"></i>', cur + 1, cur === total - 1));
	ul.appendChild(createPageItem('<i class="bi bi-chevron-double-right"></i>', total - 1, cur === total - 1));
}

async function onSubmitForm(e) {
	e.preventDefault();
	const form = e.target;

	const data = {
		voucherCode: form.voucherCode.value,
		description: form.description.value,
		discountType: form.discountType.value,
		discountValue: parseFloat(form.discountValue.value) || 0,
		minOrderAmount: parseFloat(form.minOrderAmount.value) || 0,
		startAt: form.startAt.value,
		endAt: form.endAt.value,
		usageLimit: parseInt(form.usageLimit.value) || 0
	};

	const method = editCode ? 'PUT' : 'POST';
	const url = editCode ? `${apiUrl}?voucherCode=${encodeURIComponent(editCode)}` : apiUrl;

	const res = await fetch(url, {
		method,
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(data)
	});

	const result = await res.json();
	console.log(result);
	resetForm();
	loadVouchers();
	voucherModal.hide();
}

async function onEdit(code) {
	const res = await fetch(`${apiUrl}?page=0&size=100`);
	const list = (await res.json()).data.vouchers;
	const v = list.find(x => x.voucherCode === code);
	if (!v) return;

	editCode = code;
	const form = document.getElementById('voucher-form');
	form.voucherCode.value = v.voucherCode;
	form.voucherCode.disabled = true;
	form.description.value = v.description;
	form.discountType.value = v.discountType;
	form.discountValue.value = v.discountValue;
	form.minOrderAmount.value = v.minOrderAmount;
	form.startAt.value = v.startAt;
	form.endAt.value = v.endAt;
	form.usageLimit.value = v.usageLimit;

	document.getElementById('submit-btn').textContent = 'Cập nhật';
	document.getElementById('cancel-btn').hidden = false;

	voucherModal = new bootstrap.Modal(document.getElementById('voucherModal'));
	voucherModal.show();
}

function openCreateModal() {
	resetForm();
	voucherModal = new bootstrap.Modal(document.getElementById('voucherModal'));
	voucherModal.show();
}

function resetForm() {
	editCode = null;
	const form = document.getElementById('voucher-form');
	form.reset();
	form.voucherCode.disabled = false;
	document.getElementById('submit-btn').textContent = 'Thêm mới';
	document.getElementById('cancel-btn').hidden = true;
}

function onDelete(code) {
	deleteVoucherCode = code;
	voucherToDeleteSpan.textContent = code;
	confirmDeleteModal.show();
}

async function onConfirmDelete() {
	if (!deleteVoucherCode) return;

	const res = await fetch(`${apiUrl}?voucherCode=${encodeURIComponent(deleteVoucherCode)}`, {
		method: 'DELETE'
	});
	const result = await res.json();
	console.log(result);
	confirmDeleteModal.hide();
	loadVouchers();
	deleteVoucherCode = null;
}
