package com.dungochung.shopdongho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dungochung.shopdongho.common.CurrentActor;
import com.dungochung.shopdongho.common.OrderActor;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.enums.OrderStatus;
import com.dungochung.shopdongho.enums.PaymentStatus;
import com.dungochung.shopdongho.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/orders")
public class OrderController {
	@Autowired
	private OrderService orderService;

	@GetMapping
	public String showOrder(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "admin/orders";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getAllOrder(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(required = false) OrderStatus status,
			HttpSession session) {
		return orderService.getAllOrder(page, size, role(session), status);
	}

	@GetMapping("/api/{id}")
	@ResponseBody
	public ResponseDataDto getOrderDetail(@PathVariable("id") Integer orderId, HttpSession session) {
		return orderService.getOrderDetail(orderId, role(session));
	}

	/** Chuyển trạng thái đơn theo luồng (có kiểm tra vai trò, lý do bắt buộc khi hủy/hoàn, tác động kho). */
	@PutMapping("/api/{id}/status")
	@ResponseBody
	public ResponseDataDto changeStatus(@PathVariable("id") Integer orderId, @RequestParam OrderStatus to,
			@RequestParam(required = false) String reason, HttpSession session) {
		return orderService.changeStatus(orderId, to, reason, actor(session));
	}

	@PutMapping("/api/{id}/payment")
	@ResponseBody
	public ResponseDataDto changePayment(@PathVariable("id") Integer orderId, @RequestParam PaymentStatus to,
			@RequestParam(required = false) String note, HttpSession session) {
		return orderService.changePayment(orderId, to, note, actor(session));
	}

	/** API cũ: vẫn nhận cả 2 trạng thái nhưng mỗi thay đổi đều qua kiểm tra luồng như 2 API trên. */
	@PutMapping("/api/{id}")
	@ResponseBody
	public ResponseDataDto updateOrder(@PathVariable("id") Integer orderId, @RequestParam OrderStatus orderStatus,
			@RequestParam PaymentStatus paymentStatus, HttpSession session) {
		return orderService.updateOrder(orderId, orderStatus, paymentStatus, actor(session));
	}

	@DeleteMapping("/api/{id}")
	@ResponseBody
	public ResponseDataDto deleteOrder(@PathVariable("id") Integer orderId, HttpSession session) {
		return orderService.deleteOrder(orderId, actor(session));
	}

	private static String role(HttpSession session) {
		return (String) session.getAttribute("roleName");
	}

	private static OrderActor actor(HttpSession session) {
		return OrderActor.admin(CurrentActor.username(), role(session));
	}
}
