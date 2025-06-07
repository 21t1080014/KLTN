package com.dungochung.shopdongho.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class RoleInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		HttpSession session = request.getSession(false);
		String role = (session != null) ? (String) session.getAttribute("roleName") : null;

		// Nếu chưa đăng nhập
		if (role == null) {
			response.sendRedirect("/admin/login");
			return false;
		}

		// Danh sách các path và quyền tương ứng
		String uri = request.getRequestURI();

		boolean isAuthorized = false;

		if (uri.startsWith("/admin/products") && (role.equals("admin") || role.equals("product_staff"))) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/orders") && (role.equals("admin") || role.equals("support_staff"))) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/inventory") && (role.equals("admin") || role.equals("warehouse_staff"))) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/purchases") && (role.equals("admin") || role.equals("warehouse_staff"))) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/users") && role.equals("admin")) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/brands") && role.equals("admin")) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/categories") && role.equals("admin")) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/promotions") && role.equals("admin")) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/voucher") && role.equals("admin")) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/reports") && role.equals("admin")) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/dashboard")) {
			isAuthorized = true;
		}

		if (!isAuthorized) {
			response.sendRedirect("/admin/login");
			return false;
		}

		return true;
	}
}
