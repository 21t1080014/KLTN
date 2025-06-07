import { updateCartCount } from './cart-utils.js';
import { showCart } from './cart-utils.js';
window.showCart = showCart;
document.addEventListener("DOMContentLoaded", () => {
	updateCartCount();
	showCart();
});
document.addEventListener("DOMContentLoaded", function() {
	const cartKey = "cart";
	let cart = JSON.parse(localStorage.getItem(cartKey)) || [];

	const cartItemsContainer = document.getElementById("cart-items");
	const cartTotalContainer = document.getElementById("cart-total");

	function renderCart() {
		if (cart.length === 0) {
			cartItemsContainer.innerHTML += "<p>Giỏ hàng trống.</p>";
			cartTotalContainer.innerText = "0 ₫";
			return;
		}

		let total = 0;
		let html = "";

		for (let i = 0; i < cart.length; i++) {
			const item = cart[i];
			const price = parseInt(item.price.toString().replace(/[^\d]/g, ''), 10);
			const itemTotal = price * item.quantity;
			total += itemTotal;

			html += `
				<div class="d-flex border-bottom py-3 align-items-center" data-index="${i}">
					<img src="${item.img}" alt="${item.name}" width="100" class="me-3 rounded" />
					<div class="flex-grow-1">
						<h6 class="mb-1">${item.name}</h6>
						<div class="text-muted small">SKU: ${item.sku}</div>
						<div class="text-danger fw-bold mt-1">${price.toLocaleString("vi-VN")}₫</div>
						<div class="d-flex align-items-center mt-2">
							<button class="btn btn-outline-secondary btn-sm btn-minus">−</button>
							<span class="mx-2">${item.quantity}</span>
							<button class="btn btn-outline-secondary btn-sm btn-plus">+</button>
							<a href="#" class="ms-3 text-danger btn-delete">Xóa</a>
						</div>
					</div>
				</div>
			`;
		}

		cartItemsContainer.innerHTML += html;
		cartTotalContainer.innerText = total.toLocaleString("vi-VN") + " ₫";
	}

	function saveCart() {
		localStorage.setItem(cartKey, JSON.stringify(cart));
	}

	function updateView() {
		cartItemsContainer.innerHTML = "<h4>Giỏ hàng:</h4>";
		renderCart();
	}

	// Xử lý các nút cộng, trừ, xóa
	cartItemsContainer.addEventListener("click", function(e) {
		const index = e.target.closest("[data-index]")?.dataset.index;
		if (index === undefined) return;

		if (e.target.classList.contains("btn-minus")) {
			if (cart[index].quantity > 1) {
				cart[index].quantity--;
			}
		} else if (e.target.classList.contains("btn-plus")) {
			cart[index].quantity++;
		} else if (e.target.classList.contains("btn-delete")) {
			cart.splice(index, 1);
		} else {
			return;
		}

		saveCart();
		updateView();
	});

	// Lần đầu load
	renderCart();
});
