const API_BASE = '/admin/categories/watch-types/api';
let currentPage = 0, pageSize = 5;
let lastSearch = { keyword: '' };

$(document).ready(function () {
	loadTypes();

	// Tìm kiếm
	$('#searchForm').submit(function (e) {
		e.preventDefault();
		const keyword = $('#searchKeyword').val().trim();
		if (!keyword) {
			showToast("Vui lòng nhập từ khóa tìm kiếm", "danger");
			return;
		}
		lastSearch.keyword = keyword;
		loadTypes(0);
	});

	// Thêm / sửa loại
	$('#typeForm').submit(function (e) {
		e.preventDefault();
		const name = $('#typeName').val().trim();
		const typeId = $('#typeForm').data('typeId');

		$('#typeForm .error-message').remove();

		if (!name) {
			showError($('#typeName')[0], "Tên phân loại không được để trống");
			return;
		}

		const data = new URLSearchParams();
		data.append("typeName", name);
		if (typeId) data.append("typeId", typeId);

		$.ajax({
			type: typeId ? 'PUT' : 'POST',
			url: typeId ? `${API_BASE}/${typeId}` : API_BASE,
			data: data.toString(),
			contentType: 'application/x-www-form-urlencoded',
			success: function (res) {
				if (res.responseCode === 1) {
					showToast("Lưu thành công", "success");
					$('#typeModal').modal('hide');
					loadTypes(currentPage);
				} else {
					showToast(res.responseMsg || "Đã có lỗi xảy ra", "danger");
				}
			},
			error: function () {
				showToast("Lỗi trong quá trình gửi dữ liệu", "danger");
			}
		});
	});

	// Reset form khi modal đóng
	$('#typeModal').on('hidden.bs.modal', function () {
		$('#typeForm')[0].reset();
		$('#typeForm').removeData('typeId');
		$('#typeForm .error-message').remove();
	});
});

// Load danh sách loại
async function loadTypes(page = currentPage) {
	currentPage = page;
	const params = new URLSearchParams({
		keyword: lastSearch.keyword || '',
		page,
		size: pageSize
	});
	const url = lastSearch.keyword ? `${API_BASE}/search?${params}` : `${API_BASE}?${params}`;
	const res = await fetch(url);
	const data = await res.json();

	if (data.responseCode !== 1) {
		showToast(data.message || "Không thể tải dữ liệu", "danger");
		return;
	}

	const types = data.data.types;
	const totalPages = data.data.pagination.totalPages;

	if (currentPage > totalPages - 1 && totalPages > 0) {
		currentPage = totalPages - 1;
		await loadTypes(currentPage);
		return;
	}

	renderTypes(types);
	renderPagination(data.data.pagination);
}

// Hiển thị danh sách loại
function renderTypes(types) {
	const tbody = $('#typeTableBody').empty();
	if (!types.length) {
		tbody.append(`<tr><td colspan="3" class="text-muted text-center">Không có dữ liệu</td></tr>`);
		return;
	}

	types.forEach((t, index) => {
		tbody.append(`
			<tr id="typeRow-${t.typeId}">
				<td>${currentPage * pageSize + index + 1}</td>
				<td>${t.name}</td>
				<td>
					<button class="btn btn-sm btn-warning me-2" onclick="openEditType(${t.typeId}, '${t.name.replace(/'/g, "\\'")}')">Sửa</button>
					<button class="btn btn-sm btn-danger" onclick="deleteType(${t.typeId})">Xóa</button>
				</td>
			</tr>
		`);
	});
}

// Hiển thị phân trang
function renderPagination(pagination) {
	const cur = pagination.currentPage;
	const total = pagination.totalPages;
	const ul = $('#pagination').empty();
	if (total <= 1) return;

	const pages = [0];
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
			<a class="page-link" href="#" onclick="loadTypes(0); return false;">First</a>
		</li>
		<li class="page-item ${cur === 0 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadTypes(${cur - 1}); return false;">Previous</a>
		</li>
	`);

	pages.forEach(p => {
		if (p === '...') {
			ul.append(`<li class="page-item disabled"><span class="page-link">…</span></li>`);
		} else {
			ul.append(`
				<li class="page-item ${p === cur ? 'active' : ''}">
					<a class="page-link" href="#" onclick="loadTypes(${p}); return false;">${p + 1}</a>
				</li>
			`);
		}
	});

	ul.append(`
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadTypes(${cur + 1}); return false;">Next</a>
		</li>
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadTypes(${total - 1}); return false;">Last</a>
		</li>
	`);
}

// Mở modal chỉnh sửa
function openEditType(id, name) {
	$('#typeModalLabel').text("Cập nhật phân loại");
	$('#typeName').val(name);
	$('#typeForm').data('typeId', id);
	$('#typeModal').modal('show');
}

// Mở modal thêm mới
function openAddTypeModal() {
	$('#typeModalLabel').text("Thêm phân loại");
	$('#typeForm')[0].reset();
	$('#typeForm').removeData('typeId');
	$('#typeModal').modal('show');
}

// Xóa loại
function deleteType(typeId) {
	if (!confirm("Bạn chắc chắn muốn xóa phân loại này?")) return;

	$.ajax({
		type: 'DELETE',
		url: `${API_BASE}/${typeId}?typeId=${typeId}`,
		success: function (res) {
			if (res.responseCode === 1) {
				showToast("Xóa thành công", "success");
				$(`#typeRow-${typeId}`).remove();
				loadTypes();
			} else {
				showToast(res.responseMsg ||  "Xóa thất bại", "danger");
			}
		},
		error: function () {
			showToast("Lỗi khi xóa phân loại", "danger");
		}
	});
}

// Hiển thị lỗi input
function showError(input, message) {
	const error = document.createElement('div');
	error.className = 'text-danger error-message';
	error.innerText = message;
	input.parentNode.appendChild(error);
}

// Toast
function showToast(message, type = "success") {
	const toast = $('#errorToast');
	toast.removeClass("text-bg-success text-bg-danger").addClass(`text-bg-${type}`);
	toast.find('.toast-body').text(message);
	const bsToast = new bootstrap.Toast(toast[0]);
	bsToast.show();
}
