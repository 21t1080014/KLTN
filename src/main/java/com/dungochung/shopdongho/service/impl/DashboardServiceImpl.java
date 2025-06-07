package com.dungochung.shopdongho.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.repository.OrderRepository;
import com.dungochung.shopdongho.service.DashboardService;

@Service
public class DashboardServiceImpl implements DashboardService {
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private InventoryRepository inventoryRepository;

	@Override
	public ResponseDataDto getDashboardData() {
		Map<String, Object> data = new HashMap<>();

		long totalOrders = orderRepository.countAllOrders();
		BigDecimal totalRevenue = orderRepository.getTotalRevenue();
		long totalInventory = inventoryRepository.getTotalInventory();

		Map<String, Long> orderStatusCount = new HashMap<>();
		List<Object[]> statusList = orderRepository.countOrdersByStatus();
		for (Object[] row : statusList) {
			String status = row[0].toString();
			Long count = (Long) row[1];
			orderStatusCount.put(status, count);
		}

		data.put("totalOrders", totalOrders);
		data.put("totalRevenue", totalRevenue);
		data.put("totalInventory", totalInventory);
		data.put("orderStatusCount", orderStatusCount);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Thành công", data);
	}

}
