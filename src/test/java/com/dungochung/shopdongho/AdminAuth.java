package com.dungochung.shopdongho;

import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.dungochung.shopdongho.entity.RoleEntity;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.enums.UserStatus;
import com.dungochung.shopdongho.repository.RoleReponsitory;
import com.dungochung.shopdongho.repository.UserRepository;

/**
 * Đăng nhập giả lập vào khu admin cho MockMvc theo đúng cơ chế thật: session có "userLogin" là 1 tài khoản CÓ THẬT trong DB
 * (RoleInterceptor nạp lại tài khoản mỗi request nên không thể chỉ set roleName). Chưa có nhân viên role đó thì tạo mới
 * — trong test @Transactional sẽ được rollback.
 */
final class AdminAuth {
	private AdminAuth() {
	}

	static RequestPostProcessor as(String role) {
		return request -> login(request, findOrCreate(request, role));
	}

	static RequestPostProcessor asUser(UserEntity user) {
		return request -> login(request, user);
	}

	private static MockHttpServletRequest login(MockHttpServletRequest request, UserEntity user) {
		request.getSession().setAttribute("userLogin", user);
		request.getSession().setAttribute("roleName", user.getRole().getRoleName());
		request.getSession().setAttribute("username", user.getUsername());
		return request;
	}

	private static UserEntity findOrCreate(MockHttpServletRequest request, String role) {
		ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());
		UserRepository users = ctx.getBean(UserRepository.class);
		RoleReponsitory roles = ctx.getBean(RoleReponsitory.class);
		return users.findFirstByRole_RoleNameAndStatus(role, UserStatus.ACTIVE).orElseGet(() -> {
			RoleEntity r = roles.findByRoleName(role).orElseThrow();
			UserEntity u = new UserEntity();
			String suffix = UUID.randomUUID().toString().substring(0, 8);
			u.setUsername("test-" + role + "-" + suffix);
			u.setEmail("test-" + suffix + "@example.test");
			u.setPasswordHash("x");
			u.setFullName("Test " + role);
			u.setRole(r);
			u.setStatus(UserStatus.ACTIVE);
			return users.saveAndFlush(u);
		});
	}
}
