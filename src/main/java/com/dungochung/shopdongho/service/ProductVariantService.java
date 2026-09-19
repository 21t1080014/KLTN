package com.dungochung.shopdongho.service;

import java.math.BigDecimal;
import java.util.List;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.ProductVariantEntity;

public interface ProductVariantService {
	/** Lấy biến thể mặc định của sản phẩm, tự tạo (sku = sku sản phẩm, giá = giá sản phẩm) nếu chưa có. */
	ProductVariantEntity ensureDefaultVariant(ProductEntity product);

	/** Đồng bộ giá + sku của biến thể mặc định theo sản phẩm (gọi khi sửa sản phẩm ở form cũ). */
	void syncDefaultFromProduct(ProductEntity product);

	ResponseDataDto listVariants(String productId);

	ResponseDataDto createVariant(String productId, String sku, String variantName, String color, String strapOption,
			String caseSize, BigDecimal price, String status, Integer initialQuantity, Integer lowStockThreshold);

	ResponseDataDto updateVariant(String productId, Long variantId, String variantName, String color,
			String strapOption, String caseSize, BigDecimal price, String status, Integer lowStockThreshold);

	ResponseDataDto setDefault(String productId, Long variantId);

	ResponseDataDto deleteVariant(String productId, Long variantId);

	ResponseDataDto setVariantImages(String productId, Long variantId, List<Integer> imageIds);
}
