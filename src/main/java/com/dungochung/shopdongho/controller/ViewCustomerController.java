package com.dungochung.shopdongho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ViewCustomerController {
	@Autowired
	private UserService userService;

	@GetMapping("/profile")
	public String orderPage(HttpSession session, Model model) {
		UserEntity currentUser = (UserEntity) session.getAttribute("currentUser");
		if (currentUser == null)
			return "redirect:/auth";
		model.addAttribute("userId", currentUser.getUserId());
		return "account/profile";
	}

	@GetMapping("/search-shop")
	public String searchPage(@RequestParam String keyword, Model model) {
		model.addAttribute("keyword", keyword);
		return "product/search-shop";
	}

	@GetMapping("/detail-shop/{productId}")
	public String detailPage(@PathVariable String productId, Model model) {
		model.addAttribute("productId", productId);
		return "product/detail-shop";
	}

	@GetMapping("/cart")
	public String cartPage(Model model) {
		return "cart/cart";
	}

	@GetMapping("/collections")
	public String collectionPage(Model model) {
		return "product/collections";
	}

	@GetMapping("/auth")
	public String showAuthPage(HttpSession session, Model model) {
		UserEntity currentUser = (UserEntity) session.getAttribute("currentUser");
		if (currentUser != null)
			return "redirect:/";
		model.addAttribute("user", new UserEntity());
		return "account/auth";
	}

	@GetMapping("/")
	public String showHome() {
		return "home/index";
	}

	/*
	 * @PostMapping("/register") public String handleRegister(@ModelAttribute
	 * UserEntity user, @RequestParam("rawPassword") String rawPassword,
	 * 
	 * @RequestParam(value = "profileImage", required = false) MultipartFile
	 * profileImage, RedirectAttributes redirectAttributes) { ResponseDataDto
	 * response = userService.registerCustomer(user, rawPassword, profileImage);
	 * redirectAttributes.addFlashAttribute("message", response.getResponseMsg());
	 * return "redirect:/auth"; }
	 * 
	 * @PostMapping("/login") public String handleLogin(@RequestParam String
	 * usernameOrEmail, @RequestParam String password, HttpSession session,
	 * RedirectAttributes redirectAttributes) { ResponseDataDto response =
	 * userService.login(usernameOrEmail, password); if (response.getResponseCode()
	 * == Constant.RESULT_CD_SUCCESS) { session.setAttribute("currentUser",
	 * response.getData()); System.out.println("sesion:" +
	 * session.getAttribute("currentUser")); return "redirect:/"; }
	 * redirectAttributes.addFlashAttribute("message", response.getResponseMsg());
	 * return "redirect:/auth"; }
	 */

	@GetMapping("/checkout")
	public String checkout(HttpSession session) {
		Object currentUser = session.getAttribute("currentUser");
		if (currentUser == null) {
			return "redirect:/auth";
		}
		return "checkout/checkout";
	}
}
