package com.dungochung.shopdongho.service.impl;

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
import com.dungochung.shopdongho.dto.PromotionDto;
import com.dungochung.shopdongho.dto.PromotionInfoDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.PromotionEntity;
import com.dungochung.shopdongho.repository.PromotionRepository;
import com.dungochung.shopdongho.service.PromotionProductService;
import com.dungochung.shopdongho.service.PromotionService;

import jakarta.transaction.Transactional;

@Service
public class PromotionServiceImpl implements PromotionService {
	@Autowired
	private PromotionRepository promotionRepository;
	@Autowired
	private PromotionProductService promotionProductService;

	@Override
	public ResponseDataDto getAllPromotion(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
		Page<PromotionEntity> promotion = promotionRepository.findAll(pageable);
		List<PromotionInfoDto> dtos = promotion.getContent().stream()
				.map(e -> new PromotionInfoDto(e.getPromotionId(), e.getName(), e.getDiscountType().name(),
						e.getDiscountValue(), e.getDescription(), e.getStartAt(), e.getEndAt()))
				.collect(Collectors.toList());
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("promotions", dtos);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, promotion.getTotalPages(), 3));
		data.put("totalCount", promotion.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto searchPromotion(String keyword, String discountTypeStr, String discountValue, int page,
			int size) {
		PromotionEntity.DiscountType discountType = null;
		if (discountTypeStr != null && !discountTypeStr.isBlank()) {
			discountType = PromotionEntity.DiscountType.valueOf(discountTypeStr);
		}
		Page<PromotionEntity> promotions = promotionRepository.searchPromotions(keyword, discountType, discountValue,
				PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

		List<PromotionInfoDto> dtos = promotions.getContent().stream()
				.map(e -> new PromotionInfoDto(e.getPromotionId(), e.getName(), e.getDiscountType().name(),
						e.getDiscountValue(), e.getDescription(), e.getStartAt(), e.getEndAt()))
				.collect(Collectors.toList());

		Map<String, Object> data = new HashMap<>();
		data.put("promotions", dtos);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, promotions.getTotalPages(), 3));
		data.put("totalCount", promotions.getTotalElements());

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Search results", data);
	}

	@Override
	public ResponseDataDto createPromotion(PromotionDto promotionDto) {
		PromotionEntity promotion = new PromotionEntity();
		promotion.setName(promotionDto.getName());
		promotion.setDiscountType(PromotionEntity.DiscountType.valueOf(promotionDto.getDiscountType()));
		promotion.setDiscountValue(promotionDto.getDiscountValue());
		promotion.setStartAt(promotionDto.getStartAt());
		promotion.setEndAt(promotionDto.getEndAt());
		promotion.setDescription(promotionDto.getDescription());

		promotionRepository.save(promotion);

		PromotionInfoDto dto = new PromotionInfoDto(promotion.getPromotionId(), promotion.getName(),
				promotion.getDiscountType().name(), promotion.getDiscountValue(), promotion.getDescription(),
				promotion.getStartAt(), promotion.getEndAt());

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Created promotion", dto);
	}

	@Transactional
	@Override
	public ResponseDataDto updatePromotion(Integer promotionId, PromotionDto promotionDto) {
		Optional<PromotionEntity> optionalPromotion = promotionRepository.findById(promotionId);
		if (optionalPromotion.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Promotion not found");
		}
		PromotionEntity promotion = optionalPromotion.get();
		promotion.setName(promotionDto.getName());
		promotion.setDiscountType(PromotionEntity.DiscountType.valueOf(promotionDto.getDiscountType()));
		promotion.setDiscountValue(promotionDto.getDiscountValue());
		promotion.setStartAt(promotionDto.getStartAt());
		promotion.setEndAt(promotionDto.getEndAt());
		promotion.setDescription(promotionDto.getDescription());

		promotionRepository.save(promotion);

		PromotionInfoDto dto = new PromotionInfoDto(promotion.getPromotionId(), promotion.getName(),
				promotion.getDiscountType().name(), promotion.getDiscountValue(), promotion.getDescription(),
				promotion.getStartAt(), promotion.getEndAt());

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Updated promotion", dto);
	}

	@Override
	public ResponseDataDto deletePromotion(Integer promotionId) {
		if (!promotionRepository.existsById(promotionId)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Promotion not found", promotionId);
		}
		promotionRepository.deleteById(promotionId);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Deleted promotion");
	}

	@Override
	public ResponseDataDto getPromotionById(Integer promotionId) {
		Optional<PromotionEntity> optional = promotionRepository.findById(promotionId);
		if (optional.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Promotion not found", null);
		}
		PromotionEntity entity = optional.get(); // Lấy entity ra
		PromotionInfoDto dto = new PromotionInfoDto();
		dto.setPromotionId(entity.getPromotionId());
		dto.setName(entity.getName());
		dto.setDiscountType(entity.getDiscountType().name());
		dto.setDiscountValue(entity.getDiscountValue());
		dto.setStartAt(entity.getStartAt());
		dto.setEndAt(entity.getEndAt());
		dto.setDescription(entity.getDescription());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "OK", dto);
	}

}
