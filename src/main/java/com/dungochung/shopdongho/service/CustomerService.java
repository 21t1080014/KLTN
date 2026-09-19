package com.dungochung.shopdongho.service;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.enums.UserStatus;

public interface CustomerService {
	/** Danh sách khách hàng + số đơn / chi tiêu / hạng; lọc theo từ khóa, trạng thái; sắp xếp newest|spent|orders|recent|name. */
	ResponseDataDto list(String keyword, UserStatus status, String sort, int page, int size);

	/** Hồ sơ 1 khách: thông tin, thống kê mua hàng, hạng, đơn gần đây, lịch sử khóa/mở, ghi chú nội bộ. */
	ResponseDataDto detail(String userId);

	/** Khóa / mở khóa / kích hoạt tài khoản khách. Khóa bắt buộc có lý do; ghi lịch sử. */
	ResponseDataDto changeStatus(String userId, UserStatus to, String reason, String actor);

	ResponseDataDto updateNote(String userId, String note);
}
