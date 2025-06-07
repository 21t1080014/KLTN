let currentUserId = null;

export function loadProfile() {
	$.get('/api/user-customer', response => {
		if (response.responseCode !== 1) {
			$('#profile-content').text(response.responseMsg);
			return;
		}

		const u = response.data;
		currentUserId = u.userId; // Gán userId cho phần upload ảnh

		$('#profile-content').html(`
      <div class="card p-4">
        <h5 class="mb-3 text-center">Hồ sơ của bạn</h5>
        <div class="text-center mb-3">
          <img src="${u.userImage ? `/api/imgUser/${u.userImage}` : '/img/default.png'}"
               alt="Avatar"
               id="avatarPreview"
               class="rounded-circle"
               style="width:120px; height:120px; object-fit:cover;">
          <div class="mt-2">
            <input type="file" id="inputAvatar" accept="image/*" class="form-control" style="max-width: 300px; margin: 0 auto;">
            <button class="btn btn-sm btn-outline-primary mt-2" id="btnUploadAvatar">Cập nhật ảnh đại diện</button>
          </div>
        </div>
        <div id="view-profile">
          <p><strong>Họ tên:</strong> ${u.fullName || '-'}</p>
          <p><strong>Username:</strong> ${u.username}</p>
          <p><strong>Email:</strong> ${u.email}</p>
          <p><strong>Điện thoại:</strong> ${u.phone || '-'}</p>
          <p><strong>Địa chỉ:</strong> ${u.address || '-'}</p>
          <p><strong>Trạng thái:</strong> ${u.status}</p>
          <div class="text-end">
            <button class="btn btn-primary" id="btnEditProfile">Chỉnh sửa</button>
          </div>
        </div>

        <form id="edit-profile" style="display:none;">
          <div class="mb-2">
            <label class="form-label">Họ tên</label>
            <input type="text" class="form-control" id="inputFullName" value="${u.fullName || ''}">
          </div>
          <div class="mb-2">
            <label class="form-label">Email</label>
            <input type="email" class="form-control" id="inputEmail" value="${u.email}">
          </div>
          <div class="mb-2">
            <label class="form-label">Điện thoại</label>
            <input type="text" class="form-control" id="inputPhone" value="${u.phone || ''}">
          </div>
          <div class="mb-2">
            <label class="form-label">Địa chỉ</label>
            <input type="text" class="form-control" id="inputAddress" value="${u.address || ''}">
          </div>
          <div class="text-end">
            <button type="submit" class="btn btn-success">Lưu</button>
            <button type="button" class="btn btn-secondary" id="btnCancelEdit">Hủy</button>
          </div>
        </form>
      </div>
    `);

		// ======== SỰ KIỆN ==========

		// Chỉnh sửa
		$('#btnEditProfile').click(() => {
			$('#view-profile').hide();
			$('#edit-profile').show();
		});

		$('#btnCancelEdit').click(() => {
			$('#edit-profile').hide();
			$('#view-profile').show();
		});

		$('#edit-profile').submit(function(e) {
			e.preventDefault();
			const updatedData = {
				fullName: $('#inputFullName').val(),
				email: $('#inputEmail').val(),
				phone: $('#inputPhone').val(),
				address: $('#inputAddress').val()
			};

			$.ajax({
				url: '/api/user-customer',
				method: 'PUT',
				contentType: 'application/json',
				data: JSON.stringify(updatedData),
				success: function(res) {
					if (res.responseCode === 1) {
						alert('Cập nhật thành công!');
						loadProfile(); // reload lại
					} else {
						alert('Lỗi: ' + res.responseMsg);
					}
				},
				error: function() {
					alert('Lỗi khi gửi dữ liệu');
				}
			});
		});

		// Upload avatar
		$('#btnUploadAvatar').click(() => {
			const file = $('#inputAvatar')[0].files[0];
			if (!file) {
				alert('Vui lòng chọn ảnh');
				return;
			}

			if (!currentUserId) {
				alert('Không xác định được userId');
				return;
			}

			const formData = new FormData();
			formData.append("file", file);
			formData.append("userId", currentUserId);

			$.ajax({
				url: '/api/user-customer/avatar',
				method: 'PUT',
				data: formData,
				contentType: false,
				processData: false,
				success: function(resp) {
					if (resp.responseCode === 1) {
						alert("Cập nhật ảnh thành công!");
					} else {
						alert("Lỗi ảnh: " + resp.responseMsg);
					}
					loadProfile();
				},
				error: function() {
					alert('Lỗi khi upload ảnh');
				}
			});
		});

	});
}

// Nếu không dùng bundler, bạn có thể dùng dòng này để gọi từ HTML
window.loadProfile = loadProfile;
