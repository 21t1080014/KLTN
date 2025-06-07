package com.dungochung.shopdongho.service;

import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface StrapMaterialService {
	ResponseDataDto getAllStrap(int page, int size);

	ResponseDataDto createStrap(String name);

	ResponseDataDto updateStrap(Integer strapId, String name);

	ResponseDataDto deleteStrap(Integer strapId);

	ResponseDataDto searchStrap(String keyword, int page, int size);
}
