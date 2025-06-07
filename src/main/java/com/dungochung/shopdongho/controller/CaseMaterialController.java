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
import com.dungochung.shopdongho.service.CaseMaterialService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/categories/case-materials")
public class CaseMaterialController {
	@Autowired
	private CaseMaterialService caseMaterialService;

	@GetMapping
	public String showCaseMaterial(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "pages/categories/CaseMaterial";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getAllCaseMaterial(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		return caseMaterialService.getAllCase(page, size);
	}

	@GetMapping("/api/search")
	@ResponseBody
	public ResponseDataDto searchCaseMaterial(@RequestParam("keyword") String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
		return caseMaterialService.searchCase(keyword, page, size);
	}

	@PostMapping("/api")
	@ResponseBody
	public ResponseDataDto createCaseMaterial(@RequestParam("caseName") String name) {
		return caseMaterialService.createCase(name);
	}

	@PutMapping("/api/{caseId}")
	@ResponseBody
	public ResponseDataDto updateCaseMaterial(@RequestParam("caseId") Integer caseId,
			@RequestParam("caseName") String caseName) {
		return caseMaterialService.updateCase(caseId, caseName);
	}
	@DeleteMapping("/api/{caseId}")
	@ResponseBody
	public ResponseDataDto deleteCaseMaterial(@RequestParam("caseId") Integer caseId) {
		return caseMaterialService.deleteCase(caseId);
	}
}
