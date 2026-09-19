package com.dungochung.shopdongho;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.OrderItemEntity;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.enums.PaymentMethod;
import com.dungochung.shopdongho.enums.ProductStatus;
import com.dungochung.shopdongho.enums.UserStatus;
import com.dungochung.shopdongho.repository.OrderRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.repository.UserRepository;
import com.dungochung.shopdongho.repository.UserStatusHistoryRepository;
import com.dungochung.shopdongho.service.CustomerService;
import com.dungochung.shopdongho.service.OrderService;
import com.dungochung.shopdongho.service.UserService;

import jakarta.persistence.EntityManager;

/** Module 4 - quản lý khách hàng: danh sách/thống kê/hạng, khóa-mở khóa có lý do + lịch sử, ghi chú, chặn xóa khách có đơn. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerModuleTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private EntityManager em;
	@Autowired
	private CustomerService customerService;
	@Autowired
	private UserService userService;
	@Autowired
	private OrderService orderService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private UserStatusHistoryRepository historyRepository;

	private UserEntity anyActiveCustomer() {
		return userRepository.findAll().stream()
				.filter(u -> "customer".equals(u.getRole().getRoleName()) && u.getStatus() == UserStatus.ACTIVE)
				.filter(u -> !orderRepository.existsByUserId(u.getUserId())).findFirst().orElseThrow();
	}

	@Test
	void listSupportsAllSortsFiltersAndSearch() throws Exception {
		for (String sort : new String[] { "newest", "spent", "orders", "recent", "name", "bogus" }) {
			mockMvc.perform(get("/admin/customers/api").sessionAttr("roleName", "support_staff").param("sort", sort))
					.andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value(1))
					.andExpect(jsonPath("$.data.customers").isArray());
		}
		mockMvc.perform(get("/admin/customers/api").sessionAttr("roleName", "admin").param("status", "LOCKED"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value(1));
		// tìm theo từ khóa có ký tự đặc biệt không được làm hỏng truy vấn
		mockMvc.perform(get("/admin/customers/api").sessionAttr("roleName", "admin").param("keyword", "%_'\""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value(1))
				.andExpect(jsonPath("$.data.totalCount").value(0));
	}

	@Test
	void listOnlyContainsCustomersWithOrderStats() {
		ResponseDataDto res = customerService.list(null, null, "orders", 0, 100);
		@SuppressWarnings("unchecked")
		var rows = (List<java.util.Map<String, Object>>) ((java.util.Map<String, Object>) res.getData()).get("customers");
		assertTrue(rows.size() > 0);
		long expected = userRepository.findAll().stream().filter(u -> "customer".equals(u.getRole().getRoleName())).count();
		assertEquals(expected, rows.size());
		// sắp xếp theo số đơn giảm dần
		long prev = Long.MAX_VALUE;
		for (var r : rows) {
			long c = ((Number) r.get("orderCount")).longValue();
			assertTrue(c <= prev);
			prev = c;
			assertNotNull(r.get("tier"));
		}
	}

	@Test
	void detailHasStatsAndNeverExposesPasswordHash() throws Exception {
		UserEntity buyer = userRepository.findAll().stream().filter(u -> orderRepository.existsByUserId(u.getUserId())
				&& "customer".equals(u.getRole().getRoleName())).findFirst().orElseThrow();
		String body = mockMvc.perform(get("/admin/customers/api/" + buyer.getUserId()).sessionAttr("roleName", "support_staff"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value(1))
				.andExpect(jsonPath("$.data.stats.orderCount").isNumber())
				.andExpect(jsonPath("$.data.stats.tier").exists()).andExpect(jsonPath("$.data.orders").isArray())
				.andReturn().getResponse().getContentAsString();
		assertTrue(!body.contains("passwordHash") && !body.contains("password_hash"));
		// nhân viên không phải khách => không tra được qua API khách hàng
		UserEntity staff = userRepository.findAll().stream().filter(u -> !"customer".equals(u.getRole().getRoleName()))
				.findFirst().orElseThrow();
		mockMvc.perform(get("/admin/customers/api/" + staff.getUserId()).sessionAttr("roleName", "admin"))
				.andExpect(jsonPath("$.responseCode").value(0));
	}

	@Test
	void lockNeedsReasonWritesHistoryAndUnlockClearsLock() {
		UserEntity c = anyActiveCustomer();
		String id = c.getUserId();
		assertEquals(0, customerService.changeStatus(id, UserStatus.LOCKED, "  ", "tester").getResponseCode());
		assertEquals(1, customerService.changeStatus(id, UserStatus.LOCKED, "Nghi ngờ gian lận", "tester").getResponseCode());
		em.flush();
		em.clear();
		UserEntity locked = userRepository.findById(id).orElseThrow();
		assertEquals(UserStatus.LOCKED, locked.getStatus());
		assertEquals("Nghi ngờ gian lận", locked.getLockReason());
		assertEquals("tester", locked.getLockedBy());
		assertEquals(0, customerService.changeStatus(id, UserStatus.LOCKED, "again", "tester").getResponseCode());
		assertEquals(1, customerService.changeStatus(id, UserStatus.ACTIVE, null, "tester").getResponseCode());
		em.flush();
		em.clear();
		UserEntity again = userRepository.findById(id).orElseThrow();
		assertEquals(UserStatus.ACTIVE, again.getStatus());
		assertEquals(null, again.getLockReason());
		assertEquals(2, historyRepository.findByUserIdOrderByHistoryIdDesc(id).size());
	}

	@Test
	void lockedCustomerCannotPlaceOrders() {
		UserEntity c = anyActiveCustomer();
		customerService.changeStatus(c.getUserId(), UserStatus.LOCKED, "test", "tester");
		ProductEntity p = productRepository.findAll().stream().filter(x -> x.getStatus() == ProductStatus.ACTIVE)
				.findFirst().orElseThrow();
		OrderItemEntity item = new OrderItemEntity();
		ProductEntity ref = new ProductEntity();
		ref.setProductId(p.getProductId());
		item.setProduct(ref);
		item.setQuantity(1);
		item.setPriceEach(p.getPrice());
		ResponseDataDto r = orderService.createOrder(c.getUserId(), List.of(item), p.getPrice(), PaymentMethod.COD);
		assertEquals(0, r.getResponseCode());
		assertTrue(r.getResponseMsg().contains("khóa"));
	}

	@Test
	void notesAreSavedButLengthLimited() {
		UserEntity c = anyActiveCustomer();
		assertEquals(1, customerService.updateNote(c.getUserId(), "Khách VIP, ưu tiên giao nhanh").getResponseCode());
		assertEquals("Khách VIP, ưu tiên giao nhanh", userRepository.findById(c.getUserId()).orElseThrow().getInternalNote());
		assertEquals(0, customerService.updateNote(c.getUserId(), "x".repeat(1001)).getResponseCode());
		assertEquals(1, customerService.updateNote(c.getUserId(), " ").getResponseCode());
		assertEquals(null, userRepository.findById(c.getUserId()).orElseThrow().getInternalNote());
	}

	@Test
	void customerCannotSeeInternalFieldsInOwnProfileApi() throws Exception {
		UserEntity c = anyActiveCustomer();
		customerService.updateNote(c.getUserId(), "bí mật nội bộ");
		customerService.changeStatus(c.getUserId(), UserStatus.LOCKED, "lý do nội bộ", "tester");
		customerService.changeStatus(c.getUserId(), UserStatus.ACTIVE, null, "tester");
		String body = mockMvc.perform(get("/api/user-customer").sessionAttr("currentUser", c)).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertTrue(!body.contains("bí mật nội bộ") && !body.contains("internalNote") && !body.contains("lockReason"));
	}

	@Test
	void accessControlForCustomerAdmin() throws Exception {
		mockMvc.perform(get("/admin/customers").sessionAttr("roleName", "support_staff")).andExpect(status().isOk());
		mockMvc.perform(get("/admin/customers").sessionAttr("roleName", "admin")).andExpect(status().isOk());
		for (String role : new String[] { "product_staff", "warehouse_staff" }) {
			mockMvc.perform(get("/admin/customers/api").sessionAttr("roleName", role))
					.andExpect(status().is3xxRedirection());
			mockMvc.perform(put("/admin/customers/api/x/status").sessionAttr("roleName", role).param("to", "LOCKED")
					.param("reason", "x")).andExpect(status().is3xxRedirection());
		}
		mockMvc.perform(get("/admin/customers/api")).andExpect(status().is3xxRedirection());
	}

	// ---- bảo vệ tài khoản ở trang Người dùng ----

	@Test
	void cannotDeleteUserWhoHasOrders() {
		UserEntity buyer = userRepository.findAll().stream().filter(u -> orderRepository.existsByUserId(u.getUserId()))
				.findFirst().orElseThrow();
		ResponseDataDto r = userService.deleteUser(buyer.getUserId());
		assertEquals(0, r.getResponseCode());
		assertTrue(userRepository.existsById(buyer.getUserId()));
	}

	@Test
	void cannotDeleteOrLockTheLastActiveAdmin() {
		List<UserEntity> admins = userRepository.findAll().stream()
				.filter(u -> "admin".equals(u.getRole().getRoleName()) && u.getStatus() == UserStatus.ACTIVE).toList();
		assertTrue(admins.size() >= 1);
		for (int i = 1; i < admins.size(); i++) {
			admins.get(i).setStatus(UserStatus.LOCKED);
			userRepository.save(admins.get(i));
		}
		userRepository.flush();
		UserEntity last = admins.get(0);
		assertEquals(0, userService.deleteUser(last.getUserId()).getResponseCode());
		UserEntity change = new UserEntity();
		change.setFullName(last.getFullName());
		change.setPhone(last.getPhone());
		change.setAddress(last.getAddress());
		change.setRole(last.getRole());
		change.setStatus(UserStatus.LOCKED);
		assertEquals(0, userService.updateUser(last.getUserId(), change, null).getResponseCode());
		assertEquals(UserStatus.ACTIVE, userRepository.findById(last.getUserId()).orElseThrow().getStatus());
	}

	@Test
	void cannotDeleteSelf() throws Exception {
		UserEntity someone = userRepository.findAll().stream()
				.filter(u -> !orderRepository.existsByUserId(u.getUserId()) && !"admin".equals(u.getRole().getRoleName()))
				.findFirst().orElseThrow();
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.delete("/admin/users/api/" + someone.getUserId()).sessionAttr("roleName", "admin")
				.sessionAttr("userLogin", someone)).andExpect(status().isOk())
				.andExpect(jsonPath("$.responseCode").value(0));
		assertTrue(userRepository.existsById(someone.getUserId()));
	}

	private com.dungochung.shopdongho.entity.OrderEntity order(String userId, String total,
			com.dungochung.shopdongho.enums.OrderStatus st, com.dungochung.shopdongho.enums.PaymentStatus ps) {
		var o = new com.dungochung.shopdongho.entity.OrderEntity();
		o.setUserId(userId);
		o.setTotalPrice(new java.math.BigDecimal(total));
		o.setPaymentMethod(PaymentMethod.COD);
		o.setOrderStatus(st);
		o.setPaymentStatus(ps);
		o.setCreatedAt(java.time.LocalDateTime.now());
		o.setUpdatedAt(java.time.LocalDateTime.now());
		return orderRepository.save(o);
	}

	@Test
	void spentCountsOnlyDeliveredOrCompletedPaidOrdersAndDrivesTier() {
		var os = com.dungochung.shopdongho.enums.OrderStatus.class;
		var ps = com.dungochung.shopdongho.enums.PaymentStatus.class;
		UserEntity c = anyActiveCustomer();
		order(c.getUserId(), "5000000", Enum.valueOf(os, "completed"), Enum.valueOf(ps, "paid"));
		order(c.getUserId(), "16000000", Enum.valueOf(os, "delivered"), Enum.valueOf(ps, "paid"));
		order(c.getUserId(), "9000000", Enum.valueOf(os, "canceled"), Enum.valueOf(ps, "refund_pending")); // không tính
		order(c.getUserId(), "7000000", Enum.valueOf(os, "shipping"), Enum.valueOf(ps, "paid")); // chưa giao: không tính
		em.flush();
		em.clear();
		var res = (java.util.Map<?, ?>) customerService.detail(c.getUserId()).getData();
		var stats = (java.util.Map<?, ?>) res.get("stats");
		assertEquals(4L, ((Number) stats.get("orderCount")).longValue());
		assertEquals(0, new java.math.BigDecimal("21000000").compareTo((java.math.BigDecimal) stats.get("totalSpent")));
		assertEquals("Gold", stats.get("tier")); // >= 20.000.000
		assertEquals(1L, ((Number) stats.get("canceledCount")).longValue());
		var list = (java.util.Map<?, ?>) customerService.list(c.getUsername(), null, "spent", 0, 10).getData();
		var row = ((List<?>) list.get("customers")).stream().map(x -> (java.util.Map<?, ?>) x)
				.filter(m -> c.getUserId().equals(m.get("userId"))).findFirst().orElseThrow();
		assertEquals(4L, ((Number) row.get("orderCount")).longValue());
		assertEquals(0, new java.math.BigDecimal("21000000").compareTo((java.math.BigDecimal) row.get("totalSpent")));
		assertEquals("Gold", row.get("tier"));
	}

	@Test
	void tierBoundaries() {
		var g = new java.math.BigDecimal("20000000");
		var d = new java.math.BigDecimal("100000000");
		assertEquals(com.dungochung.shopdongho.common.CustomerTier.SILVER,
				com.dungochung.shopdongho.common.CustomerTier.of(new java.math.BigDecimal("19999999"), g, d));
		assertEquals(com.dungochung.shopdongho.common.CustomerTier.GOLD,
				com.dungochung.shopdongho.common.CustomerTier.of(g, g, d));
		assertEquals(com.dungochung.shopdongho.common.CustomerTier.DIAMOND,
				com.dungochung.shopdongho.common.CustomerTier.of(d, g, d));
	}
}
