package com.dungochung.shopdongho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.enums.OrderStatus;
import com.dungochung.shopdongho.enums.PaymentMethod;
import com.dungochung.shopdongho.enums.PaymentStatus;
import com.dungochung.shopdongho.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;

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
			@RequestParam(defaultValue = "5") int size) {
		return orderService.getAllOrder(page, size);
	}

	@PutMapping("/api/{id}")
	@ResponseBody
	public ResponseDataDto updateOrder(@PathVariable("id") Integer orderId, @RequestParam OrderStatus orderStatus,
			@RequestParam PaymentStatus paymentStatus) {
		return orderService.updateOrder(orderId, orderStatus, paymentStatus);
	}

	@DeleteMapping("/api/{id}")
	@ResponseBody
	public ResponseDataDto deleteOrder(@PathVariable("id") Integer orderId) {
		return orderService.deleteOrder(orderId);
	}
}
