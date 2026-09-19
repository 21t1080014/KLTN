package com.dungochung.shopdongho.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import com.dungochung.shopdongho.common.constant.Constant;

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

		if (uri.startsWith("/admin/products")
				&& (role.equals(Constant.ROLE_ADMIN) || role.equals(Constant.ROLE_PRODUCT_STAFF))) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/orders")
				&& (role.equals(Constant.ROLE_ADMIN) || role.equals(Constant.ROLE_SUPPORT_STAFF)
						|| role.equals(Constant.ROLE_WAREHOUSE_STAFF))) {
			// warehouse_staff vào được để đóng gói/giao hàng; quyền từng thao tác do OrderWorkflow kiểm tra ở service
			isAuthorized = true;
		} else if (uri.startsWith("/admin/customers")
				&& (role.equals(Constant.ROLE_ADMIN) || role.equals(Constant.ROLE_SUPPORT_STAFF))) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/inventory")
				&& (role.equals(Constant.ROLE_ADMIN) || role.equals(Constant.ROLE_WAREHOUSE_STAFF))) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/purchases")
				&& (role.equals(Constant.ROLE_ADMIN) || role.equals(Constant.ROLE_WAREHOUSE_STAFF))) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/users") && role.equals(Constant.ROLE_ADMIN)) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/brands") && role.equals(Constant.ROLE_ADMIN)) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/categories") && role.equals(Constant.ROLE_ADMIN)) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/promotions") && role.equals(Constant.ROLE_ADMIN)) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/voucher") && role.equals(Constant.ROLE_ADMIN)) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/reports") && role.equals(Constant.ROLE_ADMIN)) {
			isAuthorized = true;
		} else if (uri.startsWith("/admin/dashboard")) {
			// Mọi nhân viên đã đăng nhập đều xem được dashboard (giữ nguyên hành vi hiện tại)
			isAuthorized = true;
		}

		if (!isAuthorized) {
			response.sendRedirect("/admin/login");
			return false;
		}

		return true;
	}
}
