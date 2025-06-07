const API_BASE = '/admin/users/api';
let currentPage = 0, pageSize = 5;
let lastSearch = { keyword: '' };

$(document).ready(function() {
	loadUsers();
	loadRoles();
	loadStatuses();
	$("#searchForm").submit(function(e) {
		e.preventDefault();
		const keyword = $("#searchInput").val().trim();
		if (!keyword) {
			showToast("Please enter a keyword.", "danger");
			return;
		}
		lastSearch.keyword = keyword;
		loadUsers(0);
	});
	$("#password").on("input", function() {
		const isEdit = $("#userForm").data('userId') !== undefined;
		const password = $(this).val().trim();

		if (isEdit && password) {
			showWarning($(this), "If you enter a password, the current password will be changed.");
		} else {
			$(this).siblings(".field-warning").remove();
		}
	});
	$("#userForm input, #userForm select").on("focus", function() {
		$(this).siblings(".error-message").remove();
	});

	$("#userForm").submit(async function(e) {
		e.preventDefault();
		if (!validateUserForm()) return;

		const userId = $("#userForm").data('userId');
		const formData = new FormData(this);

		if (!formData.get('userImage') || formData.get('userImage').size === 0) {
			const defaultImage = await getDefaultImageFile();
			formData.set('userImage', defaultImage);
		}

		const ajaxOptions = {
			type: userId ? "PUT" : "POST",
			url: userId ? `${API_BASE}/${userId}` : API_BASE,
			data: formData,
			contentType: false,
			processData: false,
			success: handleSuccess,
			error: handleError
		};

		$.ajax(ajaxOptions);
	});


	$('#confirmDeleteBtn').on('click', function() {
		if (!userIdToDelete) return;

		$.ajax({
			url: `${API_BASE}/${userIdToDelete}`,
			type: "DELETE",
			success: function(res) {
				if (res.responseCode === 1) {
					showToast(res.responseMsg, "success");
					loadUsers();
				} else {
					showToast(res.responseMsg, "danger");
				}
				$('#deleteConfirmModal').modal('hide');
				userIdToDelete = null;
			},
			error: function() {
				showToast("Error when deleting user.", "danger");
				$('#deleteConfirmModal').modal('hide');
				userIdToDelete = null;
			}
		});
	});

	$('#userModal').on('hidden.bs.modal', function() {
		$('#userForm')[0].reset();
		$('#userForm').removeData('userId');
		$('#previewImage').hide();
		$('#userForm .error-message').remove();
	});

	$("#profileImage").change(function() {
		if (this.files && this.files[0]) {
			const reader = new FileReader();
			reader.onload = e => $("#previewImage").attr("src", e.target.result).show();
			reader.readAsDataURL(this.files[0]);
		} else {
			$("#previewImage").hide();
		}
	});
});
async function getDefaultImageFile() {
	const response = await fetch('/img/default_user.png');
	const blob = await response.blob();
	return new File([blob], 'default_user.png', { type: blob.type });
}
function loadUsers(page = currentPage) {
	currentPage = page;
	const params = new URLSearchParams({
		keyword: lastSearch.keyword,
		page: currentPage,
		size: pageSize
	});
	const url = lastSearch.keyword
		? `${API_BASE}/search?${params}`
		: `${API_BASE}?${params}`;
	fetch(url)
		.then(resp => resp.json())
		.then(data => {
			renderUsers(data.data.user);
			renderPagination(data.data.pagination);
			$('#totalUserCount').text(`Total Users: ${data.data.totalCount}`);
		});
}

function renderUsers(users) {
	// Kiểm tra dữ liệu
	if (!Array.isArray(users)) {
		users = []; // Default to an empty array if users is not an array
	}
	const list = document.getElementById('userList');
	list.innerHTML = users.length
		? users.map(u => {
			const fullName = u.fullName.replace(/'/g, "\\'");
			const username = u.username.replace(/'/g, "\\'");
			const email = u.email.replace(/'/g, "\\'");
			return `
            <li class="list-group-item">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <strong>${fullName}</strong> (${username})<br>
                        <small>${email}</small>
                    </div>
                    <div>
                        <button class="btn btn-sm btn-warning me-2" onclick="openEditUserModal('${u.userId}')">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteUser('${u.userId}')">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </div>
            </li>`;
		}).join('')
		: '<li class="list-group-item text-muted">No users found.</li>';
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
			<a class="page-link" href="#" onclick="loadUsers(0);return false;">First</a>
		</li>
		<li class="page-item ${cur === 0 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadUsers(${cur - 1});return false;">Previous</a>
		</li>
	`);

	pages.forEach(p => {
		ul.append(p === '...'
			? `<li class="page-item disabled"><span class="page-link">…</span></li>`
			: `<li class="page-item ${p === cur ? 'active' : ''}">
				<a class="page-link" href="#" onclick="loadUsers(${p});return false;">${p + 1}</a>
			</li>`);
	});

	ul.append(`
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadUsers(${cur + 1});return false;">Next</a>
		</li>
		<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
			<a class="page-link" href="#" onclick="loadUsers(${total - 1});return false;">Last</a>
		</li>
	`);
}

function openAddUserModal() {
	$("#userModalLabel").text("Add User");
	$("#userForm")[0].reset();
	$("#userForm").removeData("userId");
	$("#previewImage").hide();
	$("#userModal").modal("show");
}

function openEditUserModal(userId) {
	$.get(`${API_BASE}/${userId}`, function(res) {
		if (res.responseCode !== 1) {
			showToast(res.responseMsg || "Could not load user", "danger");
			return;
		}
		const u = res.data;

		// Set dữ liệu vào form
		$("#userForm").data("userId", userId);
		$("#fullName").val(u.fullName || "");
		$("#username").val(u.username || "");
		$("#email").val(u.email || "");
		$("#phone").val(u.phone || "");
		$("#address").val(u.address || "");
		$("#role").val(u.role || "");
		$("#status").val(u.status || "");
		$("#role").val(u.role?.roleId || "");
		// Checkbox: isSeller
		$("#isSeller").prop("checked", !!u.isSeller);

		// Ảnh đại diện
		$("#previewImage").attr("src", u.profileImageUrl || "/img/default.png").show();

		// Mật khẩu để trống khi sửa
		$("#password").val("");

		// Set tiêu đề modal
		$("#userModalLabel").text("Edit User");
		$("#userModal").modal("show");
	});
}


function deleteUser(userId) {
	userIdToDelete = userId;
	$('#deleteConfirmModal').modal('show');
}

function validateUserForm() {
	let isValid = true;
	$("#userForm .error-message").remove();

	const fullName = $("#fullName").val().trim();
	const username = $("#username").val().trim();
	const email = $("#email").val().trim();
	const phone = $("#phone").val().trim();
	const password = $("#password").val().trim();
	const isEdit = $("#userForm").data('userId') !== undefined;
	const address = $("#address").val().trim();
	// Regex patterns
	const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
	const phoneRegex = /^(0|\+84)[0-9]{9}$/;

	// Full name
	if (!fullName) {
		showError($("#fullName"), "Full name is required.");
		isValid = false;
	}

	// Username
	if (!username) {
		showError($("#username"), "Username is required.");
		isValid = false;
	}
	if (!address) {
		showError($("#address"), "Address is required.");
		isValid = false;
	}

	// Email
	if (!email) {
		showError($("#email"), "Email is required.");
		isValid = false;
	} else if (!emailRegex.test(email)) {
		showError($("#email"), "Invalid email format.");
		isValid = false;
	}

	// Phone (optional)
	if (!phone) {
		showError($("#phone"), "Phone is required.");
		isValid = false;
	} else if (phone && !phoneRegex.test(phone)) {
		showError($("#phone"), "Invalid phone number format.");
		isValid = false;
	}

	// Password
	if (!isEdit && !password) {
		showError($("#password"), "Password is required for new user.");
		isValid = false;
	} else if (isEdit && password) {
		showWarning($("#password"), "If you enter a password, the current password will be changed.");
	}

	return isValid;
}

function showWarning(input, message) {
	input.siblings(".field-warning").remove();
	const warning = $('<div class="text-warning error-message field-warning"></div>').text(message);
	input.after(warning);
}

function showError(input, message) {
	input.siblings(".field-error").remove();
	const error = $('<div class="text-danger error-message field-error"></div>').text(message);
	input.after(error);
}

function showToast(message, type = "success") {
	const toast = $('#errorToast');
	toast.removeClass("text-bg-success text-bg-danger").addClass(`text-bg-${type}`);
	toast.find('.toast-body').text(message);
	new bootstrap.Toast(toast[0]).show();
}

function handleSuccess(res) {
	if (res.responseCode === 1) {
		showToast(res.responseMsg, "success");
		$("#userModal").modal("hide");
		loadUsers();
	} else {
		showToast(res.responseMsg || "Error occurred!", "danger");
	}
}
function loadRoles() {
	fetch(`${API_BASE}/role`)
		.then(response => {
			if (!response.ok) throw new Error('Lỗi khi tải danh sách vai trò');
			return response.json();
		})
		.then(data => {
			const select = document.getElementById('role');
			select.innerHTML = '';
			data.forEach(role => {
				const option = document.createElement('option');
				option.value = role.roleId;
				option.textContent = `${role.roleName}`;
				select.appendChild(option);
			});
		})
		.catch(err => console.error('Lỗi loadRoles:', err));
}

function loadStatuses() {
	fetch(`${API_BASE}/user-statuses`)
		.then(response => {
			if (!response.ok) throw new Error('Lỗi khi tải trạng thái');
			return response.json();
		})
		.then(data => {
			const select = document.getElementById('status');
			select.innerHTML = '';
			data.forEach(status => {
				const option = document.createElement('option');
				option.value = status;
				option.textContent = status;
				select.appendChild(option);
			});
		})
		.catch(err => console.error('Lỗi loadStatuses:', err));
}

function handleError(xhr) {
	showToast(xhr.responseJSON?.responseMsg || "Unexpected error occurred.", "danger");
}
