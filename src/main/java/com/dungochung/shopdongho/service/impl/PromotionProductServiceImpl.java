package com.dungochung.shopdongho.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ProductPromotionInfoDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.PromotionEntity;
import com.dungochung.shopdongho.entity.PromotionProductEntity;
import com.dungochung.shopdongho.entity.PromotionProductId;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.repository.PromotionProductRepository;
import com.dungochung.shopdongho.repository.PromotionRepository;
import com.dungochung.shopdongho.service.PromotionProductService;

@Service
public class PromotionProductServiceImpl implements PromotionProductService {

	@Autowired
	private PromotionRepository promotionRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private PromotionProductRepository promotionProductRepository;

	@Override
	public ResponseDataDto addProductToPromotion(Integer promotionId, String productId) {
		Optional<PromotionEntity> promotionOpt = promotionRepository.findById(promotionId);
		Optional<ProductEntity> productOpt = productRepository.findById(productId);

		if (promotionOpt.isEmpty() || productOpt.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Promotion or Product not found");
		}

		PromotionProductId id = new PromotionProductId(promotionId, productId);
		PromotionProductEntity entity = new PromotionProductEntity(id, promotionOpt.get(), productOpt.get());
		promotionProductRepository.save(entity);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Product added to promotion");
	}

	@Override
	public ResponseDataDto removeProductFromPromotion(Integer promotionId, String productId) {
		PromotionProductId id = new PromotionProductId(promotionId, productId);
		if (!promotionProductRepository.existsById(id)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Association not found");
		}
		promotionProductRepository.deleteById(id);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Product removed from promotion");
	}

	@Override
	public ResponseDataDto getProductsByPromotion(Integer promotionId, int page, int size) {
		Optional<PromotionEntity> promotionOpt = promotionRepository.findById(promotionId);
		if (promotionOpt.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Promotion not found");
		}

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "product.createdAt"));
		Page<PromotionProductEntity> pageResult = promotionProductRepository.findByPromotion_PromotionId(promotionId,
				pageable);

		List<ProductPromotionInfoDto> result = pageResult.getContent().stream().map(pp -> {
			ProductEntity product = pp.getProduct();
			PromotionEntity promotion = pp.getPromotion();
			return new ProductPromotionInfoDto(product.getProductId(), product.getName(), promotion.getPromotionId(),
					promotion.getName(), promotion.getDiscountType().toString(), promotion.getDiscountValue(),
					promotion.getStartAt(), promotion.getEndAt());
		}).collect(Collectors.toList());

		Map<String, Object> data = new HashMap<>();
		data.put("products", result);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, pageResult.getTotalPages(), size));
		data.put("totalCount", pageResult.getTotalElements());

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Product list with promotion details", data);
	}

	@Override
	public ResponseDataDto searchProductPromotion(Integer promotionId, String productName, int page, int size) {

		// Tạo Pageable
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "promotion.createdAt"));

		// Query theo cả promotionId và productName
		Page<PromotionProductEntity> pageResult = promotionProductRepository
				.findByPromotion_PromotionIdAndProduct_NameContainingIgnoreCase(promotionId, productName, pageable);

		// Chuyển đổi sang DTO
		List<ProductPromotionInfoDto> dtos = pageResult.getContent().stream().map(pp -> {
			ProductEntity prod = pp.getProduct();
			PromotionEntity promo = pp.getPromotion();
			return new ProductPromotionInfoDto(prod.getProductId(), prod.getName(), promo.getPromotionId(),
					promo.getName(), promo.getDiscountType().name(), promo.getDiscountValue(), promo.getStartAt(),
					promo.getEndAt());
		}).collect(Collectors.toList());

		// Đóng gói pagination và kết quả
		Map<String, Object> data = new HashMap<>();
		data.put("products", dtos);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, pageResult.getTotalPages(), 3));
		data.put("totalElements", pageResult.getTotalElements());

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Search products in promotion", data);
	}

}
