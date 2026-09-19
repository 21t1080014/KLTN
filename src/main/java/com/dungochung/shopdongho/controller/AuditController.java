package com.dungochung.shopdongho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.service.AuditService;

import jakarta.servlet.http.HttpServletRequest;

/** Xem nhật ký thao tác nhạy cảm (chỉ admin — xem AdminPermissions). */
@Controller
@RequestMapping("/admin/audit")
public class AuditController {
	@Autowired
	private AuditService auditService;

	@GetMapping
	public String show(Model model, HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "admin/audit";
	}

	@GetMapping("/api")
	@ResponseBody
	public ResponseDataDto search(@RequestParam(required = false) String action,
			@RequestParam(required = false) String actor, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return auditService.search(action, actor, page, size);
	}
}
