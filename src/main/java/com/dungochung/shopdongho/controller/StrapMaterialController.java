package com.dungochung.shopdongho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.service.StrapMaterialService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/categories/strap-materials")
public class StrapMaterialController {
	@Autowired
	private StrapMaterialService strapMaterialService;

	@GetMapping
	public String showStrapMaterial(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "admin/categories/strap-material";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getAllStrapMaterial(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		return strapMaterialService.getAllStrap(page, size);
	}
	@GetMapping("/api/search")
	@ResponseBody
	public ResponseDataDto searchStrapMaterial(@RequestParam("keyword") String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
		return strapMaterialService.searchStrap(keyword, page, size);
	}
	@PostMapping("/api")
	@ResponseBody
	public ResponseDataDto createStrapMaterial(@RequestParam("strapName") String name) {
		return strapMaterialService.createStrap(name);
	}
	@PutMapping("/api/{strapId}")
	@ResponseBody
	public ResponseDataDto updateStrapMaterial(@RequestParam("strapId") Integer strapId,
			@RequestParam("strapName") String strapName) {
		return strapMaterialService.updateStrap(strapId, strapName);
	}
	@DeleteMapping("/api/{strapId}")
	@ResponseBody
	public ResponseDataDto deleteStrapMaterial(@RequestParam("strapId") Integer strapId) {
		return strapMaterialService.deleteStrap(strapId);
	}
}
