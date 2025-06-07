export function initSearchSuggestions() {
	let debounceTimer;

	// Toggle và chặn propagation khi click icon
	$('#searchToggle').on('click', function(e) {
		e.preventDefault();
		e.stopPropagation();
		$('#searchBox').slideToggle();
		$('#searchInput').focus();
	});

	// Chặn click bên trong searchBox và suggestions khỏi lan ra document
	$('#searchBox, #searchSuggestions').on('click', e => e.stopPropagation());

	// Click ngoài toggle + box ⇒ đóng
	$(document).off('click.search').on('click.search', function() {
		$('#searchBox').slideUp();
		$('#searchSuggestions').hide();
	});

	// Xử lý gợi ý khi nhập
	$('#searchInput').on('input', function() {
		clearTimeout(debounceTimer);
		// ẩn ngay cũ và xóa nội dung cũ
		$('#searchSuggestions').hide().empty();
		const keyword = $(this).val().trim();
		if (!keyword) return;

		debounceTimer = setTimeout(() => {
			$.ajax({
				url: '/api/products-suggest',
				method: 'GET',
				data: { keyword: keyword },
				success: function(response) {
					const products = response.data?.products || [];
					const total = response.data?.total || 0;
					if (products.length === 0) {
						$('#searchSuggestions').hide().empty();
						return;
					}

					// Build HTML gợi ý sản phẩm
					let html = products.map(p => {
						const imageUrl = p.images?.[0]?.url
							? `/api/img/${p.images[0].url}`
							: '/img/default.png';
						return `
              <a href="/detail-shop/${p.productId}" class="list-group-item list-group-item-action">
                <div class="d-flex align-items-center">
                  <img src="${imageUrl}" alt="${p.name}" width="40" height="40" class="me-2 rounded">
                  <div>
                    <div class="fw-bold">${p.name}</div>
                    <small>${p.price.toLocaleString()}đ</small>
                  </div>
                </div>
              </a>
            `;
					}).join('');

					// Nếu còn nhiều kết quả hơn hiển thị
					if (total > products.length) {
						html += `
              <a href="/search-shop?keyword=${encodeURIComponent(keyword)}" class="list-group-item list-group-item-action text-center text-primary fw-semibold">
                Xem thêm ${total} kết quả...
              </a>
            `;
					}

					// Show suggestions
					$('#searchSuggestions').html(html).show();
				},
				error: function() {
					$('#searchSuggestions').hide().empty();
				}
			});
		}, 300);
	});

	// Bắt click gợi ý và điều hướng thủ công
	$('#searchSuggestions').on('click', 'a', function(e) {
		e.preventDefault();
		const href = this.href;
		window.location.href = href;
	});
}
