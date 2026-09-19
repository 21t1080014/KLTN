package com.dungochung.shopdongho.common;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.enums.OrderStatus;
import com.dungochung.shopdongho.enums.PaymentStatus;

/**
 * Luật chuyển trạng thái đơn hàng và thanh toán (máy trạng thái). Đặt tập trung 1 chỗ để service, controller và UI
 * cùng dùng một nguồn sự thật, không cho "nhảy cóc" (vd PENDING thẳng DELIVERED).
 */
public final class OrderWorkflow {
	private OrderWorkflow() {
	}

	private static final Map<OrderStatus, Set<OrderStatus>> NEXT = new EnumMap<>(OrderStatus.class);
	private static final Map<OrderStatus, Set<String>> ROLES_FOR_TARGET = new EnumMap<>(OrderStatus.class);
	private static final Map<PaymentStatus, Set<PaymentStatus>> PAYMENT_NEXT = new EnumMap<>(PaymentStatus.class);

	static {
		NEXT.put(OrderStatus.pending, EnumSet.of(OrderStatus.confirmed, OrderStatus.canceled));
		NEXT.put(OrderStatus.confirmed, EnumSet.of(OrderStatus.packing, OrderStatus.canceled));
		NEXT.put(OrderStatus.packing, EnumSet.of(OrderStatus.shipping, OrderStatus.canceled));
		// Giao thất bại/khách từ chối khi đang vận chuyển => hủy và nhập lại kho
		NEXT.put(OrderStatus.shipping, EnumSet.of(OrderStatus.delivered, OrderStatus.canceled));
		NEXT.put(OrderStatus.delivered, EnumSet.of(OrderStatus.completed, OrderStatus.refunded));
		NEXT.put(OrderStatus.completed, EnumSet.of(OrderStatus.refunded));
		NEXT.put(OrderStatus.canceled, EnumSet.noneOf(OrderStatus.class));
		NEXT.put(OrderStatus.refunded, EnumSet.noneOf(OrderStatus.class));

		// Ai được đưa đơn TỚI trạng thái nào: kho đóng gói/xuất kho, CSKH xác nhận/hủy/giao xong/hoàn, admin tất cả
		Set<String> support = Set.of(Constant.ROLE_ADMIN, Constant.ROLE_SUPPORT_STAFF);
		Set<String> warehouse = Set.of(Constant.ROLE_ADMIN, Constant.ROLE_WAREHOUSE_STAFF);
		ROLES_FOR_TARGET.put(OrderStatus.confirmed, support);
		ROLES_FOR_TARGET.put(OrderStatus.packing, warehouse);
		ROLES_FOR_TARGET.put(OrderStatus.shipping, warehouse);
		ROLES_FOR_TARGET.put(OrderStatus.delivered, support);
		ROLES_FOR_TARGET.put(OrderStatus.completed, support);
		ROLES_FOR_TARGET.put(OrderStatus.canceled, support);
		ROLES_FOR_TARGET.put(OrderStatus.refunded, support);

		PAYMENT_NEXT.put(PaymentStatus.pending, EnumSet.of(PaymentStatus.paid, PaymentStatus.failed));
		PAYMENT_NEXT.put(PaymentStatus.failed, EnumSet.of(PaymentStatus.pending, PaymentStatus.paid));
		PAYMENT_NEXT.put(PaymentStatus.paid, EnumSet.of(PaymentStatus.refund_pending));
		PAYMENT_NEXT.put(PaymentStatus.refund_pending, EnumSet.of(PaymentStatus.refunded, PaymentStatus.paid));
		PAYMENT_NEXT.put(PaymentStatus.refunded, EnumSet.noneOf(PaymentStatus.class));
	}

	/** Trạng thái cũ "processing" tương đương "confirmed". */
	@SuppressWarnings("deprecation")
	public static OrderStatus normalize(OrderStatus s) {
		return s == OrderStatus.processing ? OrderStatus.confirmed : s;
	}

	public static boolean canTransition(OrderStatus from, OrderStatus to) {
		return NEXT.getOrDefault(normalize(from), Set.of()).contains(to);
	}

	public static boolean roleMayMoveTo(String role, OrderStatus to) {
		return role != null && ROLES_FOR_TARGET.getOrDefault(to, Set.of()).contains(role);
	}

	/** Các trạng thái tiếp theo hợp lệ VÀ role này được phép thực hiện. */
	public static List<OrderStatus> allowedNext(OrderStatus from, String role) {
		return NEXT.getOrDefault(normalize(from), Set.of()).stream().filter(t -> roleMayMoveTo(role, t)).toList();
	}

	public static boolean requiresReason(OrderStatus to) {
		return to == OrderStatus.canceled || to == OrderStatus.refunded;
	}

	/** Khách tự hủy chỉ khi đơn chưa được kho xử lý. */
	public static boolean customerMayCancel(OrderStatus from) {
		OrderStatus n = normalize(from);
		return n == OrderStatus.pending || n == OrderStatus.confirmed;
	}

	public static boolean canPaymentTransition(PaymentStatus from, PaymentStatus to) {
		return PAYMENT_NEXT.getOrDefault(from, Set.of()).contains(to);
	}

	public static List<PaymentStatus> allowedPaymentNext(PaymentStatus from) {
		return List.copyOf(PAYMENT_NEXT.getOrDefault(from, Set.of()));
	}

	/** Nhãn tiếng Việt dùng chung cho UI admin/khách. */
	public static Map<String, String> statusLabels() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("pending", "Chờ xác nhận");
		m.put("processing", "Đã xác nhận");
		m.put("confirmed", "Đã xác nhận");
		m.put("packing", "Đang đóng gói");
		m.put("shipping", "Đang giao");
		m.put("delivered", "Đã giao");
		m.put("completed", "Hoàn thành");
		m.put("canceled", "Đã hủy");
		m.put("refunded", "Đã hoàn tiền/trả hàng");
		return m;
	}

	public static Map<String, String> paymentLabels() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("pending", "Chưa thanh toán");
		m.put("paid", "Đã thanh toán");
		m.put("failed", "Thanh toán thất bại/hủy");
		m.put("refund_pending", "Chờ hoàn tiền");
		m.put("refunded", "Đã hoàn tiền");
		return m;
	}
}
