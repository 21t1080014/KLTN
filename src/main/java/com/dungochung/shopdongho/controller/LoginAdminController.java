package com.dungochung.shopdongho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
		return "pages/LoginAdmin";
	}

	@PostMapping
	public String processLogin(@RequestParam("username") String username, @RequestParam("password") String password,
			HttpSession session, HttpServletRequest request, Model model) {

		ResponseDataDto response = userService.login(username, password);

		if (response.getResponseCode() == 1) {
			UserEntity user = (UserEntity) response.getData();

			// Chỉ cho phép đăng nhập admin hoặc nhân viên
			String roleName = user.getRole().getRoleName();
			if (roleName.equals("admin") || roleName.equals("product_staff") || roleName.equals("support_staff")
					|| roleName.equals("warehouse_staff")) {

				session.setAttribute("userLogin", user);
				session.setAttribute("roleName", roleName);
				return "redirect:/admin/dashboard";
			} else {
				model.addAttribute("error", "Bạn không có quyền truy cập trang quản trị.");
				return "pages/LoginAdmin";
			}
		} else {
			model.addAttribute("error", response.getResponseMsg());
			return "pages/LoginAdmin";
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
