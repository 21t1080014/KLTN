package com.dungochung.shopdongho.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.repository.UserRepository;
import com.dungochung.shopdongho.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {
	@Autowired
	private UserService userService;
	@Autowired
	private JavaMailSender mailSender;
	@Autowired
	private UserRepository userRepository;

	@PostMapping("/login")
	public String handleLogin(@RequestParam String usernameOrEmail, @RequestParam String password, HttpSession session,
			RedirectAttributes redirectAttributes) {
		ResponseDataDto response = userService.login(usernameOrEmail, password);
		if (response.getResponseCode() == Constant.RESULT_CD_SUCCESS) {
			session.setAttribute("currentUser", response.getData());
			System.out.println("sesion:" + session.getAttribute("currentUser"));
			return "redirect:/";
		}
		redirectAttributes.addFlashAttribute("message", response.getResponseMsg());
		return "redirect:/auth";
	}

	@PostMapping("/register")
	@ResponseBody
	public ResponseDataDto handleRegister(@ModelAttribute UserEntity user,
			@RequestParam("rawPassword") String rawPassword,
			@RequestParam(value = "profileImage", required = false) MultipartFile profileImage, HttpSession session) {

		// Kiểm tra trùng username hoặc email
		if (userRepository.existsByUsername(user.getUsername())) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Tên đăng nhập đã tồn tại", 400);
		}
		if (userRepository.existsByEmail(user.getEmail())) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Email đã tồn tại", 400);
		}

		// Lưu thông tin user tạm thời vào session (CHƯA LƯU DB)
		session.setAttribute("tempUser", user);
		session.setAttribute("rawPassword", rawPassword);
		session.setAttribute("profileImage", profileImage); // Hoặc lưu tên tạm nếu đã upload

		// Tạo và gửi mã xác minh
		String code = generateSixDigitCode();
		session.setAttribute("verifyCode", code);
		session.setAttribute("verifyEmail", user.getEmail());

		String subject = "Mã xác nhận đăng ký tài khoản";
		String text = "Chào " + user.getFullName() + ",\n\nMã xác nhận của bạn là: " + code
				+ "\n\nVui lòng nhập mã này để hoàn tất quá trình đăng ký.\n\nShop Đồng Hồ";

		sendEmail(user.getEmail(), subject, text);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "OK", 200);
	}

	@PostMapping("/verify-code")
	@ResponseBody
	public ResponseDataDto handleVerifyCode(@RequestBody CodeRequest request, HttpSession session) {
		String sessionCode = (String) session.getAttribute("verifyCode");
		String sessionEmail = (String) session.getAttribute("verifyEmail");

		if (sessionCode == null || sessionEmail == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Không tìm thấy thông tin xác minh. Hãy thử đăng ký lại.", 400);
		}

		if (sessionCode.equals(request.getCode())) {
			UserEntity user = (UserEntity) session.getAttribute("tempUser");
			String rawPassword = (String) session.getAttribute("rawPassword");
			MultipartFile profileImage = (MultipartFile) session.getAttribute("profileImage");

			if (user == null || rawPassword == null) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy dữ liệu người dùng", 400);
			}

			// Đăng ký chính thức
			ResponseDataDto response = userService.registerCustomer(user, rawPassword, profileImage);

			// Xoá session tạm
			session.removeAttribute("verifyCode");
			session.removeAttribute("verifyEmail");
			session.removeAttribute("tempUser");
			session.removeAttribute("rawPassword");
			session.removeAttribute("profileImage");

			if (response.getResponseCode() == Constant.RESULT_CD_SUCCESS) {
				return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Xác minh thành công!", 200);
			} else {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, response.getResponseMsg(), 500);
			}
		}

		return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Mã xác minh không đúng!", 400);
	}

	private void sendEmail(String to, String subject, String text) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(to);
		message.setSubject(subject);
		message.setText(text);
		mailSender.send(message);
	}

	private String generateSixDigitCode() {
		return String.valueOf(new Random().nextInt(900000) + 100000);
	}

	public static class CodeRequest {
		private String code;

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}

}
