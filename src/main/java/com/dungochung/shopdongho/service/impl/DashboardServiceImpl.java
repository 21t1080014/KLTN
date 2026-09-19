package com.dungochung.shopdongho.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dungochung.shopdongho.common.OrderWorkflow;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.enums.OrderStatus;
import com.dungochung.shopdongho.enums.ProductStatus;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.repository.OrderRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.service.DashboardService;
import com.dungochung.shopdongho.service.ReportService;

/**
 * Dashboard theo vai trò: server chỉ đưa vào JSON những widget mà role được xem (không chỉ ẩn ở giao diện), nên doanh thu
 * không lộ cho nhân viên không phải admin.
 * <ul>
 * <li>admin: tất cả (doanh thu, đơn, tồn kho, top bán chạy, hàng đợi việc)</li>
 * <li>support_staff: đơn hàng + hàng đợi CSKH (chờ xác nhận, chờ hoàn tiền)</li>
 * <li>warehouse_staff: đơn hàng + tồn kho + hàng đợi kho (chờ đóng gói, chờ giao)</li>
 * <li>product_staff: tồn kho (xem), top bán chạy, sản phẩm nháp</li>
 * </ul>
 */
@Service
public class DashboardServiceImpl implements DashboardService {
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private InventoryRepository inventoryRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private ReportService reportService;

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto getDashboardData(String role) {
		boolean admin = Constant.ROLE_ADMIN.equals(role);
		boolean support = Constant.ROLE_SUPPORT_STAFF.equals(role);
		boolean warehouse = Constant.ROLE_WAREHOUSE_STAFF.equals(role);
		boolean product = Constant.ROLE_PRODUCT_STAFF.equals(role);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("role", role);
		LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
		LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
		LocalDateTime tomorrow = startOfToday.plusDays(1);

		if (admin || support || warehouse) {
			Map<String, Object> orders = new LinkedHashMap<>();
			orders.put("total", orderRepository.countAllOrders());
			orders.put("today", orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startOfToday, tomorrow));
			orders.put("thisMonth", orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startOfMonth, tomorrow));
			data.put("orders", orders);

			Map<String, Long> byStatus = new LinkedHashMap<>();
			for (Object[] row : orderRepository.countOrdersByStatus()) {
				String key = OrderWorkflow.normalize((OrderStatus) row[0]).name();
				byStatus.merge(key, (Long) row[1], Long::sum);
			}
			data.put("orderStatusCount", byStatus);
			data.put("statusLabels", OrderWorkflow.statusLabels());
		}

		if (admin) {
			Map<String, Object> revenue = new LinkedHashMap<>();
			revenue.put("today", orderRepository.sumRevenue(startOfToday, tomorrow));
			revenue.put("thisMonth", orderRepository.sumRevenue(startOfMonth, tomorrow));
			revenue.put("allTime", orderRepository.sumRevenue(LocalDateTime.of(2000, 1, 1, 0, 0), tomorrow));
			List<Object[]> refund = orderRepository.refundPendingSummary();
			Object[] r = refund.isEmpty() ? new Object[] { 0L, BigDecimal.ZERO } : refund.get(0);
			revenue.put("refundPendingCount", ((Number) r[0]).longValue());
			revenue.put("refundPendingAmount", new BigDecimal(r[1].toString()));
			data.put("revenue", revenue);
			data.put("revenueLast14Days", ((Map<?, ?>) reportService
					.revenueSeries("day", LocalDate.now().minusDays(13), LocalDate.now()).getData()).get("points"));
		}

		if (admin || warehouse || product) {
			Map<String, Object> inv = new LinkedHashMap<>();
			inv.put("totalQuantity", inventoryRepository.getTotalInventory());
			Map<?, ?> low = (Map<?, ?>) reportService.lowStock(8).getData();
			inv.put("lowStockCount", low.get("count"));
			inv.put("outOfStockCount", low.get("outOfStock"));
			inv.put("lowStockItems", low.get("items"));
			data.put("inventory", inv);
		}

		if (admin || product) {
			List<Map<String, Object>> top = new java.util.ArrayList<>();
			for (Object o : (List<?>) ((Map<?, ?>) reportService.topProducts(null, null, 5).getData()).get("products")) {
				@SuppressWarnings("unchecked")
				Map<String, Object> row = new LinkedHashMap<>((Map<String, Object>) o);
				if (!admin) {
					row.remove("revenue"); // doanh thu theo sản phẩm chỉ dành cho admin
				}
				top.add(row);
			}
			data.put("topProducts", top);
		}

		// Hàng đợi việc cần làm ngay theo vai trò
		Map<String, Object> queue = new LinkedHashMap<>();
		if (admin || support) {
			queue.put("pendingConfirm", orderRepository.countByOrderStatus(OrderStatus.pending));
			if (!admin) {
				List<Object[]> refund = orderRepository.refundPendingSummary();
				queue.put("refundPending", refund.isEmpty() ? 0L : ((Number) refund.get(0)[0]).longValue());
			}
		}
		if (admin || warehouse) {
			@SuppressWarnings("deprecation")
			long toPack = orderRepository.countByOrderStatus(OrderStatus.confirmed)
					+ orderRepository.countByOrderStatus(OrderStatus.processing);
			queue.put("toPack", toPack);
			queue.put("toShip", orderRepository.countByOrderStatus(OrderStatus.packing));
		}
		if (admin || product) {
			queue.put("draftProducts", productRepository.countByStatus(ProductStatus.DRAFT));
		}
		data.put("queue", queue);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Thành công", data);
	}
}
