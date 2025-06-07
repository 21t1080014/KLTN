package com.dungochung.shopdongho.service;

import java.math.BigDecimal;
import java.util.List;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.OrderItemEntity;
import com.dungochung.shopdongho.enums.OrderStatus;
import com.dungochung.shopdongho.enums.PaymentMethod;
import com.dungochung.shopdongho.enums.PaymentStatus;

public interface OrderService {
	ResponseDataDto createOrder(String userId, List<OrderItemEntity> items, BigDecimal totalPrice,
			PaymentMethod paymentMethod);

	ResponseDataDto grantVoucherToUser(String userId, BigDecimal totalPrice);

	ResponseDataDto getOrdersByUserId(String userId, int page, int size);

	ResponseDataDto applyVoucher(String userId, BigDecimal totalPrice, String voucherCode);

	ResponseDataDto getAllOrder(int page, int size);

	ResponseDataDto updateOrder(Integer orderId, OrderStatus orderStatus, PaymentStatus paymentStatus);

	ResponseDataDto deleteOrder(Integer orderId);
}
