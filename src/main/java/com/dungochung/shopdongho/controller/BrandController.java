package com.dungochung.shopdongho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import com.dungochung.shopdongho.common.FileStorageService;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.service.BrandService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("admin/brands")
public class BrandController {
	@Autowired
	private BrandService brandService;
	@Autowired
	private FileStorageService fileStorageService;

	@GetMapping
	public String showBrand(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "pages/Brand";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getAllBrand(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		return brandService.getAllBrand(page, size);
	}

	@PostMapping("/api")
	@ResponseBody
	public ResponseDataDto creatBrand(@RequestParam("brandName") String brandName,
			@RequestParam("description") String description,
			@RequestParam(value = "logoFile", required = false) MultipartFile logoFile) {
		return brandService.creatBrand(brandName, logoFile, description);
	}

	@PutMapping("/api/{brandId}")
	@ResponseBody
	public ResponseDataDto updateBrand(@PathVariable("brandId") Integer brandId,
			@RequestParam("brandName") String brandName, @RequestParam("description") String description,
			@RequestParam(value = "logoFile", required = false) MultipartFile logoFile) {
		return brandService.updateBrand(brandId, brandName, logoFile, description);
	}

	@GetMapping("/api/search")
	@ResponseBody
	public ResponseDataDto searchBrand(@RequestParam("keyword") String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
		return brandService.searchBrand(keyword, page, size);
	}

	@DeleteMapping("/api/{brandId}")
	@ResponseBody
	public ResponseDataDto deleteBrand(@PathVariable("brandId") Integer brandId) {
		return brandService.deleteBrand(brandId);
	}

	@GetMapping("/api/img/{fileName:.+}")
	@ResponseBody
	public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
		return fileStorageService.getImageAsResponse("brands/" + fileName);
	}
}
