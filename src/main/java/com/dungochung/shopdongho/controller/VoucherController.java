package com.dungochung.shopdongho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.VoucherEntity;
import com.dungochung.shopdongho.service.VoucherService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/voucher")
public class VoucherController {
	@Autowired
	private VoucherService voucherService;

	@GetMapping
	public String showVoucher(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "admin/vouchers";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getAllVoucher(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size, @RequestParam(required = false) String keyword) {
		return voucherService.getAllVoucher(page, size, keyword);
	}

	@PostMapping("/api")
	@ResponseBody
	public ResponseDataDto createVoucher(@RequestBody VoucherEntity voucherEntity) {
		return voucherService.creatVoucher(voucherEntity);
	}

	@PutMapping("/api")
	@ResponseBody
	public ResponseDataDto updateVoucher(@RequestParam String voucherCode, @RequestBody VoucherEntity voucherEntity) {
		return voucherService.updateVoucher(voucherCode, voucherEntity);
	}

	@DeleteMapping("/api")
	@ResponseBody
	public ResponseDataDto deleteVoucher(@RequestParam String voucherCode) {
		return voucherService.deleteVoucher(voucherCode);
	}
}
