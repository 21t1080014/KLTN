package com.dungochung.shopdongho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.service.CategoryService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {
	@Autowired
	private CategoryService categoryService;

	@GetMapping
	public String showCategory(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "admin/categories/category";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getAllCategory(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		return categoryService.getAllCategory(page, size);
	}

	@GetMapping("/api/tree")
	@ResponseBody
	public ResponseDataDto getCategoryTree() {
		return categoryService.getCategoryTree();
	}

	@GetMapping("/api/search")
	@ResponseBody
	public ResponseDataDto searchCategory(@RequestParam("keyword") String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
		return categoryService.searchCategory(keyword, page, size);
	}

	@PostMapping("/api")
	@ResponseBody
	public ResponseDataDto createCategory(@RequestParam("name") String name,
			@RequestParam(value = "parentId", required = false) Integer parentId,
			@RequestParam(value = "description", required = false) String description) {
		return categoryService.createCategory(name, parentId, description);
	}

	@PutMapping("/api/{categoryId}")
	@ResponseBody
	public ResponseDataDto updateCategory(@PathVariable Integer categoryId, @RequestParam("name") String name,
			@RequestParam(value = "parentId", required = false) Integer parentId,
			@RequestParam(value = "description", required = false) String description) {
		return categoryService.updateCategory(categoryId, name, parentId, description);
	}

	@DeleteMapping("/api/{categoryId}")
	@ResponseBody
	public ResponseDataDto deleteCategory(@PathVariable Integer categoryId) {
		return categoryService.deleteCategory(categoryId);
	}
}
