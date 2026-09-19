export function loadVouchers() {
	$.get('/api/voucher', response => {
		if (response.responseCode !== 1) {
			$('#voucher-message').text(response.responseMsg);
			return;
		}
		const list = response.data;
		const c = $('#voucher-list').empty();
		if (!list.length) {
			c.append('<p class="text-sm text-charcoal-muted">Không có voucher khả dụng.</p>');
			return;
		}
		list.forEach(v => {
			c.append(`
        <div class="border border-line p-5">
          <h5 class="font-heading text-base text-charcoal mb-1">${v.voucherCode}</h5>
          <p class="text-sm text-charcoal-soft mb-2">${v.description}</p>
          <p class="text-sm text-charcoal-soft">Giảm: ${v.discountValue}${v.discountType === 'percent' ? '%' : ' VNĐ'}</p>
          <p class="text-sm text-charcoal-soft">Đơn tối thiểu: ${v.minOrderAmount} VNĐ</p>
          <p class="text-xs text-charcoal-muted mt-2">Hiệu lực: ${v.startAt} → ${v.endAt}</p>
        </div>
      `);
		});
	});
}
window.loadVouchers = loadVouchers;
