package com.dungochung.shopdongho.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

/** Audit trail khóa/mở khóa/kích hoạt tài khoản. Không FK để log còn nguyên nếu tài khoản bị xóa. */
@Entity
@Table(name = "user_status_history", indexes = @Index(name = "idx_ush_user", columnList = "user_id"))
public class UserStatusHistoryEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "history_id")
	private Long historyId;

	@Column(name = "user_id", nullable = false)
	private String userId;

	@Column(name = "from_status", length = 20)
	private String fromStatus;

	@Column(name = "to_status", nullable = false, length = 20)
	private String toStatus;

	@Column(name = "reason", length = 255)
	private String reason;

	@Column(name = "changed_by", length = 100)
	private String changedBy;

	@Column(name = "changed_at", nullable = false, updatable = false)
	private LocalDateTime changedAt;

	@PrePersist
	protected void onCreate() {
		changedAt = LocalDateTime.now();
	}

	public Long getHistoryId() { return historyId; }
	public String getUserId() { return userId; }
	public void setUserId(String userId) { this.userId = userId; }
	public String getFromStatus() { return fromStatus; }
	public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
	public String getToStatus() { return toStatus; }
	public void setToStatus(String toStatus) { this.toStatus = toStatus; }
	public String getReason() { return reason; }
	public void setReason(String reason) { this.reason = reason; }
	public String getChangedBy() { return changedBy; }
	public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
	public LocalDateTime getChangedAt() { return changedAt; }
}
