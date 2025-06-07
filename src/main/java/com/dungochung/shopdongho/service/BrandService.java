package com.dungochung.shopdongho.service;

import org.springframework.web.multipart.MultipartFile;

import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface BrandService {
	ResponseDataDto getAllBrand(int page, int size);

	ResponseDataDto creatBrand(String name, MultipartFile logoImage, String description);

	ResponseDataDto updateBrand(Integer brandId, String name, MultipartFile logoImage, String description);

	ResponseDataDto deleteBrand(Integer brandId);

	ResponseDataDto searchBrand(String keyword, int page, int size);

	ResponseDataDto getBrandNameAndId();
}
