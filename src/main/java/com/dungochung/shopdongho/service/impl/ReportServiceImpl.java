package com.dungochung.shopdongho.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dungochung.shopdongho.common.CsvUtil;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.entity.OrderEntity;
import com.dungochung.shopdongho.enums.ProductStatus;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.repository.OrderItemRepository;
import com.dungochung.shopdongho.repository.OrderRepository;
import com.dungochung.shopdongho.service.ReportService;

@Service
public class ReportServiceImpl implements ReportService {
	private static final int MAX_DAYS_DAILY = 400;
	private static final int MAX_DAYS_OTHER = 1100;
	private static final int MAX_LIMIT = 100;

	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private OrderItemRepository orderItemRepository;
	@Autowired
	private InventoryRepository inventoryRepository;

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto revenueSeries(String granularity, LocalDate from, LocalDate to) {
		String g = granularity == null ? "day" : granularity.trim().toLowerCase();
		if (!g.equals("day") && !g.equals("week") && !g.equals("month")) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "granularity phải là day, week hoặc month");
		}
		LocalDate today = LocalDate.now();
		LocalDate end = to != null ? to : today;
		LocalDate start = from != null ? from : switch (g) {
			case "week" -> end.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(11);
			case "month" -> end.withDayOfMonth(1).minusMonths(11);
			default -> end.minusDays(29);
		};
		String err = validateRange(start, end, g.equals("day") ? MAX_DAYS_DAILY : MAX_DAYS_OTHER);
		if (err != null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, err);
		}
		LocalDateTime fromDt = start.atStartOfDay();
		LocalDateTime toDt = end.plusDays(1).atStartOfDay();
		List<Object[]> rows = switch (g) {
			case "week" -> orderRepository.revenueByWeek(fromDt, toDt);
			case "month" -> orderRepository.revenueByMonth(fromDt, toDt);
			default -> orderRepository.revenueByDay(fromDt, toDt);
		};
		Map<LocalDate, Object[]> byBucket = new HashMap<>();
		for (Object[] r : rows) {
			byBucket.put(toLocalDate(r[0]), r);
		}

		// Điền các kỳ không có đơn bằng 0 để biểu đồ liên tục
		LocalDate cursor = switch (g) {
			case "week" -> start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			case "month" -> start.withDayOfMonth(1);
			default -> start;
		};
		List<Map<String, Object>> points = new ArrayList<>();
		BigDecimal totalRevenue = BigDecimal.ZERO;
		long totalOrders = 0;
		while (!cursor.isAfter(end)) {
			Object[] r = byBucket.get(cursor);
			long cnt = r == null ? 0 : ((Number) r[1]).longValue();
			BigDecimal rev = r == null ? BigDecimal.ZERO : new BigDecimal(r[2].toString());
			Map<String, Object> pt = new LinkedHashMap<>();
			pt.put("bucket", cursor.toString());
			pt.put("label", label(g, cursor));
			pt.put("orders", cnt);
			pt.put("revenue", rev);
			points.add(pt);
			totalRevenue = totalRevenue.add(rev);
			totalOrders += cnt;
			cursor = switch (g) {
				case "week" -> cursor.plusWeeks(1);
				case "month" -> cursor.plusMonths(1);
				default -> cursor.plusDays(1);
			};
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("granularity", g);
		data.put("from", start.toString());
		data.put("to", end.toString());
		data.put("points", points);
		data.put("totalRevenue", totalRevenue);
		data.put("totalOrders", totalOrders);
		data.put("avgOrderValue", totalOrders == 0 ? BigDecimal.ZERO
				: totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, RoundingMode.HALF_UP));
		data.put("definition", "Doanh thu = đơn đã giao/hoàn thành và đã thanh toán, tính theo ngày đặt hàng");
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "OK", data);
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto topProducts(LocalDate from, LocalDate to, int limit) {
		LocalDate end = to != null ? to : LocalDate.now();
		LocalDate start = from != null ? from : end.minusDays(29);
		String err = validateRange(start, end, MAX_DAYS_OTHER);
		if (err != null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, err);
		}
		int n = Math.min(Math.max(limit, 1), MAX_LIMIT);
		List<Map<String, Object>> list = new ArrayList<>();
		int rank = 1;
		for (Object[] r : orderItemRepository.topProducts(start.atStartOfDay(), end.plusDays(1).atStartOfDay(),
				PageRequest.of(0, n))) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("rank", rank++);
			m.put("productId", r[0]);
			m.put("name", r[1]);
			m.put("sku", r[2]);
			m.put("quantity", ((Number) r[3]).longValue());
			m.put("revenue", new BigDecimal(r[4].toString()));
			list.add(m);
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("from", start.toString());
		data.put("to", end.toString());
		data.put("products", list);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "OK", data);
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto lowStock(int limit) {
		int n = Math.min(Math.max(limit, 1), MAX_LIMIT);
		List<InventoryEntity> low = inventoryRepository.findLowStock().stream()
				.filter(i -> i.getVariant() != null && i.getVariant().getStatus() == ProductStatus.ACTIVE
						&& i.getProduct() != null && i.getProduct().getStatus() == ProductStatus.ACTIVE)
				.sorted(Comparator.comparingInt(InventoryEntity::getAvailableQuantity)).toList();
		List<Map<String, Object>> list = new ArrayList<>();
		for (InventoryEntity i : low.stream().limit(n).toList()) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("sku", i.getVariant().getSku());
			m.put("productName", i.getProduct().getName());
			m.put("variantName", i.getVariant().getVariantName());
			m.put("quantity", i.getQuantity());
			m.put("reserved", i.getReservedQuantity());
			m.put("available", i.getAvailableQuantity());
			m.put("threshold", i.getLowStockThreshold());
			list.add(m);
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("count", low.size());
		data.put("outOfStock", low.stream().filter(i -> i.getAvailableQuantity() <= 0).count());
		data.put("items", list);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "OK", data);
	}

	@Override
	@Transactional(readOnly = true)
	public String ordersCsv(LocalDate from, LocalDate to) {
		LocalDate end = to != null ? to : LocalDate.now();
		LocalDate start = from != null ? from : end.minusDays(29);
		String err = validateRange(start, end, MAX_DAYS_DAILY);
		if (err != null) {
			throw new IllegalArgumentException(err);
		}
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		StringBuilder sb = new StringBuilder("﻿");
		sb.append("order_id,created_at,customer_username,customer_email,total_price,payment_method,payment_status,order_status,cancel_reason\n");
		for (OrderEntity o : orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
				start.atStartOfDay(), end.plusDays(1).atStartOfDay())) {
			sb.append(o.getOrderId()).append(',').append(o.getCreatedAt().format(fmt)).append(',')
					.append(CsvUtil.cell(o.getUser() == null ? null : o.getUser().getUsername())).append(',')
					.append(CsvUtil.cell(o.getUser() == null ? null : o.getUser().getEmail())).append(',')
					.append(o.getTotalPrice().toPlainString()).append(',').append(o.getPaymentMethod()).append(',')
					.append(o.getPaymentStatus()).append(',').append(o.getOrderStatus()).append(',')
					.append(CsvUtil.cell(o.getCancelReason())).append('\n');
		}
		return sb.toString();
	}

	private static String validateRange(LocalDate from, LocalDate to, int maxDays) {
		if (from.isAfter(to)) {
			return "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc";
		}
		if (ChronoUnit.DAYS.between(from, to) > maxDays) {
			return "Khoảng thời gian tối đa " + maxDays + " ngày";
		}
		return null;
	}

	private static LocalDate toLocalDate(Object o) {
		if (o instanceof Date d) {
			return d.toLocalDate();
		}
		if (o instanceof LocalDate d) {
			return d;
		}
		if (o instanceof java.util.Date d) {
			return new Date(d.getTime()).toLocalDate();
		}
		return LocalDate.parse(o.toString().substring(0, 10));
	}

	private static String label(String g, LocalDate d) {
		return switch (g) {
			case "month" -> d.format(DateTimeFormatter.ofPattern("MM/yyyy"));
			case "week" -> "Tuần " + d.format(DateTimeFormatter.ofPattern("dd/MM"));
			default -> d.format(DateTimeFormatter.ofPattern("dd/MM"));
		};
	}
}
