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
import com.dungochung.shopdongho.entity.WatchTypeEntity;
import com.dungochung.shopdongho.repository.WatchTypeReponsitory;
import com.dungochung.shopdongho.service.WatchTypeService;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class WatchTypeServiceImpl implements WatchTypeService {
	@Autowired
	private WatchTypeReponsitory watchTypeReponsitory;

	@Override
	public ResponseDataDto getAllWatchType(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "typeId"));
		Page<WatchTypeEntity> typeList = watchTypeReponsitory.findAll(pageable);
		Map<String, Object> data = new HashMap<>();
		data.put("types", typeList.getContent());
		data.put("pagination", PaginationCommon.getPaginationInfo(page, typeList.getTotalPages(), 3));
		data.put("totalCount", typeList.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto creatWType(String name) {
		try {
			if (name == null || name.trim().isEmpty()) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Watch type name must not be empty");
			}
			if (watchTypeReponsitory.existsByName(name)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Watch type name already exists");
			}
			WatchTypeEntity typeEntity = new WatchTypeEntity();
			typeEntity.setName(name);
			WatchTypeEntity saved = watchTypeReponsitory.save(typeEntity);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Created successfully", saved);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error creating watch type : " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto updateWType(Integer typeId, String name) {
		try {
			if (name == null || name.trim().isEmpty()) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Watch type name must not be empty");
			}
			WatchTypeEntity watchTypeEntity = watchTypeReponsitory.findById(typeId).orElse(null);
			if (watchTypeEntity == null) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Watch Type not found with ID: " + typeId);
			}
			if (watchTypeReponsitory.existsByNameAndTypeIdNot(name, typeId)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Watch type name already exists");
			}
			watchTypeEntity.setName(name);
			WatchTypeEntity saved = watchTypeReponsitory.save(watchTypeEntity);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Updated successfully", saved);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error updating watch type : " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto deleteWType(Integer typeId) {
		try {
			if (!watchTypeReponsitory.existsById(typeId)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Watch Type not found with ID: " + typeId);
			}
			watchTypeReponsitory.deleteById(typeId);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Deleted successfully");
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error deleting watch type: " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto searchWType(String keyword, int page, int size) {
		try {
			Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "typeId"));
			Page<WatchTypeEntity> result = watchTypeReponsitory.findByNameContainingIgnoreCase(keyword, pageable);
			Map<String, Object> data = new HashMap<>();
			data.put("types", result.getContent());
			data.put("pagination", PaginationCommon.getPaginationInfo(page, result.getTotalPages(), 3));
			data.put("totalCount", result.getTotalElements());
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error searching watch type: " + e.getMessage());
		}
	}

}
