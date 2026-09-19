package com.dungochung.shopdongho.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.dungochung.shopdongho.common.OrderActor;
import com.dungochung.shopdongho.common.OrderWorkflow;
import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.entity.OrderEntity;
import com.dungochung.shopdongho.entity.OrderItemEntity;
import com.dungochung.shopdongho.entity.OrderStatusHistoryEntity;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.ProductVariantEntity;
import com.dungochung.shopdongho.entity.PromotionEntity;
import com.dungochung.shopdongho.entity.PromotionEntity.DiscountType;
import com.dungochung.shopdongho.entity.PromotionProductEntity;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.entity.UserVoucherEntity;
import com.dungochung.shopdongho.entity.VoucherEntity;
import com.dungochung.shopdongho.enums.OrderHistoryKind;
import com.dungochung.shopdongho.enums.OrderStatus;
import com.dungochung.shopdongho.enums.PaymentMethod;
import com.dungochung.shopdongho.enums.PaymentStatus;
import com.dungochung.shopdongho.enums.ProductStatus;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.repository.OrderItemRepository;
import com.dungochung.shopdongho.repository.OrderRepository;
import com.dungochung.shopdongho.repository.OrderStatusHistoryRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.repository.ProductVariantRepository;
import com.dungochung.shopdongho.repository.UserRepository;
import com.dungochung.shopdongho.repository.UserVoucherRepository;
import com.dungochung.shopdongho.repository.VoucherRepository;
import com.dungochung.shopdongho.service.OrderService;
import com.dungochung.shopdongho.service.StockService;

import jakarta.transaction.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

	/** Phí ship tối đa mà giao diện đang áp dụng (miễn phí từ 500.000₫), dùng để chặn tổng tiền bị đội lên. */
	private static final BigDecimal MAX_SHIPPING_FEE = BigDecimal.valueOf(30000);
	private static final int MAX_REASON_LENGTH = 255;

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

	@Autowired
	private ProductVariantRepository variantRepository;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private StockService stockService;

	@Autowired
	private OrderStatusHistoryRepository historyRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private com.dungochung.shopdongho.service.AuditService auditService;

	// ------------------------------------------------------------------ tạo đơn

	@Override
	public ResponseDataDto createOrder(String userId, List<OrderItemEntity> items, BigDecimal totalPrice,
			PaymentMethod paymentMethod) {
		if (items == null || items.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Đơn hàng phải có ít nhất 1 sản phẩm", false);
		}
		if (paymentMethod == null || totalPrice == null || totalPrice.signum() < 0) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Thông tin thanh toán không hợp lệ", false);
		}
		for (OrderItemEntity item : items) {
			if (item == null || item.getProduct() == null || item.getProduct().getProductId() == null
					|| item.getQuantity() <= 0 || item.getPriceEach() == null) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Thông tin sản phẩm trong đơn không hợp lệ", false);
			}
		}
		// Session có thể còn sống sau khi tài khoản bị khóa nên luôn kiểm tra lại trạng thái trong DB
		UserEntity buyer = userRepository.findById(userId).orElse(null);
		if (buyer == null || buyer.getStatus() != com.dungochung.shopdongho.enums.UserStatus.ACTIVE) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Tài khoản của bạn đã bị khóa hoặc chưa kích hoạt, không thể đặt hàng. Vui lòng liên hệ cửa hàng.", false);
		}
		try {
			return transactionTemplate.execute(status -> doCreateOrder(userId, items, totalPrice, paymentMethod));
		} catch (IllegalArgumentException e) {
			// Tranh nhau hàng cuối cùng: giữ hàng thất bại thì cả đơn được rollback
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, e.getMessage(), false);
		}
	}

	private ResponseDataDto doCreateOrder(String userId, List<OrderItemEntity> items, BigDecimal totalPrice,
			PaymentMethod paymentMethod) {
		// Gộp các dòng trùng sản phẩm (giữ thứ tự) để giữ hàng đúng tổng số lượng
		Map<String, Integer> qtyByProduct = new LinkedHashMap<>();
		Map<String, BigDecimal> clientPrice = new HashMap<>();
		for (OrderItemEntity item : items) {
			String pid = item.getProduct().getProductId();
			qtyByProduct.merge(pid, item.getQuantity(), Integer::sum);
			clientPrice.putIfAbsent(pid, item.getPriceEach());
		}

		// Bước 1: kiểm tra toàn bộ (còn bán, đủ hàng, giá) — chưa ghi gì, thiếu 1 món là từ chối cả đơn
		record Line(ProductEntity product, ProductVariantEntity variant, InventoryEntity inventory, int qty,
				BigDecimal unitPrice) {
		}
		List<Line> lines = new ArrayList<>();
		BigDecimal subtotal = BigDecimal.ZERO;
		for (Map.Entry<String, Integer> e : qtyByProduct.entrySet()) {
			ProductEntity product = productRepository.findById(e.getKey()).orElse(null);
			if (product == null || product.getStatus() != ProductStatus.ACTIVE) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL,
						"Sản phẩm " + e.getKey() + " không tồn tại hoặc đã ngừng kinh doanh", false);
			}
			ProductVariantEntity variant = variantRepository
					.findByProduct_ProductIdAndIsDefaultTrue(product.getProductId()).orElse(null);
			if (variant == null || variant.getStatus() != ProductStatus.ACTIVE) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL,
						"Sản phẩm " + product.getName() + " hiện không bán được", false);
			}
			InventoryEntity inv = stockService.ensureInventory(variant);
			if (inv.getAvailableQuantity() < e.getValue()) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Sản phẩm " + product.getName()
						+ " không đủ hàng (còn " + inv.getAvailableQuantity() + ", bạn đặt " + e.getValue() + ")", false);
			}
			BigDecimal unit = serverUnitPrice(product);
			BigDecimal sent = clientPrice.get(e.getKey());
			// Không cho khách tự hạ giá: giá gửi lên thấp hơn giá thật (quá 1đ làm tròn) thì từ chối
			if (sent.add(BigDecimal.ONE).compareTo(unit) < 0) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL,
						"Giá sản phẩm " + product.getName() + " đã thay đổi, vui lòng tải lại giỏ hàng", false);
			}
			subtotal = subtotal.add(unit.multiply(BigDecimal.valueOf(e.getValue())));
			lines.add(new Line(product, variant, inv, e.getValue(), unit));
		}
		// Tổng tiền (đã trừ voucher ở client) không được vượt tổng thật + phí ship tối đa
		if (totalPrice.compareTo(subtotal.add(MAX_SHIPPING_FEE)) > 0) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Tổng tiền đơn hàng không hợp lệ", false);
		}

		// Bước 2: tạo đơn + giữ hàng
		OrderEntity order = new OrderEntity();
		order.setUserId(userId);
		order.setTotalPrice(totalPrice);
		order.setPaymentMethod(paymentMethod);
		order.setPaymentStatus(PaymentStatus.pending);
		order.setOrderStatus(OrderStatus.pending);
		order.setStockReserved(true);
		order.setCreatedAt(LocalDateTime.now());
		order.setUpdatedAt(LocalDateTime.now());
		order = orderRepository.save(order);

		String ref = "ORDER#" + order.getOrderId();
		for (Line l : lines) {
			OrderItemEntity oi = new OrderItemEntity();
			oi.setOrder(order);
			oi.setProduct(l.product());
			oi.setVariant(l.variant());
			oi.setQuantity(l.qty());
			oi.setPriceEach(l.unitPrice());
			orderItemRepository.save(oi);
			stockService.reserve(l.inventory(), l.qty(), ref);
		}
		addHistory(order.getOrderId(), OrderHistoryKind.CREATED, null, OrderStatus.pending.name(), null,
				OrderActor.customer(userId));

		Map<String, Object> data = new HashMap<>();
		data.put("orderId", order.getOrderId());
		ResponseDataDto grantRes = grantVoucherToUser(userId, totalPrice);
		if (grantRes.getResponseCode() == 1 && grantRes.getData() != null
				&& !"Khong có voucher".equals(grantRes.getResponseMsg())) {
			data.put("grantedVoucher", grantRes.getData());
		}
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	/** Giá bán thật ở server: giá sản phẩm trừ khuyến mãi đang hiệu lực (cùng cách tính với trang khách). */
	private BigDecimal serverUnitPrice(ProductEntity product) {
		BigDecimal price = product.getPrice();
		if (product.getPromotionProducts() == null) {
			return price;
		}
		LocalDateTime now = LocalDateTime.now();
		for (PromotionProductEntity ppe : product.getPromotionProducts()) {
			PromotionEntity promo = ppe.getPromotion();
			if (promo != null && Boolean.TRUE.equals(promo.getIsActive()) && promo.getStartAt() != null
					&& promo.getStartAt().isBefore(now) && promo.getEndAt() != null && promo.getEndAt().isAfter(now)) {
				BigDecimal discounted = price;
				if (promo.getDiscountType() == DiscountType.percent) {
					discounted = price.subtract(price.multiply(promo.getDiscountValue()).divide(BigDecimal.valueOf(100)));
				} else if (promo.getDiscountType() == DiscountType.amount) {
					discounted = price.subtract(promo.getDiscountValue());
				}
				return discounted.max(BigDecimal.ZERO);
			}
		}
		return price;
	}

	// ------------------------------------------------------------------ khách xem đơn

	@Override
	@Transactional
	public ResponseDataDto getOrdersByUserId(String userId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<OrderEntity> ordersPage = orderRepository.findByUserId(userId, pageable);

		if (ordersPage.isEmpty()) {
			return new ResponseDataDto(200, "Không có đơn hàng nào.");
		}

		Map<String, String> statusLabels = OrderWorkflow.statusLabels();
		Map<String, String> paymentLabels = OrderWorkflow.paymentLabels();
		List<Map<String, Object>> orderDataList = ordersPage.stream().map(order -> {
			List<OrderItemEntity> items = orderItemRepository.findByOrder(order);

			List<Map<String, Object>> itemDetails = items.stream().map(item -> {
				Map<String, Object> itemMap = new HashMap<>();
				itemMap.put("productName", item.getProduct().getName());
				itemMap.put("quantity", item.getQuantity());
				itemMap.put("unitPrice", item.getPriceEach());
				itemMap.put("total", item.getPriceEach().multiply(BigDecimal.valueOf(item.getQuantity())));
				return itemMap;
			}).toList();

			Map<String, Object> orderMap = new HashMap<>();
			orderMap.put("orderId", order.getOrderId());
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
			orderMap.put("createdAt", order.getCreatedAt().format(formatter));
			orderMap.put("totalPrice", order.getTotalPrice());
			orderMap.put("paymentMethod", order.getPaymentMethod().toString());
			orderMap.put("paymentStatus", order.getPaymentStatus().toString());
			orderMap.put("paymentStatusLabel", paymentLabels.get(order.getPaymentStatus().name()));
			orderMap.put("orderStatus", order.getOrderStatus().toString());
			orderMap.put("orderStatusLabel", statusLabels.get(order.getOrderStatus().name()));
			orderMap.put("canCancel", OrderWorkflow.customerMayCancel(order.getOrderStatus()));
			orderMap.put("cancelReason", order.getCancelReason());
			orderMap.put("items", itemDetails);
			return orderMap;
		}).toList();

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

	// ------------------------------------------------------------------ admin: danh sách / chi tiết

	@Override
	@Transactional
	public ResponseDataDto getAllOrder(int page, int size, String role, OrderStatus statusFilter) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<OrderEntity> orderPage = statusFilter == null ? orderRepository.findAll(pageable)
				: orderRepository.findByOrderStatus(statusFilter, pageable);

		if (orderPage.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Không có đơn hàng nào.");
		}

		List<Map<String, Object>> orderList = orderPage.getContent().stream().map(order -> {
			Map<String, Object> m = orderSummary(order, role);
			m.put("userId", order.getUser().getUserId());
			m.put("email", order.getUser().getEmail());
			m.put("createdAt", order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
			return m;
		}).toList();

		Map<String, Object> data = new HashMap<>();
		data.put("orders", orderList);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, orderPage.getTotalPages(), 3));
		data.put("statusLabels", OrderWorkflow.statusLabels());
		data.put("paymentLabels", OrderWorkflow.paymentLabels());

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Lấy danh sách đơn hàng thành công", data);
	}

	private Map<String, Object> orderSummary(OrderEntity order, String role) {
		Map<String, Object> m = new HashMap<>();
		m.put("orderId", order.getOrderId());
		m.put("totalPrice", order.getTotalPrice());
		m.put("paymentMethod", order.getPaymentMethod().toString());
		m.put("paymentStatus", order.getPaymentStatus().toString());
		m.put("orderStatus", order.getOrderStatus().toString());
		m.put("cancelReason", order.getCancelReason());
		m.put("cancelledBy", order.getCancelledBy());
		m.put("cancelledByName", order.getCancelledByName());
		m.put("stockReserved", order.isStockReserved());
		m.put("allowedNext", OrderWorkflow.allowedNext(order.getOrderStatus(), role).stream().map(Enum::name).toList());
		m.put("allowedPayment", canEditPayment(role) ? allowedPaymentFor(order).stream().map(Enum::name).toList()
				: List.of());
		return m;
	}

	private static boolean canEditPayment(String role) {
		return Constant.ROLE_ADMIN.equals(role) || Constant.ROLE_SUPPORT_STAFF.equals(role);
	}

	/** Các trạng thái thanh toán có thể chuyển tới: theo máy trạng thái thanh toán + ngữ cảnh đơn (hoàn tiền chỉ khi đơn đã hủy/hoàn). */
	private List<PaymentStatus> allowedPaymentFor(OrderEntity order) {
		OrderStatus st = OrderWorkflow.normalize(order.getOrderStatus());
		boolean closed = st == OrderStatus.canceled || st == OrderStatus.refunded;
		return OrderWorkflow.allowedPaymentNext(order.getPaymentStatus()).stream().filter(p -> {
			if (p == PaymentStatus.refund_pending || p == PaymentStatus.refunded) {
				return closed;
			}
			if (p == PaymentStatus.paid || p == PaymentStatus.pending) {
				// Đơn đã hủy/hoàn thì không thu tiền hay mở lại thanh toán nữa
				return !closed;
			}
			return true;
		}).toList();
	}

	@Override
	@Transactional
	public ResponseDataDto getOrderDetail(Integer orderId, String role) {
		OrderEntity order = orderRepository.findById(orderId).orElse(null);
		if (order == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy đơn hàng có ID: " + orderId);
		}
		Map<String, Object> data = orderSummary(order, role);
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
		data.put("createdAt", order.getCreatedAt().format(fmt));
		UserEntity u = order.getUser();
		if (u != null) {
			Map<String, Object> customer = new HashMap<>();
			customer.put("userId", u.getUserId());
			customer.put("fullName", u.getFullName());
			customer.put("email", u.getEmail());
			customer.put("phone", u.getPhone());
			customer.put("address", u.getAddress());
			data.put("customer", customer);
		}
		List<Map<String, Object>> items = new ArrayList<>();
		for (OrderItemEntity oi : orderItemRepository.findByOrder(order)) {
			Map<String, Object> im = new HashMap<>();
			im.put("productId", oi.getProduct().getProductId());
			im.put("productName", oi.getProduct().getName());
			ProductVariantEntity v = oi.getVariant();
			im.put("sku", v != null ? v.getSku() : oi.getProduct().getSku());
			im.put("variantName", v != null ? v.getVariantName() : null);
			im.put("quantity", oi.getQuantity());
			im.put("unitPrice", oi.getPriceEach());
			im.put("total", oi.getPriceEach().multiply(BigDecimal.valueOf(oi.getQuantity())));
			items.add(im);
		}
		data.put("items", items);
		List<Map<String, Object>> history = new ArrayList<>();
		for (OrderStatusHistoryEntity h : historyRepository.findByOrderIdOrderByHistoryIdAsc(orderId)) {
			Map<String, Object> hm = new HashMap<>();
			hm.put("kind", h.getKind().name());
			hm.put("from", h.getFromValue());
			hm.put("to", h.getToValue());
			hm.put("reason", h.getReason());
			hm.put("by", h.getChangedBy());
			hm.put("role", h.getChangedByRole());
			hm.put("at", h.getChangedAt().format(fmt));
			history.add(hm);
		}
		data.put("history", history);
		data.put("statusLabels", OrderWorkflow.statusLabels());
		data.put("paymentLabels", OrderWorkflow.paymentLabels());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "OK", data);
	}

	// ------------------------------------------------------------------ đổi trạng thái

	@Override
	public ResponseDataDto changeStatus(Integer orderId, OrderStatus to, String reason, OrderActor actor) {
		try {
			return transactionTemplate.execute(status -> doChangeStatus(orderId, to, reason, actor));
		} catch (IllegalArgumentException | IllegalStateException e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, e.getMessage());
		}
	}

	private ResponseDataDto doChangeStatus(Integer orderId, OrderStatus to, String reason, OrderActor actor) {
		if (to == null) {
			return fail("Thiếu trạng thái đích");
		}
		OrderEntity order = orderRepository.findById(orderId).orElse(null);
		if (order == null) {
			return fail("Không tìm thấy đơn hàng có ID: " + orderId);
		}
		OrderStatus from = OrderWorkflow.normalize(order.getOrderStatus());
		if (from == to) {
			return fail("Đơn hàng đã ở trạng thái này");
		}
		if (!OrderWorkflow.canTransition(from, to)) {
			return fail("Không thể chuyển đơn từ \"" + label(from) + "\" sang \"" + label(to)
					+ "\" (không được nhảy cóc hoặc đi ngược trạng thái)");
		}
		boolean customerCancel = "CUSTOMER".equals(actor.type());
		if (!customerCancel && !OrderWorkflow.roleMayMoveTo(actor.role(), to)) {
			return fail("Vai trò " + actor.role() + " không có quyền chuyển đơn sang \"" + label(to) + "\"");
		}
		String cleanReason = reason == null ? null : reason.trim();
		if (cleanReason != null && cleanReason.isEmpty()) {
			cleanReason = null;
		}
		if (OrderWorkflow.requiresReason(to) && cleanReason == null) {
			return fail("Vui lòng nhập lý do khi " + (to == OrderStatus.canceled ? "hủy đơn" : "hoàn tiền/trả hàng"));
		}
		if (cleanReason != null && cleanReason.length() > MAX_REASON_LENGTH) {
			return fail("Lý do quá dài (tối đa " + MAX_REASON_LENGTH + " ký tự)");
		}
		// Đơn thanh toán online phải được thu tiền trước khi kho đóng gói; hoàn thành đơn cũng phải đã thu tiền
		if (to == OrderStatus.packing && order.getPaymentMethod() != PaymentMethod.COD
				&& order.getPaymentStatus() != PaymentStatus.paid) {
			return fail("Đơn thanh toán online phải ở trạng thái \"Đã thanh toán\" trước khi đóng gói");
		}
		if (to == OrderStatus.completed && order.getPaymentStatus() != PaymentStatus.paid
				&& !(order.getPaymentMethod() == PaymentMethod.COD)) {
			return fail("Đơn chưa thanh toán không thể hoàn thành");
		}

		String ref = "ORDER#" + orderId;
		if (order.isStockReserved()) {
			applyStockEffect(order, from, to, ref);
		}

		order.setOrderStatus(to);
		order.setUpdatedAt(LocalDateTime.now());
		if (to == OrderStatus.canceled || to == OrderStatus.refunded) {
			order.setCancelReason(cleanReason);
			order.setCancelledBy(customerCancel ? "CUSTOMER" : "ADMIN");
			order.setCancelledByName(actor.username());
			order.setCancelledAt(LocalDateTime.now());
		}
		orderRepository.save(order);
		addHistory(orderId, OrderHistoryKind.STATUS, from.name(), to.name(), cleanReason, actor);

		// Tác động tới thanh toán
		PaymentStatus pay = order.getPaymentStatus();
		if (to == OrderStatus.delivered && pay == PaymentStatus.pending && order.getPaymentMethod() == PaymentMethod.COD) {
			// COD: shipper đã thu tiền khi giao
			autoPayment(order, PaymentStatus.paid, "Thu tiền khi giao hàng (COD)", actor);
		} else if ((to == OrderStatus.canceled || to == OrderStatus.refunded) && pay == PaymentStatus.paid) {
			autoPayment(order, PaymentStatus.refund_pending, "Cần hoàn tiền cho khách", actor);
		} else if (to == OrderStatus.canceled && pay == PaymentStatus.pending) {
			autoPayment(order, PaymentStatus.failed, "Đơn đã hủy, không thu tiền", actor);
		}
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Đã chuyển đơn sang \"" + label(to) + "\"", orderId);
	}

	/** Kho: giữ hàng lúc đặt; xuất kho khi giao cho vận chuyển; hủy trước khi xuất thì nhả hàng; hủy/hoàn sau khi xuất thì nhập lại kho. */
	private void applyStockEffect(OrderEntity order, OrderStatus from, OrderStatus to, String ref) {
		List<OrderItemEntity> items = orderItemRepository.findByOrder(order);
		for (OrderItemEntity oi : items) {
			InventoryEntity inv = inventoryFor(oi);
			if (inv == null) {
				continue;
			}
			if (to == OrderStatus.shipping) {
				stockService.ship(inv, oi.getQuantity(), ref);
			} else if (to == OrderStatus.canceled) {
				if (from == OrderStatus.shipping) {
					stockService.restock(inv, oi.getQuantity(), ref);
				} else {
					stockService.release(inv, oi.getQuantity(), ref);
				}
			} else if (to == OrderStatus.refunded) {
				stockService.restock(inv, oi.getQuantity(), ref);
			}
		}
	}

	private InventoryEntity inventoryFor(OrderItemEntity oi) {
		ProductVariantEntity v = oi.getVariant();
		if (v == null) {
			v = variantRepository.findByProduct_ProductIdAndIsDefaultTrue(oi.getProduct().getProductId()).orElse(null);
		}
		return v == null ? null : stockService.ensureInventory(v);
	}

	private void autoPayment(OrderEntity order, PaymentStatus to, String note, OrderActor actor) {
		PaymentStatus from = order.getPaymentStatus();
		order.setPaymentStatus(to);
		orderRepository.save(order);
		addHistory(order.getOrderId(), OrderHistoryKind.PAYMENT, from.name(), to.name(), note, actor);
	}

	@Override
	public ResponseDataDto changePayment(Integer orderId, PaymentStatus to, String note, OrderActor actor) {
		try {
			return transactionTemplate.execute(status -> doChangePayment(orderId, to, note, actor));
		} catch (IllegalArgumentException | IllegalStateException e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, e.getMessage());
		}
	}

	private ResponseDataDto doChangePayment(Integer orderId, PaymentStatus to, String note, OrderActor actor) {
		if (to == null) {
			return fail("Thiếu trạng thái thanh toán");
		}
		if (!canEditPayment(actor.role())) {
			return fail("Vai trò " + actor.role() + " không có quyền đổi trạng thái thanh toán");
		}
		OrderEntity order = orderRepository.findById(orderId).orElse(null);
		if (order == null) {
			return fail("Không tìm thấy đơn hàng có ID: " + orderId);
		}
		PaymentStatus from = order.getPaymentStatus();
		if (from == to) {
			return fail("Đơn hàng đã ở trạng thái thanh toán này");
		}
		if (!OrderWorkflow.canPaymentTransition(from, to) || !allowedPaymentFor(order).contains(to)) {
			return fail("Không thể chuyển thanh toán từ \"" + OrderWorkflow.paymentLabels().get(from.name())
					+ "\" sang \"" + OrderWorkflow.paymentLabels().get(to.name()) + "\" khi đơn ở trạng thái \""
					+ label(order.getOrderStatus()) + "\"");
		}
		String clean = note == null || note.isBlank() ? null : note.trim();
		if (clean != null && clean.length() > MAX_REASON_LENGTH) {
			return fail("Ghi chú quá dài (tối đa " + MAX_REASON_LENGTH + " ký tự)");
		}
		order.setPaymentStatus(to);
		order.setUpdatedAt(LocalDateTime.now());
		orderRepository.save(order);
		addHistory(orderId, OrderHistoryKind.PAYMENT, from.name(), to.name(), clean, actor);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Đã cập nhật thanh toán", orderId);
	}

	@Override
	public ResponseDataDto cancelByCustomer(String userId, Integer orderId, String reason, String username) {
		OrderEntity order = orderRepository.findById(orderId).orElse(null);
		// Không lộ việc đơn tồn tại nhưng thuộc người khác (chống IDOR)
		if (order == null || !order.getUserId().equals(userId)) {
			return fail("Không tìm thấy đơn hàng");
		}
		if (!OrderWorkflow.customerMayCancel(order.getOrderStatus())) {
			return fail("Đơn hàng đã được xử lý, bạn không thể tự hủy. Vui lòng liên hệ cửa hàng.");
		}
		String r = reason == null || reason.isBlank() ? "Khách hàng hủy đơn" : reason;
		return changeStatus(orderId, OrderStatus.canceled, r, OrderActor.customer(username));
	}

	@Override
	public ResponseDataDto updateOrder(Integer orderId, OrderStatus orderStatus, PaymentStatus paymentStatus,
			OrderActor actor) {
		// API cũ gửi cả 2 giá trị: chỉ áp dụng phần thực sự thay đổi, mỗi phần đều qua kiểm tra luồng
		OrderEntity order = orderRepository.findById(orderId).orElse(null);
		if (order == null) {
			return fail("Không tìm thấy đơn hàng có ID: " + orderId);
		}
		ResponseDataDto res = new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Không có thay đổi", orderId);
		if (orderStatus != null && OrderWorkflow.normalize(orderStatus) != OrderWorkflow.normalize(order.getOrderStatus())) {
			res = changeStatus(orderId, OrderWorkflow.normalize(orderStatus), null, actor);
			if (res.getResponseCode() != Constant.RESULT_CD_SUCCESS) {
				return res;
			}
		}
		if (paymentStatus != null) {
			OrderEntity fresh = orderRepository.findById(orderId).orElse(null);
			if (fresh != null && fresh.getPaymentStatus() != paymentStatus) {
				res = changePayment(orderId, paymentStatus, null, actor);
			}
		}
		return res;
	}

	@Override
	public ResponseDataDto deleteOrder(Integer orderId, OrderActor actor) {
		try {
			return transactionTemplate.execute(status -> doDeleteOrder(orderId, actor));
		} catch (IllegalArgumentException | IllegalStateException e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, e.getMessage());
		}
	}

	private ResponseDataDto doDeleteOrder(Integer orderId, OrderActor actor) {
		OrderEntity order = orderRepository.findById(orderId).orElse(null);
		if (order == null) {
			return fail("Không tìm thấy đơn hàng có ID: " + orderId);
		}
		if (!Constant.ROLE_ADMIN.equals(actor.role())) {
			return fail("Chỉ quản trị viên mới được xóa đơn hàng");
		}
		if (order.getPaymentStatus() == PaymentStatus.pending && order.getOrderStatus() == OrderStatus.pending) {
			if (order.isStockReserved()) {
				String ref = "ORDER#" + orderId;
				for (OrderItemEntity oi : orderItemRepository.findByOrder(order)) {
					InventoryEntity inv = inventoryFor(oi);
					if (inv != null) {
						stockService.release(inv, oi.getQuantity(), ref);
					}
				}
			}
			orderItemRepository.deleteAll(orderItemRepository.findByOrder(order));
			orderRepository.delete(order);
			addHistory(orderId, OrderHistoryKind.STATUS, OrderStatus.pending.name(), "DELETED", "Xóa đơn", actor);
			auditService.log("ORDER_DELETE", "ORDER", String.valueOf(orderId), "total=" + order.getTotalPrice());
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Xóa đơn hàng thành công", orderId);
		}
		return fail("Chỉ được xóa đơn hàng chưa thanh toán và đang chờ xử lý. Các đơn khác hãy dùng chức năng Hủy đơn.");
	}

	// ------------------------------------------------------------------ helpers

	private void addHistory(Integer orderId, OrderHistoryKind kind, String from, String to, String reason,
			OrderActor actor) {
		OrderStatusHistoryEntity h = new OrderStatusHistoryEntity();
		h.setOrderId(orderId);
		h.setKind(kind);
		h.setFromValue(from);
		h.setToValue(to);
		h.setReason(reason);
		h.setChangedBy(actor.username());
		h.setChangedByRole(actor.role());
		historyRepository.save(h);
	}

	private static ResponseDataDto fail(String msg) {
		return new ResponseDataDto(Constant.RESULT_CD_FAIL, msg);
	}

	private static String label(OrderStatus s) {
		return OrderWorkflow.statusLabels().getOrDefault(s.name(), s.name());
	}

}
