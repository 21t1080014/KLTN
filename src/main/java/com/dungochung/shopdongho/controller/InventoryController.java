package com.dungochung.shopdongho.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.service.InventoryService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/inventory")
public class InventoryController {
	@Autowired
	private InventoryService inventoryService;

	@GetMapping
	public String showInventory(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "pages/Inventory";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getAllInventory(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		return inventoryService.getAllInventory(page, size);
	}

	@GetMapping("/api/search")
	@ResponseBody
	public ResponseDataDto searchInventory(@RequestParam(required = false) String productName,
			@RequestParam(required = false) String fromDate, @RequestParam(required = false) String toDate,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
		Timestamp from = null, to = null;
		if (fromDate != null && !fromDate.isBlank()) {
			from = Timestamp.valueOf(fromDate + " 00:00:00");
		}
		if (toDate != null && !toDate.isBlank()) {
			to = Timestamp.valueOf(toDate + " 23:59:59");
		}
		return inventoryService.searchInventory(productName, from, to, page, size);
	}

	@PutMapping("/api/{productId}")
	@ResponseBody
	public ResponseDataDto updateInventory(@PathVariable String productId, @RequestParam int quantity) {
		return inventoryService.updateInventoryQuantity(productId, quantity);
	}

	@DeleteMapping("/api/{productId}")
	@ResponseBody
	public ResponseDataDto deleteInventory(@PathVariable String productId) {
		return inventoryService.deleteInventory(productId);
	}

	@GetMapping("/api/low-stock")
	@ResponseBody
	public ResponseDataDto getLowStockInventories(@RequestParam(defaultValue = "5") int threshold) {
		return inventoryService.getLowStockInventories(threshold);
	}

	@GetMapping("/api/product/{productId}")
	@ResponseBody
	public ResponseDataDto getInventoryByProductId(@PathVariable String productId) {
		return inventoryService.getInventoryByProductId(productId);
	}

}
