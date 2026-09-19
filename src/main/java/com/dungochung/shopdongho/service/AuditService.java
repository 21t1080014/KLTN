package com.dungochung.shopdongho.service;

import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface AuditService {
	/** Ghi log với người thao tác lấy từ phiên hiện tại (username + role + IP). */
	void log(String action, String targetType, String targetId, String detail);

	/** Ghi log với người thao tác chỉ định rõ (dùng cho đăng nhập, khi chưa/không có phiên). */
	void logAs(String actor, String role, String action, String targetType, String targetId, String detail);

	ResponseDataDto search(String action, String actor, int page, int size);
}
