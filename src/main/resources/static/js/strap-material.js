const API_BASE = '/admin/categories/strap-materials/api';
let currentPage = 0, pageSize = 5;
let lastSearch = { keyword: '' };

$(document).ready(function () {
	loadStraps();

	// Tìm kiếm
	$('#searchForm').submit(function (e) {
		e.preventDefault();
		const keyword = $('#searchKeyword').val().trim();
		if (!keyword) {
			showToast("Vui lòng nhập từ khóa tìm kiếm", "danger");
			return;
		}
		lastSearch.keyword = keyword;
		loadStraps(0);
	});

	// Thêm / Sửa chất liệu
	$('#strapForm').submit(function (e) {
		e.preventDefault();
		const name = $('#strapName').val().trim();
		const strapId = $('#strapForm').data('strapId');

		$('#strapForm .error-message').remove();

		if (!name) {
			showError($('#strapName')[0], "Tên chất liệu không được để trống");
			return;
		}

		const data = new URLSearchParams();
		data.append("strapName", name);
		if (strapId) data.append("strapId", strapId);

		$.ajax({
			type: strapId ? 'PUT' : 'POST',
			url: strapId ? `${API_BASE}/${strapId}` : API_BASE,
			data: data.toString(),
			contentType: 'application/x-www-form-urlencoded',
			success: function (res) {
				if (res.responseCode === 1) {
					showToast("Lưu thành công", "success");
					$('#strapModal').modal('hide');
					loadStraps(currentPage);
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
	$('#strapModal').on('hidden.bs.modal', function () {
		$('#strapForm')[0].reset();
		$('#strapForm').removeData('strapId');
		$('#strapForm .error-message').remove();
	});
});

// Load danh sách chất liệu
async function loadStraps(page = currentPage) {
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

	const straps = data.data.strapMaterials;
	const totalPages = data.data.pagination.totalPages;

	if (currentPage > totalPages - 1 && totalPages > 0) {
		currentPage = totalPages - 1;
		await loadStraps(currentPage);
		return;
	}

	renderStraps(straps);
	renderPagination(data.data.pagination);
}

// Hiển thị bảng chất liệu
function renderStraps(straps) {
	const tbody = $('#strapTableBody').empty();
	if (!straps.length) {
		tbody.append(`<tr><td colspan="3" class="text-muted text-center">Không có dữ liệu</td></tr>`);
		return;
	}

	straps.forEach((s, index) => {
		tbody.append(`
			<tr id="strapRow-${s.strapId}">
				<td>${currentPage * pageSize + index + 1}</td>
				<td>${s.name}</td>
				<td>
					<button class="btn btn-sm btn-warning me-2" onclick="openEditStrap(${s.strapId}, '${s.name.replace(/'/g, "\\'")}')">Sửa</button>
					<button class="btn btn-sm btn-danger" onclick="deleteStrap(${s.strapId})">Xóa</button>
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
			<a class="page-link" href="#" onclick="loadStraps(0); return false;">First</a>
		</li>
		<li class="page-item ${cur === 0 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadStraps(${cur - 1}); return false;">Previous</a>
		</li>
	`);

	pages.forEach(p => {
		if (p === '...') {
			ul.append(`<li class="page-item disabled"><span class="page-link">…</span></li>`);
		} else {
			ul.append(`
				<li class="page-item ${p === cur ? 'active' : ''}">
					<a class="page-link" href="#" onclick="loadStraps(${p}); return false;">${p + 1}</a>
				</li>
			`);
		}
	});

	ul.append(`
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadStraps(${cur + 1}); return false;">Next</a>
		</li>
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadStraps(${total - 1}); return false;">Last</a>
		</li>
	`);
}

// Mở modal thêm chất liệu
function openAddStrapModal() {
	$('#strapModalLabel').text("Thêm chất liệu");
	$('#strapForm')[0].reset();
	$('#strapForm').removeData('strapId');
	$('#strapModal').modal('show');
}

// Mở modal sửa chất liệu
function openEditStrap(id, name) {
	$('#strapModalLabel').text("Chỉnh sửa chất liệu");
	$('#strapName').val(name);
	$('#strapForm').data('strapId', id);
	$('#strapModal').modal('show');
}

// Xóa chất liệu
function deleteStrap(id) {
	if (!confirm("Bạn có chắc muốn xóa chất liệu này?")) return;

	$.ajax({
		type: 'DELETE',
		url: `${API_BASE}/${id}?strapId=${id}`,
		success: function (res) {
			if (res.responseCode === 1) {
				showToast("Xóa thành công", "success");
				loadStraps(currentPage);
			} else {
				showToast(res.responseMsg || "Xóa thất bại", "danger");
			}
		},
		error: function () {
			showToast("Lỗi khi xóa", "danger");
		}
	});
}

// Hiển thị thông báo lỗi
function showError(input, msg) {
	const error = `<div class="error-message text-danger mt-1 small">${msg}</div>`;
	$(input).after(error);
}

// Hiển thị toast
function showToast(msg, type = "danger") {
	const toast = $('#errorToast');
	toast.removeClass("text-bg-success text-bg-danger").addClass(`text-bg-${type}`);
	toast.find('.toast-body').text(msg);
	const bsToast = new bootstrap.Toast(toast[0]);
	bsToast.show();
}
