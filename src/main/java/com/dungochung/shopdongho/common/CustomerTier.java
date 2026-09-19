package com.dungochung.shopdongho.common;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import com.dungochung.shopdongho.enums.OrderStatus;
import com.dungochung.shopdongho.enums.PaymentStatus;

/**
 * Hạng khách hàng suy ra từ tổng chi tiêu (không lưu DB nên luôn khớp dữ liệu đơn hàng).
 * Chi tiêu chỉ tính đơn đã giao/hoàn thành VÀ đã thanh toán; đơn hủy/hoàn tiền không tính.
 * Tên hạng khớp bảng membership_levels có sẵn (Silver/Gold/Diamond); ngưỡng cấu hình ở application.properties.
 */
public enum CustomerTier {
	SILVER("Silver"), GOLD("Gold"), DIAMOND("Diamond");

	/** Các trạng thái đơn được tính vào chi tiêu. */
	public static final Set<OrderStatus> SPEND_STATUSES = EnumSet.of(OrderStatus.delivered, OrderStatus.completed);
	public static final PaymentStatus SPEND_PAYMENT = PaymentStatus.paid;

	private final String label;

	CustomerTier(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static CustomerTier of(BigDecimal spent, BigDecimal goldMin, BigDecimal diamondMin) {
		if (spent == null) {
			return SILVER;
		}
		if (spent.compareTo(diamondMin) >= 0) {
			return DIAMOND;
		}
		if (spent.compareTo(goldMin) >= 0) {
			return GOLD;
		}
		return SILVER;
	}
}
