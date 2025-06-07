package com.dungochung.shopdongho.service;

import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface CaseMaterialService {
	ResponseDataDto getAllCase(int page, int size);

	ResponseDataDto createCase(String name);

	ResponseDataDto updateCase(Integer caseId, String name);

	ResponseDataDto deleteCase(Integer caseId);

	ResponseDataDto searchCase(String keyword, int page, int size);
}
