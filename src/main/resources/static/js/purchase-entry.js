const API_BASE = '/admin/purchases/api';
let currentPage = 0, pageSize = 5;
let lastSearch = {
	productName: '',
	fromDate: '',
	toDate: '',
	minPrice: '',
	maxPrice: ''
};
$(document).ready(function() {
	loadEntries();

	// Gợi ý tên sản phẩm khi nhập
	const $productInput = $('#productSearch');
	const $productIdHidden = $('#productId');
	const $suggestionList = $('#productSuggestionList');
	let debounceTimer;
	$('#searchForm').submit(function(e) {
		e.preventDefault();
		lastSearch.productName = $('#productName').val().trim();
		lastSearch.fromDate = $('#fromDate').val();
		lastSearch.toDate = $('#toDate').val();
		lastSearch.minPrice = $('#minPrice').val();
		lastSearch.maxPrice = $('#maxPrice').val();

		loadEntries(0);
	});

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

	$(document).on('click', function(e) {
		if (!$(e.target).closest('#productSearch, #productSuggestionList').length) {
			$suggestionList.hide();
		}
	});

	// Gửi form lưu phiếu nhập
	$('#purchaseEntryForm').on('submit', function(e) {
		e.preventDefault();
		submitEntryForm();
	});
});

// Mở modal thêm
function openAddBrandModal() {
	clearForm();
	$('#purchaseEntryModal').modal('show');
}

async function loadEntries(page = currentPage) {
	currentPage = page;

	const params = new URLSearchParams({
		productName: lastSearch.productName || '',
		fromDate: lastSearch.fromDate || '',
		toDate: lastSearch.toDate || '',
		minPrice: lastSearch.minPrice || '',
		maxPrice: lastSearch.maxPrice || '',
		page,
		size: pageSize
	});

	const hasSearchParams = Object.values(lastSearch).some(v => v); // Kiểm tra có ít nhất 1 giá trị
	const url = hasSearchParams ? `${API_BASE}/search?${params}` : `${API_BASE}?${params}`;

	try {
		const res = await fetch(url);
		const data = await res.json();

		if (data.responseCode !== 1) {
			showErrorToast(data.responseMsg || "Không thể tải dữ liệu phiếu nhập", "danger");
			return;
		}

		const entries = data.data.entry || [];
		const totalPages = data.data.pagination.totalPages;

		if (!Array.isArray(entries)) {
			showErrorToast("Dữ liệu phiếu nhập không hợp lệ", "danger");
			return;
		}

		const $entryList = $('#entryList').empty();

		if (!entries.length) {
			$entryList.append(`<li class="list-group-item text-muted text-center">Không có dữ liệu</li>`);
		} else {
			entries.forEach(entry => {
				const item = `
                    <li class="list-group-item d-flex justify-content-between align-items-center">
                        <div>
                            <strong>${entry.productName}</strong> - ${entry.quantity} cái - ${formatCurrency(entry.importPrice)}
                            <br><small class="text-muted">Ngày nhập: ${formatDate(entry.importedAt)}</small>
                        </div>
                        <div>
                            <button class="btn btn-sm btn-warning me-2" onclick="editEntry(${entry.entryId})">Sửa</button>
                            <button class="btn btn-sm btn-danger" onclick="showDeleteModal(${entry.entryId})">Xóa</button>
                        </div>
                    </li>`;
				$entryList.append(item);
			});
		}

		$('#totalEntryCount').text(`Tổng số: ${data.data.totalCount} mục`);
		renderPagination(data.data.pagination);

	} catch (error) {
		console.error("Lỗi khi tải phiếu nhập:", error);
		showErrorToast("Đã xảy ra lỗi khi tải dữ liệu", "danger");
	}
}



function renderPagination(pagination) {
	const cur = pagination.currentPage;
	const total = pagination.totalPages;
	const ul = $('#pagination').empty();
	if (total <= 1) return;

	const pages = [];
	pages.push(0); // luôn có trang đầu

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

	pages.push(total - 1); // luôn có trang cuối

	// First & Previous
	ul.append(`
		<li class="page-item ${cur === 0 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadEntries(0); return false;">First</a>
		</li>
		<li class="page-item ${cur === 0 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadEntries(${cur - 1}); return false;">Previous</a>
		</li>
	`);

	// Số trang & "..."
	pages.forEach(p => {
		if (p === '...') {
			ul.append(`
				<li class="page-item disabled"><span class="page-link">…</span></li>
			`);
		} else {
			ul.append(`
				<li class="page-item ${p === cur ? 'active' : ''}">
					<a class="page-link" href="#" onclick="loadEntries(${p}); return false;">${p + 1}</a>
				</li>
			`);
		}
	});

	// Next & Last
	ul.append(`
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadEntries(${cur + 1}); return false;">Next</a>
		</li>
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadEntries(${total - 1}); return false;">Last</a>
		</li>
	`);
}

// Mở modal sửa
function editEntry(id) {
	$.get(`/admin/purchases/${id}`)
		.done(res => {
			const entry = res.data;
			$('#entryId').val(entry.entryId);
			$('#productSearch').val(entry.productName);
			$('#productId').val(entry.productId);
			$('#quantity').val(entry.quantity);
			$('#importPrice').val(entry.importPrice);
			if (entry.importedAt) {
				const isoString = new Date(entry.importedAt).toISOString().slice(0, 16);
				$('#importedAt').val(isoString);
			}
			$('#note').val(entry.note || '');
			$('#purchaseEntryModal').modal('show');
		})
		.fail(() => showErrorToast("Không thể tải phiếu nhập"));
}

// Gửi dữ liệu thêm/sửa
function submitEntryForm() {
	const entryId = $('#entryId').val();
	const payload = {
		product: { productId: $('#productId').val() },
		quantity: $('#quantity').val(),
		importPrice: $('#importPrice').val(),
		importedAt: $('#importedAt').val(),
		note: $('#note').val()
	};

	const request = entryId
		? $.ajax({ url: `${API_BASE}/${entryId}`, type: 'PUT', contentType: 'application/json', data: JSON.stringify(payload) })
		: $.ajax({ url: `${API_BASE}`, type: 'POST', contentType: 'application/json', data: JSON.stringify(payload) });

	request.done(() => {
		$('#purchaseEntryModal').modal('hide');
		loadEntries();
	})
		.fail(res => {
			const errorMsg = res.responseMsg || "Lưu phiếu nhập thất bại";
			showErrorToast(errorMsg);
		});
}

// Hiển thị modal xác nhận xóa
let deletingId = null;
function showDeleteModal(id) {
	deletingId = id;
	$('#deleteConfirmModal').modal('show');
}

$('#confirmDeleteBtn').on('click', function() {
	if (!deletingId) return;

	$.ajax({
		url: `${API_BASE}/${deletingId}`,
		type: 'DELETE'
	}).done(() => {
		$('#deleteConfirmModal').modal('hide');
		loadEntries();
	}).fail(res => {
		const errorMsg = res.responseJSON?.message || "Xóa phiếu nhập thất bại";
		showErrorToast(errorMsg);
	});

	deletingId = null;
});

// Helpers
function clearForm() {
	$('#purchaseEntryForm')[0].reset();
	$('#entryId').val('');
	$('#productId').val('');
	$('#productSuggestionList').hide();
}

function formatDate(dateStr) {
	if (!dateStr) return '';
	return new Date(dateStr).toLocaleString('vi-VN');
}

function formatCurrency(amount) {
	return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function showErrorToast(msg) {
	const $toast = $('#errorToast');
	$toast.find('.toast-body').text(msg);
	const toast = new bootstrap.Toast($toast[0]);
	toast.show();
}
