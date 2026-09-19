package com.dungochung.shopdongho.service;

import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.entity.ProductVariantEntity;
import com.dungochung.shopdongho.enums.MovementType;

/**
 * Điểm duy nhất được phép thay đổi số lượng tồn kho: mọi thay đổi đều ghi 1 dòng vào inventory_movements
 * (ai làm, lúc nào, trước/sau).
 */
public interface StockService {
	/** Lấy dòng tồn kho của biến thể, tự tạo (quantity=0) nếu chưa có. */
	InventoryEntity ensureInventory(ProductVariantEntity variant);

	/** Cộng/trừ tồn kho thực (delta có thể âm). Ném IllegalArgumentException nếu kết quả < 0. */
	InventoryEntity adjust(InventoryEntity inventory, int delta, MovementType type, String reference, String note);

	/** Đặt tồn kho thực về giá trị mới (kiểm kê). Ném IllegalArgumentException nếu < số đang giữ. */
	InventoryEntity setQuantity(InventoryEntity inventory, int newQuantity, String note);

	/** Giữ hàng cho đơn mới: tăng reserved. Ném IllegalArgumentException nếu không đủ hàng có thể bán. */
	InventoryEntity reserve(InventoryEntity inventory, int quantity, String reference);

	/** Nhả hàng đang giữ (hủy đơn chưa xuất kho): giảm reserved. */
	InventoryEntity release(InventoryEntity inventory, int quantity, String reference);

	/** Xuất kho khi giao hàng: giảm cả quantity lẫn reserved. */
	InventoryEntity ship(InventoryEntity inventory, int quantity, String reference);

	/** Nhập lại kho khi hàng đã xuất bị trả về. */
	InventoryEntity restock(InventoryEntity inventory, int quantity, String reference);
}
