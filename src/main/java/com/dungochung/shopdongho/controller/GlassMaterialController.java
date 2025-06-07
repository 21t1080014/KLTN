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
import com.dungochung.shopdongho.service.GlassMaterialService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/categories/glass-materials")
public class GlassMaterialController {
	@Autowired
	private GlassMaterialService glassMaterialService;

	@GetMapping
	public String showGlassW(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "pages/categories/WatchGlass";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getAllWatchGlass(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		return glassMaterialService.getAllWatchGlass(page, size);
	}

	@PostMapping("/api")
	@ResponseBody
	public ResponseDataDto createWGlass(@RequestParam("glassName") String name) {
		return glassMaterialService.creatWGlass(name);
	}

	@PutMapping("/api/{glassId}")
	@ResponseBody
	public ResponseDataDto updateWGlass(@RequestParam("glassId") Integer glassId,
			@RequestParam("glassName") String glassName) {
		return glassMaterialService.updateWGlass(glassId, glassName);
	}

	@DeleteMapping("/api/{glassId}")
	@ResponseBody
	public ResponseDataDto deleteWGlass(@RequestParam("glassId") Integer glassId) {
		return glassMaterialService.deleteWGlass(glassId);
	}

	@GetMapping("/api/search")
	@ResponseBody
	public ResponseDataDto searchWGlass(@RequestParam("keyword") String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
		return glassMaterialService.searchWGlass(keyword, page, size);
	}
}
