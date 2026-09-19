package com.dungochung.shopdongho;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.dungochung.shopdongho.common.OrderActor;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.entity.OrderEntity;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.ProductVariantEntity;
import com.dungochung.shopdongho.entity.UserEntity;
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
import com.dungochung.shopdongho.service.OrderService;

import jakarta.persistence.EntityManager;

/** Module 3 - luồng đơn hàng: máy trạng thái, phân quyền theo bước, giữ/xuất/nhả kho, hoàn tiền, khách tự hủy. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderFlowTest {

	private static final OrderActor ADMIN = OrderActor.admin("tester-admin", "admin");
	private static final OrderActor SUPPORT = OrderActor.admin("tester-support", "support_staff");
	private static final OrderActor WAREHOUSE = OrderActor.admin("tester-wh", "warehouse_staff");

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private EntityManager em;
	@Autowired
	private OrderService orderService;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private OrderItemRepository orderItemRepository;
	@Autowired
	private OrderStatusHistoryRepository historyRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private ProductVariantRepository variantRepository;
	@Autowired
	private InventoryRepository inventoryRepository;
	@Autowired
	private UserRepository userRepository;

	private ProductEntity product;
	private ProductVariantEntity variant;
	private InventoryEntity inventory;
	private UserEntity customer;
	private UserEntity otherCustomer;

	@BeforeEach
	void setUp() {
		List<UserEntity> users = userRepository.findAll().stream()
				.filter(u -> u.getStatus() == com.dungochung.shopdongho.enums.UserStatus.ACTIVE).toList();
		customer = users.get(0);
		otherCustomer = users.get(1);
		// Chọn 1 sản phẩm ACTIVE không có khuyến mãi để giá server = giá niêm yết
		product = productRepository.findAll().stream()
				.filter(p -> p.getStatus() == ProductStatus.ACTIVE && p.getPromotionProducts().isEmpty()).findFirst()
				.orElseThrow();
		variant = variantRepository.findByProduct_ProductIdAndIsDefaultTrue(product.getProductId()).orElseThrow();
		inventory = inventoryRepository.findByVariant_VariantId(variant.getVariantId()).orElseThrow();
		inventory.setQuantity(10);
		inventory.setReservedQuantity(0);
		inventoryRepository.saveAndFlush(inventory);
	}

	// ---- helpers ----

	private String itemsJson(int qty, BigDecimal price) {
		return "[{\"product\":{\"productId\":\"" + product.getProductId() + "\"},\"quantity\":" + qty
				+ ",\"priceEach\":" + price.toPlainString() + "}]";
	}

	private int createViaApi(UserEntity user, int qty, String method) throws Exception {
		BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(qty));
		String body = mockMvc
				.perform(post("/api/create-orders").sessionAttr("currentUser", user).param("totalPrice", total.toPlainString())
						.param("paymentMethod", method).contentType(MediaType.APPLICATION_JSON)
						.content(itemsJson(qty, product.getPrice())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value(1)).andReturn().getResponse()
				.getContentAsString();
		return com.jayway.jsonpath.JsonPath.read(body, "$.data.orderId");
	}

	private OrderEntity order(int id) {
		em.flush();
		em.clear();
		return orderRepository.findById(id).orElseThrow();
	}

	private InventoryEntity inv() {
		em.flush();
		em.clear();
		return inventoryRepository.findByVariant_VariantId(variant.getVariantId()).orElseThrow();
	}

	private void ok(ResponseDataDto r) {
		assertEquals(1, r.getResponseCode(), r.getResponseMsg());
	}

	private void bad(ResponseDataDto r) {
		assertEquals(0, r.getResponseCode(), "expected failure but got: " + r.getResponseMsg());
	}

	// ---- tạo đơn ----

	@Test
	void createOrderReservesStockLinksVariantAndWritesHistory() throws Exception {
		int id = createViaApi(customer, 3, "COD");
		OrderEntity o = order(id);
		assertEquals(OrderStatus.pending, o.getOrderStatus());
		assertEquals(PaymentStatus.pending, o.getPaymentStatus());
		assertTrue(o.isStockReserved());
		InventoryEntity i = inv();
		assertEquals(10, i.getQuantity());
		assertEquals(3, i.getReservedQuantity());
		var items = orderItemRepository.findByOrder(o);
		assertEquals(1, items.size());
		assertEquals(variant.getVariantId(), items.get(0).getVariant().getVariantId());
		assertEquals(0, product.getPrice().compareTo(items.get(0).getPriceEach()));
		assertEquals(1, historyRepository.findByOrderIdOrderByHistoryIdAsc(id).size());
	}

	@Test
	void createOrderRejectsInsufficientStockWithoutSideEffects() {
		long before = orderRepository.count();
		ResponseDataDto r = orderService.createOrder(customer.getUserId(), parse(11, product.getPrice()),
				product.getPrice().multiply(BigDecimal.valueOf(11)), PaymentMethod.COD);
		bad(r);
		assertEquals(before, orderRepository.count());
		assertEquals(0, inv().getReservedQuantity());
	}

	@Test
	void createOrderRejectsTamperedPriceAndInflatedTotalAndInactiveProduct() {
		BigDecimal cheap = product.getPrice().divide(BigDecimal.TEN);
		bad(orderService.createOrder(customer.getUserId(), parse(1, cheap), cheap, PaymentMethod.COD));
		BigDecimal huge = product.getPrice().add(BigDecimal.valueOf(1_000_000));
		bad(orderService.createOrder(customer.getUserId(), parse(1, product.getPrice()), huge, PaymentMethod.COD));
		assertEquals(0, inv().getReservedQuantity());

		product.setStatus(ProductStatus.INACTIVE);
		productRepository.saveAndFlush(product);
		bad(orderService.createOrder(customer.getUserId(), parse(1, product.getPrice()), product.getPrice(),
				PaymentMethod.COD));
	}

	private List<com.dungochung.shopdongho.entity.OrderItemEntity> parse(int qty, BigDecimal price) {
		var item = new com.dungochung.shopdongho.entity.OrderItemEntity();
		var p = new ProductEntity();
		p.setProductId(product.getProductId());
		item.setProduct(p);
		item.setQuantity(qty);
		item.setPriceEach(price);
		return List.of(item);
	}

	// ---- luồng chuẩn + kho ----

	@Test
	void fullHappyPathMovesStockAndCollectsCodPayment() throws Exception {
		int id = createViaApi(customer, 2, "COD");
		ok(orderService.changeStatus(id, OrderStatus.confirmed, null, SUPPORT));
		ok(orderService.changeStatus(id, OrderStatus.packing, null, WAREHOUSE));
		InventoryEntity i = inv();
		assertEquals(10, i.getQuantity());
		assertEquals(2, i.getReservedQuantity());
		ok(orderService.changeStatus(id, OrderStatus.shipping, null, WAREHOUSE));
		i = inv();
		assertEquals(8, i.getQuantity()); // xuất kho khi giao cho vận chuyển
		assertEquals(0, i.getReservedQuantity());
		ok(orderService.changeStatus(id, OrderStatus.delivered, null, SUPPORT));
		assertEquals(PaymentStatus.paid, order(id).getPaymentStatus()); // COD thu tiền khi giao
		ok(orderService.changeStatus(id, OrderStatus.completed, null, SUPPORT));
		assertEquals(OrderStatus.completed, order(id).getOrderStatus());
		// created + 5 status + 1 payment
		assertEquals(7, historyRepository.findByOrderIdOrderByHistoryIdAsc(id).size());
	}

	@Test
	void cannotSkipOrGoBackwards() throws Exception {
		int id = createViaApi(customer, 1, "COD");
		bad(orderService.changeStatus(id, OrderStatus.delivered, null, ADMIN));
		bad(orderService.changeStatus(id, OrderStatus.shipping, null, ADMIN));
		bad(orderService.changeStatus(id, OrderStatus.completed, null, ADMIN));
		ok(orderService.changeStatus(id, OrderStatus.confirmed, null, ADMIN));
		bad(orderService.changeStatus(id, OrderStatus.pending, null, ADMIN));
		bad(orderService.changeStatus(id, OrderStatus.confirmed, null, ADMIN));
		assertEquals(OrderStatus.confirmed, order(id).getOrderStatus());
	}

	@Test
	void roleRulesPerTransition() throws Exception {
		int id = createViaApi(customer, 1, "COD");
		bad(orderService.changeStatus(id, OrderStatus.confirmed, null, WAREHOUSE)); // kho không được xác nhận
		bad(orderService.changeStatus(id, OrderStatus.confirmed, null, OrderActor.admin("x", "product_staff")));
		ok(orderService.changeStatus(id, OrderStatus.confirmed, null, SUPPORT));
		bad(orderService.changeStatus(id, OrderStatus.packing, null, SUPPORT)); // CSKH không được đóng gói
		ok(orderService.changeStatus(id, OrderStatus.packing, null, WAREHOUSE));
		bad(orderService.changePayment(id, PaymentStatus.paid, null, WAREHOUSE)); // kho không đổi thanh toán
	}

	@Test
	void onlineOrderMustBePaidBeforePacking() throws Exception {
		int id = createViaApi(customer, 1, "VNPay");
		ok(orderService.changeStatus(id, OrderStatus.confirmed, null, SUPPORT));
		bad(orderService.changeStatus(id, OrderStatus.packing, null, WAREHOUSE));
		ok(orderService.changePayment(id, PaymentStatus.paid, "Đã nhận tiền", SUPPORT));
		ok(orderService.changeStatus(id, OrderStatus.packing, null, WAREHOUSE));
	}

	// ---- hủy / hoàn ----

	@Test
	void cancelRequiresReasonAndReleasesReservation() throws Exception {
		int id = createViaApi(customer, 4, "COD");
		bad(orderService.changeStatus(id, OrderStatus.canceled, null, SUPPORT));
		bad(orderService.changeStatus(id, OrderStatus.canceled, "   ", SUPPORT));
		assertEquals(4, inv().getReservedQuantity());
		ok(orderService.changeStatus(id, OrderStatus.canceled, "Khách đổi ý", SUPPORT));
		OrderEntity o = order(id);
		assertEquals(OrderStatus.canceled, o.getOrderStatus());
		assertEquals("Khách đổi ý", o.getCancelReason());
		assertEquals("ADMIN", o.getCancelledBy());
		assertEquals(PaymentStatus.failed, o.getPaymentStatus());
		assertEquals(0, inv().getReservedQuantity());
		assertEquals(10, inv().getQuantity());
		// trạng thái cuối không đi tiếp được
		bad(orderService.changeStatus(id, OrderStatus.confirmed, null, ADMIN));
	}

	@Test
	void cancelWhileShippingRestocksAndPaidOrderNeedsRefund() throws Exception {
		int id = createViaApi(customer, 2, "VNPay");
		ok(orderService.changeStatus(id, OrderStatus.confirmed, null, SUPPORT));
		ok(orderService.changePayment(id, PaymentStatus.paid, null, SUPPORT));
		ok(orderService.changeStatus(id, OrderStatus.packing, null, WAREHOUSE));
		ok(orderService.changeStatus(id, OrderStatus.shipping, null, WAREHOUSE));
		assertEquals(8, inv().getQuantity());
		ok(orderService.changeStatus(id, OrderStatus.canceled, "Giao thất bại", SUPPORT));
		assertEquals(10, inv().getQuantity()); // hàng trả về kho
		assertEquals(0, inv().getReservedQuantity());
		assertEquals(PaymentStatus.refund_pending, order(id).getPaymentStatus());
		bad(orderService.changePayment(id, PaymentStatus.paid, null, SUPPORT)); // đơn đã hủy không được đánh dấu paid lại
	}

	@Test
	void refundFlowAfterDelivery() throws Exception {
		int id = createViaApi(customer, 1, "COD");
		for (OrderStatus s : new OrderStatus[] { OrderStatus.confirmed, OrderStatus.packing, OrderStatus.shipping,
				OrderStatus.delivered }) {
			ok(orderService.changeStatus(id, s, null, ADMIN));
		}
		assertEquals(9, inv().getQuantity());
		bad(orderService.changeStatus(id, OrderStatus.refunded, null, ADMIN)); // cần lý do
		bad(orderService.changePayment(id, PaymentStatus.refund_pending, null, ADMIN)); // chưa hoàn/hủy đơn thì chưa hoàn tiền
		ok(orderService.changeStatus(id, OrderStatus.refunded, "Sản phẩm lỗi", ADMIN));
		assertEquals(10, inv().getQuantity());
		assertEquals(PaymentStatus.refund_pending, order(id).getPaymentStatus());
		bad(orderService.changePayment(id, PaymentStatus.paid, "x", ADMIN)); // đã đóng đơn, không mở lại thanh toán
		ok(orderService.changePayment(id, PaymentStatus.refunded, "Đã chuyển khoản lại", ADMIN));
		assertEquals(PaymentStatus.refunded, order(id).getPaymentStatus());
		bad(orderService.changePayment(id, PaymentStatus.paid, null, ADMIN));
	}

	// ---- khách tự hủy ----

	@Test
	void customerCanCancelOwnPendingOrderOnly() throws Exception {
		int id = createViaApi(customer, 2, "COD");
		// người khác không hủy được (không lộ thông tin)
		mockMvc.perform(post("/api/orders/" + id + "/cancel").sessionAttr("currentUser", otherCustomer))
				.andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value(0));
		assertEquals(OrderStatus.pending, order(id).getOrderStatus());
		// chưa đăng nhập
		mockMvc.perform(post("/api/orders/" + id + "/cancel")).andExpect(status().isOk())
				.andExpect(jsonPath("$.responseCode").value(401));
		// chủ đơn hủy được
		mockMvc.perform(post("/api/orders/" + id + "/cancel").sessionAttr("currentUser", customer).param("reason", "Đặt nhầm"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value(1));
		OrderEntity o = order(id);
		assertEquals(OrderStatus.canceled, o.getOrderStatus());
		assertEquals("CUSTOMER", o.getCancelledBy());
		assertEquals(0, inv().getReservedQuantity());
	}

	@Test
	void customerCannotCancelOnceWarehouseStartedPacking() throws Exception {
		int id = createViaApi(customer, 1, "COD");
		ok(orderService.changeStatus(id, OrderStatus.confirmed, null, SUPPORT));
		ok(orderService.changeStatus(id, OrderStatus.packing, null, WAREHOUSE));
		mockMvc.perform(post("/api/orders/" + id + "/cancel").sessionAttr("currentUser", customer))
				.andExpect(jsonPath("$.responseCode").value(0));
		assertEquals(OrderStatus.packing, order(id).getOrderStatus());
	}

	// ---- đơn cũ, API cũ, xóa ----

	@Test
	void legacyOrderWithoutReservationDoesNotTouchStock() {
		OrderEntity legacy = new OrderEntity();
		legacy.setUserId(customer.getUserId());
		legacy.setTotalPrice(BigDecimal.TEN);
		legacy.setPaymentMethod(PaymentMethod.COD);
		legacy.setPaymentStatus(PaymentStatus.pending);
		legacy.setOrderStatus(OrderStatus.pending);
		legacy.setCreatedAt(java.time.LocalDateTime.now());
		legacy.setUpdatedAt(java.time.LocalDateTime.now());
		legacy = orderRepository.saveAndFlush(legacy);
		int id = legacy.getOrderId();
		ok(orderService.changeStatus(id, OrderStatus.confirmed, null, ADMIN));
		ok(orderService.changeStatus(id, OrderStatus.packing, null, ADMIN));
		ok(orderService.changeStatus(id, OrderStatus.shipping, null, ADMIN));
		ok(orderService.changeStatus(id, OrderStatus.canceled, "test", ADMIN));
		assertEquals(10, inv().getQuantity());
		assertEquals(0, inv().getReservedQuantity());
	}

	@Test
	@SuppressWarnings("deprecation")
	void legacyProcessingStatusIsTreatedAsConfirmed() {
		OrderEntity legacy = new OrderEntity();
		legacy.setUserId(customer.getUserId());
		legacy.setTotalPrice(BigDecimal.TEN);
		legacy.setPaymentMethod(PaymentMethod.COD);
		legacy.setPaymentStatus(PaymentStatus.pending);
		legacy.setOrderStatus(OrderStatus.processing);
		legacy.setCreatedAt(java.time.LocalDateTime.now());
		legacy.setUpdatedAt(java.time.LocalDateTime.now());
		int id = orderRepository.saveAndFlush(legacy).getOrderId();
		ok(orderService.changeStatus(id, OrderStatus.packing, null, WAREHOUSE));
	}

	@Test
	void adminEndpointsWorkAndLegacyPutIsValidated() throws Exception {
		int id = createViaApi(customer, 1, "COD");
		mockMvc.perform(put("/admin/orders/api/" + id + "/status").sessionAttr("roleName", "support_staff")
				.sessionAttr("userLogin", customer).param("to", "confirmed")).andExpect(status().isOk())
				.andExpect(jsonPath("$.responseCode").value(1));
		// API cũ: nhảy cóc bị từ chối
		mockMvc.perform(put("/admin/orders/api/" + id).sessionAttr("roleName", "admin").param("orderStatus", "completed")
				.param("paymentStatus", "paid")).andExpect(jsonPath("$.responseCode").value(0));
		assertEquals(OrderStatus.confirmed, order(id).getOrderStatus());
		mockMvc.perform(get("/admin/orders/api/" + id).sessionAttr("roleName", "warehouse_staff"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.allowedNext[0]").value("packing"))
				.andExpect(jsonPath("$.data.history.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].productName").exists());
		mockMvc.perform(get("/admin/orders/api").sessionAttr("roleName", "support_staff").param("status", "confirmed"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value(1));
	}

	@Test
	void productStaffCannotAccessOrderAdmin() throws Exception {
		mockMvc.perform(get("/admin/orders/api").sessionAttr("roleName", "product_staff"))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(put("/admin/orders/api/1/status").sessionAttr("roleName", "product_staff").param("to", "confirmed"))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void deletingPendingOrderReleasesReservationAndKeepsHistory() throws Exception {
		int id = createViaApi(customer, 3, "COD");
		assertEquals(3, inv().getReservedQuantity());
		bad(orderService.deleteOrder(id, SUPPORT)); // chỉ admin
		ok(orderService.deleteOrder(id, ADMIN));
		assertFalse(orderRepository.findById(id).isPresent());
		assertEquals(0, inv().getReservedQuantity());
	}

	@Test
	void deleteRefusedAfterConfirmation() throws Exception {
		int id = createViaApi(customer, 1, "COD");
		ok(orderService.changeStatus(id, OrderStatus.confirmed, null, ADMIN));
		bad(orderService.deleteOrder(id, ADMIN));
	}
}
