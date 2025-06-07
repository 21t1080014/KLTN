package com.dungochung.shopdongho.service;

import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface PromotionProductService {
	ResponseDataDto addProductToPromotion(Integer promotionId, String productId);

	ResponseDataDto removeProductFromPromotion(Integer promotionId, String productId);

	ResponseDataDto getProductsByPromotion(Integer promotionId, int page, int size);

	ResponseDataDto searchProductPromotion(Integer promotionId, String productName, int page, int size);
}
