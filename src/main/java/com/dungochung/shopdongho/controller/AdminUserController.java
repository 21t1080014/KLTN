package com.dungochung.shopdongho.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.RoleEntity;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.enums.UserStatus;
import com.dungochung.shopdongho.repository.RoleReponsitory;
import com.dungochung.shopdongho.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
	@Autowired
	private UserService userService;
	@Autowired
	private RoleReponsitory roleReponsitory;

	@GetMapping
	public String showUser(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "admin/users";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getAllUsers(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		return userService.getAllUsers(page, size);
	}

	@GetMapping("/api/search")
	@ResponseBody
	public ResponseDataDto searchUser(@RequestParam("keyword") String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
		return userService.searchByName(keyword, page, size);
	}

	@PostMapping("/api")
	@ResponseBody
	public ResponseDataDto createUser(@RequestParam("fullName") String fullName,
			@RequestParam("username") String username, @RequestParam("email") String email,
			@RequestParam("phone") String phone, @RequestParam("role") Integer roleId,
			@RequestParam("password") String password, @RequestParam("address") String address,
			@RequestParam(value = "image", required = false) MultipartFile profileImage,
			@RequestParam("status") String status) {
		RoleEntity role = roleReponsitory.findById(roleId).orElse(null);
		UserEntity user = new UserEntity();
		user.setFullName(fullName);
		user.setUsername(username);
		user.setPhone(phone);
		user.setAddress(address);
		user.setEmail(email);
		user.setStatus(UserStatus.valueOf(status));
		user.setRole(role);
		return userService.createUser(user, password, profileImage);
	}

	@PutMapping("/api/{userId}")
	@ResponseBody
	public ResponseDataDto updateUser(@PathVariable String userId, @RequestParam("fullName") String fullName,
			@RequestParam("username") String username, @RequestParam("email") String email,
			@RequestParam("phone") String phone, @RequestParam("role") Integer roleId,
			@RequestParam("password") String password, @RequestParam("address") String address,
			@RequestParam(value = "image", required = false) MultipartFile profileImage,
			@RequestParam("status") String status) {
		RoleEntity role = roleReponsitory.findById(roleId).orElse(null);
		UserEntity user = new UserEntity();
		user.setFullName(fullName);
		user.setUsername(username);
		user.setEmail(email);
		user.setPhone(phone);
		user.setAddress(address);
		user.setStatus(UserStatus.valueOf(status));
		user.setRole(role);
		// password rỗng nghĩa là không đổi mật khẩu (xem UserServiceImpl.updateUser)
		if (password != null && !password.isEmpty()) {
			user.setPasswordHash(password);
		}
		return userService.updateUser(userId, user, profileImage);
	}

	@DeleteMapping("/api/{userId}")
	@ResponseBody
	public ResponseDataDto deleteUser(@PathVariable String userId) {
		return userService.deleteUser(userId);
	}

	@GetMapping("/api/{userId}")
	@ResponseBody
	public ResponseDataDto getUserById(@PathVariable String userId) {
		return userService.getUserById(userId);
	}

	@GetMapping("/api/user-statuses")
	@ResponseBody
	public UserStatus[] getUserStatuses() {
		return UserStatus.values();
	}

	@GetMapping("/api/role")
	@ResponseBody
	public List<RoleEntity> getAllRoles() {
		return roleReponsitory.findAll();
	}
}
