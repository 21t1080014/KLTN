import { updateCartCount, showCart } from './cart-utils.js';
import { initSearchSuggestions } from './search-suggestions.js';

// Gán global để dùng ngoài HTML
window.showLogin = showLogin;
window.showRegister = showRegister;
window.showCart = showCart;

// Khi trang tải xong
document.addEventListener("DOMContentLoaded", () => {
	updateCartCount();
	showCart();
	initSearchSuggestions();

	const btnSubmitRegister = document.getElementById("btnSubmitRegister");
	const btnSubmitVerify = document.getElementById("btnSubmitVerify");

	if (btnSubmitRegister) {
		btnSubmitRegister.addEventListener("click", register);
	}

	if (btnSubmitVerify) {
		btnSubmitVerify.addEventListener("click", submitVerifyCode);
	}

	const loginForm = document.getElementById("loginFormElement");
	if (loginForm) {
		loginForm.addEventListener("submit", (e) => {
			e.preventDefault();
			login();
		});
	}
});

// Chuyển sang form đăng nhập
function showLogin() {
	document.getElementById("loginForm").style.display = "block";
	document.getElementById("registerForm").style.display = "none";
	document.getElementById("btnLogin").classList.add("active");
	document.getElementById("btnRegister").classList.remove("active");
}

// Chuyển sang form đăng ký
function showRegister() {
	document.getElementById("loginForm").style.display = "none";
	document.getElementById("registerForm").style.display = "block";
	document.getElementById("btnRegister").classList.add("active");
	document.getElementById("btnLogin").classList.remove("active");
}

// Xử lý đăng nhập
async function login() {
	const usernameOrEmail = document.getElementById("usernameOrEmail").value;
	const password = document.getElementById("loginPassword").value;

	const formData = new FormData();
	formData.append("usernameOrEmail", usernameOrEmail);
	formData.append("password", password);

	try {
		const response = await fetch("/login", {
			method: "POST",
			body: formData
		});

		if (response.redirected) {
			window.location.href = response.url;
		} else {
			alert("Đăng nhập thất bại!");
		}
	} catch (err) {
		console.error("Lỗi đăng nhập:", err);
		alert("Có lỗi xảy ra khi đăng nhập.");
	}
}

// Xử lý đăng ký
async function register() {
	const form = document.querySelector("#registerForm form");
	const formData = new FormData(form);

	try {
		const response = await fetch("/register", {
			method: "POST",
			body: formData
		});
		const result = await response.json();

		if (result.responseCode === 1 && result.responseMsg === "OK") {
			// Hiển thị modal xác minh
			const modalEl = document.getElementById("verifyModal");
			const modal = new bootstrap.Modal(modalEl);
			modal.show();
		} else {
			alert(result.responseMsg || "Đăng ký thất bại!");
		}
	} catch (err) {
		console.error("Lỗi khi đăng ký:", err);
		alert("Lỗi hệ thống. Vui lòng thử lại sau.");
	}
}

// Gửi mã xác minh
async function submitVerifyCode() {
	const codeInput = document.getElementById("verifyCodeInput").value.trim();

	if (codeInput.length !== 6) {
		showVerifyError("Mã xác minh phải gồm 6 chữ số.");
		return;
	}

	try {
		const response = await fetch("/verify-code", {
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			},
			body: JSON.stringify({ code: codeInput })
		});
		const result = await response.json();

		if (result.responseCode === 1) {
			alert("Xác minh thành công! Bạn có thể đăng nhập.");
			const modalEl = document.getElementById("verifyModal");
			const modal = bootstrap.Modal.getInstance(modalEl);
			modal.hide();
			showLogin();
		} else {
			showVerifyError(result.responseMsg || "Mã xác minh không đúng.");
		}
	} catch (err) {
		console.error("Lỗi khi xác minh:", err);
		showVerifyError("Lỗi hệ thống. Vui lòng thử lại.");
	}
}
window.submitVerifyCode = submitVerifyCode;
// Hiển thị lỗi xác minh
function showVerifyError(msg) {
	const feedback = document.getElementById("verifyFeedback");
	if (feedback) {
		feedback.textContent = msg;
		feedback.style.display = "block";
	}
}
