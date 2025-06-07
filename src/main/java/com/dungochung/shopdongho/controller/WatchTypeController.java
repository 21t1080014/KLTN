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
import com.dungochung.shopdongho.service.WatchTypeService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/categories/watch-types")
public class WatchTypeController {
	@Autowired
	private WatchTypeService watchTypeService;

	@GetMapping
	public String showWatchType(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "pages/categories/WatchType";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getAllWType(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		return watchTypeService.getAllWatchType(page, size);
	}

	@PostMapping("/api")
	@ResponseBody
	public ResponseDataDto creatWType(@RequestParam("typeName") String name) {
		return watchTypeService.creatWType(name);
	}

	@PutMapping("/api/{typeId}")
	@ResponseBody
	public ResponseDataDto updateWType(@RequestParam("typeId") Integer typeId,
			@RequestParam("typeName") String typeName) {
		return watchTypeService.updateWType(typeId, typeName);
	}

	@DeleteMapping("/api/{typeId}")
	@ResponseBody
	public ResponseDataDto deleteWType(@RequestParam("typeId") Integer typeId) {
		return watchTypeService.deleteWType(typeId);
	}

	@GetMapping("/api/search")
	@ResponseBody
	public ResponseDataDto searchWType(@RequestParam("keyword") String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
		return watchTypeService.searchWType(keyword, page, size);
	}
}
