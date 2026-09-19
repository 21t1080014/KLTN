package com.dungochung.shopdongho.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.dungochung.shopdongho.common.AdminPermissions;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.enums.UserStatus;
import com.dungochung.shopdongho.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Kiểm soát truy cập /admin/**. Mỗi request:
 * 1. Phải có phiên đăng nhập admin (session "userLogin").
 * 2. Nạp lại tài khoản từ DB: bị khóa/xóa/hạ vai trò thì phiên đang mở mất hiệu lực NGAY (trước đây session vẫn dùng được
 *    đến khi hết hạn) và role luôn lấy theo DB, không tin giá trị cũ trong session.
 * 3. Đối chiếu ma trận {@link AdminPermissions} theo role + phương thức HTTP + đường dẫn.
 * Không có quyền: gọi API (/api/, không phải GET) nhận JSON 401/403; trang HTML thì chuyển về đăng nhập / dashboard.
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

	@Autowired
	private UserRepository userRepository;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		HttpSession session = request.getSession(false);
		UserEntity sessionUser = session == null ? null : (UserEntity) session.getAttribute("userLogin");
		if (sessionUser == null) {
			return deny(request, response, 401, "/admin/login");
		}

		UserEntity user = userRepository.findById(sessionUser.getUserId()).orElse(null);
		if (user == null || user.getStatus() != UserStatus.ACTIVE || user.getRole() == null
				|| !AdminPermissions.STAFF_ROLES.contains(user.getRole().getRoleName())) {
			session.invalidate();
			return deny(request, response, 401, "/admin/login");
		}

		String role = user.getRole().getRoleName();
		session.setAttribute("roleName", role); // menu/UI luôn theo role hiện tại trong DB
		session.setAttribute("username", user.getUsername());

		if (!AdminPermissions.isAllowed(role, request.getMethod(), request.getRequestURI())) {
			return deny(request, response, 403, "/admin/dashboard");
		}
		return true;
	}

	private boolean deny(HttpServletRequest request, HttpServletResponse response, int status, String redirectTo)
			throws IOException {
		String uri = request.getRequestURI();
		boolean api = uri.contains("/api/") || uri.endsWith("/api") || uri.contains("/export/")
				|| !"GET".equalsIgnoreCase(request.getMethod());
		if (api) {
			response.setStatus(status);
			response.setContentType("application/json;charset=UTF-8");
			String msg = status == 401 ? "Phiên đăng nhập đã hết hạn hoặc tài khoản không còn hiệu lực"
					: "Bạn không có quyền thực hiện thao tác này";
			response.getWriter().write("{\"responseCode\":" + status + ",\"responseMsg\":\"" + msg + "\"}");
		} else {
			response.sendRedirect(redirectTo);
		}
		return false;
	}
}
