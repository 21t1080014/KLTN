const roleFlags = document.getElementById('roleFlags');
const canEdit = !roleFlags || roleFlags.dataset.canEdit === 'true';
const canDelete = !roleFlags || roleFlags.dataset.canDelete === 'true';
const API_BASE = '/admin/inventory/api';
let currentPage = 0, pageSize = 5;
let lastSearch = {
	productName: '',
	fromDate: '',
	toDate: ''
};
$(document).ready(function() {
	loadInventory(0);
	checkLowStock();
	$('#btnLowStock').on('click', checkLowStock);
	$('#searchForm').on('submit', function(e) {
		e.preventDefault();
		lastSearch.productName = $('#productName').val();
		lastSearch.fromDate = $('#fromDate').val();
		lastSearch.toDate = $('#toDate').val();
		loadInventory(0);
	});
	$('#editInventoryForm').on('submit', async function(e) {
		e.preventDefault();
		const variantId = $('#editProductId').val(); // ô ẩn này giờ chứa variantId
		const quantity = $('#editQuantity').val();
		const params = new URLSearchParams({ quantity, note: $('#editNote').val(), lowStockThreshold: $('#editThreshold').val() });

		try {
			const res = await fetch(`${API_BASE}/variant/${variantId}?${params}`, {
				method: 'PUT'
			});
			const json = await res.json();

			if (json.responseCode === 1) {
				// Ẩn modal và reload danh sách
				bootstrap.Modal.getInstance(document.getElementById('editInventoryModal')).hide();
				loadInventory();
			} else {
				showErrorToast(json.responseMsg || 'Cập nhật thất bại');
			}
		} catch (err) {
			console.error(err);
			showErrorToast('Lỗi khi cập nhật tồn kho');
		}
	});
});
async function loadInventory(page = currentPage) {
	const params = new URLSearchParams({
		productName: lastSearch.productName || '',
		fromDate: lastSearch.fromDate || '',
		toDate: lastSearch.toDate || '',
		page,
		size: pageSize
	});
	const hasSearchParams = Object.values(lastSearch).some(v => v);
	const url = hasSearchParams ? `${API_BASE}/search?${params}` : `${API_BASE}?${params}`;
	try {
		const res = await fetch(url);
		const data = await res.json();

		if (data.responseCode === 1) {
			renderInventoryList(data.data.inventory);
			renderPagination(data.data.pagination);
			$('#totalInventoryCount').text(`Tổng số: ${data.data.totalCount}`);
		} else {
			showErrorToast(data.message || 'Không thể tải dữ liệu');
		}
	} catch (error) {
		console.error(error);
		showErrorToast('Lỗi kết nối đến server');
	}
}
function renderInventoryList(list) {
	const $list = $('#inventoryList');
	$list.empty();

	if (list.length === 0) {
		$list.append('<li class="list-group-item text-muted">Không có dữ liệu</li>');
		return;
	}

	list.forEach(inventory => {
		const html = `
      <li class="list-group-item d-flex justify-content-between align-items-center">
        <div>
          <strong>${inventory.productName}</strong>
          ${inventory.variantName ? `<span class="text-muted"> — ${inventory.variantName} (${inventory.sku})</span>` : ''}
          ${inventory.lowStock ? '<span class="badge bg-danger ms-2">Tồn thấp</span>' : ''}<br />
          <small>Tồn thực: ${inventory.quantity} · Đang giữ: ${inventory.reservedQuantity} · Có thể bán: ${inventory.availableQuantity} · Ngưỡng cảnh báo: ${inventory.lowStockThreshold}</small>
        </div>
        <div>
          ${canEdit ? `<button class="btn btn-sm btn-warning me-2" onclick="openEditModal('${inventory.variantId}')">Sửa</button>` : ''}
          <button class="btn btn-sm btn-secondary me-2" onclick="openHistory('${inventory.variantId}')">Lịch sử</button>
          ${canDelete ? `<button class="btn btn-sm btn-danger" onclick="confirmDelete('${inventory.variantId}')">Xóa</button>` : ''}
        </div>
      </li>
    `;
		$list.append(html);
	});
}

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
      <a class="page-link" href="#" onclick="loadInventory(0);return false;">First</a>
    </li>
    <li class="page-item ${cur === 0 ? 'disabled' : ''}">
      <a class="page-link" href="#" onclick="loadInventory(${cur - 1});return false;">Previous</a>
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
          <a class="page-link" href="#" onclick="loadInventory(${p});return false;">${p + 1}</a>
        </li>
      `);
		}
	});

	// Next & Last
	ul.append(`
    <li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
      <a class="page-link" href="#" onclick="loadInventory(${cur + 1});return false;">Next</a>
    </li>
    <li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
      <a class="page-link" href="#" onclick="loadInventory(${total - 1});return false;">Last </a>
    </li>
  `);
}
// Mở modal Edit và load dữ liệu từng item
async function openEditModal(variantId) {
	try {
		const res = await fetch(`${API_BASE}/variant/${variantId}`);
		const json = await res.json();
		if (json.responseCode !== 1) {
			showErrorToast(json.responseMsg || 'Không tải được thông tin');
			return;
		}
		const item = json.data;
		$('#editProductId').val(item.variantId);
		$('#editProductName').val(item.productName + (item.variantName ? ' — ' + item.variantName + ' (' + item.sku + ')' : ''));
		$('#editQuantity').val(item.quantity);
		$('#editThreshold').val(item.lowStockThreshold);
		$('#editNote').val('');
		new bootstrap.Modal(document.getElementById('editInventoryModal')).show();
	} catch (e) {
		console.error(e);
		showErrorToast('Lỗi khi tải dữ liệu');
	}
}
async function checkLowStock() {
	try {
		// không truyền threshold: server dùng ngưỡng cảnh báo riêng của từng biến thể
		const res = await fetch(`${API_BASE}/low-stock`);
		const json = await res.json();
		if (json.responseCode !== 1) {
			showErrorToast(json.responseMsg || 'Không lấy được dữ liệu tồn kho thấp');
			return;
		}
		const list = json.data; // mảng InventoryDto
		if (list.length === 0) {
			// không có sản phẩm thấp: hiển thị toast thông báo
			showErrorToast('Không có sản phẩm tồn kho thấp');
			return;
		}
		// nếu có, đổ vào modal và show
		const $lowList = $('#lowStockList').empty();
		list.forEach(item => {
			$lowList.append(`
        <li class="list-group-item d-flex justify-content-between align-items-center">
          <span>${item.productName}${item.variantName ? ' — ' + item.variantName + ' (' + item.sku + ')' : ''}</span>
          <span class="badge bg-danger rounded-pill">${item.availableQuantity} / ngưỡng ${item.lowStockThreshold}</span>
        </li>
      `);
		});
		new bootstrap.Modal(document.getElementById('lowStockModal')).show();
	} catch (e) {
		console.error(e);
		showErrorToast('Lỗi khi kiểm tra tồn kho thấp');
	}
}
// Mở modal xác nhận xóa
function confirmDelete(variantId) {
	// gắn variantId lên nút confirm
	$('#confirmDeleteBtn').data('product-id', variantId);
	new bootstrap.Modal(document.getElementById('deleteConfirmModal')).show();
}

// Khi người dùng bấm nút Delete trong modal confirm
$('#confirmDeleteBtn').on('click', async function() {
	const productId = $(this).data('product-id');
	try {
		const res = await fetch(`${API_BASE}/variant/${productId}`, { method: 'DELETE' });
		const json = await res.json();
		if (json.responseCode === 1) {
			bootstrap.Modal.getInstance(document.getElementById('deleteConfirmModal')).hide();
			loadInventory();
		} else {
			showErrorToast(json.responseMsg || 'Xóa thất bại');
		}
	} catch (e) {
		console.error(e);
		showErrorToast('Lỗi khi xóa');
	}
});

function showErrorToast(msg) {
	const $toast = $('#errorToast');
	$toast.find('.toast-body').text(msg);
	const toast = new bootstrap.Toast($toast[0]);
	toast.show();
}
// ---- Lịch sử biến động tồn kho của 1 biến thể ----
async function openHistory(variantId, page = 0) {
	try {
		const res = await fetch(`${API_BASE}/movements?variantId=${variantId}&page=${page}&size=10`);
		const json = await res.json();
		if (json.responseCode !== 1) return showErrorToast(json.responseMsg || 'Không tải được lịch sử');
		const rows = json.data.movements.map(m => `
			<tr>
				<td>${m.createdAt || ''}</td><td>${m.type}</td>
				<td class="${m.change < 0 ? 'text-danger' : 'text-success'}">${m.change > 0 ? '+' : ''}${m.change}</td>
				<td>${m.before} → ${m.after}</td><td>${m.reference || ''}</td><td>${m.note || ''}</td><td>${m.createdBy || ''}</td>
			</tr>`).join('');
		$('#historyBody').html(rows || '<tr><td colspan="7" class="text-muted text-center">Chưa có biến động</td></tr>');
		const pg = json.data.pagination;
		$('#historyPager').html(pg.totalPages > 1 ? Array.from({ length: pg.totalPages }, (_, i) =>
			`<button class="btn btn-sm ${i === pg.currentPage ? 'btn-primary' : 'btn-outline-secondary'} me-1" onclick="openHistory('${variantId}', ${i})">${i + 1}</button>`).join('') : '');
		bootstrap.Modal.getOrCreateInstance(document.getElementById('historyModal')).show();
	} catch (e) {
		console.error(e);
		showErrorToast('Lỗi khi tải lịch sử');
	}
}
