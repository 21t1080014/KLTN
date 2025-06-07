package com.dungochung.shopdongho.service;

import java.util.List;

import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface ProductCustomerService {
	ResponseDataDto getFeaturedProducts(int brandId);

	ResponseDataDto getAllProductCustomer(int page, int size);

	ResponseDataDto getNewestProducts();

	ResponseDataDto getActivePromotionalProducts();

	ResponseDataDto getProductById(String productId);

	ResponseDataDto searchProducts(String keyword, int page, int size);

	ResponseDataDto suggestProducts(String keyword);

	ResponseDataDto searchProducts(List<String> genders, List<String> segments, List<String> brandNames,
			Double priceMin, Double priceMax, String sortBy, String sortDir, int page, int size);

	ResponseDataDto getGenders();

	ResponseDataDto getSegment();

}
