package com.dungochung.shopdongho.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.service.ProductVariantService;

/** Quản lý biến thể của sản phẩm. Nằm dưới /admin/products nên dùng chung rule phân quyền của module sản phẩm. */
@RestController
@RequestMapping("/admin/products/api/{productId}/variants")
public class ProductVariantController {

	@Autowired
	private ProductVariantService variantService;

	@GetMapping
	public ResponseDataDto list(@PathVariable String productId) {
		return variantService.listVariants(productId);
	}

	@PostMapping
	public ResponseDataDto create(@PathVariable String productId, @RequestParam String sku,
			@RequestParam String variantName, @RequestParam(required = false) String color,
			@RequestParam(required = false) String strapOption, @RequestParam(required = false) String caseSize,
			@RequestParam BigDecimal price, @RequestParam(required = false) String status,
			@RequestParam(required = false) Integer initialQuantity,
			@RequestParam(required = false) Integer lowStockThreshold) {
		return variantService.createVariant(productId, sku, variantName, color, strapOption, caseSize, price, status,
				initialQuantity, lowStockThreshold);
	}

	@PutMapping("/{variantId}")
	public ResponseDataDto update(@PathVariable String productId, @PathVariable Long variantId,
			@RequestParam String variantName, @RequestParam(required = false) String color,
			@RequestParam(required = false) String strapOption, @RequestParam(required = false) String caseSize,
			@RequestParam BigDecimal price, @RequestParam(required = false) String status,
			@RequestParam(required = false) Integer lowStockThreshold) {
		return variantService.updateVariant(productId, variantId, variantName, color, strapOption, caseSize, price,
				status, lowStockThreshold);
	}

	@PutMapping("/{variantId}/default")
	public ResponseDataDto setDefault(@PathVariable String productId, @PathVariable Long variantId) {
		return variantService.setDefault(productId, variantId);
	}

	@PutMapping("/{variantId}/images")
	public ResponseDataDto setImages(@PathVariable String productId, @PathVariable Long variantId,
			@RequestParam(required = false) List<Integer> imageIds) {
		return variantService.setVariantImages(productId, variantId, imageIds);
	}

	@DeleteMapping("/{variantId}")
	public ResponseDataDto delete(@PathVariable String productId, @PathVariable Long variantId) {
		return variantService.deleteVariant(productId, variantId);
	}
}
