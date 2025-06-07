package com.dungochung.shopdongho.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalModelAttributeAdvice {

	@ModelAttribute
	public void addCommonAttributes(Model model, HttpSession session) {
		model.addAttribute("currentUser", session.getAttribute("currentUser"));
	}
}
