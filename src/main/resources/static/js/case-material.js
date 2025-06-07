const API_BASE = '/admin/categories/case-materials/api';
let currentPage = 0, pageSize = 5;
let lastSearch = { keyword: '' };
let deleteId = null;

$(document).ready(function() {
	loadCases();

	$('#searchKeyword').on('keyup', function(e) {
		if (e.key === 'Enter') searchCases();
	});

	// Form submit
	$('#caseForm').submit(function(e) {
		e.preventDefault();
		const name = $('#caseName').val().trim();
		const caseId = $('#caseForm').data('caseId');

		if (!name) {
			showError($('#caseName')[0], "Tên chất liệu không được để trống");
			return;
		}

		const data = new URLSearchParams();
		data.append("caseName", name);
		if (caseId) data.append("caseId", caseId);

		$.ajax({
			type: caseId ? 'PUT' : 'POST',
			url: caseId ? `${API_BASE}/${caseId}` : API_BASE,
			data: data.toString(),
			contentType: 'application/x-www-form-urlencoded',
			success: function(res) {
				if (res.responseCode === 1) {
					showToast("Lưu thành công", "success");
					$('#caseModal').modal('hide');
					loadCases(currentPage);
				} else {
					showToast(res.responseMsg || "Đã có lỗi xảy ra", "danger");
				}
			},
			error: function() {
				showToast("Lỗi trong quá trình gửi dữ liệu", "danger");
			}
		});
	});

	$('#caseModal').on('hidden.bs.modal', function() {
		$('#caseForm')[0].reset();
		$('#caseForm').removeData('caseId');
		$('#caseForm .error-message').remove();
	});

	$('#deleteConfirmBtn').click(function() {
		if (!deleteId) return;
		$.ajax({
			type: 'DELETE',
			url: `${API_BASE}/${deleteId}?caseId=${deleteId}`,
			success: function(res) {
				$('#deleteModal').modal('hide');
				if (res.responseCode === 1) {
					showToast("Xóa thành công", "success");
					loadCases(currentPage);
				} else {
					showToast(res.responseMsg || "Xóa thất bại", "danger");
				}
			},
			error: function() {
				showToast("Lỗi khi xóa", "danger");
			}
		});
	});
});

function searchCases() {
	const keyword = $('#searchKeyword').val().trim();
	lastSearch.keyword = keyword;
	loadCases(0);
}

async function loadCases(page = currentPage) {
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

	const cases = data.data.caseMaterials;
	const totalPages = data.data.pagination.totalPages;

	if (currentPage > totalPages - 1 && totalPages > 0) {
		currentPage = totalPages - 1;
		await loadCases(currentPage);
		return;
	}

	renderCases(cases);
	renderPagination(data.data.pagination);
}

function renderCases(items) {
	const tbody = $('#caseTableBody').empty();
	if (!items.length) {
		tbody.append(`<tr><td colspan="3" class="text-muted text-center">Không có dữ liệu</td></tr>`);
		return;
	}

	items.forEach((item, index) => {
		tbody.append(`
			<tr>
				<td>${currentPage * pageSize + index + 1}</td>
				<td>${item.name}</td>
				<td>
					<button class="btn btn-sm btn-warning me-2" onclick="openEditCase(${item.caseId}, '${item.name.replace(/'/g, "\\'")}')">Sửa</button>
					<button class="btn btn-sm btn-danger" onclick="openDeleteModal(${item.caseId})">Xóa</button>
				</td>
			</tr>
		`);
	});
}

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
			<a class="page-link" href="#" onclick="loadCases(0); return false;">First</a>
		</li>
		<li class="page-item ${cur === 0 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadCases(${cur - 1}); return false;">Previous</a>
		</li>
	`);

	pages.forEach(p => {
		if (p === '...') {
			ul.append(`<li class="page-item disabled"><span class="page-link">…</span></li>`);
		} else {
			ul.append(`
				<li class="page-item ${p === cur ? 'active' : ''}">
					<a class="page-link" href="#" onclick="loadCases(${p}); return false;">${p + 1}</a>
				</li>
			`);
		}
	});

	ul.append(`
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadCases(${cur + 1}); return false;">Next</a>
		</li>
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadCases(${total - 1}); return false;">Last</a>
		</li>
	`);
}

function openAddCaseModal() {
	$('#caseModalLabel').text("Thêm chất liệu");
	$('#caseForm')[0].reset();
	$('#caseForm').removeData('caseId');
	$('#caseModal').modal('show');
}

function openEditCase(id, name) {
	$('#caseModalLabel').text("Chỉnh sửa chất liệu");
	$('#caseName').val(name);
	$('#caseForm').data('caseId', id);
	$('#caseModal').modal('show');
}

function openDeleteModal(id) {
	deleteId = id;
	$('#deleteModal').modal('show');
}

function showError(input, msg) {
	const error = `<div class="error-message text-danger mt-1 small">${msg}</div>`;
	$(input).after(error);
}

function showToast(msg, type = "danger") {
	const toast = $('#errorToast');
	toast.removeClass("text-bg-success text-bg-danger").addClass(`text-bg-${type}`);
	toast.find('.toast-body').text(msg);
	const bsToast = new bootstrap.Toast(toast[0]);
	bsToast.show();
}
