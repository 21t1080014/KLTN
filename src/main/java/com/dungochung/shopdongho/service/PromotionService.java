package com.dungochung.shopdongho.service;

import com.dungochung.shopdongho.dto.PromotionDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface PromotionService {
	ResponseDataDto getAllPromotion(int page, int size);

	ResponseDataDto searchPromotion(String keyword, String discountType, String discountValue, int page, int size);

	ResponseDataDto createPromotion(PromotionDto promotionDto);

	ResponseDataDto updatePromotion(Integer promotionId, PromotionDto promotionDto);

	ResponseDataDto deletePromotion(Integer promotionId);

	ResponseDataDto getPromotionById(Integer promotionId);
}
