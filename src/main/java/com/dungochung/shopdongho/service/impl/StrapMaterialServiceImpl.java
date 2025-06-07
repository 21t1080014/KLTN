package com.dungochung.shopdongho.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.StrapMaterialEntity;
import com.dungochung.shopdongho.repository.StraMaterialReponsitory;
import com.dungochung.shopdongho.service.StrapMaterialService;

@Service
public class StrapMaterialServiceImpl implements StrapMaterialService {
	@Autowired
	private StraMaterialReponsitory straMaterialReponsitory;

	@Override
	public ResponseDataDto getAllStrap(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "strapMaterialId"));
		Page<StrapMaterialEntity> strapList = straMaterialReponsitory.findAll(pageable);
		Map<String, Object> data = new HashMap<>();
		data.put("strapMaterials", strapList.getContent());
		data.put("pagination", PaginationCommon.getPaginationInfo(page, strapList.getTotalPages(), 3));
		data.put("totalCount", strapList.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto createStrap(String name) {
		try {
			if (name == null || name.trim().isEmpty()) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Strap material name must not be empty");
			}
			if (straMaterialReponsitory.existsByName(name)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Strap material name already exists");
			}
			StrapMaterialEntity strapEntity = new StrapMaterialEntity();
			strapEntity.setName(name);
			StrapMaterialEntity saved = straMaterialReponsitory.save(strapEntity);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Created successfully", saved);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error creating strap material : " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto updateStrap(Integer strapId, String name) {
		try {
			if (name == null || name.trim().isEmpty()) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Strap material name must not be empty");
			}
			StrapMaterialEntity strapMaterialEntity = straMaterialReponsitory.findById(strapId).orElse(null);
			if (strapMaterialEntity == null) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Strap material not found with ID: " + strapId);
			}
			if (straMaterialReponsitory.existsByNameAndStrapMaterialIdNot(name, strapId)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Strap material name already exists");
			}
			strapMaterialEntity.setName(name);
			StrapMaterialEntity saved = straMaterialReponsitory.save(strapMaterialEntity);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Updated successfully", saved);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error updating strap material : " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto deleteStrap(Integer strapId) {
		try {
			if (!straMaterialReponsitory.existsById(strapId)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Strap Material not found with ID: " + strapId);
			}
			straMaterialReponsitory.deleteById(strapId);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Deleted successfully");
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error deleting Strap Material: " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto searchStrap(String keyword, int page, int size) {
		try {
			Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "strapMaterialId"));
			Page<StrapMaterialEntity> result = straMaterialReponsitory.findByNameContainingIgnoreCase(keyword,
					pageable);
			Map<String, Object> data = new HashMap<>();
			data.put("strapMaterials", result.getContent());
			data.put("pagination", PaginationCommon.getPaginationInfo(page, result.getTotalPages(), 3));
			data.put("totalCount", result.getTotalElements());
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error searching strap material: " + e.getMessage());
		}
	}

}
