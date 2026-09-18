import { currency } from './product-utils.js';
import { renderProductCard } from './product-utils.js';
import { initSearchSuggestions } from './search-suggestions.js';
import { updateCartCount } from './cart-utils.js';
import { showCart } from './cart-utils.js';
const productId = document.body.dataset.productId;
console.log("Id:", productId);
let thumbnails = [];
let currentImageIndex = 0;
let availableQuantity = 0;

/*function updateCartCount() {
	const cart = JSON.parse(localStorage.getItem("cart")) || [];
	const totalQuantity = cart.reduce((sum, item) => sum + item.quantity, 0);
	document.getElementById("cart-count").innerText = totalQuantity;
}*/
window.showCart = showCart;
document.addEventListener("DOMContentLoaded", () => {
	updateCartCount();
	showCart();
});

function renderThumbnails(images, productName) {
	const thumbnailContainer = document.getElementById("thumbnail-images");
	thumbnailContainer.innerHTML = "";

	thumbnails = images;

	images.forEach((src, index) => {
		const img = document.createElement("img");
		img.src = src;
		img.className = "thumb";
		img.alt = (productName || "Sản phẩm") + " - ảnh " + (index + 1);
		img.onclick = () => {
			currentImageIndex = index;
			updateMainImage(src);
		};
		thumbnailContainer.appendChild(img);
	});

	if (images.length > 0) {
		updateMainImage(images[0]);
	}
}

function updateMainImage(src) {
	document.getElementById("main-image").src = src;
}

// Chuyển ảnh khi click main-image ở màn nhỏ
document.addEventListener("DOMContentLoaded", function() {
	const mainImageContainer = document.querySelector(".main-image");
	mainImageContainer.addEventListener("click", () => {
		if (window.innerWidth <= 768 && thumbnails.length > 1) {
			currentImageIndex = (currentImageIndex + 1) % thumbnails.length;
			updateMainImage(thumbnails[currentImageIndex]);
		}
	});
});

document.addEventListener("DOMContentLoaded", function() {
	fetch(`/api/product-detail/${productId}`)
		.then(res => res.json())
		.then(data => {
			if (data.responseCode === 1) {
				const p = data.data;
				availableQuantity = p.quantity;
				// Đặt tên sản phẩm
				document.getElementById("product-name").innerText = p.name;
				document.getElementById("product-name-title").innerText = p.name;

				// Các thông tin khác
				document.getElementById("brand-name").innerText = p.brandName || "Đang cập nhật";
				document.getElementById("origin").innerText = p.origin;
				document.getElementById("warranty").innerText = p.warrantyPeriod;
				document.getElementById("gender").innerText = p.gender === "MALE" ? "Nam" : "Nữ";
				document.getElementById("description").innerText = p.description;

				// Hiển thị giá
				const price = parseFloat(p.price);
				const discount = parseFloat(p.discountPrice);
				document.getElementById("discount-price").innerText = discount.toLocaleString() + "đ";
				document.getElementById("original-price").innerText = (price !== discount ? price.toLocaleString() + "đ" : "");
				// Cập nhật tình trạng hàng
				const statusSpan = document.querySelector(".in-stock");
				if (p.quantity > 0) {
					statusSpan.innerText = "Còn hàng";
					statusSpan.classList.remove("text-danger");
					statusSpan.classList.add("text-success");
				} else {
					statusSpan.innerText = "Hết hàng";
					statusSpan.classList.remove("text-success");
					statusSpan.classList.add("text-danger");
				}

				// === ⚠️ Xử lý badge giảm giá
				const badge = document.querySelector(".main-image .badge-sale");
				if (p.discountType && p.discountValue > 0) {
					if (p.discountType === "PERCENT") {
						badge.innerText = `-${p.discountValue}%`;
					} else if (p.discountType === "AMOUNT") {
						badge.innerText = `-${p.discountValue.toLocaleString()}đ`;
					}
					badge.style.display = "block";
				} else {
					badge.style.display = "none"; // Ẩn nếu không có giảm giá
				}

				// Hiển thị ảnh
				const imageUrls = p.images.map(img => `/api/img/${img.url}`);
				renderThumbnails(imageUrls, p.name);
				renderProductDetailTable(p);
			}
		});
});
function renderProductDetailTable(p) {
	const table = document.getElementById("product-detail-table");
	table.innerHTML = ""; // Xóa nội dung cũ

	// Mảng các cặp key: label hiển thị
	const fields = [
		["sku", "Mã sản phẩm"],
		["brandName", "Thương hiệu"],
		["origin", "Xuất xứ"],
		["warrantyPeriod", "Bảo hành"],
		["gender", "Giới tính"],
		["segment", "Phân khúc"],
		["typeName", "Loại máy"],
		["caseMaterialName", "Chất liệu vỏ"],
		["strapMaterialName", "Chất liệu dây"],
		["glassMaterialName", "Chất liệu kính"]
		/*["condition", "Tình trạng"],*/
		/*["price", "Giá gốc"],*/
		/*["discountType", "Loại giảm giá"],
		["discountValue", "Giá trị giảm"],
		["discountPrice", "Giá sau giảm"],*/
		/*["createdAt", "Ngày tạo"],
		["updatedAt", "Ngày cập nhật"]*/
	];

	fields.forEach(([key, label]) => {
		const value = p[key];
		if (value !== null && value !== undefined && value !== "") {
			const row = document.createElement("tr");

			const labelCell = document.createElement("td");
			labelCell.textContent = label;
			labelCell.style.fontWeight = "bold";

			const valueCell = document.createElement("td");
			if (key === "price" || key === "discountPrice" || key === "discountValue") {
				valueCell.textContent = Number(value).toLocaleString() + (key === "discountValue" && p.discountType === "AMOUNT" ? "đ" : (key === "discountValue" && p.discountType === "PERCENT" ? "%" : "đ"));
			} else {
				valueCell.textContent = value;
			}

			row.appendChild(labelCell);
			row.appendChild(valueCell);
			table.appendChild(row);
		}
	});
}

function decreaseQuantity() {
	const q = document.getElementById("quantity");
	if (parseInt(q.value) > 1) q.value--;
}
function increaseQuantity() {
	const q = document.getElementById("quantity");
	q.value = parseInt(q.value) + 1;
}
function addToCart() {
	const quantity = parseInt(document.getElementById("quantity").value);
	const id = productId;
	const name = document.getElementById("product-name-title").innerText;
	const price = document.getElementById("discount-price").innerText;
	const img = document.getElementById("main-image").src;

	if (availableQuantity === 0) {
		alert("Sản phẩm hiện đã hết hàng.");
		return;
	}

	let cart = JSON.parse(localStorage.getItem("cart")) || [];

	// Kiểm tra nếu sản phẩm đã tồn tại thì tăng số lượng
	const existingItem = cart.find(item => item.id === id);
	const currentInCart = existingItem ? existingItem.quantity : 0;
	const totalAfterAdd = currentInCart + quantity;

	if (totalAfterAdd > availableQuantity) {
		alert(`Chỉ còn lại ${availableQuantity} sản phẩm trong kho. Bạn đã có ${currentInCart} sản phẩm trong giỏ.`);
		return;
	}

	if (existingItem) {
		existingItem.quantity = totalAfterAdd;
	} else {
		cart.push({ id, name, price, quantity, img });
	}

	localStorage.setItem("cart", JSON.stringify(cart));

	updateCartCount(); // Cập nhật số hiển thị

	alert("Đã thêm " + quantity + " sản phẩm vào giỏ hàng!");
}

$(document).ready(function() {
	initSearchSuggestions();
});
document.querySelectorAll(".tab").forEach(tab => {
	tab.addEventListener("click", () => {
		// Bỏ active tất cả tab button
		document.querySelectorAll(".tab").forEach(t => t.classList.remove("active"));
		// Thêm active cho tab hiện tại
		tab.classList.add("active");

		// Ẩn hết các tab-content
		document.querySelectorAll(".tab-content").forEach(tc => tc.classList.remove("active"));

		// Hiển thị tab-content tương ứng với tab đang nhấn
		const tabName = tab.dataset.tab;
		document.getElementById(tabName).classList.add("active");
	});
});
document.addEventListener("DOMContentLoaded", () => {
	const addToCartBtn = document.getElementById("add-to-cart-btn");
	addToCartBtn.addEventListener("click", addToCart);

	const buyNowBtn = document.getElementById("buy-now-btn");
	if (buyNowBtn) {
		buyNowBtn.addEventListener("click", () => {
			if (availableQuantity === 0) {
				alert("Sản phẩm hiện đã hết hàng.");
				return;
			}
			addToCart();
			window.location.href = "/checkout";
		});
	}
});
// Khi trang load, có thể set mặc định
document.getElementById("detail").classList.add("active");