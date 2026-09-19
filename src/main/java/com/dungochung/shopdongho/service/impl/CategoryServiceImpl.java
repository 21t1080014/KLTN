package com.dungochung.shopdongho.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.CategoryDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.CategoryEntity;
import com.dungochung.shopdongho.repository.CategoryRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.service.CategoryService;

@Service
public class CategoryServiceImpl implements CategoryService {

	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private ProductRepository productRepository;

	private CategoryDto toDto(CategoryEntity c) {
		return new CategoryDto(c.getCategoryId(), c.getName(), c.getParent() != null ? c.getParent().getCategoryId() : null,
				c.getParent() != null ? c.getParent().getName() : null, c.getDescription());
	}

	@Override
	public ResponseDataDto getAllCategory(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "categoryId"));
		Page<CategoryEntity> categoryPage = categoryRepository.findAll(pageable);
		List<CategoryDto> dtos = categoryPage.getContent().stream().map(this::toDto).collect(Collectors.toList());

		Map<String, Object> data = new HashMap<>();
		data.put("categories", dtos);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, categoryPage.getTotalPages(), 3));
		data.put("totalCount", categoryPage.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto searchCategory(String keyword, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "categoryId"));
		Page<CategoryEntity> categoryPage = categoryRepository.findByNameContainingIgnoreCase(keyword, pageable);
		List<CategoryDto> dtos = categoryPage.getContent().stream().map(this::toDto).collect(Collectors.toList());

		Map<String, Object> data = new HashMap<>();
		data.put("categories", dtos);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, categoryPage.getTotalPages(), 3));
		data.put("totalCount", categoryPage.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto getCategoryTree() {
		List<CategoryEntity> roots = categoryRepository.findByParentIsNull();
		List<CategoryDto> tree = roots.stream().map(this::buildTree).collect(Collectors.toList());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", tree);
	}

	private CategoryDto buildTree(CategoryEntity entity) {
		CategoryDto dto = toDto(entity);
		List<CategoryEntity> children = categoryRepository.findByParent_CategoryId(entity.getCategoryId());
		dto.setChildren(children.stream().map(this::buildTree).collect(Collectors.toList()));
		return dto;
	}

	@Override
	public ResponseDataDto createCategory(String name, Integer parentId, String description) {
		if (name == null || name.trim().isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Tên danh mục không được để trống", null);
		}

		CategoryEntity parent = null;
		if (parentId != null) {
			parent = categoryRepository.findById(parentId).orElse(null);
			if (parent == null) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Danh mục cha không tồn tại", null);
			}
		}

		boolean duplicated = parentId != null ? categoryRepository.existsByNameAndParent_CategoryId(name, parentId)
				: categoryRepository.existsByNameAndParentIsNull(name);
		if (duplicated) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Tên danh mục đã tồn tại trong cùng cấp cha", null);
		}

		CategoryEntity category = new CategoryEntity();
		category.setName(name.trim());
		category.setParent(parent);
		category.setDescription(description);
		CategoryEntity saved = categoryRepository.save(category);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Tạo danh mục thành công", toDto(saved));
	}

	@Override
	public ResponseDataDto updateCategory(Integer categoryId, String name, Integer parentId, String description) {
		if (name == null || name.trim().isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Tên danh mục không được để trống", null);
		}

		CategoryEntity category = categoryRepository.findById(categoryId).orElse(null);
		if (category == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy danh mục", null);
		}

		CategoryEntity parent = null;
		if (parentId != null) {
			if (parentId.equals(categoryId)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Danh mục không thể là cha của chính nó", null);
			}
			parent = categoryRepository.findById(parentId).orElse(null);
			if (parent == null) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Danh mục cha không tồn tại", null);
			}
			// Chặn vòng lặp: parent mới không được là con/cháu của category hiện tại
			CategoryEntity cursor = parent;
			while (cursor != null) {
				if (cursor.getCategoryId().equals(categoryId)) {
					return new ResponseDataDto(Constant.RESULT_CD_FAIL,
							"Không thể chọn 1 danh mục con của chính nó làm danh mục cha", null);
				}
				cursor = cursor.getParent();
			}
		}

		// Kiểm tra trùng tên trong cùng cấp cha, bỏ qua chính bản thân category đang sửa
		boolean nameTaken = (parentId != null ? categoryRepository.findByParent_CategoryId(parentId)
				: categoryRepository.findByParentIsNull()).stream()
				.anyMatch(c -> !c.getCategoryId().equals(categoryId) && c.getName().equalsIgnoreCase(name.trim()));
		if (nameTaken) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Tên danh mục đã tồn tại trong cùng cấp cha", null);
		}

		category.setName(name.trim());
		category.setParent(parent);
		category.setDescription(description);
		CategoryEntity saved = categoryRepository.save(category);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Cập nhật danh mục thành công", toDto(saved));
	}

	@Override
	public ResponseDataDto deleteCategory(Integer categoryId) {
		CategoryEntity category = categoryRepository.findById(categoryId).orElse(null);
		if (category == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy danh mục", null);
		}

		List<CategoryEntity> children = categoryRepository.findByParent_CategoryId(categoryId);
		if (!children.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Không thể xóa: danh mục này còn " + children.size() + " danh mục con. Xóa danh mục con trước.", null);
		}

		long productCount = productRepository.countByCategory_CategoryId(categoryId);
		if (productCount > 0) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Không thể xóa: còn " + productCount + " sản phẩm đang gắn danh mục này.", null);
		}

		categoryRepository.deleteById(categoryId);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Xóa danh mục thành công", null);
	}
}
