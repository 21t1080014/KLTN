package com.dungochung.shopdongho.service;

import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface DashboardService {
	/** Dữ liệu dashboard đã lọc theo vai trò của người xem. */
	ResponseDataDto getDashboardData(String role);
}
