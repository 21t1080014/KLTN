// quick-view-cart.js

import { updateCartCount } from './cart-utils.js';

/**
 * Khởi tạo Quick View modal và xử lý nút thêm vào giỏ trong Quick View
 */
export function initQuickViewCart() {
	// Tăng/giảm số lượng ngay trong modal
	window.incrementQty = () => {
		const $q = document.getElementById('qv-qty');
		const val = Math.max(1, parseInt($q.value, 10) + 1);
		$q.value = val;
	};
	window.decrementQty = () => {
		const $q = document.getElementById('qv-qty');
		const val = Math.max(1, parseInt($q.value, 10) - 1);
		$q.value = val;
	};

	// Bắt sự kiện cho nút Thêm vào giỏ ở Quick View modal
	document.addEventListener('click', e => {
		if (!e.target.matches('#qv-add-to-cart')) return;
		e.preventDefault();

		// Lấy productId kubva data-attribute hoặc từ detail link
		let productId = e.target.getAttribute('data-product-id');
		if (!productId) {
			const href = document.getElementById('qv-detail-link').getAttribute('href');
			productId = href.substring(href.lastIndexOf('/') + 1);
		}

		// Lấy thông tin sản phẩm từ modal
		const name = document.getElementById('qv-name').textContent.trim();
		const priceText = document.getElementById('qv-discounted').textContent.trim() ||
			document.getElementById('qv-original').textContent.trim();
		const imgEl = document.querySelector('#qv-carousel-inner .carousel-item.active img');
		const img = imgEl ? imgEl.getAttribute('src') : '';
		const quantity = parseInt(document.getElementById('qv-qty').value, 10) || 1;

		// Cập nhật giỏ hàng trong localStorage
		const cart = JSON.parse(localStorage.getItem('cart')) || [];
		const existing = cart.find(item => item.id === productId);
		if (existing) {
			existing.quantity += quantity;
		} else {
			cart.push({ id: productId, name, price: priceText, img, quantity });
		}
		localStorage.setItem('cart', JSON.stringify(cart));

		// Cập nhật số lượng giỏ hàng trên UI
		updateCartCount();

		// Đóng Quick View modal
		const modalEl = document.getElementById('quickViewModal');
		const qvModal = bootstrap.Modal.getInstance(modalEl);
		if (qvModal) qvModal.hide();

		// Thông báo
		alert(`Đã thêm vào giỏ: ${name} (x${quantity})`);
	});
}
