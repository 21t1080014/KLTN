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
		const productId = $('#editProductId').val();
		const quantity = $('#editQuantity').val();

		try {
			// Gửi PUT với query param ?quantity=
			const res = await fetch(`${API_BASE}/${productId}?quantity=${encodeURIComponent(quantity)}`, {
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
          <strong>${inventory.productName}</strong><br />
          <small>Số lượng: ${inventory.quantity}</small>
        </div>
        <div>
          <button class="btn btn-sm btn-warning me-2" onclick="openEditModal('${inventory.productId}')">Sửa</button>
          <button class="btn btn-sm btn-danger" onclick="confirmDelete('${inventory.productId}')">Xóa</button>
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
async function openEditModal(productId) {
	try {
		const res = await fetch(`${API_BASE}/product/${productId}`);
		const json = await res.json();
		if (json.responseCode !== 1) {
			showErrorToast(json.responseMsg || 'Không tải được thông tin');
			return;
		}
		const item = json.data;
		$('#editProductId').val(item.productId);
		$('#editProductName').val(item.productName);
		$('#editQuantity').val(item.quantity);
		new bootstrap.Modal(document.getElementById('editInventoryModal')).show();
	} catch (e) {
		console.error(e);
		showErrorToast('Lỗi khi tải dữ liệu');
	}
}
async function checkLowStock() {
	try {
		const threshold = 5; // hoặc lấy dynamic nếu bạn có input
		const res = await fetch(`${API_BASE}/low-stock?threshold=${threshold}`);
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
          <span>${item.productName}</span>
          <span class="badge bg-danger rounded-pill">${item.quantity}</span>
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
function confirmDelete(productId) {
	// gắn productId lên nút confirm
	$('#confirmDeleteBtn').data('product-id', productId);
	new bootstrap.Modal(document.getElementById('deleteConfirmModal')).show();
}

// Khi người dùng bấm nút Delete trong modal confirm
$('#confirmDeleteBtn').on('click', async function() {
	const productId = $(this).data('product-id');
	try {
		const res = await fetch(`${API_BASE}/${productId}`, { method: 'DELETE' });
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