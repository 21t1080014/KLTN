const API_BASE = '/admin/categories/api';
let currentPage = 0, pageSize = 5;
let lastSearch = { keyword: '' };
let allCategoriesFlat = [];

$(document).ready(function() {
	loadCategories();
	loadCategoryTree();
	loadParentOptions();

	$('#categoryForm').submit(function(e) {
		e.preventDefault();
		const name = $('#categoryName').val().trim();
		const parentId = $('#categoryParent').val();
		const description = $('#categoryDescription').val().trim();
		const categoryId = $('#categoryForm').data('categoryId');

		if (!name) {
			showFieldError($('#categoryName')[0], "Tên danh mục không được để trống");
			return;
		}

		const data = new URLSearchParams();
		data.append("name", name);
		if (parentId) data.append("parentId", parentId);
		if (description) data.append("description", description);

		$.ajax({
			type: categoryId ? 'PUT' : 'POST',
			url: categoryId ? `${API_BASE}/${categoryId}` : API_BASE,
			data: data.toString(),
			contentType: 'application/x-www-form-urlencoded',
			success: function(res) {
				if (res.responseCode === 1) {
					showToast("Lưu thành công", "success");
					$('#categoryModal').modal('hide');
					loadCategories(currentPage);
					loadCategoryTree();
					loadParentOptions();
				} else {
					showToast(res.responseMsg || "Đã có lỗi xảy ra", "danger");
				}
			},
			error: function(xhr) {
				const msg = xhr.responseJSON && xhr.responseJSON.responseMsg ? xhr.responseJSON.responseMsg : "Lỗi trong quá trình gửi dữ liệu";
				showToast(msg, "danger");
			}
		});
	});

	$('#categoryModal').on('hidden.bs.modal', function() {
		$('#categoryForm')[0].reset();
		$('#categoryForm').removeData('categoryId');
		$('#categoryForm .error-message').remove();
	});
});

function searchCategories() {
	const keyword = $('#searchKeyword').val().trim();
	lastSearch.keyword = keyword;
	loadCategories(0);
}

function loadCategories(page = currentPage) {
	currentPage = page;
	const url = lastSearch.keyword
		? `${API_BASE}/search?keyword=${encodeURIComponent(lastSearch.keyword)}&page=${page}&size=${pageSize}`
		: `${API_BASE}?page=${page}&size=${pageSize}`;
	$.get(url, function(res) {
		if (res.responseCode !== 1) {
			showToast(res.responseMsg || "Không thể tải dữ liệu", "danger");
			return;
		}
		renderCategories(res.data.categories);
		renderPagination(res.data.pagination);
	});
}

function loadParentOptions() {
	$.get(`${API_BASE}?page=0&size=1000`, function(res) {
		if (res.responseCode === 1) {
			allCategoriesFlat = res.data.categories;
			const $select = $('#categoryParent');
			$select.find('option:not(:first)').remove();
			allCategoriesFlat.forEach(c => {
				$select.append(`<option value="${c.categoryId}">${c.parentName ? c.parentName + ' > ' : ''}${c.name}</option>`);
			});
		}
	});
}

function loadCategoryTree() {
	$.get(`${API_BASE}/tree`, function(res) {
		if (res.responseCode === 1) {
			renderTree(res.data);
		}
	});
}

function renderTree(nodes, depth = 0) {
	const $container = depth === 0 ? $('#categoryTree').empty() : null;
	if (!nodes || nodes.length === 0) {
		if (depth === 0) $('#categoryTree').html('<p class="text-muted small">Chưa có danh mục nào.</p>');
		return;
	}
	nodes.forEach(node => {
		const indent = '&nbsp;&nbsp;&nbsp;&nbsp;'.repeat(depth);
		$('#categoryTree').append(`<div>${indent}${depth > 0 ? '↳ ' : ''}${node.name}</div>`);
		if (node.children && node.children.length > 0) {
			renderTree(node.children, depth + 1);
		}
	});
}

function renderCategories(items) {
	const tbody = $('#categoryTableBody').empty();
	if (!items || !items.length) {
		tbody.append(`<tr><td colspan="5" class="text-muted text-center">Không có dữ liệu</td></tr>`);
		return;
	}
	items.forEach(item => {
		tbody.append(`
			<tr>
				<td>${item.categoryId}</td>
				<td>${item.name}</td>
				<td>${item.parentName || '<span class="text-muted">(gốc)</span>'}</td>
				<td>${item.description || ''}</td>
				<td>
					<button class="btn btn-sm btn-warning me-2" onclick='openEditCategory(${JSON.stringify(item)})'>Sửa</button>
					<button class="btn btn-sm btn-danger" onclick="deleteCategory(${item.categoryId})">Xóa</button>
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
	} else if (cur <= 3) {
		pages.push(1, 2, 3, '...');
	} else if (cur >= total - 4) {
		pages.push('...', total - 4, total - 3, total - 2);
	} else {
		pages.push('...', cur - 1, cur, cur + 1, '...');
	}
	pages.push(total - 1);

	ul.append(`
		<li class="page-item ${cur === 0 ? 'disabled' : ''}"><a href="#" class="page-link" onclick="loadCategories(0);return false;">First</a></li>
		<li class="page-item ${cur === 0 ? 'disabled' : ''}"><a href="#" class="page-link" onclick="loadCategories(${cur - 1});return false;">Previous</a></li>
	`);
	pages.forEach(p => {
		if (p === '...') {
			ul.append(`<li class="page-item disabled"><span class="page-link">…</span></li>`);
		} else {
			ul.append(`<li class="page-item ${p === cur ? 'active' : ''}"><a href="#" class="page-link" onclick="loadCategories(${p});return false;">${p + 1}</a></li>`);
		}
	});
	ul.append(`
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}"><a href="#" class="page-link" onclick="loadCategories(${cur + 1});return false;">Next</a></li>
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}"><a href="#" class="page-link" onclick="loadCategories(${total - 1});return false;">Last</a></li>
	`);
}

function openAddCategoryModal() {
	$('#categoryModalLabel').text("Thêm danh mục");
	$('#categoryForm')[0].reset();
	$('#categoryForm').removeData('categoryId');
	$('#categoryModal').modal('show');
}

function openEditCategory(item) {
	$('#categoryModalLabel').text("Chỉnh sửa danh mục");
	$('#categoryName').val(item.name);
	$('#categoryParent').val(item.parentId || '');
	$('#categoryDescription').val(item.description || '');
	$('#categoryForm').data('categoryId', item.categoryId);
	$('#categoryModal').modal('show');
}

function deleteCategory(id) {
	if (!confirm('Bạn có chắc chắn muốn xóa danh mục này?')) return;
	$.ajax({
		type: 'DELETE',
		url: `${API_BASE}/${id}`,
		success: function(res) {
			if (res.responseCode === 1) {
				showToast("Xóa thành công", "success");
				loadCategories(currentPage);
				loadCategoryTree();
				loadParentOptions();
			} else {
				showToast(res.responseMsg || "Xóa thất bại", "danger");
			}
		},
		error: function(xhr) {
			const msg = xhr.responseJSON && xhr.responseJSON.responseMsg ? xhr.responseJSON.responseMsg : "Lỗi khi xóa";
			showToast(msg, "danger");
		}
	});
}

function showFieldError(input, msg) {
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
