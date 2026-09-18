//orderCustomer.js
import { initSearchSuggestions } from './search-suggestions.js';
import { updateCartCount, showCart } from './cart-utils.js';
document.addEventListener("DOMContentLoaded", () => {
	updateCartCount();
	showCart();
	initSearchSuggestions();
});
function loadOrders(page = 0) {
	$.get(`/api/orders?page=${page}&size=2`, function(response) {
		if (response.responseCode !== 1) {
			$('#message').text(response.responseMsg);
			return;
		}

		const data = response.data;
		const orders = data.orders;
		const pagination = data.pagination;

		const container = $('#order-list');
		container.empty();

		orders.forEach(order => {
			const itemsHtml = order.items.map(item => `
					<tr>
						<td class="py-2 text-sm text-charcoal-soft">${item.productName}</td>
						<td class="py-2 text-sm text-charcoal-soft text-center">${item.quantity}</td>
						<td class="py-2 text-sm text-charcoal-soft text-right">${item.unitPrice}</td>
						<td class="py-2 text-sm text-charcoal text-right">${item.total}</td>
					</tr>`).join("");

			container.append(`
					<div class="order border border-line p-6">
						<div class="grid grid-cols-2 gap-2 text-sm text-charcoal-soft mb-4">
							<div><strong class="text-charcoal">Mã đơn hàng:</strong> ${order.orderId}</div>
							<div><strong class="text-charcoal">Ngày tạo:</strong> ${order.createdAt}</div>
							<div><strong class="text-charcoal">Tổng tiền:</strong> ${order.totalPrice} VNĐ</div>
							<div><strong class="text-charcoal">Phương thức thanh toán:</strong> ${order.paymentMethod}</div>
							<div><strong class="text-charcoal">Trạng thái thanh toán:</strong> ${order.paymentStatus}</div>
							<div><strong class="text-charcoal">Trạng thái đơn hàng:</strong> ${order.orderStatus}</div>
						</div>
						<h4 class="text-sm tracking-wide2 uppercase text-charcoal mb-2">Sản phẩm</h4>
						<table class="w-full">
							<thead>
								<tr class="border-b border-line">
									<th class="py-2 text-left text-xs text-charcoal-muted">Sản phẩm</th>
									<th class="py-2 text-center text-xs text-charcoal-muted">Số lượng</th>
									<th class="py-2 text-right text-xs text-charcoal-muted">Đơn giá</th>
									<th class="py-2 text-right text-xs text-charcoal-muted">Thành tiền</th>
								</tr>
							</thead>
							<tbody class="divide-y divide-line">${itemsHtml}</tbody>
						</table>
					</div>
				`);
		});

		renderPagination(pagination);
	});
}
function renderPagination(pagination) {
	const cur = pagination.currentPage;
	const total = pagination.totalPages;
	const ul = $('#pagination').empty();
	if (total <= 1) return;

	const pages = [];
	pages.push(0); // luôn luôn có trang đầu

	if (total <= 7) {
		for (let i = 1; i < total - 1; i++) pages.push(i);
	} else {
		if (cur <= 3) {
			for (let i = 1; i <= 3; i++) pages.push(i);
			pages.push('...');
		} else if (cur >= total - 4) {
			pages.push('...');
			for (let i = total - 4; i < total - 1; i++) pages.push(i);
		} else {
			pages.push('...');
			pages.push(cur - 1, cur, cur + 1);
			pages.push('...');
		}
	}

	pages.push(total - 1);

	ul.append(`
			<li class="page-item ${cur === 0 ? 'disabled' : ''}">
				<a class="page-link" href="#" onclick="loadOrders(0);return false;">First</a>
			</li>
			<li class="page-item ${cur === 0 ? 'disabled' : ''}">
				<a class="page-link" href="#" onclick="loadOrders(${cur - 1});return false;">Previous</a>
			</li>
		`);

	pages.forEach(p => {
		if (p === '...') {
			ul.append(`
					<li class="page-item disabled">
						<span class="page-link">…</span>
					</li>
				`);
		} else {
			ul.append(`
					<li class="page-item ${p === cur ? 'active' : ''}">
						<a class="page-link" href="#" onclick="loadOrders(${p});return false;">${p + 1}</a>
					</li>
				`);
		}
	});

	ul.append(`
			<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
				<a class="page-link" href="#" onclick="loadOrders(${cur + 1});return false;">Next</a>
			</li>
			<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
				<a class="page-link" href="#" onclick="loadOrders(${total - 1});return false;">Last</a>
			</li>
		`);
}


$(document).ready(() => {
	loadOrders(); // gọi trang đầu tiên khi vừa load
});
window.loadOrders = loadOrders;
window.renderPagination = renderPagination;