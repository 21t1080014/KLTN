package com.dungochung.shopdongho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/login")
public class LoginAdminController {
	@org.springframework.beans.factory.annotation.Autowired
	private com.dungochung.shopdongho.service.AuditService auditService;


	@Autowired
	private UserService userService;

	@GetMapping
	public String showLogin(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "admin/login";
	}

	@PostMapping
	public String processLogin(@RequestParam("username") String username, @RequestParam("password") String password,
			HttpSession session, HttpServletRequest request, Model model) {

		ResponseDataDto response = userService.login(username, password);

		if (response.getResponseCode() == 1) {
			UserEntity user = (UserEntity) response.getData();

			// Tài khoản bị khóa / chưa kích hoạt không được vào khu quản trị
			if (user.getStatus() != com.dungochung.shopdongho.enums.UserStatus.ACTIVE) {
				auditService.logAs(username, user.getRole().getRoleName(), "LOGIN_BLOCKED", "USER", user.getUserId(),
						"Tài khoản " + user.getStatus());
				model.addAttribute("error", "Tài khoản đã bị khóa hoặc chưa được kích hoạt.");
				return "admin/login";
			}
			// Chỉ cho phép đăng nhập admin hoặc nhân viên
			String roleName = user.getRole().getRoleName();
			if (roleName.equals(Constant.ROLE_ADMIN) || roleName.equals(Constant.ROLE_PRODUCT_STAFF)
					|| roleName.equals(Constant.ROLE_SUPPORT_STAFF) || roleName.equals(Constant.ROLE_WAREHOUSE_STAFF)) {

				// Đổi session id sau khi đăng nhập thành công để tránh session fixation
				request.changeSessionId();
				session = request.getSession();
				session.setAttribute("userLogin", user);
				session.setAttribute("roleName", roleName);
				session.setAttribute("username", user.getUsername());
				auditService.logAs(user.getUsername(), roleName, "LOGIN_SUCCESS", "USER", user.getUserId(), null);
				return "redirect:/admin/dashboard";
			} else {
				auditService.logAs(username, roleName, "LOGIN_BLOCKED", "USER", user.getUserId(),
						"Không phải tài khoản nhân viên");
				model.addAttribute("error", "Bạn không có quyền truy cập trang quản trị.");
				return "admin/login";
			}
		} else {
			auditService.logAs(username, null, "LOGIN_FAILED", "USER", null, null);
			// Thông báo chung để không lộ tên đăng nhập nào tồn tại
			model.addAttribute("error", "Sai tên đăng nhập hoặc mật khẩu.");
			return "admin/login";
		}
	}

	// logout
	@GetMapping("/logout")
	public String logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return "redirect:/admin/login";
	}
}
