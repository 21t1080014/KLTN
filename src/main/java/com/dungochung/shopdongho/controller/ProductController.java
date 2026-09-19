package com.dungochung.shopdongho.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.dungochung.shopdongho.common.FileStorageService;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.BrandEntity;
import com.dungochung.shopdongho.entity.CategoryEntity;
import com.dungochung.shopdongho.repository.CategoryRepository;
import com.dungochung.shopdongho.entity.CaseMaterialEntity;
import com.dungochung.shopdongho.entity.GlassMaterialEntity;
import com.dungochung.shopdongho.entity.StrapMaterialEntity;
import com.dungochung.shopdongho.entity.WatchTypeEntity;
import com.dungochung.shopdongho.enums.Gender;
import com.dungochung.shopdongho.enums.ProductCondition;
import com.dungochung.shopdongho.enums.ProductStatus;
import com.dungochung.shopdongho.enums.Segment;
import com.dungochung.shopdongho.repository.BrandReponsitory;
import com.dungochung.shopdongho.repository.CaseMaterialReponsitory;
import com.dungochung.shopdongho.repository.GlassMaterialReponsitory;
import com.dungochung.shopdongho.repository.StraMaterialReponsitory;
import com.dungochung.shopdongho.repository.WatchTypeReponsitory;
import com.dungochung.shopdongho.service.ProductService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/products")
public class ProductController {
	@Autowired
	private ProductService productService;
	@Autowired
	private BrandReponsitory brandRepository;
	@Autowired
	private FileStorageService fileStorageService;
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

	@GetMapping
	public String showProduct(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "admin/products";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getAllProduct(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		return productService.getAllProduct(page, size);
	}

	@PostMapping("/api")
	@ResponseBody
	public ResponseDataDto createProduct(@RequestParam String sku, @RequestParam String name,
			@RequestParam Integer brandId, @RequestParam Integer typeId, @RequestParam Integer caseMaterialId,
			@RequestParam Integer strapMaterialId, @RequestParam Integer glassMaterialId, @RequestParam String origin,
			@RequestParam ProductCondition condition, @RequestParam String warrantyPeriod,
			@RequestParam BigDecimal price, @RequestParam Gender gender, @RequestParam Segment segment,
			@RequestParam String description, @RequestParam(required = false) Integer categoryId,
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(required = false) List<MultipartFile> images) {
		BrandEntity brand = brandRepository.findById(brandId).orElse(null);
		WatchTypeEntity type = watchTypeRepository.findById(typeId).orElse(null);
		CaseMaterialEntity caseMaterial = caseMaterialRepository.findById(caseMaterialId).orElse(null);
		StrapMaterialEntity strapMaterial = strapMaterialRepository.findById(strapMaterialId).orElse(null);
		GlassMaterialEntity glassMaterial = glassMaterialRepository.findById(glassMaterialId).orElse(null);
		CategoryEntity category = categoryId != null ? categoryRepository.findById(categoryId).orElse(null) : null;

		return productService.creatProduct(sku, name, brand, type, caseMaterial, strapMaterial, glassMaterial, category,
				status, origin, condition, warrantyPeriod, price, gender, segment, description, images);
	}

	@PutMapping("/api/{productId}")
	@ResponseBody
	public ResponseDataDto updateProduct(@PathVariable String productId, @RequestParam String sku,
			@RequestParam String name, @RequestParam Integer brandId, @RequestParam Integer typeId,
			@RequestParam Integer caseMaterialId, @RequestParam Integer strapMaterialId,
			@RequestParam Integer glassMaterialId, @RequestParam String origin,
			@RequestParam ProductCondition condition, @RequestParam String warrantyPeriod,
			@RequestParam BigDecimal price, @RequestParam Gender gender, @RequestParam Segment segment,
			@RequestParam String description, @RequestParam(required = false) Integer categoryId,
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(required = false) List<MultipartFile> images,
			@RequestParam(required = false) List<String> oldImages) {

		BrandEntity brand = brandRepository.findById(brandId).orElse(null);
		WatchTypeEntity type = watchTypeRepository.findById(typeId).orElse(null);
		CaseMaterialEntity caseMaterial = caseMaterialRepository.findById(caseMaterialId).orElse(null);
		StrapMaterialEntity strapMaterial = strapMaterialRepository.findById(strapMaterialId).orElse(null);
		GlassMaterialEntity glassMaterial = glassMaterialRepository.findById(glassMaterialId).orElse(null);
		CategoryEntity category = categoryId != null ? categoryRepository.findById(categoryId).orElse(null) : null;

		return productService.updateProduct(productId, sku, name, brand, type, caseMaterial, strapMaterial,
				glassMaterial, category, status, origin, condition, warrantyPeriod, price, gender, segment, description,
				images, oldImages);
	}

	@DeleteMapping("/api/{productId}")
	@ResponseBody
	public ResponseDataDto deleteProduct(@PathVariable String productId) {
		return productService.deleteProduct(productId);
	}

	@GetMapping("/api/search")
	@ResponseBody
	public ResponseDataDto searchProductNamePrice(@RequestParam String keyword, @RequestParam Double minPrice,
			@RequestParam Double maxPrice, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		return productService.searchProduct(keyword, minPrice, maxPrice, page, size);
	}

	@GetMapping("/api/{productId}")
	@ResponseBody
	public ResponseDataDto getProductById(@PathVariable String productId) {
		return productService.getProductById(productId);
	}

	@GetMapping("/api/form-options")
	@ResponseBody
	public ResponseDataDto getFormOptions() {
		return productService.getFormOptions();
	}

	@GetMapping("/api/img/{fileName:.+}")
	@ResponseBody
	public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
		return fileStorageService.getImageAsResponse("products/" + fileName);
	}
}
