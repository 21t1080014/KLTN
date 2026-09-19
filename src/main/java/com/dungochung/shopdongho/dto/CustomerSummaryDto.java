package com.dungochung.shopdongho.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.dungochung.shopdongho.enums.UserStatus;

/** Một dòng trong danh sách khách hàng: thông tin cơ bản + số liệu mua hàng tổng hợp. */
public class CustomerSummaryDto {
	private final String userId;
	private final String username;
	private final String fullName;
	private final String email;
	private final String phone;
	private final UserStatus status;
	private final LocalDateTime createdAt;
	private final long orderCount;
	private final BigDecimal totalSpent;
	private final LocalDateTime lastOrderAt;

	public CustomerSummaryDto(String userId, String username, String fullName, String email, String phone,
			UserStatus status, LocalDateTime createdAt, Long orderCount, BigDecimal totalSpent,
			LocalDateTime lastOrderAt) {
		this.userId = userId;
		this.username = username;
		this.fullName = fullName;
		this.email = email;
		this.phone = phone;
		this.status = status;
		this.createdAt = createdAt;
		this.orderCount = orderCount == null ? 0 : orderCount;
		this.totalSpent = totalSpent == null ? BigDecimal.ZERO : totalSpent;
		this.lastOrderAt = lastOrderAt;
	}

	public String getUserId() { return userId; }
	public String getUsername() { return username; }
	public String getFullName() { return fullName; }
	public String getEmail() { return email; }
	public String getPhone() { return phone; }
	public UserStatus getStatus() { return status; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public long getOrderCount() { return orderCount; }
	public BigDecimal getTotalSpent() { return totalSpent; }
	public LocalDateTime getLastOrderAt() { return lastOrderAt; }
}
