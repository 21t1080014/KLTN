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
import com.dungochung.shopdongho.entity.CaseMaterialEntity;
import com.dungochung.shopdongho.repository.CaseMaterialReponsitory;
import com.dungochung.shopdongho.service.CaseMaterialService;

@Service
public class CaseMaterialServiceImpl implements CaseMaterialService {
	@Autowired
	private CaseMaterialReponsitory caseMaterialReponsitory;

	@Override
	public ResponseDataDto getAllCase(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "caseMaterialId"));
		Page<CaseMaterialEntity> caseList = caseMaterialReponsitory.findAll(pageable);
		Map<String, Object> data = new HashMap<>();
		data.put("caseMaterials", caseList.getContent());
		data.put("pagination", PaginationCommon.getPaginationInfo(page, caseList.getTotalPages(), 3));
		data.put("totalCount", caseList.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto createCase(String name) {
		try {
			if (name == null || name.trim().isEmpty()) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Case material name must not be empty");
			}
			if (caseMaterialReponsitory.existsByName(name)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Case material name already exists");
			}
			CaseMaterialEntity caseEntity = new CaseMaterialEntity();
			caseEntity.setName(name);
			CaseMaterialEntity saved = caseMaterialReponsitory.save(caseEntity);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Created successfully", saved);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error creating case material: " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto updateCase(Integer caseId, String name) {
		try {
			if (name == null || name.trim().isEmpty()) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Case material name must not be empty");
			}
			CaseMaterialEntity caseMaterialEntity = caseMaterialReponsitory.findById(caseId).orElse(null);
			if (caseMaterialEntity == null) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Case material not found with ID: " + caseId);
			}
			if (caseMaterialReponsitory.existsByNameAndCaseMaterialIdNot(name, caseId)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Case material name already exists");
			}
			caseMaterialEntity.setName(name);
			CaseMaterialEntity saved = caseMaterialReponsitory.save(caseMaterialEntity);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Updated successfully", saved);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error updating case material: " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto deleteCase(Integer caseId) {
		try {
			if (!caseMaterialReponsitory.existsById(caseId)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Case material not found with ID: " + caseId);
			}
			caseMaterialReponsitory.deleteById(caseId);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Deleted successfully");
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error deleting case material: " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto searchCase(String keyword, int page, int size) {
		try {
			Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "caseMaterialId"));
			Page<CaseMaterialEntity> result = caseMaterialReponsitory.findByNameContainingIgnoreCase(keyword, pageable);
			Map<String, Object> data = new HashMap<>();
			data.put("caseMaterials", result.getContent());
			data.put("pagination", PaginationCommon.getPaginationInfo(page, result.getTotalPages(), 3));
			data.put("totalCount", result.getTotalElements());
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Error searching case material: " + e.getMessage());
		}
	}

}
