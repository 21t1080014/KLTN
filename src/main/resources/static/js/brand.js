const API_BASE = '/admin/brands/api';
let currentPage = 0, pageSize = 5;
let lastSearch = { name: '' };

$(document).ready(function() {
	loadBrands();
	$("#searchForm").submit(function(e) {
		e.preventDefault();
		const name = $("#searchInput").val().trim();
		if (!name) {
			showToast("Please enter search criteria", "danger");
			return;
		}
		lastSearch.name = name;
		loadBrands(0);
	});
	// Sự kiện xử lý form thêm / sửa thương hiệu
	$("#brandForm").submit(function(e) {
		e.preventDefault();
		if (!validateBrandForm()) {
			return;
		}
		var formData = new FormData(this);
		const brandId = $("#brandForm").data('brandId'); // Kiểm tra xem có id thương hiệu không (sửa)

		if (brandId) {
			// Sửa thương hiệu
			$.ajax({
				type: "PUT",
				url: `${API_BASE}/${brandId}`,
				data: formData,
				contentType: false,
				processData: false,
				success: function(res) {
					if (res.responseCode === 1) {
						showToast(res.responseMsg, "success");
						$('#brandModal').modal('hide');
						loadBrands();
					} else {
						showToast(res.responseMsg || "An error occurred!", "danger");
					}
				},
				error: function(xhr, status, error) {
					showToast(xhr.responseJSON?.responseMsg || "Unexpected error occurred.", "danger");
				}
			});
		} else {
			// Thêm thương hiệu mới
			$.ajax({
				type: "POST",
				url: `${API_BASE}`,
				data: formData,
				contentType: false,
				processData: false,
				success: function(res) {
					if (res.responseCode === 1) {
						showToast(res.responseMsg, "success");
						$('#brandModal').modal('hide');
						loadBrands();
					} else {
						showToast(res.responseMsg || "An error occurred!", "danger");
					}
				},
				error: function(xhr, status, error) {
					showToast(xhr.responseJSON?.responseMsg || "Unexpected error occurred.", "danger");
				}
			});
		}
	});
	// Sự kiện xóa thương hiệu
	$('#confirmDeleteBtn').on('click', function() {
		if (!brandIdToDelete) return;

		$.ajax({
			url: `${API_BASE}/${brandIdToDelete}`,
			type: "DELETE",
			success: function(res) {
				if (res.responseCode === 1) {
					showToast(res.responseMsg, "success");
					$(`#brandRow-${brandIdToDelete}`).remove();
					loadBrands();
				} else {
					showToast(res.responseMsg, "danger");
				}
				$('#deleteConfirmModal').modal('hide');
				brandIdToDelete = null;
			},
			error: function(jqXHR, textStatus, errorThrown) {
				showToast("Error when deleting brand.", "danger");
				$('#deleteConfirmModal').modal('hide');
				brandIdToDelete = null;
			}
		});
	});

	// Xem ảnh logo khi click vào hình
	$('#brandTableBody').on('click', 'img[data-bs-toggle="tooltip"]', function() {
		const src = $(this).attr('src');
		$('#overlayImage').attr('src', src);
		$('#imageOverlay').fadeIn(200);
	});

	$('#imageOverlay').on('click', function() {
		$(this).fadeOut(200);
	});

	$(document).on('keydown', function(e) {
		if (e.key === 'Escape') {
			$('#imageOverlay').fadeOut(200);
		}
	});

	// Reset form khi modal đóng
	$('#brandModal').on('hidden.bs.modal', function() {
		$('#brandForm')[0].reset();
		$('#brandForm').removeData('brandId'); // Xóa dữ liệu id nếu có
		$('#previewImage').hide();
		$('#brandForm .error-message').remove();
	});

	// Sự kiện thay đổi logo
	$("#logoFile").change(function() {
		var input = this;
		if (input.files && input.files[0]) {
			var reader = new FileReader();
			reader.onload = function(e) {
				$("#previewImage").attr("src", e.target.result).show();
			};
			reader.readAsDataURL(input.files[0]);
		} else {
			$("#previewImage").hide();
		}
	});
})
// Load danh sách theo trang
async function loadBrands(page = currentPage) {
	currentPage = page;
	const params = new URLSearchParams({
		keyword: lastSearch.name,
		page: currentPage,
		size: pageSize
	});
	const url = (lastSearch.name) ? `${API_BASE}/search?${params}` : `${API_BASE}?${params}`;
	const resp = await fetch(url);
	const data = await resp.json();

	const brands = data.data.brands;
	const totalPages = data.data.pagination.totalPages;

	if (currentPage > totalPages - 1 && totalPages > 0) {
		currentPage = totalPages - 1;
		await loadBrands(currentPage);
		return;
	}

	if (brands.length === 0 && totalPages > 0) {
		currentPage = 0;
		await loadBrands(currentPage);
		return;
	}

	renderBrands(brands);
	$('#totalBrandCount').text("Total Brands: " + data.data.totalCount);
	renderPagination(data.data.pagination);

}

// Render danh sách brand
function renderBrands(brands) {
	const list = document.getElementById('brandList');
	list.innerHTML = brands.length
		? brands.map(b => {
			const id = b.brandId;
			const name = b.name.replace(/'/g, "\\'");
			const desc = (b.description || '').replace(/'/g, "\\'");
			const logo = b.logoImage || '';

			return `
          <li class="list-group-item" id="brandRow-${id}">
            <div class="d-flex align-items-center justify-content-between">
              <div class="d-flex align-items-center">
                <img 
                  src="${logo ? '/admin/brands/api/img/' + logo : '/img/default.png'}"
                  alt="${b.name}" 
                  class="me-3 thumbnail"
                  style="width:80px;height:80px;object-fit:contain;cursor:pointer;"
					data-bs-toggle="tooltip" 
				  title="Click để xem lớn"
                />
                <div>
                  <strong>${b.name}</strong><br>
                  <small>${b.description || ''}</small>
                </div>
              </div>
              <div class="d-flex justify-content-end mt-2">
                <button 
                  class="btn btn-sm btn-warning me-2"
                  onclick="openEditBrandModal(
                    ${id},
                    '${name}',
                    '${desc}',
                    '${logo}'
                  )"
                >
                  <i class="bi bi-pencil"></i>
                </button>
                <button 
                  class="btn btn-sm btn-danger"
                  onclick="deleteBrand(${id})"
                >
                  <i class="bi bi-trash"></i>
                </button>
              </div>
            </div>
          </li>
        `;
		}).join('')
		: '<li class="list-group-item text-muted">Không có thương hiệu nào.</li>';
}



// Render phân trang
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
      <a class="page-link" href="#" onclick="loadBrands(0);return false;">First</a>
    </li>
    <li class="page-item ${cur === 0 ? 'disabled' : ''}">
      <a class="page-link" href="#" onclick="loadBrands(${cur - 1});return false;">Previous</a>
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
          <a class="page-link" href="#" onclick="loadBrands(${p});return false;">${p + 1}</a>
        </li>
      `);
		}
	});

	// Next & Last
	ul.append(`
    <li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
      <a class="page-link" href="#" onclick="loadBrands(${cur + 1});return false;">Next</a>
    </li>
    <li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
      <a class="page-link" href="#" onclick="loadBrands(${total - 1});return false;">Last </a>
    </li>
  `);
}

// Hàm validate dữ liệu form
function validateBrandForm() {
	let isValid = true;
	const title = $('#brandName');
	const image = $('#logoFile')[0];

	$('#brandForm .error-message').remove(); // Xóa lỗi cũ

	if (!title.val().trim()) {
		showError(title, "Brand name cannot be empty.");
		isValid = false;
	}

	const isEdit = $("#brandForm").data('brandId') !== undefined;
	if (!isEdit) {
		if (!image.files || image.files.length === 0) {
			showError($('#logoFile'), "Please choose a brand logo.");
			isValid = false;
		}
	}

	return isValid;
}
// Tìm kiếm thương hiệu
/*function filterBrands() {
	const keyword = document.getElementById('searchInput').value.toLowerCase();
	const filtered = allBrands.filter(b => b.name.toLowerCase().includes(keyword));
	renderBrands(filtered);
}*/

// Hàm hiển thị lỗi
function showError(input, message) {
	const error = document.createElement('div');
	error.className = 'text-danger error-message';
	error.innerText = message;
	input.parentNode.appendChild(error);
}

// Hàm hiển thị thông báo
function showToast(message, type = "success") {
	const toast = $('#errorToast');
	toast.removeClass("text-bg-success text-bg-danger").addClass(`text-bg-${type}`);
	toast.find('.toast-body').text(message);
	const bsToast = new bootstrap.Toast(toast[0]);
	bsToast.show();
}

// Hàm mở modal chỉnh sửa
/*function openEditBrandModal(brandId) {
	$.get(`${API_BASE}/${brandId}`, function(data) {
		if (data.responseCode !== 1) {
			showToast(data.responseMsg || "Error loading brand.", "danger");
			return;
		}

		const brand = data.data;

		// Set tiêu đề modal
		$("#brandModalLabel").text("Edit Brand");

		// Điền dữ liệu vào form
		$('#brandName').val(brand.brandName);
		$('#description').val(brand.description);
		$('#logoFile').val('');

		// Đặt brandId vào form để xác định đây là sửa
		$('#brandForm').data('brandId', brandId);

		// Hiển thị modal
		$('#brandModal').modal('show');
	});
}*/
function openEditBrandModal(id, name, desc, logo) {
	// Set tiêu đề modal
	$("#brandModalLabel").text("Edit Brand");
	//$("#brandId").val(id);
	$("#brandName").val(name);
	$("description").val(desc);
	$("#brandForm").data('brandId', id);
	$("#logoFile").val('');
	// 4) Preview ảnh nếu có
	if (logo) {
		// URL này phải khớp với @GetMapping bạn đã khai báo
		$("#previewImage")
			.attr("src", `/admin/brands/api/img/${logo}`)
			.show();
	} else {
		$("#previewImage").hide();
	}

	// 5) Mở modal
	$('#brandModal').modal('show');
}

// Hàm mở modal thêm mới
function openAddBrandModal() {
	// Set tiêu đề modal
	$("#brandModalLabel").text("Add New Brand");

	// Xóa dữ liệu trong form
	$('#brandForm')[0].reset();
	$('#brandForm').removeData('brandId');

	// Hiển thị modal
	$('#brandModal').modal('show');
}

// Hàm xóa thương hiệu
function deleteBrand(brandId) {
	brandIdToDelete = brandId;
	$('#deleteConfirmModal').modal('show');
}
$(document).on('click', '.thumbnail', function() {
	const src = $(this).attr('src');
	$('#overlayImage').attr('src', src);
	$('#imageOverlay').css('display', 'flex');
});

// Khi click vào overlay (hoặc nhấn ESC), ẩn overlay
$('#imageOverlay').on('click', function() {
	$(this).hide();
});
$(document).on('keydown', function(e) {
	if (e.key === 'Escape') {
		$('#imageOverlay').hide();
	}
});
// Khi trang load xong thì load data
document.addEventListener('DOMContentLoaded', () => loadBrands());