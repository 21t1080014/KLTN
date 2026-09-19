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
	ResponseDataDto getLowStockInventories(Integer threshold);

	// Lịch sử thay đổi tồn kho (nếu bạn log lại lịch sử vào bảng riêng)
	ResponseDataDto getInventoryChangeHistory(String productId, int page, int size);

	// Xóa dòng tồn kho của biến thể mặc định (chỉ khi tồn = 0 và không giữ hàng)
	ResponseDataDto deleteInventory(String productId);

	// ---- Theo biến thể ----
	ResponseDataDto getVariantInventory(Long variantId);

	// Kiểm kê: đặt lại tồn thực của biến thể (ghi lịch sử), tùy chọn cập nhật ngưỡng cảnh báo
	ResponseDataDto updateVariantStock(Long variantId, int quantity, String note, Integer lowStockThreshold);

	ResponseDataDto deleteVariantInventory(Long variantId);

	ResponseDataDto getMovements(Long variantId, String productId, int page, int size);
}
