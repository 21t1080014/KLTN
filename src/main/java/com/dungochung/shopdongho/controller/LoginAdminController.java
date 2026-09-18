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

			// Chỉ cho phép đăng nhập admin hoặc nhân viên
			String roleName = user.getRole().getRoleName();
			if (roleName.equals(Constant.ROLE_ADMIN) || roleName.equals(Constant.ROLE_PRODUCT_STAFF)
					|| roleName.equals(Constant.ROLE_SUPPORT_STAFF) || roleName.equals(Constant.ROLE_WAREHOUSE_STAFF)) {

				// Đổi session id sau khi đăng nhập thành công để tránh session fixation
				request.changeSessionId();
				session = request.getSession();
				session.setAttribute("userLogin", user);
				session.setAttribute("roleName", roleName);
				return "redirect:/admin/dashboard";
			} else {
				model.addAttribute("error", "Bạn không có quyền truy cập trang quản trị.");
				return "admin/login";
			}
		} else {
			model.addAttribute("error", response.getResponseMsg());
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
