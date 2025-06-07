package com.dungochung.shopdongho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dungochung.shopdongho.dto.PromotionDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.service.ProductService;
import com.dungochung.shopdongho.service.PromotionProductService;
import com.dungochung.shopdongho.service.PromotionService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/promotions")
public class PromotionController {
	@Autowired
	private PromotionService promotionService;
	@Autowired
	private ProductService productService;
	@Autowired
	private PromotionProductService promotionProductService;

	@GetMapping
	public String showPromotion(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "pages/Promotion";
	}

	// Get all promotions with pagination
	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getAllPromotions(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return promotionService.getAllPromotion(page, size);
	}

	@GetMapping("/api/search")
	@ResponseBody
	public ResponseDataDto searchPromotions(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String discountType, @RequestParam(required = false) String discountValue,
			@RequestParam(required = false) String productName, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return promotionService.searchPromotion(keyword, discountType, discountValue, page, size);
	}

	@GetMapping("/api/{promotionId}")
	@ResponseBody
	public ResponseDataDto getPromotionById(@PathVariable Integer promotionId) {
		return promotionService.getPromotionById(promotionId);
	}

	// Create new promotion

	@PostMapping("/api")
	@ResponseBody
	public ResponseDataDto createPromotion(@RequestBody PromotionDto dto) {
		return promotionService.createPromotion(dto);
	}

	// Update promotion

	@PutMapping("/api/{promotionId}")
	@ResponseBody
	public ResponseDataDto updatePromotion(@PathVariable Integer promotionId, @RequestBody PromotionDto dto) {
		return promotionService.updatePromotion(promotionId, dto);
	}

	// Delete promotion

	@DeleteMapping("/api/{promotionId}")
	@ResponseBody
	public ResponseDataDto deletePromotion(@PathVariable Integer promotionId) {
		return promotionService.deletePromotion(promotionId);
	}

	// Add product to promotion

	@PostMapping("/api/{promotionId}/products/{productId}")
	@ResponseBody
	public ResponseDataDto addProductToPromotion(@PathVariable Integer promotionId, @PathVariable String productId) {
		return promotionProductService.addProductToPromotion(promotionId, productId);
	}

	// Remove product from promotion

	@DeleteMapping("/api/{promotionId}/products/{productId}")
	@ResponseBody
	public ResponseDataDto removeProductFromPromotion(@PathVariable Integer promotionId,

			@PathVariable String productId) {
		return promotionProductService.removeProductFromPromotion(promotionId, productId);
	}

	@GetMapping("/api/search-product-name")
	@ResponseBody
	public ResponseDataDto searchProductName(@RequestParam("keyword") String keyword) {
		return productService.searchProductName(keyword);
	}

	// Get all products in a promotion

	@GetMapping("/api/{promotionId}/products")
	@ResponseBody
	public ResponseDataDto getProductsByPromotion(@PathVariable Integer promotionId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "3") int size) {
		return promotionProductService.getProductsByPromotion(promotionId, page, size);
	}

	@GetMapping("/api/{promotionId}/search-products")
	@ResponseBody
	public ResponseDataDto searchProductPromotion(@PathVariable Integer promotionId, @RequestParam String productName,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		return promotionProductService.searchProductPromotion(promotionId, productName, page, size);
	}

}
