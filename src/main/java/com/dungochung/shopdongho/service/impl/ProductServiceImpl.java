package com.dungochung.shopdongho.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dungochung.shopdongho.common.FileStorageService;
import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ImageDTO;
import com.dungochung.shopdongho.dto.PaginationDto;
import com.dungochung.shopdongho.dto.ProductDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.BrandEntity;
import com.dungochung.shopdongho.entity.CategoryEntity;
import com.dungochung.shopdongho.repository.CategoryRepository;
import com.dungochung.shopdongho.entity.CaseMaterialEntity;
import com.dungochung.shopdongho.entity.GlassMaterialEntity;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.ProductImageEntity;
import com.dungochung.shopdongho.entity.StrapMaterialEntity;
import com.dungochung.shopdongho.entity.WatchTypeEntity;
import com.dungochung.shopdongho.enums.Gender;
import com.dungochung.shopdongho.enums.ProductCondition;
import com.dungochung.shopdongho.enums.ProductStatus;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.repository.OrderItemRepository;
import com.dungochung.shopdongho.repository.ProductVariantRepository;
import com.dungochung.shopdongho.service.ProductVariantService;
import com.dungochung.shopdongho.service.StockService;
import org.springframework.transaction.annotation.Transactional;
import com.dungochung.shopdongho.enums.Segment;
import com.dungochung.shopdongho.repository.BrandReponsitory;
import com.dungochung.shopdongho.repository.CaseMaterialReponsitory;
import com.dungochung.shopdongho.repository.GlassMaterialReponsitory;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.repository.ProductImageRepository;
import com.dungochung.shopdongho.repository.StraMaterialReponsitory;
import com.dungochung.shopdongho.repository.WatchTypeReponsitory;
import com.dungochung.shopdongho.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {
	@org.springframework.beans.factory.annotation.Autowired
	private com.dungochung.shopdongho.service.AuditService auditService;

	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private BrandReponsitory brandRepository;
	@Autowired
	private WatchTypeReponsitory watchTypeRepository;
	@Autowired
	private CaseMaterialReponsitory caseMaterialRepository;
	@Autowired
	private StraMaterialReponsitory strapMaterialRepository;
	@Autowired
	private GlassMaterialReponsitory glassMaterialRepository;
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private ProductVariantRepository variantRepository;
	@Autowired
	private ProductVariantService variantService;
	@Autowired
	private StockService stockService;
	@Autowired
	private InventoryRepository inventoryRepository;
	@Autowired
	private OrderItemRepository orderItemRepository;
	@Autowired
	private ProductImageRepository imageRepository;
	@Autowired
	private FileStorageService fileStorageService;

	@Override
	public ResponseDataDto getAllProduct(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
		Page<ProductEntity> productPage = productRepository.findAll(pageable);
		List<ProductDto> productDtos = productPage.stream()
				.map(product -> new ProductDto(product.getProductId(), product.getSku(), product.getName(),
						product.getOrigin(), product.getCondition(), product.getWarrantyPeriod(), product.getPrice(),
						product.getGender(), product.getSegment(), product.getDescription(),
						product.getBrand() != null ? product.getBrand().getName() : null,
						product.getType() != null ? product.getType().getName() : null,
						product.getCaseMaterial() != null ? product.getCaseMaterial().getName() : null,
						product.getStrapMaterial() != null ? product.getStrapMaterial().getName() : null,
						product.getGlassMaterial() != null ? product.getGlassMaterial().getName() : null,
						product.getCreatedAt(), product.getUpdatedAt(), product.getImages().stream().map(img -> {
							ImageDTO dto = new ImageDTO();
							dto.setImageId(img.getImageId());
							dto.setUrl(img.getUrl());
							dto.setAltText(img.getAltText());
							dto.setSortOrder(img.getSortOrder());
							return dto;
						}).collect(Collectors.toList())))
				.collect(Collectors.toList());

		applyCategories(productDtos, productPage.getContent());

		Map<String, Object> response = new HashMap<>();
		response.put("products", productDtos);
		PaginationDto paginationInfo = new PaginationDto(page, productPage.getTotalPages(),
				PaginationCommon.getPaginationInfo(page, productPage.getTotalPages(), 3).getStartPage(),
				PaginationCommon.getPaginationInfo(page, productPage.getTotalPages(), 3).getEndPage());

		response.put("pagination", paginationInfo);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", response);
	}

	@Override
	@Transactional
	public ResponseDataDto creatProduct(String sku, String name, BrandEntity brand, WatchTypeEntity type,
			CaseMaterialEntity caseMaterial, StrapMaterialEntity strapMaterial, GlassMaterialEntity glassMaterial,
			CategoryEntity category, ProductStatus status, String origin, ProductCondition condition,
			String warrantyPeriod, BigDecimal price, Gender gender, Segment segment, String description,
			List<MultipartFile> images) {
		if (brand == null || type == null || caseMaterial == null || strapMaterial == null || glassMaterial == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "One or more category information is invalid.", null);
		}

		sku = sku == null ? null : sku.trim();
		ProductEntity product = new ProductEntity();
		product.setSku(sku);
		product.setName(name);
		product.setBrand(brand);
		product.setType(type);
		product.setCaseMaterial(caseMaterial);
		product.setStrapMaterial(strapMaterial);
		product.setGlassMaterial(glassMaterial);
		product.setCategory(category);
		product.setStatus(status != null ? status : ProductStatus.ACTIVE);
		if (price == null || price.signum() <= 0) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Giá phải lớn hơn 0", null);
		}
		if (skuUsedByOtherProduct(sku, null)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "SKU đã tồn tại", null);
		}
		product.setOrigin(origin);
		product.setCondition(condition);
		product.setWarrantyPeriod(warrantyPeriod);
		product.setPrice(price);
		product.setGender(gender);
		product.setSegment(segment);
		product.setDescription(description);
		// Tạm thời chưa set images, vì phải save product trước mới có ID
		ProductEntity savedProduct = productRepository.save(product);
		// Mọi sản phẩm luôn có 1 biến thể mặc định + dòng tồn kho (0) để module kho/đơn hàng hoạt động
		stockService.ensureInventory(variantService.ensureDefaultVariant(savedProduct));

		// Xử lý ảnh nếu có
		if (images != null && !images.isEmpty()) {
			AtomicInteger sortIndex = new AtomicInteger(0); // dùng để đếm ảnh
			List<ProductImageEntity> productImages = images.stream().filter(image -> image != null && !image.isEmpty()) // CHẶN
																														// FILE
																														// RỖNG
																														// Ở
																														// ĐÂY
					.map(image -> {
						try {
							String fileName = fileStorageService.saveFile(image, "products");
							System.out.println("img:" + fileName);
							if (fileName == null || fileName.isBlank()) {
								return null; // bỏ qua ảnh lỗi
							}
							ProductImageEntity imgEntity = new ProductImageEntity();
							imgEntity.setUrl(fileName);
							imgEntity.setProduct(savedProduct);
							imgEntity.setSortOrder(sortIndex.getAndIncrement());
							return imgEntity;
						} catch (IOException e) {
							e.printStackTrace();
							return null;
						}
					}).filter(Objects::nonNull).collect(Collectors.toList());
			imageRepository.saveAll(productImages);
			savedProduct.setImages(productImages);
			productRepository.save(savedProduct); // cập nhật lại product với list images
		}

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Product created successfully", null);
	}

	@Override
	@Transactional
	public ResponseDataDto updateProduct(String productId, String sku, String name, BrandEntity brand,
			WatchTypeEntity type, CaseMaterialEntity caseMaterial, StrapMaterialEntity strapMaterial,
			GlassMaterialEntity glassMaterial, CategoryEntity category, ProductStatus status, String origin,
			ProductCondition condition, String warrantyPeriod, BigDecimal price, Gender gender, Segment segment,
			String description, List<MultipartFile> images, List<String> oldImageNames) {

		ProductEntity existingProduct = productRepository.findById(productId).orElse(null);
		if (existingProduct == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Product not found", null);
		}

		// Cập nhật thông tin cơ bản
		sku = sku == null ? null : sku.trim();
		existingProduct.setSku(sku);
		existingProduct.setName(name);
		existingProduct.setBrand(brand);
		existingProduct.setType(type);
		existingProduct.setCaseMaterial(caseMaterial);
		existingProduct.setStrapMaterial(strapMaterial);
		existingProduct.setGlassMaterial(glassMaterial);
		existingProduct.setCategory(category);
		if (status != null) {
			existingProduct.setStatus(status);
		}
		if (price == null || price.signum() <= 0) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Giá phải lớn hơn 0", null);
		}
		if (skuUsedByOtherProduct(sku, productId)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "SKU đã tồn tại", null);
		}
		existingProduct.setOrigin(origin);
		existingProduct.setCondition(condition);
		existingProduct.setWarrantyPeriod(warrantyPeriod);
		existingProduct.setPrice(price);
		existingProduct.setGender(gender);
		existingProduct.setSegment(segment);
		existingProduct.setDescription(description);

		// Xử lý ảnh cũ
		List<ProductImageEntity> retainedImages = new ArrayList<>();
		if (existingProduct.getImages() != null) {
			for (ProductImageEntity oldImg : existingProduct.getImages()) {
				if (oldImageNames != null && oldImageNames.contains(oldImg.getUrl())) {
					retainedImages.add(oldImg); // Giữ lại
				} else {
					fileStorageService.deleteFile("products", oldImg.getUrl()); // Xoá file
				}
			}
			existingProduct.getImages().clear(); // Clear toàn bộ list
			for (int i = 0; i < retainedImages.size(); i++) {
				retainedImages.get(i).setSortOrder(i);
			} // thiết lập lại sort order
			existingProduct.getImages().addAll(retainedImages); // Thêm lại ảnh cần giữ
		}

		// Xử lý ảnh mới (nếu có)
		if (images != null && !images.isEmpty()) {
			int order = retainedImages.size(); // tiếp tục từ số thứ tự cuối cùng
			for (MultipartFile image : images) {
				try {
					String fileName = fileStorageService.saveFile(image, "products");
					if (fileName != null && !fileName.isEmpty()) {
						ProductImageEntity imgEntity = new ProductImageEntity();
						imgEntity.setUrl(fileName);
						imgEntity.setProduct(existingProduct);
						imgEntity.setSortOrder(order++);
						existingProduct.getImages().add(imgEntity);
					}
				} catch (IOException e) {
					e.printStackTrace();
					return new ResponseDataDto(Constant.RESULT_CD_FAIL, "File upload failed", null);
				}
			}
		}

		ProductEntity savedExisting = productRepository.save(existingProduct);
		// giá/sku sản phẩm luôn là giá/sku của biến thể mặc định
		variantService.syncDefaultFromProduct(savedExisting);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Product updated successfully", null);
	}

	@Override
	@Transactional
	public ResponseDataDto deleteProduct(String productId) {
		ProductEntity product = productRepository.findById(productId).orElse(null);
		if (product == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Product not found", null);
		}

		if (orderItemRepository.existsByProduct_ProductId(productId)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Không thể xóa: sản phẩm đã có trong đơn hàng. Hãy chuyển trạng thái sang DISCONTINUED thay vì xóa.", null);
		}

		// Xoá ảnh vật lý khỏi ổ đĩa
		if (product.getImages() != null) {
			for (ProductImageEntity img : product.getImages()) {
				fileStorageService.deleteFile("products", img.getUrl());
			}
		}

		// Xoá tồn kho + biến thể trước (lịch sử kho inventory_movements được giữ lại để đối soát)
		inventoryRepository.deleteAll(inventoryRepository.findAll().stream()
				.filter(i -> i.getProduct() != null && productId.equals(i.getProduct().getProductId())).toList());
		if (product.getImages() != null) {
			product.getImages().forEach(img -> img.setVariant(null));
		}
		productRepository.saveAndFlush(product);
		variantRepository.deleteAll(variantRepository.findByProduct_ProductIdOrderBySortOrderAscVariantIdAsc(productId));
		productRepository.delete(product);
		auditService.log("PRODUCT_DELETE", "PRODUCT", productId, "sku=" + product.getSku() + ", name=" + product.getName());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Product deleted successfully", null);
	}

	/** SKU đã bị biến thể của sản phẩm khác (hoặc biến thể phụ của chính nó) chiếm dụng? */
	private boolean skuUsedByOtherProduct(String sku, String selfProductId) {
		if (sku == null) {
			return false;
		}
		return variantRepository.findBySku(sku.trim())
				.filter(v -> selfProductId == null || !v.getProduct().getProductId().equals(selfProductId) || !v.isDefault())
				.isPresent();
	}

	// productDtos được map theo đúng thứ tự của products nên gán danh mục theo index
	private void applyCategories(List<ProductDto> dtos, List<ProductEntity> products) {
		for (int i = 0; i < dtos.size() && i < products.size(); i++) {
			CategoryEntity c = products.get(i).getCategory();
			if (c != null) {
				dtos.get(i).setCategoryId(c.getCategoryId());
				dtos.get(i).setCategoryName(c.getName());
			}
			dtos.get(i).setStatus(products.get(i).getStatus().name());
			dtos.get(i).setAvailableQuantity(products.get(i).getAvailableQuantity());
			dtos.get(i).setVariantCount(variantRepository.countByProduct_ProductId(products.get(i).getProductId()));
		}
	}

	private Double getMaxPrice() {
		BigDecimal maxPrice = productRepository.findMaxPrice();
		return maxPrice != null ? maxPrice.doubleValue() : 0.0;
	}

	@Override
	public ResponseDataDto searchProduct(String keyword, Double minPrice, Double maxPrice, int page, int size) {
		// Xử lý dữ liệu đầu vào để tránh lỗi NullPointer
		if (keyword == null) {
			keyword = "";
		} else {
			keyword = keyword.trim().toLowerCase();
		}

		if (minPrice == null) {
			minPrice = 0.0;
		}
		if (maxPrice == null) {
			maxPrice = getMaxPrice(); // Giả sử bạn đã có phương thức này để lấy giá cao nhất trong DB
		}

		// Phân trang
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

		// Truy vấn danh sách sản phẩm theo điều kiện
		Page<ProductEntity> productPage = productRepository.findByNameContainingIgnoreCaseAndPriceBetween(keyword,
				minPrice, maxPrice, pageable);

		// Convert sang DTO
		List<ProductDto> productDtos = productPage.stream()
				.map(product -> new ProductDto(product.getProductId(), product.getSku(), product.getName(),
						product.getOrigin(), product.getCondition(), product.getWarrantyPeriod(), product.getPrice(),
						product.getGender(), product.getSegment(), product.getDescription(),
						product.getBrand() != null ? product.getBrand().getName() : null,
						product.getType() != null ? product.getType().getName() : null,
						product.getCaseMaterial() != null ? product.getCaseMaterial().getName() : null,
						product.getStrapMaterial() != null ? product.getStrapMaterial().getName() : null,
						product.getGlassMaterial() != null ? product.getGlassMaterial().getName() : null,
						product.getCreatedAt(), product.getUpdatedAt(), product.getImages().stream().map(img -> {
							ImageDTO dto = new ImageDTO();
							dto.setImageId(img.getImageId());
							dto.setUrl(img.getUrl());
							return dto;
						}).collect(Collectors.toList())))
				.collect(Collectors.toList());

		applyCategories(productDtos, productPage.getContent());

		// Chuẩn bị response
		Map<String, Object> response = new HashMap<>();
		response.put("products", productDtos);
		PaginationDto paginationInfo = new PaginationDto(page, productPage.getTotalPages(),
				PaginationCommon.getPaginationInfo(page, productPage.getTotalPages(), 3).getStartPage(),
				PaginationCommon.getPaginationInfo(page, productPage.getTotalPages(), 3).getEndPage());

		response.put("pagination", paginationInfo);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Search result", response);
	}

	@Override
	public ResponseDataDto getFormOptions() {
		Map<String, Object> response = new HashMap<>();
		response.put("brands", brandRepository.findAll());
		response.put("types", watchTypeRepository.findAll());
		response.put("caseMaterials", caseMaterialRepository.findAll());
		response.put("strapMaterials", strapMaterialRepository.findAll());
		response.put("glassMaterials", glassMaterialRepository.findAll());
		response.put("categories", categoryRepository.findAll().stream().map(c -> {
			Map<String, Object> m = new HashMap<>();
			m.put("categoryId", c.getCategoryId());
			m.put("name", (c.getParent() != null ? c.getParent().getName() + " > " : "") + c.getName());
			return m;
		}).collect(Collectors.toList()));

		response.put("conditions",
				Arrays.stream(ProductCondition.values()).map(Enum::name).collect(Collectors.toList()));
		response.put("statuses", Arrays.stream(ProductStatus.values()).map(Enum::name).collect(Collectors.toList()));
		response.put("genders", Arrays.stream(Gender.values()).map(Enum::name).collect(Collectors.toList()));
		response.put("segments", Arrays.stream(Segment.values()).map(Enum::name).collect(Collectors.toList()));

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Form data loaded", response);
	}

	@Override
	public ResponseDataDto getProductById(String productId) {
		ProductEntity product = productRepository.findById(productId).orElse(null);
		if (product == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Product not found", null);
		}

		ProductDto productDto = new ProductDto(product.getProductId(), product.getSku(), product.getName(),
				product.getOrigin(), product.getCondition(), product.getWarrantyPeriod(), product.getPrice(),
				product.getGender(), product.getSegment(), product.getDescription(),
				product.getBrand() != null ? product.getBrand().getName() : null,
				product.getType() != null ? product.getType().getName() : null,
				product.getCaseMaterial() != null ? product.getCaseMaterial().getName() : null,
				product.getStrapMaterial() != null ? product.getStrapMaterial().getName() : null,
				product.getGlassMaterial() != null ? product.getGlassMaterial().getName() : null,
				product.getCreatedAt(), product.getUpdatedAt(),
				product.getImages() != null ? product.getImages().stream().map(img -> {
					ImageDTO dto = new ImageDTO();
					dto.setImageId(img.getImageId());
					dto.setUrl(img.getUrl());
					dto.setAltText(img.getAltText());
					dto.setSortOrder(img.getSortOrder());
					return dto;
				}).collect(Collectors.toList()) : null);

		if (product.getCategory() != null) {
			productDto.setCategoryId(product.getCategory().getCategoryId());
			productDto.setCategoryName(product.getCategory().getName());
		}
		productDto.setStatus(product.getStatus().name());
		productDto.setAvailableQuantity(product.getAvailableQuantity());
		productDto.setVariantCount(variantRepository.countByProduct_ProductId(productId));
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", productDto);
	}

	@Override
	public ResponseDataDto searchProductName(String keyword) {
		List<ProductEntity> list = productRepository.findTop10ByNameContainingIgnoreCase(keyword);
		List<Map<String, String>> results = list.stream().map(p -> Map.of("id", p.getProductId(), "name", p.getName()))
				.collect(Collectors.toList());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "OK", results);
	}

}
