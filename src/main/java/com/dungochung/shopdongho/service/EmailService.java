package com.dungochung.shopdongho.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

	@Autowired
	private JavaMailSender mailSender;

	/**
	 * Gửi email đơn giản (chỉ text). Nếu cần HTML, có thể dùng MimeMessage, nhưng ở
	 * đây chỉ cần text là đủ.
	 */
	public void sendSimpleMessage(String to, String subject, String text) {
		SimpleMailMessage message = new SimpleMailMessage();
		// Nếu muốn khác "from", bạn có thể setFrom(...) ở đây, nhưng Spring Boot sẽ mặc
		// định
		// dùng spring.mail.username làm từ địa chỉ gửi
		message.setTo(to);
		message.setSubject(subject);
		message.setText(text);
		mailSender.send(message);
	}
}
