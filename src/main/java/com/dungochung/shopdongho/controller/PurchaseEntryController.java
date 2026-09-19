package com.dungochung.shopdongho.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.PurchaseEntriesEntity;
import com.dungochung.shopdongho.service.ProductService;
import com.dungochung.shopdongho.service.PurchaseEntriesService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/purchases")
public class PurchaseEntryController {
	@Autowired
	private ProductService productService;
	@Autowired
	private PurchaseEntriesService entriesService;

	@GetMapping
	public String showPurchaseEntry(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "admin/purchase-entries";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getAllEntries(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		return entriesService.getAllEntries(page, size);
	}

	// search trong modal add/edit
	@GetMapping("/api/search-product-name")
	@ResponseBody
	public ResponseDataDto searchProductName(@RequestParam("keyword") String keyword) {
		return productService.searchProductName(keyword);
	}

	@DeleteMapping("/api/{id}")
	@ResponseBody
	public ResponseDataDto deleteEntry(@PathVariable Long id) {
		return entriesService.deleteEntry(id);
	}

	@PostMapping("/api")
	@ResponseBody
	public ResponseDataDto createEntry(@RequestBody PurchaseEntriesEntity entry) {
		return entriesService.createEntry(entry);
	}

	@PutMapping("/api/{id}")
	@ResponseBody
	public ResponseDataDto updateEntry(@PathVariable Long id, @RequestBody PurchaseEntriesEntity entry) {
		return entriesService.updateEntry(entry, id);
	}

	@GetMapping("/api/search")
	@ResponseBody
	public ResponseDataDto searchEntries(@RequestParam(required = false) String productName,
			@RequestParam(required = false) String fromDate, @RequestParam(required = false) String toDate,
			@RequestParam(required = false) String minPrice, @RequestParam(required = false) String maxPrice,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
		Timestamp from = null, to = null;
		BigDecimal min = null, max = null;
		try {
			if (fromDate != null && !fromDate.isBlank()) {
				from = Timestamp.valueOf(fromDate + " 00:00:00");
			}
			if (toDate != null && !toDate.isBlank()) {
				to = Timestamp.valueOf(toDate + " 23:59:59");
			}
			if (minPrice != null && !minPrice.isBlank()) {
				min = new BigDecimal(minPrice);
			}
			if (maxPrice != null && !maxPrice.isBlank()) {
				max = new BigDecimal(maxPrice);
			}
		} catch (Exception e) {
			return new ResponseDataDto(0, "Lỗi định dạng tham số: " + e.getMessage(), null);
		}
		return entriesService.searchEntries(productName, from, to, min, max, page, size);
	}

	@GetMapping("/{id}")
	@ResponseBody
	public ResponseDataDto getEntryById(@PathVariable Long id) {
		return entriesService.getEntriesId(id);
	}

}
