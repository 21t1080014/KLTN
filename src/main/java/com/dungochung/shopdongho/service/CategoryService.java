package com.dungochung.shopdongho.service;

import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface CategoryService {
	ResponseDataDto getAllCategory(int page, int size);

	ResponseDataDto getCategoryTree();

	ResponseDataDto searchCategory(String keyword, int page, int size);

	ResponseDataDto createCategory(String name, Integer parentId, String description);

	ResponseDataDto updateCategory(Integer categoryId, String name, Integer parentId, String description);

	ResponseDataDto deleteCategory(Integer categoryId);
}
