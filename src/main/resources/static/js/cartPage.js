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
			cartItemsContainer.innerHTML = `
				<h4 class="font-heading text-xl text-charcoal mb-6">Giỏ hàng</h4>
				<div class="text-center text-charcoal-muted py-16">
					<i class="bi bi-bag fs-1"></i>
					<p class="mt-3 text-sm">Giỏ hàng trống.</p>
				</div>
			`;
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
				<div class="flex border-b border-line py-5 items-center gap-4" data-index="${i}">
					<img src="${item.img}" alt="${item.name}" width="100" class="border border-line object-cover" />
					<div class="flex-1">
						<h6 class="text-charcoal mb-1">${item.name}</h6>
						<div class="text-charcoal-muted text-xs">SKU: ${item.sku}</div>
						<div class="price-current mt-1">${price.toLocaleString("vi-VN")}₫</div>
						<div class="flex items-center gap-3 mt-3">
							<button class="btn-icon !w-8 !h-8 btn-minus">−</button>
							<span class="text-sm">${item.quantity}</span>
							<button class="btn-icon !w-8 !h-8 btn-plus">+</button>
							<a href="#" class="ml-3 text-xs text-sale hover:underline btn-delete">Xóa</a>
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
		cartItemsContainer.innerHTML = "<h4 class=\"font-heading text-xl text-charcoal mb-6\">Giỏ hàng</h4>";
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
