export function loadVouchers() {
	$.get('/api/voucher', response => {
		if (response.responseCode !== 1) {
			$('#voucher-message').text(response.responseMsg);
			return;
		}
		const list = response.data;
		const c = $('#voucher-list').empty();
		if (!list.length) {
			c.append('<p>Không có voucher khả dụng.</p>');
			return;
		}
		list.forEach(v => {
			c.append(`
        <div class="border p-3 mb-3 rounded">
          <h5>${v.voucherCode}</h5>
          <p>${v.description}</p>
          <p>Giảm: ${v.discountValue}${v.discountType === 'percent' ? '%' : ' VNĐ'}</p>
          <p>Đơn tối thiểu: ${v.minOrderAmount} VNĐ</p>
          <p>Hiệu lực: ${v.startAt} → ${v.endAt}</p>
        </div>
      `);
		});
	});
}
window.loadVouchers = loadVouchers;
