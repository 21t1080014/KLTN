// product-utils.js
export const currency = n => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(n);

export function renderProductCard(p) {

	return `
    <div class="col">
      <div class="product-card group" data-product-id="${p.productId}">
        <div class="relative aspect-square overflow-hidden bg-ivory">
          <img src="${p.imageUrl}" alt="${p.productName}" class="w-full h-full object-contain">

          ${p.discountPercent > 0
			? `<span class="badge-sale">-${p.discountPercent}%</span>`
			: ``}

          <!-- Nếu hết hàng thì hiển thị dòng chữ -->
          ${p.quantity === 0
			? `<div class="out-of-stock-overlay absolute inset-0 flex items-center justify-center bg-charcoal/50">
               <span class="text-white text-sm tracking-wide2 uppercase">Hết hàng</span>
             </div>`
			: ''}

          <!-- Overlay nút: luôn hiện trên mobile, hiện khi hover trên desktop -->
          <div class="card-overlay absolute inset-x-0 bottom-0 flex md:opacity-0 md:group-hover:opacity-100 transition-opacity duration-200 items-center justify-center gap-2 px-3 pb-3">
		<button class="btn-secondary !bg-white !px-3 !py-2 text-xs add-to-cart-btn"
		        data-id="${p.productId}"
		        ${p.quantity === 0 ? 'disabled' : ''}>
		  Thêm vào giỏ
		</button>
            <button class="btn-icon !bg-white quick-view-btn"
		          data-product-id="${p.productId}" aria-label="Xem nhanh">
		    <i class="bi bi-eye"></i>
		  </button>
          </div>
        </div>

        <div class="p-4">
          <h6 class="card-title text-sm text-charcoal-soft truncate">${p.productName}</h6>
          <div class="flex items-baseline flex-wrap gap-2 mt-1">
            <strong class="price-current text-base">${currency(p.discountedPrice)}</strong>
            ${p.discountedPrice < p.originalPrice
				? `<small class="price-original">${currency(p.originalPrice)}</small>`
				: ''}
          </div>
        </div>
      </div>
    </div>`;
}

