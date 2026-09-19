package com.dungochung.shopdongho.enums;

/**
 * Luồng chuẩn: pending -> confirmed -> packing -> shipping -> delivered -> completed.
 * Nhánh phụ: canceled (hủy), refunded (hoàn tiền/trả hàng sau khi giao).
 * {@link #processing} là giá trị CŨ (đã migrate sang confirmed), giữ lại để đọc dữ liệu cũ, không dùng cho đơn mới.
 * Tên viết thường để khớp dữ liệu/enum cột sẵn có trong DB.
 */
public enum OrderStatus {
	pending, @Deprecated processing, confirmed, packing, shipping, delivered, completed, canceled, refunded
}
