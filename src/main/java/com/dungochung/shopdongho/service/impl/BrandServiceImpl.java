package com.dungochung.shopdongho.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.dungochung.shopdongho.dto.BrandIdNameDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.BrandEntity;
import com.dungochung.shopdongho.repository.BrandReponsitory;
import com.dungochung.shopdongho.service.BrandService;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class BrandServiceImpl implements BrandService {
	@Autowired
	private BrandReponsitory brandReponsitory;
	@Autowired
	private FileStorageService fileStorageService;

	@Override
	public ResponseDataDto getAllBrand(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "brandId"));
		Page<BrandEntity> brandList = brandReponsitory.findAll(pageable);
		Map<String, Object> data = new HashMap<>();
		data.put("brands", brandList.getContent());
		data.put("pagination", PaginationCommon.getPaginationInfo(page, brandList.getTotalPages(), 3));
		data.put("totalCount", brandList.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}
	@Override
	public ResponseDataDto getBrandNameAndId() {
		List<BrandIdNameDto> brandList = brandReponsitory.findAllBrandIdAndName();
		Map<String, Object> data = new HashMap<>();
		data.put("brandsName", brandList);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto creatBrand(String name, MultipartFile logoImage, String description) {
		try {
			if (brandReponsitory.existsByName(name)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Brand name already exists");
			}
			String logoUrl = null;
			if (logoImage != null && !logoImage.isEmpty()) {
				logoUrl = fileStorageService.saveFile(logoImage, "brands");
			}
			BrandEntity brandEntity = new BrandEntity();
			brandEntity.setName(name);
			brandEntity.setLogoImage(logoUrl);
			brandEntity.setDescription(description);
			BrandEntity saved = brandReponsitory.save(brandEntity);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Created successfully", saved);
		} catch (Exception e) {
			e.getStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error creating brand: " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto deleteBrand(Integer brandId) {
		Optional<BrandEntity> optional = brandReponsitory.findById(brandId);
		if (optional.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Brand not found");
		}

//		if (productRepository.existsByBrand_BrandId(brandId)) {
//			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
//					"Brand is in use by one or more products and cannot be deleted.");
//		}

		BrandEntity brand = optional.get();
		String logoPath = brand.getLogoImage();

		brandReponsitory.delete(brand);

		if (logoPath != null && !logoPath.isEmpty()) {
			boolean deleted = fileStorageService.deleteFile("brands", logoPath);
			if (!deleted) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Database deleted but file deletion failed");
			}
		}

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Deleted successfully");
	}

	@Override
	public ResponseDataDto updateBrand(Integer brandId, String name, MultipartFile logoImage, String description) {
		try {
			BrandEntity brandEntity = brandReponsitory.findById(brandId).orElse(null);
			if (brandEntity == null) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Brand not found with ID: " + brandId);
			}

			// Check for name conflict (excluding current brand)
			if (brandReponsitory.existsByNameAndBrandIdNot(name, brandId)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Brand name already exists");
			}

			if (logoImage != null && !logoImage.isEmpty()) {
				String logoUrl = fileStorageService.saveFile(logoImage, "brands");
				brandEntity.setLogoImage(logoUrl);
			}

			brandEntity.setName(name);
			brandEntity.setDescription(description);

			BrandEntity updated = brandReponsitory.save(brandEntity);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Updated successfully", updated);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error updating brand: " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto searchBrand(String keyword, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "brandId"));
		Page<BrandEntity> result = brandReponsitory.findByNameContainingIgnoreCase(keyword, pageable);
		Map<String, Object> data = new HashMap<>();
		data.put("brands", result.getContent());
		data.put("pagination", PaginationCommon.getPaginationInfo(page, result.getTotalPages(), 3));
		data.put("totalCount", result.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

}
