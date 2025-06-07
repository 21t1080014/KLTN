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
import com.dungochung.shopdongho.entity.GlassMaterialEntity;
import com.dungochung.shopdongho.entity.WatchTypeEntity;
import com.dungochung.shopdongho.repository.GlassMaterialReponsitory;
import com.dungochung.shopdongho.service.GlassMaterialService;

@Service
public class GlassMaterialServiceImpl implements GlassMaterialService {
	@Autowired
	private GlassMaterialReponsitory glassMaterialReponsitory;

	@Override
	public ResponseDataDto getAllWatchGlass(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "glassMaterialId"));
		Page<GlassMaterialEntity> glassList = glassMaterialReponsitory.findAll(pageable);
		Map<String, Object> data = new HashMap<>();
		data.put("glass", glassList.getContent());
		data.put("pagination", PaginationCommon.getPaginationInfo(page, glassList.getTotalPages(), 3));
		data.put("totalCount", glassList.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto creatWGlass(String name) {
		try {
			if (name == null || name.trim().isEmpty()) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Watch type name must not be empty");
			}
			if (glassMaterialReponsitory.existsByName(name)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Watch type name already exists");
			}
			GlassMaterialEntity glassMaterialEntity = new GlassMaterialEntity();
			glassMaterialEntity.setName(name);
			GlassMaterialEntity saved = glassMaterialReponsitory.save(glassMaterialEntity);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Created successfully", saved);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error creating watch glass : " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto updateWGlass(Integer glassId, String name) {
		try {
			GlassMaterialEntity glassMaterialEntity = glassMaterialReponsitory.findById(glassId).orElse(null);
			if (glassMaterialEntity == null) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Watch Glass not found with ID: " + glassId);
			}
			if (glassMaterialReponsitory.existsByNameAndGlassMaterialIdNot(name, glassId)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Watch glass name already exists");
			}
			glassMaterialEntity.setName(name);
			GlassMaterialEntity saved = glassMaterialReponsitory.save(glassMaterialEntity);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Updated successfully", saved);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error updating watch type : " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto deleteWGlass(Integer glassId) {
		try {
			if (!glassMaterialReponsitory.existsById(glassId)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Watch Glass not found with ID: " + glassId);
			}
			glassMaterialReponsitory.deleteById(glassId);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Deleted successfully");
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error deleting watch glass: " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto searchWGlass(String keyword, int page, int size) {
		try {
			Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "glassMaterialId"));
			Page<GlassMaterialEntity> result = glassMaterialReponsitory.findByNameContainingIgnoreCase(keyword,
					pageable);
			Map<String, Object> data = new HashMap<>();
			data.put("glass", result.getContent());
			data.put("pagination", PaginationCommon.getPaginationInfo(page, result.getTotalPages(), 3));
			data.put("totalCount", result.getTotalElements());
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error searching watch glass: " + e.getMessage());
		}
	}

}
