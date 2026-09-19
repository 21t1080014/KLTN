package com.dungochung.shopdongho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dungochung.shopdongho.common.CurrentActor;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.enums.UserStatus;
import com.dungochung.shopdongho.service.CustomerService;

import jakarta.servlet.http.HttpServletRequest;

/** Quản lý khách hàng (admin + support_staff): tra cứu, hồ sơ mua hàng, hạng, khóa/mở khóa, ghi chú nội bộ. */
@Controller
@RequestMapping("/admin/customers")
public class CustomerAdminController {
	@Autowired
	private CustomerService customerService;

	@GetMapping
	public String showCustomers(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "admin/customers";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto list(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) UserStatus status, @RequestParam(defaultValue = "newest") String sort,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		return customerService.list(keyword, status, sort, page, size);
	}

	@GetMapping("/api/{userId}")
	@ResponseBody
	public ResponseDataDto detail(@PathVariable String userId) {
		return customerService.detail(userId);
	}

	@PutMapping("/api/{userId}/status")
	@ResponseBody
	public ResponseDataDto changeStatus(@PathVariable String userId, @RequestParam UserStatus to,
			@RequestParam(required = false) String reason) {
		return customerService.changeStatus(userId, to, reason, CurrentActor.username());
	}

	@PutMapping("/api/{userId}/note")
	@ResponseBody
	public ResponseDataDto updateNote(@PathVariable String userId, @RequestParam(required = false) String note) {
		return customerService.updateNote(userId, note);
	}
}
