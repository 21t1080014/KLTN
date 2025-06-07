// checkoutPage.js
import 'https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js';
import { updateCartCount } from './cart-utils.js';
import { showCart } from './cart-utils.js';
window.showCart = showCart;
document.addEventListener("DOMContentLoaded", () => {
	updateCartCount();
	showCart();
	// Hiển thị mô tả theo phương thức thanh toán
	const codRadio = document.getElementById('cod');
	const bankRadio = document.getElementById('bank');
	const codDesc = document.getElementById('codDescription');
	const bankDesc = document.getElementById('bankDescription');

	function updatePaymentDescription() {
		if (codRadio.checked) {
			codDesc.style.display = 'block';
			bankDesc.style.display = 'none';
		} else if (bankRadio.checked) {
			codDesc.style.display = 'none';
			bankDesc.style.display = 'block';
		}
	}

	// Gán sự kiện khi chọn radio button
	codRadio.addEventListener('change', updatePaymentDescription);
	bankRadio.addEventListener('change', updatePaymentDescription);

	// Gọi hàm lúc load để hiển thị đúng
	updatePaymentDescription();

});
/**
 * Hiển thị modal xác nhận rồi gọi callback khi người dùng nhấn OK
 * @param {string} message - Nội dung HTML trong modal
 * @param {Function} callback - Hàm được gọi sau khi bấm OK
 */
function showConfirmation(message, callback) {
	const modalBody = document.getElementById('confirmationModalBody');
	modalBody.innerHTML = message;

	const modalEl = document.getElementById('confirmationModal');
	const bsModal = new bootstrap.Modal(modalEl);

	// Khi modal đã ẩn hoàn toàn (sau click OK), gọi callback
	modalEl.addEventListener('hidden.bs.modal', function handler() {
		modalEl.removeEventListener('hidden.bs.modal', handler);
		if (callback) callback();
	});

	bsModal.show();
}

document.addEventListener("DOMContentLoaded", () => {
	const cart = JSON.parse(localStorage.getItem("cart")) || [];
	const orderSummary = document.getElementById("order-summary");
	const subtotalEl = document.getElementById("subtotal");
	const shippingFeeEl = document.getElementById("shipping-fee");
	const totalEl = document.getElementById("total-amount");
	const couponInput = document.getElementById("coupon");
	const applyCouponBtn = couponInput.nextElementSibling;
	const placeOrderBtn = document.getElementById("place-order-btn");

	let subtotal = 0;
	let discountAmount = 0;
	let html = "";

	// Tính subtotal và render danh sách item
	cart.forEach(item => {
		const price = parseInt(item.price.toString().replace(/[^\d]/g, ''), 10);
		const itemTotal = price * item.quantity;
		subtotal += itemTotal;
		html += `
      <div class="d-flex justify-content-between mb-2">
        <div>${item.name} (x${item.quantity})</div>
        <div>${itemTotal.toLocaleString('vi-VN')}₫</div>
      </div>`;
	});

	const shippingFee = subtotal >= 500000 ? 0 : 30000;

	function updateTotals() {
		orderSummary.innerHTML = html || "<p>Giỏ hàng trống.</p>";
		subtotalEl.innerText = subtotal.toLocaleString('vi-VN') + "₫";
		shippingFeeEl.innerText = shippingFee.toLocaleString('vi-VN') + "₫";
		totalEl.innerText = (subtotal + shippingFee - discountAmount)
			.toLocaleString('vi-VN') + "₫";
	}

	updateTotals();

	// Xử lý apply voucher
	applyCouponBtn.addEventListener("click", () => {
		const voucherCode = couponInput.value.trim();
		if (!voucherCode) {
			showConfirmation("<p>Vui lòng nhập mã giảm giá.</p>");
			return;
		}

		const isLoggedIn = document.body.dataset.loggedIn === "true";
		if (!isLoggedIn) {
			showConfirmation("<p>Bạn cần đăng nhập để sử dụng mã giảm giá.</p>", () => {
				window.location.href = "/auth";
			});
			return;
		}

		const userId = document.body.dataset.userId;
		fetch("/api/apply-voucher", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({
				userId,
				totalPrice: subtotal + shippingFee,
				voucherCode
			})
		})
			.then(res => res.json())
			.then(data => {
				if (data.responseCode === 1) {
					const newTotal = data.data;
					discountAmount = (subtotal + shippingFee) - newTotal;
					updateTotals();
					showConfirmation("<p>Áp dụng mã giảm giá thành công!</p>");
				} else {
					showConfirmation(`<p>Không thể áp dụng mã: ${data.responseMsg}</p>`);
					discountAmount = 0;
					updateTotals();
				}
			})
			.catch(err => {
				console.error(err);
				showConfirmation("<p>Lỗi khi áp dụng mã giảm giá.</p>");
			});
	});

	// Xử lý đặt hàng
	placeOrderBtn.addEventListener("click", e => {
		e.preventDefault();

		const isLoggedIn = document.body.dataset.loggedIn === "true";
		if (!isLoggedIn) {
			window.location.href = "/auth";
			return;
		}

		const userId = document.body.dataset.userId;
		const paymentInput = document.querySelector('input[name="payment"]:checked');
		const paymentMethod = paymentInput?.id === "cod" ? "COD" : "TRANSFER";

		const items = cart.map(item => ({
			product: { productId: item.id },
			quantity: item.quantity,
			priceEach: parseInt(item.price.toString().replace(/[^\d]/g, ''), 10)
		}));

		const totalPrice = subtotal + shippingFee - discountAmount;

		fetch(`/api/create-orders?userId=${userId}&totalPrice=${totalPrice}&paymentMethod=${paymentMethod}`, {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(items)
		})
			.then(res => res.json())
			.then(data => {
				if (data.responseCode === 1) {
					const resData = data.data;
					// Đảm bảo lấy orderId dù data.data là số hay object
					const orderId = typeof resData === 'object' ? resData.orderId : resData;
					const grantedVoucher = typeof resData === 'object' ? resData.grantedVoucher : null;

					// Tạo nội dung modal
					let msg = `<p>Đặt hàng thành công!<br>Mã đơn: <strong>${orderId}</strong></p>`;
					if (grantedVoucher) {
						msg += `<p>Bạn nhận voucher: <strong>${grantedVoucher}</strong></p>`;
					}

					// Hiện modal, sau OK mới xóa cart + redirect
					showConfirmation(msg, () => {
						localStorage.removeItem("cart");
						window.location.href = "/checkout";
					});

				} else {
					showConfirmation(`<p>Đặt hàng thất bại:<br>${data.responseMsg}</p>`);
				}
			})
			.catch(err => {
				console.error(err);
				showConfirmation("<p>Có lỗi xảy ra khi gửi đơn hàng.</p>");
			});
	});
});
