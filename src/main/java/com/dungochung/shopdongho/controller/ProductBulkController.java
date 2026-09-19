package com.dungochung.shopdongho.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.service.ProductBulkService;

/** Import/export CSV và cập nhật hàng loạt. Nằm dưới /admin/products nên dùng rule phân quyền của module sản phẩm. */
@RestController
@RequestMapping("/admin/products/api")
public class ProductBulkController {

	@Autowired
	private ProductBulkService bulkService;

	@GetMapping("/export")
	public ResponseEntity<byte[]> export() {
		byte[] body = bulkService.exportCsv().getBytes(StandardCharsets.UTF_8);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"products.csv\"")
				.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)).body(body);
	}

	@PostMapping("/import")
	public ResponseDataDto importCsv(@RequestParam("file") MultipartFile file) {
		try {
			return bulkService.importCsv(file);
		} catch (IOException e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không đọc được file", null);
		} catch (IllegalStateException e) {
			// lỗi khi áp dụng (vd tồn kho thấp hơn số đang giữ) => transaction rollback toàn bộ
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Import bị hủy: " + e.getMessage(), null);
		}
	}

	@PostMapping("/bulk-status")
	public ResponseDataDto bulkStatus(@RequestParam List<String> productIds, @RequestParam String status) {
		return bulkService.bulkStatus(productIds, status);
	}

	@PostMapping("/bulk-price")
	public ResponseDataDto bulkPrice(@RequestParam List<String> productIds, @RequestParam String mode,
			@RequestParam BigDecimal value) {
		return bulkService.bulkPrice(productIds, mode, value);
	}
}
