package com.dungochung.shopdongho.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

/**
 * Nhật ký thao tác nhạy cảm ở khu quản trị (đăng nhập, quản lý người dùng, xóa dữ liệu). Chỉ ghi thêm, không sửa/xóa.
 * Không FK tới bảng nào để log còn nguyên khi đối tượng bị xóa.
 */
@Entity
@Table(name = "admin_audit_log", indexes = { @Index(name = "idx_audit_created", columnList = "created_at"),
		@Index(name = "idx_audit_actor", columnList = "actor") })
public class AuditLogEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "audit_id")
	private Long auditId;

	@Column(name = "actor", length = 100)
	private String actor;

	@Column(name = "actor_role", length = 30)
	private String actorRole;

	@Column(name = "action", nullable = false, length = 40)
	private String action;

	@Column(name = "target_type", length = 40)
	private String targetType;

	@Column(name = "target_id", length = 100)
	private String targetId;

	@Column(name = "detail", length = 500)
	private String detail;

	@Column(name = "ip", length = 64)
	private String ip;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}

	public Long getAuditId() { return auditId; }
	public String getActor() { return actor; }
	public void setActor(String actor) { this.actor = actor; }
	public String getActorRole() { return actorRole; }
	public void setActorRole(String actorRole) { this.actorRole = actorRole; }
	public String getAction() { return action; }
	public void setAction(String action) { this.action = action; }
	public String getTargetType() { return targetType; }
	public void setTargetType(String targetType) { this.targetType = targetType; }
	public String getTargetId() { return targetId; }
	public void setTargetId(String targetId) { this.targetId = targetId; }
	public String getDetail() { return detail; }
	public void setDetail(String detail) { this.detail = detail; }
	public String getIp() { return ip; }
	public void setIp(String ip) { this.ip = ip; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
