package com.dungochung.shopdongho.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.service.ReportService;

import jakarta.servlet.http.HttpServletRequest;

/** Báo cáo kinh doanh (chỉ admin — xem AdminPermissions): doanh thu theo ngày/tuần/tháng, top bán chạy, tồn thấp, xuất đơn hàng. */
@Controller
@RequestMapping("/admin/reports")
public class ReportController {
	@Autowired
	private ReportService reportService;

	@GetMapping
	public String show(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "admin/reports";
	}

	@GetMapping("/api/revenue")
	@ResponseBody
	public ResponseDataDto revenue(@RequestParam(defaultValue = "day") String granularity,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return reportService.revenueSeries(granularity, from, to);
	}

	@GetMapping("/api/top-products")
	@ResponseBody
	public ResponseDataDto topProducts(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "10") int limit) {
		return reportService.topProducts(from, to, limit);
	}

	@GetMapping("/api/low-stock")
	@ResponseBody
	public ResponseDataDto lowStock(@RequestParam(defaultValue = "50") int limit) {
		return reportService.lowStock(limit);
	}

	@GetMapping("/export/orders.csv")
	public ResponseEntity<?> exportOrders(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		try {
			byte[] body = reportService.ordersCsv(from, to).getBytes(StandardCharsets.UTF_8);
			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders.csv\"")
					.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)).body(body);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
					.body(new ResponseDataDto(Constant.RESULT_CD_FAIL, e.getMessage()));
		}
	}
}
