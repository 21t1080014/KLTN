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
      <div class="border border-line p-8 max-w-xl">
        <h5 class="font-heading text-lg text-charcoal text-center mb-6">Hồ sơ của bạn</h5>
        <div class="text-center mb-6">
          <img src="${u.userImage ? `/api/imgUser/${u.userImage}` : '/img/default.png'}"
               alt="Avatar"
               id="avatarPreview"
               class="rounded-full mx-auto"
               style="width:120px; height:120px; object-fit:cover;">
          <div class="mt-3">
            <input type="file" id="inputAvatar" accept="image/*" class="input-field mx-auto" style="max-width: 300px;">
            <button class="btn-secondary !px-4 !py-2 text-xs mt-3" id="btnUploadAvatar">Cập nhật ảnh đại diện</button>
          </div>
        </div>
        <div id="view-profile" class="space-y-2 text-sm text-charcoal-soft">
          <p><strong class="text-charcoal">Họ tên:</strong> ${u.fullName || '-'}</p>
          <p><strong class="text-charcoal">Username:</strong> ${u.username}</p>
          <p><strong class="text-charcoal">Email:</strong> ${u.email}</p>
          <p><strong class="text-charcoal">Điện thoại:</strong> ${u.phone || '-'}</p>
          <p><strong class="text-charcoal">Địa chỉ:</strong> ${u.address || '-'}</p>
          <p><strong class="text-charcoal">Trạng thái:</strong> ${u.status}</p>
          <div class="text-right pt-2">
            <button class="btn-primary" id="btnEditProfile">Chỉnh sửa</button>
          </div>
        </div>

        <form id="edit-profile" style="display:none;" class="space-y-3">
          <div>
            <label class="label-field">Họ tên</label>
            <input type="text" class="input-field" id="inputFullName" value="${u.fullName || ''}">
          </div>
          <div>
            <label class="label-field">Email</label>
            <input type="email" class="input-field" id="inputEmail" value="${u.email}">
          </div>
          <div>
            <label class="label-field">Điện thoại</label>
            <input type="text" class="input-field" id="inputPhone" value="${u.phone || ''}">
          </div>
          <div>
            <label class="label-field">Địa chỉ</label>
            <input type="text" class="input-field" id="inputAddress" value="${u.address || ''}">
          </div>
          <div class="text-right pt-2 flex justify-end gap-2">
            <button type="button" class="btn-secondary" id="btnCancelEdit">Hủy</button>
            <button type="submit" class="btn-primary">Lưu</button>
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
