package com.dungochung.shopdongho.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface ProductBulkService {
	/** Xuất CSV (UTF-8 có BOM) mỗi dòng 1 biến thể: sku,product_name,variant_name,price,product_status,variant_status,quantity,reserved,low_stock_threshold. */
	String exportCsv();

	/** Nhập CSV cập nhật theo SKU biến thể (cột bắt buộc: sku; tùy chọn: price, status, quantity). Tất cả hoặc không gì cả. */
	ResponseDataDto importCsv(MultipartFile file) throws IOException;

	/** Đổi trạng thái hàng loạt cho các sản phẩm. */
	ResponseDataDto bulkStatus(List<String> productIds, String status);

	/** Đổi giá hàng loạt: mode = PERCENT (tăng/giảm %), AMOUNT (cộng/trừ số tiền), SET (đặt giá cố định). Áp cho mọi biến thể. */
	ResponseDataDto bulkPrice(List<String> productIds, String mode, BigDecimal value);
}
