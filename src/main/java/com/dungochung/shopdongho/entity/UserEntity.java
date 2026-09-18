package com.dungochung.shopdongho.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UuidGenerator;

import com.dungochung.shopdongho.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.AUTO)
	@Column(name = "user_id")
	private String userId;

	@Column(name = "username", unique = true, nullable = false)
	private String username;

	@Column(name = "email", unique = true, nullable = false)
	private String email;

	@JsonIgnore
	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "full_name")
	private String fullName;

	@Column(name = "phone")
	private String phone;

	@Column(name = "address")
	private String address;

	@ManyToOne
	@JoinColumn(name = "role_id", nullable = false)
	private RoleEntity role;

	@Column(name = "is_seller", nullable = false)
	private boolean isSeller;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private UserStatus status;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
	@Column(name = "profile_image", nullable = true, length = 255)
	private String userImage;

	@PrePersist
	protected void onCreate() {
		createdAt = updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public UserEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public UserEntity(String userId, String username, String email, String passwordHash, String fullName, String phone,
			String address, RoleEntity role, boolean isSeller, UserStatus status, LocalDateTime createdAt,
			LocalDateTime updatedAt, String userImage) {
		super();
		this.userId = userId;
		this.username = username;
		this.email = email;
		this.passwordHash = passwordHash;
		this.fullName = fullName;
		this.phone = phone;
		this.address = address;
		this.role = role;
		this.isSeller = isSeller;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.userImage = userImage;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public RoleEntity getRole() {
		return role;
	}

	public void setRole(RoleEntity role) {
		this.role = role;
	}

	public boolean isSeller() {
		return isSeller;
	}

	public void setSeller(boolean isSeller) {
		this.isSeller = isSeller;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getUserImage() {
		return userImage;
	}

	public void setUserImage(String userImage) {
		this.userImage = userImage;
	}

}
