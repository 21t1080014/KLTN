package com.dungochung.shopdongho.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.PaginationDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.OrderEntity;
import com.dungochung.shopdongho.entity.OrderItemEntity;
import com.dungochung.shopdongho.entity.PromotionEntity.DiscountType;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.entity.UserVoucherEntity;
import com.dungochung.shopdongho.entity.VoucherEntity;
import com.dungochung.shopdongho.enums.OrderStatus;
import com.dungochung.shopdongho.enums.PaymentMethod;
import com.dungochung.shopdongho.enums.PaymentStatus;
import com.dungochung.shopdongho.repository.OrderItemRepository;
import com.dungochung.shopdongho.repository.OrderRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.repository.UserRepository;
import com.dungochung.shopdongho.repository.UserVoucherRepository;
import com.dungochung.shopdongho.repository.VoucherRepository;
import com.dungochung.shopdongho.service.OrderService;

import jakarta.transaction.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderItemRepository orderItemRepository;

	@Autowired
	private VoucherRepository voucherRepository;

	@Autowired
	private UserVoucherRepository userVoucherRepository;
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Override
	public ResponseDataDto createOrder(String userId, List<OrderItemEntity> items, BigDecimal totalPrice,
			PaymentMethod paymentMethod) {
		// 1. Kiểm tra xem items có null hoặc rỗng không
		if (items == null || items.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Đơn hàng phải có ít nhất 1 sản phẩm", false);
		}

		// 2. Kiểm tra từng orderItem xem có null hay thiếu thông tin quan trọng hay
		// không
		for (OrderItemEntity item : items) {
			if (item == null || item.getProduct() == null || item.getQuantity() <= 0 || item.getPriceEach() == null) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Thông tin sản phẩm trong đơn không hợp lệ", false);
			}
		}
		// Tạo đơn hàng
		OrderEntity order = new OrderEntity();
		order.setUserId(userId);
		order.setTotalPrice(totalPrice);
		order.setPaymentMethod(paymentMethod);
		order.setPaymentStatus(PaymentStatus.pending);
		order.setOrderStatus(OrderStatus.pending);
		order.setCreatedAt(LocalDateTime.now());
		order.setUpdatedAt(LocalDateTime.now());
		order = orderRepository.save(order);

		// Lưu từng order item
		for (OrderItemEntity item : items) {
			item.setOrder(order);
			orderItemRepository.save(item);
		}
		Map<String, Object> data = new HashMap<>();
		data.put("orderId", order.getOrderId());
		ResponseDataDto grantRes = grantVoucherToUser(userId, totalPrice);
		if (grantRes.getResponseCode() == 1 && grantRes.getData() != null
				&& !"Khong có voucher".equals(grantRes.getResponseMsg())) {
			data.put("grantedVoucher", grantRes.getData());
		}
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	@Transactional
	public ResponseDataDto getOrdersByUserId(String userId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<OrderEntity> ordersPage = orderRepository.findByUserId(userId, pageable);

		if (ordersPage.isEmpty()) {
			return new ResponseDataDto(200, "Không có đơn hàng nào.");
		}

		List<Map<String, Object>> orderDataList = ordersPage.stream().map(order -> {
			List<OrderItemEntity> items = orderItemRepository.findByOrder(order);

			List<Map<String, Object>> itemDetails = items.stream().map(item -> {
				Map<String, Object> itemMap = new HashMap<>();
				itemMap.put("productName", item.getProduct().getName());
				itemMap.put("quantity", item.getQuantity());
				itemMap.put("unitPrice", item.getPriceEach());
				itemMap.put("total", item.getPriceEach().multiply(BigDecimal.valueOf(item.getQuantity())));
				return itemMap;
			}).collect(Collectors.toList());

			Map<String, Object> orderMap = new HashMap<>();
			orderMap.put("orderId", order.getOrderId());
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
			orderMap.put("createdAt", order.getCreatedAt().format(formatter));
			orderMap.put("totalPrice", order.getTotalPrice());
			orderMap.put("paymentMethod", order.getPaymentMethod().toString());
			orderMap.put("paymentStatus", order.getPaymentStatus().toString());
			orderMap.put("orderStatus", order.getOrderStatus().toString());
			orderMap.put("items", itemDetails);
			return orderMap;
		}).collect(Collectors.toList());

		// Đóng gói orders và pagination vào một Map
		Map<String, Object> responseData = new HashMap<>();
		responseData.put("orders", orderDataList);
		responseData.put("pagination", PaginationCommon.getPaginationInfo(page, ordersPage.getTotalPages(), 3));

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Lấy danh sách đơn hàng thành công", responseData);
	}

	@Override
	public ResponseDataDto grantVoucherToUser(String userId, BigDecimal totalPrice) {
		LocalDate today = LocalDate.now();

		// Tìm 1 voucher hợp lệ
		Optional<VoucherEntity> optionalVoucher = voucherRepository
				.findFirstByStartAtBeforeAndEndAtAfterAndUsageLimitGreaterThanAndMinOrderAmountLessThanEqual(today,
						today, 0, totalPrice);

		if (optionalVoucher.isEmpty()) {
			return new ResponseDataDto(200, "Khong có voucher");
		}

		VoucherEntity voucher = optionalVoucher.get();

		// Kiểm tra đã được cấp chưa
		boolean exists = userVoucherRepository.existsByUser_UserIdAndVoucher_VoucherCodeAndUsedAtIsNull(userId,
				voucher.getVoucherCode());
		if (exists) {
			return new ResponseDataDto(200, "Đã cấp voucher này cho người dùng.");
		}
		UserEntity user = userRepository.findById(userId).orElse(null);
		// Cấp voucher
		UserVoucherEntity userVoucher = new UserVoucherEntity();
		userVoucher.setUser(user);
		userVoucher.setVoucher(voucher);
		userVoucher.setUsedAt(null);
		userVoucherRepository.save(userVoucher);

		// Cập nhật usage_limit
		voucher.setUsageLimit(voucher.getUsageLimit() - 1);
		voucherRepository.save(voucher);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Đã cấp voucher thành công.", voucher.getVoucherCode());
	}

	@Override
	public ResponseDataDto applyVoucher(String userId, BigDecimal totalPrice, String voucherCode) {
		if (voucherCode == null || voucherCode.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Không có voucher áp dụng", totalPrice);
		}

		Optional<VoucherEntity> optionalVoucher = voucherRepository.findByVoucherCode(voucherCode);
		if (optionalVoucher.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Voucher không tồn tại hoặc đã hết hạn");
		}

		VoucherEntity voucher = optionalVoucher.get();
		LocalDate today = LocalDate.now();

		if (voucher.getStartAt() != null && voucher.getStartAt().isAfter(today)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Voucher chưa đến thời gian sử dụng");
		}

		if (voucher.getEndAt() != null && voucher.getEndAt().isBefore(today)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Voucher đã hết hạn");
		}

		// Tính giảm giá
		BigDecimal discountedPrice = totalPrice;
		if (voucher.getDiscountType() == DiscountType.percent) {
			BigDecimal discount = totalPrice.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100));
			discountedPrice = totalPrice.subtract(discount);
		} else if (voucher.getDiscountType() == DiscountType.amount) {
			discountedPrice = totalPrice.subtract(voucher.getDiscountValue());
		}

		if (discountedPrice.compareTo(BigDecimal.ZERO) < 0) {
			discountedPrice = BigDecimal.ZERO;
		}

		// Tìm userVoucher đã được cấp trước đó
		Optional<UserVoucherEntity> optionalUserVoucher = userVoucherRepository
				.findByUserUserIdAndVoucherVoucherCode(userId, voucherCode);

		if (optionalUserVoucher.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Bạn chưa được cấp mã giảm giá này.");
		}

		UserVoucherEntity userVoucher = optionalUserVoucher.get();
		if (userVoucher.getUsedAt() != null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Mã giảm giá đã được sử dụng.");
		}

		// Đánh dấu đã sử dụng
		userVoucher.setUsedAt(LocalDateTime.now());
		userVoucherRepository.save(userVoucher);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Áp dụng voucher thành công", discountedPrice);
	}

	@Override
	public ResponseDataDto getAllOrder(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<OrderEntity> orderPage = orderRepository.findAll(pageable);

		if (orderPage.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Không có đơn hàng nào.");
		}

		List<Map<String, Object>> orderList = orderPage.getContent().stream().map(order -> {
			Map<String, Object> orderMap = new HashMap<>();
			orderMap.put("orderId", order.getOrderId());
			orderMap.put("userId", order.getUser().getUserId());
			orderMap.put("email", order.getUser().getEmail());
			orderMap.put("totalPrice", order.getTotalPrice());
			orderMap.put("paymentMethod", order.getPaymentMethod().toString());
			orderMap.put("paymentStatus", order.getPaymentStatus().toString());
			orderMap.put("orderStatus", order.getOrderStatus().toString());
			orderMap.put("createdAt", order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
			return orderMap;
		}).collect(Collectors.toList());

		Map<String, Object> data = new HashMap<>();
		data.put("orders", orderList);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, orderPage.getTotalPages(), 3));

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Lấy danh sách đơn hàng thành công", data);
	}

	@Override
	public ResponseDataDto updateOrder(Integer orderId, OrderStatus orderStatus, PaymentStatus paymentStatus){
		Optional<OrderEntity> optionalOrder = orderRepository.findById(orderId);
		if (optionalOrder.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy đơn hàng có ID: " + orderId);
		}

		OrderEntity order = optionalOrder.get();
		order.setOrderStatus(orderStatus);
		order.setPaymentStatus(paymentStatus);
		order.setUpdatedAt(LocalDateTime.now());
		orderRepository.save(order);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Cập nhật đơn hàng thành công", order.getOrderId());
	}

	@Override
	public ResponseDataDto deleteOrder(Integer orderId) {
		Optional<OrderEntity> optionalOrder = orderRepository.findById(orderId);
		if (optionalOrder.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy đơn hàng có ID: " + orderId);
		}

		OrderEntity order = optionalOrder.get();
		if (order.getPaymentStatus() == PaymentStatus.pending && order.getOrderStatus() == OrderStatus.pending) {
			orderRepository.delete(order);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Xóa đơn hàng thành công", orderId);
		} else {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Chỉ được xóa đơn hàng chưa thanh toán và đang chờ xử lý.");
		}
	}

}
