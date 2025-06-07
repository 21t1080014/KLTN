// cart-utils.js

function getCart() {
	return JSON.parse(localStorage.getItem("cart")) || [];
}

function saveCart(cart) {
	localStorage.setItem("cart", JSON.stringify(cart));
}

export function updateCartCount() {
	const cart = getCart();
	const totalQuantity = cart.reduce((sum, item) => sum + (parseInt(item.quantity, 10) || 0), 0);
	const cartCount = document.getElementById("cart-count");
	if (cartCount) {
		cartCount.innerText = totalQuantity;
	}
}

export function showCart() {
	const cart = getCart();
	const container = document.getElementById("cart-content");
	const totalContainer = document.getElementById("cart-total");

	if (!container) return;

	if (cart.length === 0) {
		container.innerHTML = `
			<div class="text-center text-muted py-4">
				<i class="bi bi-cart-x fs-1"></i>
				<p class="mt-2">Giỏ hàng trống.</p>
			</div>
		`;
		if (totalContainer) {
			totalContainer.innerHTML = `
				<span class="text-danger">Tổng tiền:</span><span>0 ₫</span>
			`;
		}
		return;
	}

	let html = "<div class='list-group'>";
	let total = 0;

	for (const item of cart) {
		const itemPrice = parseInt(item.price.toString().replace(/[^\d]/g, ""), 10) || 0;
		const itemQuantity = parseInt(item.quantity, 10) || 0;
		const itemTotal = itemPrice * itemQuantity;
		total += itemTotal;

		html += `
		  <div class="list-group-item d-flex align-items-start gap-3">
		    <img src="${item.img}" alt="${item.name}" width="70" height="70" class="rounded border" style="object-fit: cover;" />
		    <div class="flex-grow-1 overflow-hidden">
		      <div class="fw-semibold" style="word-break: break-word;">
		        ${item.name}
		      </div>
		      <div class="small text-muted mb-1">Giá: ${item.price}</div>
		      <div class="d-flex align-items-center gap-2">
		        <input type="number" value="${itemQuantity}" min="1"
		          class="form-control form-control-sm w-50"
		          onchange="updateItemQuantity('${item.id}', this.value)" />
		        <button class="btn btn-sm btn-outline-danger"
		          onclick="removeItemFromCart('${item.id}')">
		          Xóa
		        </button>
		      </div>
		    </div>
		  </div>
		`;

	}

	html += "</div>";
	container.innerHTML = html;

	if (totalContainer) {
		totalContainer.innerHTML = `
			<span class="text-danger">Tổng tiền:</span><span>${total.toLocaleString("vi-VN")} ₫</span>
		`;
	}
}


export function clearCart() {
	localStorage.removeItem("cart");
	updateCartCount();
	showCart();
}

export function removeItemFromCart(productId) {
	let cart = getCart();
	cart = cart.filter(item => item.id !== productId);
	saveCart(cart);
	updateCartCount();
	showCart();
}

export function updateItemQuantity(productId, newQuantity) {
	let cart = getCart();
	const index = cart.findIndex(item => item.id === productId);
	if (index !== -1) {
		cart[index].quantity = newQuantity > 0 ? newQuantity : 1;
		saveCart(cart);
		updateCartCount();
		showCart();
	}
}

// Đảm bảo các hàm xóa/cập nhật vẫn hoạt động từ HTML
window.removeItemFromCart = removeItemFromCart;
window.updateItemQuantity = updateItemQuantity;
