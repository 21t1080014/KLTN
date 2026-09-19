package com.dungochung.shopdongho.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.BrandEntity;
import com.dungochung.shopdongho.entity.CategoryEntity;
import com.dungochung.shopdongho.entity.CaseMaterialEntity;
import com.dungochung.shopdongho.entity.GlassMaterialEntity;
import com.dungochung.shopdongho.entity.StrapMaterialEntity;
import com.dungochung.shopdongho.entity.WatchTypeEntity;
import com.dungochung.shopdongho.enums.Gender;
import com.dungochung.shopdongho.enums.ProductCondition;
import com.dungochung.shopdongho.enums.ProductStatus;
import com.dungochung.shopdongho.enums.Segment;

public interface ProductService {
	ResponseDataDto getAllProduct(int page, int size);

	ResponseDataDto creatProduct(String sku, String name, BrandEntity brand, WatchTypeEntity type,
			CaseMaterialEntity caseMaterial, StrapMaterialEntity strapMaterial, GlassMaterialEntity glassMaterial,
			CategoryEntity category, ProductStatus status, String origin, ProductCondition condition, String warrantyPeriod, BigDecimal price, Gender gender,
			Segment segment, String description, List<MultipartFile> images);

	ResponseDataDto updateProduct(String productId, String sku, String name, BrandEntity brand, WatchTypeEntity type,
			CaseMaterialEntity caseMaterial, StrapMaterialEntity strapMaterial, GlassMaterialEntity glassMaterial,
			CategoryEntity category, ProductStatus status, String origin, ProductCondition condition, String warrantyPeriod, BigDecimal price, Gender gender,
			Segment segment, String description, List<MultipartFile> images, List<String> oldImageNames);

	ResponseDataDto deleteProduct(String productId);

	ResponseDataDto getFormOptions();

	ResponseDataDto getProductById(String productId);

	ResponseDataDto searchProductName(String keyword);

	ResponseDataDto searchProduct(String keyword, Double priceStart, Double priceEnd, int page, int size);
}
