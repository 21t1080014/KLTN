package com.dungochung.shopdongho.service;

import java.sql.Timestamp;

import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface InventoryService {
	ResponseDataDto getAllInventory(int page, int size);

	ResponseDataDto searchInventory(String productName, Timestamp fromDate, Timestamp toDate, int page, int size);

	// Cập nhật số lượng tồn kho cho một sản phẩms
	ResponseDataDto updateInventoryQuantity(String productId, int quantity);

	// Lấy tồn kho của một sản phẩm cụ thể
	ResponseDataDto getInventoryByProductId(String productId);

	// Cảnh báo tồn kho thấp hơn mức tối thiểu (nếu có logic min_quantity)
	ResponseDataDto getLowStockInventories(int threshold);

	// Lịch sử thay đổi tồn kho (nếu bạn log lại lịch sử vào bảng riêng)
	ResponseDataDto getInventoryChangeHistory(String productId, int page, int size);

	// Xóa dữ liệu tồn kho (nên hạn chế dùng trừ khi gỡ bỏ sản phẩm)
	ResponseDataDto deleteInventory(String productId);
}
