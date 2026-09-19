package com.dungochung.shopdongho.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dungochung.shopdongho.common.CustomerTier;
import com.dungochung.shopdongho.common.OrderWorkflow;
import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.CustomerSummaryDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.OrderEntity;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.entity.UserStatusHistoryEntity;
import com.dungochung.shopdongho.enums.OrderStatus;
import com.dungochung.shopdongho.enums.PaymentStatus;
import com.dungochung.shopdongho.enums.UserStatus;
import com.dungochung.shopdongho.repository.OrderRepository;
import com.dungochung.shopdongho.repository.UserRepository;
import com.dungochung.shopdongho.repository.UserStatusHistoryRepository;
import com.dungochung.shopdongho.service.CustomerService;

@Service
public class CustomerServiceImpl implements CustomerService {
	private static final int MAX_REASON_LENGTH = 255;
	private static final int MAX_NOTE_LENGTH = 1000;
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private UserStatusHistoryRepository historyRepository;

	@Value("${shop.tier.gold-min-spend:20000000}")
	private BigDecimal goldMin;
	@Value("${shop.tier.diamond-min-spend:100000000}")
	private BigDecimal diamondMin;

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto list(String keyword, UserStatus status, String sort, int page, int size) {
		int safeSize = Math.min(Math.max(size, 1), 100);
		int safePage = Math.max(page, 0);
		Page<CustomerSummaryDto> result = userRepository.searchCustomers(keyword, status, sort,
				PageRequest.of(safePage, safeSize));
		List<Map<String, Object>> rows = new ArrayList<>();
		for (CustomerSummaryDto c : result.getContent()) {
			Map<String, Object> m = new HashMap<>();
			m.put("userId", c.getUserId());
			m.put("username", c.getUsername());
			m.put("fullName", c.getFullName());
			m.put("email", c.getEmail());
			m.put("phone", c.getPhone());
			m.put("status", c.getStatus().name());
			m.put("createdAt", c.getCreatedAt() == null ? null : c.getCreatedAt().format(DATE_TIME));
			m.put("orderCount", c.getOrderCount());
			m.put("totalSpent", c.getTotalSpent());
			m.put("tier", CustomerTier.of(c.getTotalSpent(), goldMin, diamondMin).label());
			m.put("lastOrderAt", c.getLastOrderAt() == null ? null : c.getLastOrderAt().format(DATE_TIME));
			rows.add(m);
		}
		Map<String, Object> data = new HashMap<>();
		data.put("customers", rows);
		data.put("totalCount", result.getTotalElements());
		data.put("pagination", PaginationCommon.getPaginationInfo(safePage, result.getTotalPages(), 3));
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "OK", data);
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto detail(String userId) {
		UserEntity u = customerOrNull(userId);
		if (u == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy khách hàng");
		}
		Map<String, Object> data = new HashMap<>();
		data.put("userId", u.getUserId());
		data.put("username", u.getUsername());
		data.put("fullName", u.getFullName());
		data.put("email", u.getEmail());
		data.put("phone", u.getPhone());
		data.put("address", u.getAddress());
		data.put("status", u.getStatus().name());
		data.put("createdAt", u.getCreatedAt() == null ? null : u.getCreatedAt().format(DATE_TIME));
		data.put("lockReason", u.getLockReason());
		data.put("lockedBy", u.getLockedBy());
		data.put("lockedAt", u.getLockedAt() == null ? null : u.getLockedAt().format(DATE_TIME));
		data.put("internalNote", u.getInternalNote());

		// Thống kê mua hàng (1 truy vấn nhóm theo trạng thái)
		long total = 0, done = 0, canceled = 0, refunded = 0, spendOrders = 0;
		BigDecimal spent = BigDecimal.ZERO;
		LocalDateTime first = null, last = null;
		for (Object[] r : orderRepository.summarizeByUser(userId)) {
			OrderStatus os = OrderWorkflow.normalize((OrderStatus) r[0]);
			PaymentStatus ps = (PaymentStatus) r[1];
			long cnt = ((Number) r[2]).longValue();
			BigDecimal sum = (BigDecimal) r[3];
			total += cnt;
			if (os == OrderStatus.canceled) {
				canceled += cnt;
			} else if (os == OrderStatus.refunded) {
				refunded += cnt;
			} else if (os == OrderStatus.completed || os == OrderStatus.delivered) {
				done += cnt;
			}
			if (CustomerTier.SPEND_STATUSES.contains(os) && ps == CustomerTier.SPEND_PAYMENT) {
				spent = spent.add(sum);
				spendOrders += cnt;
			}
			LocalDateTime mn = (LocalDateTime) r[4];
			LocalDateTime mx = (LocalDateTime) r[5];
			if (first == null || (mn != null && mn.isBefore(first))) {
				first = mn;
			}
			if (last == null || (mx != null && mx.isAfter(last))) {
				last = mx;
			}
		}
		Map<String, Object> stats = new HashMap<>();
		stats.put("orderCount", total);
		stats.put("deliveredCount", done);
		stats.put("canceledCount", canceled);
		stats.put("refundedCount", refunded);
		stats.put("totalSpent", spent);
		stats.put("avgOrderValue", spendOrders == 0 ? BigDecimal.ZERO
				: spent.divide(BigDecimal.valueOf(spendOrders), 0, RoundingMode.HALF_UP));
		stats.put("firstOrderAt", first == null ? null : first.format(DATE_TIME));
		stats.put("lastOrderAt", last == null ? null : last.format(DATE_TIME));
		CustomerTier tier = CustomerTier.of(spent, goldMin, diamondMin);
		stats.put("tier", tier.label());
		if (tier == CustomerTier.SILVER) {
			stats.put("nextTier", CustomerTier.GOLD.label());
			stats.put("toNextTier", goldMin.subtract(spent));
		} else if (tier == CustomerTier.GOLD) {
			stats.put("nextTier", CustomerTier.DIAMOND.label());
			stats.put("toNextTier", diamondMin.subtract(spent));
		}
		data.put("stats", stats);

		List<Map<String, Object>> orders = new ArrayList<>();
		Map<String, String> sl = OrderWorkflow.statusLabels();
		Map<String, String> pl = OrderWorkflow.paymentLabels();
		for (OrderEntity o : orderRepository
				.findByUserId(userId, PageRequest.of(0, 20, Sort.by("createdAt").descending())).getContent()) {
			Map<String, Object> om = new HashMap<>();
			om.put("orderId", o.getOrderId());
			om.put("createdAt", o.getCreatedAt().format(DATE_TIME));
			om.put("totalPrice", o.getTotalPrice());
			om.put("orderStatus", sl.get(o.getOrderStatus().name()));
			om.put("paymentStatus", pl.get(o.getPaymentStatus().name()));
			orders.add(om);
		}
		data.put("orders", orders);

		List<Map<String, Object>> history = new ArrayList<>();
		for (UserStatusHistoryEntity h : historyRepository.findByUserIdOrderByHistoryIdDesc(userId)) {
			Map<String, Object> hm = new HashMap<>();
			hm.put("from", h.getFromStatus());
			hm.put("to", h.getToStatus());
			hm.put("reason", h.getReason());
			hm.put("by", h.getChangedBy());
			hm.put("at", h.getChangedAt().format(DATE_TIME));
			history.add(hm);
		}
		data.put("statusHistory", history);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "OK", data);
	}

	@Override
	@Transactional
	public ResponseDataDto changeStatus(String userId, UserStatus to, String reason, String actor) {
		UserEntity u = customerOrNull(userId);
		if (u == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy khách hàng");
		}
		if (to == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Thiếu trạng thái");
		}
		UserStatus from = u.getStatus();
		if (from == to) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Tài khoản đã ở trạng thái này");
		}
		if (to == UserStatus.PENDING) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không thể đưa tài khoản về trạng thái chờ kích hoạt");
		}
		String clean = reason == null || reason.isBlank() ? null : reason.trim();
		if (to == UserStatus.LOCKED && clean == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Vui lòng nhập lý do khóa tài khoản");
		}
		if (clean != null && clean.length() > MAX_REASON_LENGTH) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Lý do quá dài (tối đa " + MAX_REASON_LENGTH + " ký tự)");
		}
		u.setStatus(to);
		if (to == UserStatus.LOCKED) {
			u.setLockReason(clean);
			u.setLockedAt(LocalDateTime.now());
			u.setLockedBy(actor);
		} else {
			u.setLockReason(null);
			u.setLockedAt(null);
			u.setLockedBy(null);
		}
		userRepository.save(u);
		UserStatusHistoryEntity h = new UserStatusHistoryEntity();
		h.setUserId(userId);
		h.setFromStatus(from.name());
		h.setToStatus(to.name());
		h.setReason(clean);
		h.setChangedBy(actor);
		historyRepository.save(h);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS,
				to == UserStatus.LOCKED ? "Đã khóa tài khoản" : "Đã mở khóa/kích hoạt tài khoản", userId);
	}

	@Override
	@Transactional
	public ResponseDataDto updateNote(String userId, String note) {
		UserEntity u = customerOrNull(userId);
		if (u == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy khách hàng");
		}
		String clean = note == null || note.isBlank() ? null : note.trim();
		if (clean != null && clean.length() > MAX_NOTE_LENGTH) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Ghi chú quá dài (tối đa " + MAX_NOTE_LENGTH + " ký tự)");
		}
		u.setInternalNote(clean);
		userRepository.save(u);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Đã lưu ghi chú", userId);
	}

	/** Chỉ thao tác trên tài khoản khách; nhân viên quản lý ở trang Người dùng (chỉ admin). */
	private UserEntity customerOrNull(String userId) {
		UserEntity u = userRepository.findById(userId).orElse(null);
		if (u == null || u.getRole() == null || !Constant.ROLE_CUSTOMER.equals(u.getRole().getRoleName())) {
			return null;
		}
		return u;
	}
}
