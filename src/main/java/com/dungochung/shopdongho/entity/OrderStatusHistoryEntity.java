package com.dungochung.shopdongho.entity;

import java.time.LocalDateTime;

import com.dungochung.shopdongho.enums.OrderHistoryKind;

import jakarta.persistence.*;

/** Audit trail đơn hàng: mỗi lần đổi trạng thái đơn/thanh toán ghi 1 dòng. Không FK để vẫn còn khi xóa đơn. */
@Entity
@Table(name = "order_status_history", indexes = @Index(name = "idx_osh_order", columnList = "order_id"))
public class OrderStatusHistoryEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "history_id")
	private Long historyId;

	@Column(name = "order_id", nullable = false)
	private Integer orderId;

	@Enumerated(EnumType.STRING)
	@Column(name = "kind", nullable = false, length = 20)
	private OrderHistoryKind kind;

	@Column(name = "from_value", length = 30)
	private String fromValue;

	@Column(name = "to_value", nullable = false, length = 30)
	private String toValue;

	@Column(name = "reason", length = 255)
	private String reason;

	@Column(name = "changed_by", length = 100)
	private String changedBy;

	@Column(name = "changed_by_role", length = 30)
	private String changedByRole;

	@Column(name = "changed_at", nullable = false, updatable = false)
	private LocalDateTime changedAt;

	@PrePersist
	protected void onCreate() {
		changedAt = LocalDateTime.now();
	}

	public Long getHistoryId() { return historyId; }
	public Integer getOrderId() { return orderId; }
	public void setOrderId(Integer orderId) { this.orderId = orderId; }
	public OrderHistoryKind getKind() { return kind; }
	public void setKind(OrderHistoryKind kind) { this.kind = kind; }
	public String getFromValue() { return fromValue; }
	public void setFromValue(String fromValue) { this.fromValue = fromValue; }
	public String getToValue() { return toValue; }
	public void setToValue(String toValue) { this.toValue = toValue; }
	public String getReason() { return reason; }
	public void setReason(String reason) { this.reason = reason; }
	public String getChangedBy() { return changedBy; }
	public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
	public String getChangedByRole() { return changedByRole; }
	public void setChangedByRole(String changedByRole) { this.changedByRole = changedByRole; }
	public LocalDateTime getChangedAt() { return changedAt; }
}
