package com.dungochung.shopdongho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.service.DashboardService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/dashboard")
public class DashboardController {
	@Autowired
	private DashboardService dashboardService;
	@GetMapping
	public String showDashboard(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "admin/dashboard";
	}
	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto getDashboard() {
		return dashboardService.getDashboardData();
	}
}
