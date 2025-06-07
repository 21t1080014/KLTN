package com.dungochung.shopdongho.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.GenderDto;
import com.dungochung.shopdongho.dto.ImageDTO;
import com.dungochung.shopdongho.dto.ProductInfoAllDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.dto.SegmentDto;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.PromotionEntity;
import com.dungochung.shopdongho.entity.PromotionProductEntity;
import com.dungochung.shopdongho.enums.Gender;
import com.dungochung.shopdongho.enums.Segment;
import com.dungochung.shopdongho.repository.OrderItemRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.repository.PromotionProductRepository;
import com.dungochung.shopdongho.service.ProductCustomerService;

import jakarta.transaction.Transactional;

@Service
public class ProductCustomerServiceImpl implements ProductCustomerService {
	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private PromotionProductRepository promotionProductRepository;
	@Autowired
	private OrderItemRepository orderItemRepository;

	@Override
	public ResponseDataDto getActivePromotionalProducts() {
		LocalDateTime now = LocalDateTime.now();

		List<ProductEntity> products = productRepository.findPromotionalProducts(now);

		List<ProductInfoAllDto> dtos = products.stream().map(this::mapToProductInfoAllDto) // dùng lại hàm đã có
				.collect(Collectors.toList());

		Map<String, Object> data = new HashMap<>();
		data.put("products", dtos);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Danh sách sản phẩm khuyến mãi", data);
	}

	@Override
	public ResponseDataDto getNewestProducts() {
		List<ProductEntity> newestProducts = productRepository.findTop10ByOrderByCreatedAtDesc();

		List<ProductInfoAllDto> dtos = newestProducts.stream().map(this::mapToProductInfoAllDto)
				.collect(Collectors.toList());

		Map<String, Object> data = new HashMap<>();
		data.put("products", dtos);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto getFeaturedProducts(int brandId) {
		List<Object[]> topSelling = orderItemRepository.findTopSellingProductIdsByBrandId(brandId);
		List<ProductEntity> featured = new ArrayList<>();

		if (!topSelling.isEmpty()) {
			for (Object[] row : topSelling) {
				String pid = (String) row[0];
				productRepository.findWithPromotionsAndImagesById(pid).ifPresent(featured::add);
				if (featured.size() >= 10)
					break;
			}
		} else {
			for (Segment seg : List.of(Segment.LUXURY, Segment.SPORT, Segment.CASUAL)) {
				List<ProductEntity> segList = productRepository.findWithPromotionsAndImagesByBrandAndSegment(brandId,
						seg);
				for (ProductEntity p : segList) {
					featured.add(p);
					if (featured.size() >= 10)
						break;
				}
				if (featured.size() >= 10)
					break;
			}
		}

		List<ProductInfoAllDto> dtos = featured.stream().map(this::mapToProductInfoAllDto).collect(Collectors.toList());

		Map<String, Object> data = new HashMap<>();
		data.put("products", dtos);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto getProductById(String productId) {
		return productRepository.findWithPromotionsAndImagesById(productId)
				.map(p -> new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", mapToProductInfoAllDto(p)))
				.orElseGet(
						() -> new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy sản phẩm id=" + productId));
	}

	private ProductInfoAllDto mapToProductInfoAllDto(ProductEntity product) {
		ProductInfoAllDto dto = new ProductInfoAllDto();

		dto.setProductId(product.getProductId());
		dto.setSku(product.getSku());
		dto.setName(product.getName());
		dto.setOrigin(product.getOrigin());
		dto.setCondition(product.getCondition());
		dto.setWarrantyPeriod(product.getWarrantyPeriod());
		dto.setPrice(product.getPrice());
		dto.setGender(product.getGender());
		dto.setSegment(product.getSegment());
		dto.setDescription(product.getDescription());

		// Các trường brand, type, vật liệu có thể null nên kiểm tra trước
		dto.setBrandName(product.getBrand() != null ? product.getBrand().getName() : null);
		dto.setTypeName(product.getType() != null ? product.getType().getName() : null);
		dto.setCaseMaterialName(product.getCaseMaterial() != null ? product.getCaseMaterial().getName() : null);
		dto.setStrapMaterialName(product.getStrapMaterial() != null ? product.getStrapMaterial().getName() : null);
		dto.setGlassMaterialName(product.getGlassMaterial() != null ? product.getGlassMaterial().getName() : null);

		dto.setCreatedAt(product.getCreatedAt());
		dto.setUpdatedAt(product.getUpdatedAt());
		if (product.getInventory() != null) {
			dto.setQuantity(product.getInventory().getQuantity());
		} else {
			dto.setQuantity(0);
		}
		// Chuyển danh sách ảnh
		if (product.getImages() != null) {
			List<ImageDTO> imageDTOs = product.getImages().stream().map(img -> {
				ImageDTO imgs = new ImageDTO();
				imgs.setImageId(img.getImageId());
				imgs.setUrl(img.getUrl());
				imgs.setAltText(img.getAltText());
				imgs.setSortOrder(img.getSortOrder());
				return imgs;
			}).toList();
			dto.setImages(imageDTOs);
		} else {
			dto.setImages(List.of());
		}

		// Mặc định khuyến mãi
		dto.setDiscountType(null);
		dto.setDiscountValue(null);
		dto.setDiscountPrice(product.getPrice());

		// Kiểm tra khuyến mãi hợp lệ (đang active và trong khoảng thời gian)
		if (product.getPromotionProducts() != null && !product.getPromotionProducts().isEmpty()) {
			LocalDateTime now = LocalDateTime.now();
			for (PromotionProductEntity ppe : product.getPromotionProducts()) {
				PromotionEntity promo = ppe.getPromotion();
				if (promo != null && Boolean.TRUE.equals(promo.getIsActive()) && promo.getStartAt() != null
						&& promo.getStartAt().isBefore(now) && promo.getEndAt() != null
						&& promo.getEndAt().isAfter(now)) {

					// Đổi tên discountType thành "PERCENT" hoặc "FIXED" theo yêu cầu
					if (promo.getDiscountType() == PromotionEntity.DiscountType.percent) {
						dto.setDiscountType("PERCENT");
					} else if (promo.getDiscountType() == PromotionEntity.DiscountType.amount) {
						dto.setDiscountType("FIXED");
					} else {
						dto.setDiscountType(null);
					}

					dto.setDiscountValue(promo.getDiscountValue());

					BigDecimal discountPrice = product.getPrice();
					if (promo.getDiscountType() == PromotionEntity.DiscountType.percent) {
						BigDecimal discount = discountPrice.multiply(promo.getDiscountValue())
								.divide(BigDecimal.valueOf(100));
						discountPrice = discountPrice.subtract(discount);
					} else if (promo.getDiscountType() == PromotionEntity.DiscountType.amount) {
						discountPrice = discountPrice.subtract(promo.getDiscountValue());
					}

					dto.setDiscountPrice(discountPrice.max(BigDecimal.ZERO));
					break; // lấy 1 khuyến mãi hợp lệ đầu tiên
				}
			}
		}

		return dto;
	}

	@Override
	public ResponseDataDto searchProducts(String keyword, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
		// Tìm kiếm sản phẩm theo tên hoặc SKU
		Page<ProductEntity> products = productRepository
				.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(keyword, keyword, pageable);

		// Chuyển sang DTO
		List<ProductInfoAllDto> resultDtos = products.stream().map(this::mapToProductInfoAllDto)
				.collect(Collectors.toList());
		long totalCount = productRepository.countByNameOrSku(keyword);
		Map<String, Object> data = new HashMap<>();
		data.put("products", resultDtos);
		data.put("total", totalCount);
		data.put("keywordss", keyword);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, products.getTotalPages(), 3));
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	@Transactional
	public ResponseDataDto getAllProductCustomer(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

		// 2. Lấy Page<ProductEntity> từ DB
		Page<ProductEntity> productPage = productRepository.findAll(pageable);

		// 3. Map từng entity sang DTO
		List<ProductInfoAllDto> dtos = productPage.stream().map(this::mapToProductInfoAllDto).toList();
		Map<String, Object> data = new HashMap<>();
		data.put("products", dtos);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, productPage.getTotalPages(), 3));
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	@Transactional
	public ResponseDataDto searchProducts(List<String> genders, List<String> segments, List<String> brandNames,
			Double priceMin, Double priceMax, String sortBy, String sortDir, int page, int size) {

		List<String> allowed = List.of("price", "name", "createdAt", "soldQuantity", "stockQuantity");
		if (sortBy == null || !allowed.contains(sortBy))
			sortBy = "createdAt";
		if (sortDir == null || (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")))
			sortDir = "desc";

		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
		Pageable pageable = PageRequest.of(page, size, sort);

		Page<ProductEntity> pg = productRepository.advancedSearch(genders, segments, brandNames, priceMin, priceMax,
				pageable);

		List<ProductInfoAllDto> dtos = pg.stream().map(this::mapToProductInfoAllDto).toList();

		Map<String, Object> data = new HashMap<>();
		data.put("products", dtos);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, pg.getTotalPages(), size));
		return new ResponseDataDto(1, "Success", data);
	}

	@Override
	public ResponseDataDto suggestProducts(String keyword) {
		try {
			Pageable pageable = PageRequest.of(0, 7);

			// Bước 1: Truy vấn danh sách ID có phân trang
			Page<Long> page = productRepository.findIdsByNameOrSku(keyword, keyword, pageable);
			List<Long> productIds = page.getContent();

			// Nếu không có sản phẩm thì trả về luôn
			if (productIds.isEmpty()) {
				Map<String, Object> emptyData = new HashMap<>();
				emptyData.put("products", Collections.emptyList());
				emptyData.put("total", 0);
				return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Không tìm thấy sản phẩm", emptyData);
			}

			// Bước 2: Truy vấn sản phẩm đầy đủ theo ID
			List<ProductEntity> products = productRepository.findByProductIdIn(productIds);

			// Ánh xạ sang DTO
			List<ProductInfoAllDto> dtos = products.stream().map(this::mapToProductInfoAllDto)
					.collect(Collectors.toList());

			// Đếm tổng số sản phẩm khớp từ khóa
			long totalCount = productRepository.countByNameOrSku(keyword);

			Map<String, Object> data = new HashMap<>();
			data.put("products", dtos);
			data.put("total", totalCount);

			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Lỗi server: " + e.getMessage(), null);
		}
	}

	@Override
	public ResponseDataDto getGenders() {
		// Lấy tất cả enum Gender
		List<GenderDto> genders = Arrays.stream(Gender.values()).map(g -> new GenderDto(g.name(), g.getDbValue()))
				.collect(Collectors.toList());

		// Đóng gói vào map
		Map<String, Object> data = new HashMap<>();
		data.put("genders", genders);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Lấy danh sách gender thành công", data);
	}

	@Override
	public ResponseDataDto getSegment() {
		List<SegmentDto> segment = Arrays.stream(Segment.values()).map(g -> new SegmentDto(g.name(), g.getDbValue()))
				.collect(Collectors.toList());

		// Đóng gói vào map
		Map<String, Object> data = new HashMap<>();
		data.put("segments", segment);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Lấy danh sách Phân khúc thành công", data);
	}

}
