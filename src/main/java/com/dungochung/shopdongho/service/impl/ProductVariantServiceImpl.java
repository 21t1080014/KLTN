package com.dungochung.shopdongho.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.dto.VariantDto;
import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.ProductImageEntity;
import com.dungochung.shopdongho.entity.ProductVariantEntity;
import com.dungochung.shopdongho.enums.MovementType;
import com.dungochung.shopdongho.enums.ProductStatus;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.repository.ProductImageRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.repository.ProductVariantRepository;
import com.dungochung.shopdongho.service.ProductVariantService;
import com.dungochung.shopdongho.service.StockService;

@Service
@Transactional
public class ProductVariantServiceImpl implements ProductVariantService {

	@Autowired
	private ProductVariantRepository variantRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private InventoryRepository inventoryRepository;
	@Autowired
	private ProductImageRepository imageRepository;
	@Autowired
	private StockService stockService;

	@Override
	public ProductVariantEntity ensureDefaultVariant(ProductEntity product) {
		return variantRepository.findByProduct_ProductIdAndIsDefaultTrue(product.getProductId()).orElseGet(() -> {
			ProductVariantEntity v = new ProductVariantEntity();
			v.setProduct(product);
			v.setSku(product.getSku());
			v.setVariantName("Mặc định");
			v.setPrice(product.getPrice());
			v.setDefault(true);
			v.setStatus(ProductStatus.ACTIVE);
			return variantRepository.save(v);
		});
	}

	@Override
	public void syncDefaultFromProduct(ProductEntity product) {
		ProductVariantEntity v = ensureDefaultVariant(product);
		v.setSku(product.getSku());
		v.setPrice(product.getPrice());
		variantRepository.save(v);
	}

	private VariantDto toDto(ProductVariantEntity v) {
		VariantDto d = new VariantDto();
		d.setVariantId(v.getVariantId());
		d.setProductId(v.getProduct().getProductId());
		d.setSku(v.getSku());
		d.setVariantName(v.getVariantName());
		d.setColor(v.getColor());
		d.setStrapOption(v.getStrapOption());
		d.setCaseSize(v.getCaseSize());
		d.setPrice(v.getPrice());
		d.setDefault(v.isDefault());
		d.setStatus(v.getStatus().name());
		d.setSortOrder(v.getSortOrder());
		inventoryRepository.findByVariant_VariantId(v.getVariantId()).ifPresent(i -> {
			d.setQuantity(i.getQuantity());
			d.setReservedQuantity(i.getReservedQuantity());
			d.setAvailableQuantity(i.getAvailableQuantity());
			d.setLowStockThreshold(i.getLowStockThreshold());
		});
		List<Integer> imageIds = v.getProduct().getImages() == null ? new ArrayList<>()
				: v.getProduct().getImages().stream()
						.filter(img -> img.getVariant() != null && img.getVariant().getVariantId().equals(v.getVariantId()))
						.map(ProductImageEntity::getImageId).collect(Collectors.toList());
		d.setImageIds(imageIds);
		return d;
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto listVariants(String productId) {
		if (!productRepository.existsById(productId)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Sản phẩm không tồn tại", null);
		}
		List<VariantDto> list = variantRepository.findByProduct_ProductIdOrderBySortOrderAscVariantIdAsc(productId)
				.stream().map(this::toDto).collect(Collectors.toList());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", list);
	}

	private ProductStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			return ProductStatus.ACTIVE;
		}
		return ProductStatus.valueOf(status.trim().toUpperCase());
	}

	@Override
	public ResponseDataDto createVariant(String productId, String sku, String variantName, String color,
			String strapOption, String caseSize, BigDecimal price, String status, Integer initialQuantity,
			Integer lowStockThreshold) {
		ProductEntity product = productRepository.findById(productId).orElse(null);
		if (product == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Sản phẩm không tồn tại", null);
		}
		if (sku == null || sku.isBlank() || variantName == null || variantName.isBlank()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "SKU và tên biến thể không được để trống", null);
		}
		if (price == null || price.signum() <= 0) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Giá phải lớn hơn 0", null);
		}
		if (initialQuantity != null && initialQuantity < 0) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Số lượng ban đầu không được âm", null);
		}
		sku = sku.trim();
		if (variantRepository.existsBySku(sku)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "SKU đã tồn tại", null);
		}
		ProductStatus st;
		try {
			st = parseStatus(status);
		} catch (IllegalArgumentException e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Trạng thái không hợp lệ", null);
		}
		// Đảm bảo luôn có biến thể mặc định trước khi thêm biến thể phụ
		ensureDefaultVariant(product);

		ProductVariantEntity v = new ProductVariantEntity();
		v.setProduct(product);
		v.setSku(sku);
		v.setVariantName(variantName.trim());
		v.setColor(color);
		v.setStrapOption(strapOption);
		v.setCaseSize(caseSize);
		v.setPrice(price);
		v.setDefault(false);
		v.setStatus(st);
		v.setSortOrder((int) variantRepository.countByProduct_ProductId(productId));
		v = variantRepository.save(v);

		InventoryEntity inv = stockService.ensureInventory(v);
		if (lowStockThreshold != null && lowStockThreshold >= 0) {
			inv.setLowStockThreshold(lowStockThreshold);
			inventoryRepository.save(inv);
		}
		if (initialQuantity != null && initialQuantity > 0) {
			stockService.adjust(inv, initialQuantity, MovementType.INITIAL, "VARIANT-" + v.getVariantId(),
					"Số lượng ban đầu khi tạo biến thể");
		}
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Tạo biến thể thành công", toDto(v));
	}

	private ProductVariantEntity findOwned(String productId, Long variantId) {
		return variantRepository.findById(variantId).filter(v -> v.getProduct().getProductId().equals(productId))
				.orElse(null);
	}

	@Override
	public ResponseDataDto updateVariant(String productId, Long variantId, String variantName, String color,
			String strapOption, String caseSize, BigDecimal price, String status, Integer lowStockThreshold) {
		ProductVariantEntity v = findOwned(productId, variantId);
		if (v == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy biến thể", null);
		}
		if (variantName == null || variantName.isBlank()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Tên biến thể không được để trống", null);
		}
		if (price == null || price.signum() <= 0) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Giá phải lớn hơn 0", null);
		}
		ProductStatus st;
		try {
			st = parseStatus(status);
		} catch (IllegalArgumentException e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Trạng thái không hợp lệ", null);
		}
		if (v.isDefault() && st != ProductStatus.ACTIVE) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Biến thể mặc định phải ACTIVE. Muốn ẩn cả sản phẩm hãy đổi trạng thái sản phẩm.", null);
		}
		v.setVariantName(variantName.trim());
		v.setColor(color);
		v.setStrapOption(strapOption);
		v.setCaseSize(caseSize);
		v.setPrice(price);
		v.setStatus(st);
		variantRepository.save(v);

		if (v.isDefault()) {
			// Giá của sản phẩm luôn là giá của biến thể mặc định (storefront/đơn hàng đang đọc products.price)
			v.getProduct().setPrice(price);
			productRepository.save(v.getProduct());
		}
		if (lowStockThreshold != null && lowStockThreshold >= 0) {
			InventoryEntity inv = stockService.ensureInventory(v);
			inv.setLowStockThreshold(lowStockThreshold);
			inventoryRepository.save(inv);
		}
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Cập nhật biến thể thành công", toDto(v));
	}

	@Override
	public ResponseDataDto setDefault(String productId, Long variantId) {
		ProductVariantEntity target = findOwned(productId, variantId);
		if (target == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy biến thể", null);
		}
		if (target.getStatus() != ProductStatus.ACTIVE) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Chỉ biến thể ACTIVE mới được làm mặc định", null);
		}
		for (ProductVariantEntity v : variantRepository.findByProduct_ProductIdOrderBySortOrderAscVariantIdAsc(productId)) {
			boolean shouldBeDefault = v.getVariantId().equals(variantId);
			if (v.isDefault() != shouldBeDefault) {
				v.setDefault(shouldBeDefault);
				variantRepository.save(v);
			}
		}
		target.getProduct().setPrice(target.getPrice());
		productRepository.save(target.getProduct());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Đã đặt làm biến thể mặc định", toDto(target));
	}

	@Override
	public ResponseDataDto deleteVariant(String productId, Long variantId) {
		ProductVariantEntity v = findOwned(productId, variantId);
		if (v == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy biến thể", null);
		}
		if (v.isDefault()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Không thể xóa biến thể mặc định. Hãy đặt biến thể khác làm mặc định trước.", null);
		}
		InventoryEntity inv = inventoryRepository.findByVariant_VariantId(variantId).orElse(null);
		if (inv != null) {
			if (inv.getQuantity() > 0 || inv.getReservedQuantity() > 0) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL,
						"Không thể xóa: biến thể còn tồn kho/đang giữ hàng. Hãy đưa về 0 hoặc chuyển sang INACTIVE.", null);
			}
			inventoryRepository.delete(inv);
		}
		// gỡ liên kết ảnh riêng của biến thể (ảnh vẫn thuộc sản phẩm)
		Set<ProductImageEntity> imgs = imageRepository.findAll().stream()
				.filter(i -> i.getVariant() != null && i.getVariant().getVariantId().equals(variantId))
				.collect(Collectors.toSet());
		imgs.forEach(i -> {
			i.setVariant(null);
			imageRepository.save(i);
		});
		variantRepository.delete(v);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Đã xóa biến thể", null);
	}

	@Override
	public ResponseDataDto setVariantImages(String productId, Long variantId, List<Integer> imageIds) {
		ProductVariantEntity v = findOwned(productId, variantId);
		if (v == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy biến thể", null);
		}
		ProductEntity product = v.getProduct();
		Set<Integer> wanted = imageIds == null ? Set.of() : Set.copyOf(imageIds);
		if (product.getImages() != null) {
			Set<Integer> owned = product.getImages().stream().map(ProductImageEntity::getImageId)
					.collect(Collectors.toSet());
			if (!owned.containsAll(wanted)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Có ảnh không thuộc sản phẩm này", null);
			}
			for (ProductImageEntity img : product.getImages()) {
				boolean mine = img.getVariant() != null && img.getVariant().getVariantId().equals(variantId);
				if (wanted.contains(img.getImageId())) {
					img.setVariant(v);
					imageRepository.save(img);
				} else if (mine) {
					img.setVariant(null);
					imageRepository.save(img);
				}
			}
		}
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Đã cập nhật ảnh biến thể", toDto(v));
	}
}
