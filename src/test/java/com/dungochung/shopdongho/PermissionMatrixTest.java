package com.dungochung.shopdongho;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.dungochung.shopdongho.common.AdminPermissions;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.enums.UserStatus;
import com.dungochung.shopdongho.repository.AuditLogRepository;
import com.dungochung.shopdongho.repository.RoleReponsitory;
import com.dungochung.shopdongho.repository.UserRepository;

/** Module 5 - rà soát phân quyền: ma trận role x module x phương thức, phiên bị thu hồi ngay khi khóa/hạ quyền, đăng nhập chặn tài khoản khóa. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PermissionMatrixTest {
	private static final String PW = "Passw0rd!test";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RoleReponsitory roleRepository;
	@Autowired
	private AuditLogRepository auditRepository;

	private UserEntity newUser(String role, UserStatus status) {
		UserEntity u = new UserEntity();
		String s = UUID.randomUUID().toString().substring(0, 8);
		u.setUsername("pm-" + s);
		u.setEmail("pm-" + s + "@example.test");
		u.setPasswordHash(BCrypt.hashpw(PW, BCrypt.gensalt(4)));
		u.setFullName("PM " + role);
		u.setRole(roleRepository.findByRoleName(role).orElseThrow());
		u.setStatus(status);
		return userRepository.saveAndFlush(u);
	}

	private int code(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b) throws Exception {
		return mockMvc.perform(b).andReturn().getResponse().getStatus();
	}

	// ---- ma trận thuần (không cần DB) ----

	@Test
	void matrixTable() {
		String[] all = { "admin", "product_staff", "support_staff", "warehouse_staff" };
		// module, GET, POST/PUT, DELETE => role được phép
		Object[][] table = {
				{ "/admin/products/api", "admin,product_staff", "admin,product_staff", "admin" },
				{ "/admin/inventory/api", "admin,warehouse_staff,product_staff", "admin,warehouse_staff", "admin,warehouse_staff" },
				{ "/admin/purchases/api", "admin,warehouse_staff", "admin,warehouse_staff", "admin,warehouse_staff" },
				{ "/admin/orders/api", "admin,support_staff,warehouse_staff", "admin,support_staff,warehouse_staff", "admin" },
				{ "/admin/customers/api", "admin,support_staff", "admin,support_staff", "admin" },
				{ "/admin/users/api", "admin", "admin", "admin" },
				{ "/admin/brands/api", "admin", "admin", "admin" },
				{ "/admin/categories/api", "admin", "admin", "admin" },
				{ "/admin/promotions/api", "admin", "admin", "admin" },
				{ "/admin/voucher/api", "admin", "admin", "admin" },
				{ "/admin/reports", "admin", "admin", "admin" },
				{ "/admin/dashboard", "admin,product_staff,support_staff,warehouse_staff",
						"admin,product_staff,support_staff,warehouse_staff", "admin" },
				{ "/admin/some-new-module/api", "admin", "admin", "admin" } };
		for (Object[] row : table) {
			String uri = (String) row[0];
			for (String role : all) {
				assertEquals(((String) row[1]).contains(role), AdminPermissions.isAllowed(role, "GET", uri),
						role + " GET " + uri);
				assertEquals(((String) row[2]).contains(role), AdminPermissions.isAllowed(role, "POST", uri),
						role + " POST " + uri);
				assertEquals(((String) row[2]).contains(role), AdminPermissions.isAllowed(role, "PUT", uri),
						role + " PUT " + uri);
				assertEquals(((String) row[3]).contains(role), AdminPermissions.isAllowed(role, "DELETE", uri),
						role + " DELETE " + uri);
			}
		}
		assertFalse(AdminPermissions.isAllowed("customer", "GET", "/admin/dashboard"));
		assertFalse(AdminPermissions.isAllowed(null, "GET", "/admin/dashboard"));
		// ranh giới đoạn đường dẫn: /admin/brandsX không được hưởng luật của /admin/brands (và ngược lại không lọt sang module khác)
		assertFalse(AdminPermissions.isAllowed("product_staff", "GET", "/admin/productsX"));
		assertTrue(AdminPermissions.isAllowed("product_staff", "GET", "/admin/products"));
	}

	// ---- qua HTTP thật ----

	@Test
	void anonymousIsRejected() throws Exception {
		assertEquals(302, code(get("/admin/orders")));
		assertEquals(401, code(get("/admin/orders/api")));
		assertEquals(401, code(put("/admin/orders/api/1/status").param("to", "confirmed")));
	}

	@Test
	void crossModuleAccessIsDenied() throws Exception {
		assertEquals(403, code(get("/admin/orders/api").with(AdminAuth.as("product_staff"))));
		assertEquals(403, code(get("/admin/products/api").with(AdminAuth.as("warehouse_staff"))));
		assertEquals(403, code(get("/admin/products/api").with(AdminAuth.as("support_staff"))));
		assertEquals(403, code(get("/admin/inventory/api").with(AdminAuth.as("support_staff"))));
		assertEquals(403, code(get("/admin/purchases/api").with(AdminAuth.as("support_staff"))));
		assertEquals(403, code(get("/admin/customers/api").with(AdminAuth.as("warehouse_staff"))));
		for (String role : new String[] { "product_staff", "support_staff", "warehouse_staff" }) {
			assertEquals(403, code(get("/admin/users/api").with(AdminAuth.as(role))), role + " users");
			assertEquals(403, code(get("/admin/brands/api").with(AdminAuth.as(role))), role + " brands");
			assertEquals(403, code(get("/admin/promotions/api").with(AdminAuth.as(role))), role + " promotions");
			assertEquals(403, code(get("/admin/voucher/api").with(AdminAuth.as(role))), role + " voucher");
		}
		// trang HTML bị từ chối thì chuyển về dashboard, không hiện trang thao tác
		assertEquals(302, code(get("/admin/users").with(AdminAuth.as("support_staff"))));
		assertEquals(302, code(get("/admin/productsX").with(AdminAuth.as("product_staff"))));
	}

	@Test
	void productStaffSeesInventoryButCannotWriteIt() throws Exception {
		assertEquals(200, code(get("/admin/inventory/api").with(AdminAuth.as("product_staff"))));
		assertEquals(200, code(get("/admin/inventory").with(AdminAuth.as("product_staff"))));
		assertEquals(403, code(put("/admin/inventory/api/variant/1").param("quantity", "5").with(AdminAuth.as("product_staff"))));
		assertNotEquals(403, code(put("/admin/inventory/api/variant/999999").param("quantity", "5")
				.with(AdminAuth.as("warehouse_staff"))));
	}

	@Test
	void destructiveOperationsFollowLeastPrivilege() throws Exception {
		assertEquals(403, code(delete("/admin/products/api/nope").with(AdminAuth.as("product_staff"))));
		assertNotEquals(403, code(delete("/admin/products/api/nope").with(AdminAuth.as("admin"))));
		assertEquals(403, code(delete("/admin/purchases/api/999999").with(AdminAuth.as("product_staff"))));
		assertNotEquals(403, code(delete("/admin/purchases/api/999999").with(AdminAuth.as("warehouse_staff"))));
		assertEquals(403, code(delete("/admin/orders/api/999999").with(AdminAuth.as("support_staff"))));
		assertEquals(403, code(delete("/admin/orders/api/999999").with(AdminAuth.as("warehouse_staff"))));
		assertEquals(403, code(delete("/admin/inventory/api/variant/1").with(AdminAuth.as("product_staff"))));
		assertNotEquals(403, code(delete("/admin/inventory/api/variant/999999").with(AdminAuth.as("warehouse_staff"))));
	}

	@Test
	void lockedStaffLosesAccessOnTheNextRequest() throws Exception {
		UserEntity staff = newUser("support_staff", UserStatus.ACTIVE);
		MockHttpSession session = new MockHttpSession();
		assertEquals(200, code(get("/admin/orders/api").session(session).with(AdminAuth.asUser(staff))));
		assertEquals(200, code(get("/admin/orders/api").session(session)));

		staff.setStatus(UserStatus.LOCKED);
		userRepository.saveAndFlush(staff);
		assertEquals(401, code(get("/admin/orders/api").session(session)));
		assertTrue(session.isInvalid(), "session phải bị hủy khi tài khoản bị khóa");
	}

	@Test
	void demotedOrDeletedStaffIsCutOffImmediately() throws Exception {
		UserEntity boss = newUser("admin", UserStatus.ACTIVE);
		MockHttpSession session = new MockHttpSession();
		assertEquals(200, code(get("/admin/users/api").session(session).with(AdminAuth.asUser(boss))));

		boss.setRole(roleRepository.findByRoleName("support_staff").orElseThrow());
		userRepository.saveAndFlush(boss);
		assertEquals(403, code(get("/admin/users/api").session(session)));
		assertEquals("support_staff", session.getAttribute("roleName"), "role trong session phải theo DB");
		assertEquals(200, code(get("/admin/orders/api").session(session)));

		userRepository.delete(boss);
		userRepository.flush();
		assertEquals(401, code(get("/admin/orders/api").session(session)));
	}

	@Test
	void roleChangedToNonStaffIsRejected() throws Exception {
		UserEntity staff = newUser("support_staff", UserStatus.ACTIVE);
		MockHttpSession session = new MockHttpSession();
		assertEquals(200, code(get("/admin/orders/api").session(session).with(AdminAuth.asUser(staff))));
		staff.setRole(roleRepository.findByRoleName("customer").orElseThrow());
		userRepository.saveAndFlush(staff);
		assertEquals(401, code(get("/admin/orders/api").session(session)));
	}

	// ---- đăng nhập ----

	@Test
	void adminLoginRefusesLockedAndPendingAccounts() throws Exception {
		for (UserStatus st : new UserStatus[] { UserStatus.LOCKED, UserStatus.PENDING }) {
			UserEntity u = newUser("admin", st);
			MvcResult r = mockMvc.perform(post("/admin/login").param("username", u.getUsername()).param("password", PW))
					.andExpect(status().isOk()).andReturn();
			assertNull(r.getRequest().getSession().getAttribute("userLogin"), st + " không được đăng nhập");
			assertNotNull(r.getModelAndView().getModel().get("error"));
		}
		UserEntity ok = newUser("support_staff", UserStatus.ACTIVE);
		MvcResult r = mockMvc.perform(post("/admin/login").param("username", ok.getUsername()).param("password", PW))
				.andExpect(status().is3xxRedirection()).andReturn();
		assertEquals("/admin/dashboard", r.getResponse().getRedirectedUrl());
		assertNotNull(r.getRequest().getSession().getAttribute("userLogin"));
	}

	@Test
	void adminLoginRefusesCustomersAndWrongPassword() throws Exception {
		UserEntity customer = newUser("customer", UserStatus.ACTIVE);
		MvcResult r = mockMvc.perform(post("/admin/login").param("username", customer.getUsername()).param("password", PW))
				.andExpect(status().isOk()).andReturn();
		assertNull(r.getRequest().getSession().getAttribute("userLogin"));
		UserEntity staff = newUser("admin", UserStatus.ACTIVE);
		r = mockMvc.perform(post("/admin/login").param("username", staff.getUsername()).param("password", "sai-mat-khau"))
				.andExpect(status().isOk()).andReturn();
		assertNull(r.getRequest().getSession().getAttribute("userLogin"));
	}

	@Test
	void storefrontLoginRefusesLockedCustomersAndStaff() throws Exception {
		UserEntity locked = newUser("customer", UserStatus.LOCKED);
		MvcResult r = mockMvc.perform(post("/login").param("usernameOrEmail", locked.getUsername()).param("password", PW))
				.andExpect(status().is3xxRedirection()).andReturn();
		assertEquals("/auth", r.getResponse().getRedirectedUrl());
		assertNull(r.getRequest().getSession().getAttribute("currentUser"));

		UserEntity staff = newUser("admin", UserStatus.ACTIVE);
		r = mockMvc.perform(post("/login").param("usernameOrEmail", staff.getUsername()).param("password", PW))
				.andExpect(status().is3xxRedirection()).andReturn();
		assertNull(r.getRequest().getSession().getAttribute("currentUser"));

		UserEntity ok = newUser("customer", UserStatus.ACTIVE);
		r = mockMvc.perform(post("/login").param("usernameOrEmail", ok.getUsername()).param("password", PW))
				.andExpect(status().is3xxRedirection()).andReturn();
		assertEquals("/", r.getResponse().getRedirectedUrl());
		assertNotNull(r.getRequest().getSession().getAttribute("currentUser"));
	}

	// ---- audit log ----

	private long count(String action) {
		return auditRepository.findAll().stream().filter(a -> action.equals(a.getAction())).count();
	}

	@Test
	void loginAttemptsAreAuditedWithGenericErrorMessages() throws Exception {
		long ok0 = count("LOGIN_SUCCESS"), bad0 = count("LOGIN_FAILED"), blocked0 = count("LOGIN_BLOCKED");
		UserEntity staff = newUser("support_staff", UserStatus.ACTIVE);
		mockMvc.perform(post("/admin/login").param("username", staff.getUsername()).param("password", PW));
		MvcResult wrongPw = mockMvc.perform(post("/admin/login").param("username", staff.getUsername()).param("password", "sai"))
				.andReturn();
		MvcResult noUser = mockMvc.perform(post("/admin/login").param("username", "khong-ton-tai-" + UUID.randomUUID()).param("password", "sai"))
				.andReturn();
		// không lộ tài khoản nào tồn tại: hai lỗi giống hệt nhau
		assertEquals(wrongPw.getModelAndView().getModel().get("error"), noUser.getModelAndView().getModel().get("error"));
		UserEntity locked = newUser("admin", UserStatus.LOCKED);
		mockMvc.perform(post("/admin/login").param("username", locked.getUsername()).param("password", PW));
		assertEquals(ok0 + 1, count("LOGIN_SUCCESS"));
		assertEquals(bad0 + 2, count("LOGIN_FAILED"));
		assertEquals(blocked0 + 1, count("LOGIN_BLOCKED"));
	}

	@Test
	void userManagementAndDestructiveActionsAreAudited() throws Exception {
		UserEntity boss = newUser("admin", UserStatus.ACTIVE);
		UserEntity target = newUser("support_staff", UserStatus.ACTIVE);
		long upd0 = count("USER_UPDATE"), del0 = count("USER_DELETE");
		mockMvc.perform(put("/admin/users/api/" + target.getUserId()).with(AdminAuth.asUser(boss))
				.param("fullName", "X").param("username", target.getUsername()).param("email", target.getEmail())
				.param("phone", "").param("address", "").param("password", "")
				.param("role", String.valueOf(roleRepository.findByRoleName("warehouse_staff").orElseThrow().getRoleId()))
				.param("status", "ACTIVE")).andExpect(status().isOk());
		assertEquals(upd0 + 1, count("USER_UPDATE"));
		var entry = auditRepository.findAll().stream().filter(a -> "USER_UPDATE".equals(a.getAction())
				&& target.getUserId().equals(a.getTargetId())).findFirst().orElseThrow();
		assertTrue(entry.getDetail().contains("support_staff -> warehouse_staff"), entry.getDetail());
		assertEquals(boss.getUsername(), entry.getActor());
		assertFalse(entry.getDetail().toLowerCase().contains("passw"));

		mockMvc.perform(delete("/admin/users/api/" + target.getUserId()).with(AdminAuth.asUser(boss))).andExpect(status().isOk());
		assertEquals(del0 + 1, count("USER_DELETE"));
	}

	@Test
	void auditPageAndApiAreAdminOnly() throws Exception {
		assertEquals(200, code(get("/admin/audit").with(AdminAuth.as("admin"))));
		assertEquals(200, code(get("/admin/audit/api").with(AdminAuth.as("admin")).param("actor", "%_'")));
		for (String role : new String[] { "product_staff", "support_staff", "warehouse_staff" }) {
			assertEquals(403, code(get("/admin/audit/api").with(AdminAuth.as(role))), role);
		}
	}
}
