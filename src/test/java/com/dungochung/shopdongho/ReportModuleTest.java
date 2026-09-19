package com.dungochung.shopdongho;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.dungochung.shopdongho.entity.OrderEntity;
import com.dungochung.shopdongho.entity.OrderItemEntity;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.enums.OrderStatus;
import com.dungochung.shopdongho.enums.PaymentMethod;
import com.dungochung.shopdongho.enums.PaymentStatus;
import com.dungochung.shopdongho.enums.UserStatus;
import com.dungochung.shopdongho.repository.OrderItemRepository;
import com.dungochung.shopdongho.repository.OrderRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.repository.RoleReponsitory;
import com.dungochung.shopdongho.repository.UserRepository;
import com.dungochung.shopdongho.service.ReportService;

import jakarta.persistence.EntityManager;

/** Module 6 - báo cáo + dashboard theo vai trò: doanh thu ngày/tuần/tháng, top bán chạy, tồn thấp, CSV, không lộ doanh thu cho non-admin. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportModuleTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private EntityManager em;
	@Autowired
	private ReportService reportService;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private OrderItemRepository orderItemRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RoleReponsitory roleRepository;

	private UserEntity buyer;
	private List<ProductEntity> products;

	@BeforeEach
	void setUp() {
		buyer = userRepository.findAll().stream().filter(u -> "customer".equals(u.getRole().getRoleName())).findFirst()
				.orElseThrow();
		products = productRepository.findAll().subList(0, 3);
		// Dữ liệu cố định ở tháng 3-4/2020 để không lẫn với dữ liệu thật
		// T2 02/03 10:00 completed+paid 5tr (P0 x2 @2.5tr)
		order("2020-03-02T10:00", "5000000", OrderStatus.completed, PaymentStatus.paid, products.get(0), 2, "2500000");
		// T2 02/03 15:00 delivered+paid 3tr (P1 x1 @3tr)
		order("2020-03-02T15:00", "3000000", OrderStatus.delivered, PaymentStatus.paid, products.get(1), 1, "3000000");
		// 03/03 canceled, đang chờ hoàn tiền: KHÔNG tính (P1 x100)
		order("2020-03-03T09:00", "9000000", OrderStatus.canceled, PaymentStatus.refund_pending, products.get(1), 100, "90000");
		// 04/03 đang giao (chưa giao xong): KHÔNG tính
		order("2020-03-04T09:00", "7000000", OrderStatus.shipping, PaymentStatus.paid, products.get(2), 50, "140000");
		// T2 10/03 completed+paid 2tr (P0 x1 @2tr)
		order("2020-03-10T12:00", "2000000", OrderStatus.completed, PaymentStatus.paid, products.get(0), 1, "2000000");
		// CN 05/04 completed+paid 4tr (P2 x4 @1tr)
		order("2020-04-05T20:00", "4000000", OrderStatus.completed, PaymentStatus.paid, products.get(2), 4, "1000000");
		// 06/04 delivered nhưng CHƯA thanh toán: KHÔNG tính
		order("2020-04-06T08:00", "6000000", OrderStatus.delivered, PaymentStatus.pending, products.get(1), 30, "200000");
		em.flush();
		em.clear();
	}

	private void order(String at, String total, OrderStatus st, PaymentStatus ps, ProductEntity p, int qty, String unit) {
		OrderEntity o = new OrderEntity();
		o.setUserId(buyer.getUserId());
		o.setTotalPrice(new BigDecimal(total));
		o.setPaymentMethod(PaymentMethod.COD);
		o.setOrderStatus(st);
		o.setPaymentStatus(ps);
		o.setCreatedAt(LocalDateTime.now());
		o.setUpdatedAt(LocalDateTime.now());
		o = orderRepository.saveAndFlush(o);
		OrderItemEntity oi = new OrderItemEntity();
		oi.setOrder(o);
		oi.setProduct(p);
		oi.setQuantity(qty);
		oi.setPriceEach(new BigDecimal(unit));
		orderItemRepository.saveAndFlush(oi);
		// @PrePersist luôn đặt created_at = now nên chỉnh lại ngày bằng SQL
		em.createNativeQuery("UPDATE orders SET created_at = :d WHERE order_id = :id")
				.setParameter("d", LocalDateTime.parse(at)).setParameter("id", o.getOrderId()).executeUpdate();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> data(com.dungochung.shopdongho.dto.ResponseDataDto r) {
		assertEquals(1, r.getResponseCode(), r.getResponseMsg());
		return (Map<String, Object>) r.getData();
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> points(Map<String, Object> d) {
		return (List<Map<String, Object>>) d.get("points");
	}

	@Test
	void dailySeriesCountsOnlyRealizedPaidOrdersAndZeroFills() {
		var d = data(reportService.revenueSeries("day", LocalDate.of(2020, 3, 1), LocalDate.of(2020, 4, 30)));
		assertEquals(61, points(d).size()); // 31 + 30 ngày, kể cả ngày không có đơn
		assertEquals(0, new BigDecimal("14000000").compareTo((BigDecimal) d.get("totalRevenue")));
		assertEquals(4L, d.get("totalOrders"));
		assertEquals(0, new BigDecimal("3500000").compareTo((BigDecimal) d.get("avgOrderValue")));
		var mar2 = points(d).stream().filter(p -> "2020-03-02".equals(p.get("bucket"))).findFirst().orElseThrow();
		assertEquals(2L, mar2.get("orders"));
		assertEquals(0, new BigDecimal("8000000").compareTo((BigDecimal) mar2.get("revenue")));
		var mar3 = points(d).stream().filter(p -> "2020-03-03".equals(p.get("bucket"))).findFirst().orElseThrow();
		assertEquals(0L, mar3.get("orders")); // đơn hủy không tính
	}

	@Test
	void weeklyAndMonthlySeries() {
		var w = data(reportService.revenueSeries("week", LocalDate.of(2020, 3, 1), LocalDate.of(2020, 4, 30)));
		var byBucket = points(w).stream().collect(java.util.stream.Collectors.toMap(p -> (String) p.get("bucket"), p -> p));
		assertEquals(0, new BigDecimal("8000000").compareTo((BigDecimal) byBucket.get("2020-03-02").get("revenue")));
		assertEquals(0, new BigDecimal("2000000").compareTo((BigDecimal) byBucket.get("2020-03-09").get("revenue")));
		// 05/04/2020 là Chủ nhật => thuộc tuần bắt đầu thứ Hai 30/03
		assertEquals(0, new BigDecimal("4000000").compareTo((BigDecimal) byBucket.get("2020-03-30").get("revenue")));
		assertEquals(0, new BigDecimal("14000000").compareTo((BigDecimal) w.get("totalRevenue")));

		var m = data(reportService.revenueSeries("month", LocalDate.of(2020, 3, 1), LocalDate.of(2020, 4, 30)));
		assertEquals(2, points(m).size());
		assertEquals(0, new BigDecimal("10000000").compareTo((BigDecimal) points(m).get(0).get("revenue")));
		assertEquals(3L, points(m).get(0).get("orders"));
		assertEquals(0, new BigDecimal("4000000").compareTo((BigDecimal) points(m).get(1).get("revenue")));
	}

	@Test
	void defaultsAndValidation() {
		assertEquals(30, points(data(reportService.revenueSeries("day", null, null))).size());
		assertEquals(12, points(data(reportService.revenueSeries("month", null, null))).size());
		assertEquals(0, reportService.revenueSeries("year", null, null).getResponseCode());
		assertEquals(0, reportService.revenueSeries("day", LocalDate.of(2020, 5, 1), LocalDate.of(2020, 4, 1)).getResponseCode());
		assertEquals(0, reportService.revenueSeries("day", LocalDate.of(2015, 1, 1), LocalDate.of(2020, 1, 1)).getResponseCode());
		assertEquals(0, reportService.topProducts(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 4, 1), 5).getResponseCode());
	}

	@Test
	@SuppressWarnings("unchecked")
	void topProductsRankByQuantityIgnoringUnrealizedOrders() {
		var d = data(reportService.topProducts(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 4, 30), 10));
		var list = (List<Map<String, Object>>) d.get("products");
		assertEquals(3, list.size());
		assertEquals(products.get(2).getProductId(), list.get(0).get("productId")); // P2: 4 (đơn giao đang đi không tính)
		assertEquals(4L, list.get(0).get("quantity"));
		assertEquals(products.get(0).getProductId(), list.get(1).get("productId")); // P0: 2 + 1
		assertEquals(3L, list.get(1).get("quantity"));
		assertEquals(0, new BigDecimal("7000000").compareTo((BigDecimal) list.get(1).get("revenue")));
		assertEquals(products.get(1).getProductId(), list.get(2).get("productId")); // P1: 1 (đơn hủy/chưa trả không tính)
		assertEquals(1L, list.get(2).get("quantity"));
		assertEquals(1, ((List<?>) data(reportService.topProducts(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 4, 30), 1)).get("products")).size());
	}

	@Test
	void ordersCsvIsEscapedAndFormulaSafe() {
		UserEntity evil = new UserEntity();
		evil.setUsername("=cmd|calc,\"x\"");
		evil.setEmail("evil-" + System.nanoTime() + "@example.test");
		evil.setPasswordHash("x");
		evil.setRole(roleRepository.findByRoleName("customer").orElseThrow());
		evil.setStatus(UserStatus.ACTIVE);
		evil = userRepository.saveAndFlush(evil);
		OrderEntity o = new OrderEntity();
		o.setUserId(evil.getUserId());
		o.setTotalPrice(BigDecimal.TEN);
		o.setPaymentMethod(PaymentMethod.COD);
		o.setOrderStatus(OrderStatus.canceled);
		o.setPaymentStatus(PaymentStatus.failed);
		o.setCancelReason("@SUM(A1),\nxuống dòng");
		o.setCreatedAt(LocalDateTime.now());
		o.setUpdatedAt(LocalDateTime.now());
		o = orderRepository.saveAndFlush(o);
		em.createNativeQuery("UPDATE orders SET created_at = :d WHERE order_id = :id")
				.setParameter("d", LocalDateTime.parse("2019-01-15T10:00")).setParameter("id", o.getOrderId()).executeUpdate();
		em.flush();
		em.clear();
		String csv = reportService.ordersCsv(LocalDate.of(2019, 1, 15), LocalDate.of(2019, 1, 15));
		assertTrue(csv.startsWith("﻿order_id,created_at"));
		assertTrue(csv.contains("\"'=cmd|calc,\"\"x\"\"\""), csv); // nháy đơn đứng trước '=' + escape ngoặc kép
		assertTrue(csv.contains("\"'@SUM(A1),\nxuống dòng\""), csv);
		assertFalse(csv.contains(",=cmd"));
		// khoảng ngày sai
		try {
			reportService.ordersCsv(LocalDate.of(2020, 1, 2), LocalDate.of(2020, 1, 1));
			org.junit.jupiter.api.Assertions.fail();
		} catch (IllegalArgumentException expected) {
			// ok
		}
	}

	@Test
	void reportEndpointsAreAdminOnly() throws Exception {
		mockMvc.perform(get("/admin/reports/api/revenue").with(AdminAuth.as("admin")).param("granularity", "month"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value(1))
				.andExpect(jsonPath("$.data.points.length()").value(12));
		mockMvc.perform(get("/admin/reports").with(AdminAuth.as("admin"))).andExpect(status().isOk());
		mockMvc.perform(get("/admin/reports/export/orders.csv").with(AdminAuth.as("admin")).param("from", "2020-03-01")
				.param("to", "2020-03-31")).andExpect(status().isOk());
		mockMvc.perform(get("/admin/reports/export/orders.csv").with(AdminAuth.as("admin")).param("from", "2020-03-05")
				.param("to", "2020-03-01")).andExpect(status().isBadRequest());
		for (String role : new String[] { "product_staff", "support_staff", "warehouse_staff" }) {
			for (String url : new String[] { "/admin/reports/api/revenue", "/admin/reports/api/top-products",
					"/admin/reports/api/low-stock", "/admin/reports/export/orders.csv" }) {
				mockMvc.perform(get(url).with(AdminAuth.as(role))).andExpect(status().isForbidden());
			}
		}
	}

	@Test
	void dashboardOnlyContainsWidgetsForTheRole() throws Exception {
		mockMvc.perform(get("/admin/dashboard/api").with(AdminAuth.as("admin"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.revenue.thisMonth").exists()).andExpect(jsonPath("$.data.orders.total").exists())
				.andExpect(jsonPath("$.data.inventory.totalQuantity").exists())
				.andExpect(jsonPath("$.data.revenueLast14Days.length()").value(14))
				.andExpect(jsonPath("$.data.topProducts").isArray()).andExpect(jsonPath("$.data.queue.pendingConfirm").exists())
				.andExpect(jsonPath("$.data.queue.toPack").exists());

		mockMvc.perform(get("/admin/dashboard/api").with(AdminAuth.as("support_staff"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.orders.total").exists()).andExpect(jsonPath("$.data.orderStatusCount").exists())
				.andExpect(jsonPath("$.data.queue.pendingConfirm").exists()).andExpect(jsonPath("$.data.queue.refundPending").exists())
				.andExpect(jsonPath("$.data.revenue").doesNotExist()).andExpect(jsonPath("$.data.inventory").doesNotExist())
				.andExpect(jsonPath("$.data.topProducts").doesNotExist()).andExpect(jsonPath("$.data.revenueLast14Days").doesNotExist());

		mockMvc.perform(get("/admin/dashboard/api").with(AdminAuth.as("warehouse_staff"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.orders.total").exists()).andExpect(jsonPath("$.data.inventory.lowStockCount").exists())
				.andExpect(jsonPath("$.data.queue.toPack").exists()).andExpect(jsonPath("$.data.queue.toShip").exists())
				.andExpect(jsonPath("$.data.revenue").doesNotExist()).andExpect(jsonPath("$.data.topProducts").doesNotExist());

		String product = mockMvc.perform(get("/admin/dashboard/api").with(AdminAuth.as("product_staff"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.inventory.totalQuantity").exists()).andExpect(jsonPath("$.data.topProducts").isArray())
				.andExpect(jsonPath("$.data.queue.draftProducts").exists()).andExpect(jsonPath("$.data.orders").doesNotExist())
				.andExpect(jsonPath("$.data.revenue").doesNotExist()).andReturn().getResponse().getContentAsString();
		assertFalse(product.contains("\"revenue\""), "product_staff không được thấy doanh thu (kể cả theo sản phẩm)");
	}

	@Test
	void lowStockListsOnlySellableItemsSortedByAvailability() {
		var d = data(reportService.lowStock(50));
		@SuppressWarnings("unchecked")
		var items = (List<Map<String, Object>>) d.get("items");
		int prev = Integer.MIN_VALUE;
		for (var i : items) {
			int available = (Integer) i.get("available");
			assertTrue(available >= prev);
			assertTrue(available <= (Integer) i.get("threshold"));
			prev = available;
		}
		assertTrue(((Number) d.get("count")).intValue() >= items.size());
	}
}
