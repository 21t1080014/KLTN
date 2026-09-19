package com.dungochung.shopdongho.service;

import java.time.LocalDate;

import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface ReportService {
	/** Doanh thu theo ngày/tuần/tháng (granularity = day|week|month); from/to (bao gồm cả 2 đầu) có thể null => mặc định theo granularity. */
	ResponseDataDto revenueSeries(String granularity, LocalDate from, LocalDate to);

	/** Top sản phẩm bán chạy trong kỳ (from/to null => 30 ngày gần nhất). */
	ResponseDataDto topProducts(LocalDate from, LocalDate to, int limit);

	/** Các biến thể sắp hết hàng (bán được <= ngưỡng) của sản phẩm đang bán. */
	ResponseDataDto lowStock(int limit);

	/** CSV danh sách đơn trong kỳ để đối soát/kế toán. */
	String ordersCsv(LocalDate from, LocalDate to);
}
