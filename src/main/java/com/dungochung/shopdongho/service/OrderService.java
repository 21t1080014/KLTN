package com.dungochung.shopdongho.service;

import java.math.BigDecimal;
import java.util.List;

import com.dungochung.shopdongho.common.OrderActor;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.OrderItemEntity;
import com.dungochung.shopdongho.enums.OrderStatus;
import com.dungochung.shopdongho.enums.PaymentMethod;
import com.dungochung.shopdongho.enums.PaymentStatus;

public interface OrderService {
	/** Tạo đơn: kiểm tra sản phẩm ACTIVE, dùng giá phía server, giữ tồn kho (reserve) — thiếu hàng thì từ chối cả đơn. */
	ResponseDataDto createOrder(String userId, List<OrderItemEntity> items, BigDecimal totalPrice,
			PaymentMethod paymentMethod);

	ResponseDataDto grantVoucherToUser(String userId, BigDecimal totalPrice);

	ResponseDataDto getOrdersByUserId(String userId, int page, int size);

	ResponseDataDto applyVoucher(String userId, BigDecimal totalPrice, String voucherCode);

	/** Danh sách đơn cho admin; mỗi đơn kèm các bước chuyển tiếp mà role này được phép làm. */
	ResponseDataDto getAllOrder(int page, int size, String role, OrderStatus statusFilter);

	ResponseDataDto getOrderDetail(Integer orderId, String role);

	/** Đổi trạng thái đơn theo máy trạng thái (không nhảy cóc), kiểm tra role, lý do bắt buộc khi hủy/hoàn, tác động kho + thanh toán, ghi lịch sử. */
	ResponseDataDto changeStatus(Integer orderId, OrderStatus to, String reason, OrderActor actor);

	/** Đổi trạng thái thanh toán theo luật riêng (paid -> refund_pending -> refunded...). */
	ResponseDataDto changePayment(Integer orderId, PaymentStatus to, String note, OrderActor actor);

	/** Khách tự hủy đơn của chính mình khi đơn chưa vào đóng gói. */
	ResponseDataDto cancelByCustomer(String userId, Integer orderId, String reason, String username);

	/** API cũ (gửi cả 2 trạng thái): giờ được chuyển thành các bước đổi trạng thái có kiểm tra. */
	ResponseDataDto updateOrder(Integer orderId, OrderStatus orderStatus, PaymentStatus paymentStatus, OrderActor actor);

	ResponseDataDto deleteOrder(Integer orderId, OrderActor actor);
}
