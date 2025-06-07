// product-utils.js
export const currency = n => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(n);

export function renderProductCard(p) {

	return `
    <div class="col">
      <div class="product-card position-relative overflow-hidden" data-product-id="${p.productId}">
        ${p.discountPercent > 0
			? `<span class="badge-sale">-${p.discountPercent}%</span>`
			: ``}

        <img src="${p.imageUrl}" alt="${p.productName}" class="w-100">

        <!-- Nếu hết hàng thì hiển thị dòng chữ -->
        ${p.quantity === 0
			? `<div class="out-of-stock-overlay d-flex justify-content-center align-items-center">
               <span class="text-white fw-bold fs-5">Hết hàng</span>
             </div>`
			: ''}

        <!-- Overlay nút -->
        <div class="card-overlay d-flex justify-content-center align-items-center">
		<button class="btn btn-sm btn-primary me-2 add-to-cart-btn"
		        data-id="${p.productId}"
		        ${p.quantity === 0 ? 'disabled' : ''}>
		  Thêm vào giỏ
		</button>
          <button class="btn btn-sm btn-light quick-view-btn" 
		          data-product-id="${p.productId}">
		    Xem nhanh
		  </button>
        </div>

        <h6 class="card-title mt-2">${p.productName}</h6>
        <div class="d-inline-flex align-items-baseline flex-nowrap gap-2">
          <strong class="text-danger">${currency(p.discountedPrice)}</strong>
          ${p.discountedPrice < p.originalPrice
			? `<small class="text-muted text-decoration-line-through">${currency(p.originalPrice)}</small>`
			: ''}
        </div>
      </div>
    </div>`;
}

