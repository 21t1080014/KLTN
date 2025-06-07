const API_BASE = '/admin/categories/glass-materials/api';
let currentPage = 0, pageSize = 5;
let lastSearch = { keyword: '' };

$(document).ready(function() {
	loadGlassMaterials();

	// Tìm kiếm
	$('#searchForm').submit(function(e) {
		e.preventDefault();
		const keyword = $('#searchKeyword').val().trim();
		if (!keyword) {
			showToast("Vui lòng nhập từ khóa tìm kiếm", "danger");
			return;
		}
		lastSearch.keyword = keyword;
		loadGlassMaterials(0);
	});

	// Thêm / sửa vật liệu kính
	$('#glassForm').submit(function(e) {
		e.preventDefault();
		const name = $('#glassName').val().trim();
		const glassId = $('#glassForm').data('glassMaterialId');

		$('#glassForm .error-message').remove();
		console.log("glassId là:", glassId);
		if (!name) {
			showError($('#glassName')[0], "Tên vật liệu kính không được để trống");
			return;
		}

		const data = new URLSearchParams();
		data.append("glassName", name);
		if (glassId) data.append("glassId", glassId);

		$.ajax({
			type: glassId ? 'PUT' : 'POST',
			url: glassId ? `${API_BASE}/${glassId}` : API_BASE,
			data: data.toString(),
			contentType: 'application/x-www-form-urlencoded',
			success: function(res) {
				if (res.responseCode === 1) {
					showToast("Lưu thành công", "success");
					$('#glassModal').modal('hide');
					loadGlassMaterials(currentPage);
				} else {
					showToast(res.responseMsg || "Đã có lỗi xảy ra", "danger");
				}
			},
			error: function() {
				showToast("Lỗi trong quá trình gửi dữ liệu", "danger");
			}
		});
	});

	// Reset form khi modal đóng
	$('#glassModal').on('hidden.bs.modal', function() {
		$('#glassForm')[0].reset();
		$('#glassForm').removeData('glassId');
		$('#glassForm .error-message').remove();
	});
});

// Load danh sách vật liệu kính
async function loadGlassMaterials(page = currentPage) {
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

	const glassList = data.data.glass ;
	const totalPages = data.data.pagination?.totalPages || 1;

	if (currentPage > totalPages - 1 && totalPages > 0) {
		currentPage = totalPages - 1;
		await loadGlassMaterials(currentPage);
		return;
	}

	renderGlassList(glassList);
	renderPagination(data.data.pagination);
}

// Hiển thị danh sách vật liệu kính
function renderGlassList(list) {
	const tbody = $('#glassTableBody').empty();
	if (!list.length) {
		tbody.append(`<tr><td colspan="3" class="text-muted text-center">Không có dữ liệu</td></tr>`);
		return;
	}

	list.forEach((g, index) => {
		tbody.append(`
			<tr id="glassRow-${g.glassId}">
				<td>${currentPage * pageSize + index + 1}</td>
				<td>${g.name}</td>
				<td>
					<button class="btn btn-sm btn-warning me-2" onclick="openEditGlass(${g.glassMaterialId}, '${g.name.replace(/'/g, "\\'")}')">Sửa</button>
					<button class="btn btn-sm btn-danger" onclick="deleteGlass(${g.glassMaterialId})">Xóa</button>
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
			<a class="page-link" href="#" onclick="loadGlassMaterials(0); return false;">First</a>
		</li>
		<li class="page-item ${cur === 0 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadGlassMaterials(${cur - 1}); return false;">Previous</a>
		</li>
	`);

	pages.forEach(p => {
		if (p === '...') {
			ul.append(`<li class="page-item disabled"><span class="page-link">…</span></li>`);
		} else {
			ul.append(`
				<li class="page-item ${p === cur ? 'active' : ''}">
					<a class="page-link" href="#" onclick="loadGlassMaterials(${p}); return false;">${p + 1}</a>
				</li>
			`);
		}
	});

	ul.append(`
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadGlassMaterials(${cur + 1}); return false;">Next</a>
		</li>
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadGlassMaterials(${total - 1}); return false;">Last</a>
		</li>
	`);
}

// Mở modal chỉnh sửa
function openEditGlass(id, name) {
	$('#glassModalLabel').text("Cập nhật vật liệu kính");
	$('#glassName').val(name);
	$('#glassForm').data('glassMaterialId', id);
	$('#glassModal').modal('show');
}

// Mở modal thêm mới
function openAddGlassModal() {
	$('#glassModalLabel').text("Thêm vật liệu kính");
	$('#glassForm')[0].reset();
	$('#glassForm').removeData('glassMaterialId');
	$('#glassModal').modal('show');
}

// Xóa vật liệu kính
function deleteGlass(glassId) {
	if (!confirm("Bạn chắc chắn muốn xóa vật liệu kính này?")) return;

	$.ajax({
		type: 'DELETE',
		url: `${API_BASE}/${glassId}?glassId=${glassId}`,
		success: function(res) {
			if (res.responseCode === 1) {
				showToast("Xóa thành công", "success");
				$(`#glassRow-${glassId}`).remove();
				loadGlassMaterials();
			} else {
				showToast(res.responseMsg || "Xóa thất bại", "danger");
			}
		},
		error: function() {
			showToast("Lỗi khi xóa vật liệu kính", "danger");
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
