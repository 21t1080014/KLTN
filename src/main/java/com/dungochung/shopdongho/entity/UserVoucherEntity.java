package com.dungochung.shopdongho.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "user_vouchers")
public class UserVoucherEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@ManyToOne
	@JoinColumn(name = "voucher_code", nullable = false)
	private VoucherEntity voucher;

	@Column(name = "used_at")
	private LocalDateTime usedAt;

	public UserVoucherEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}

	public VoucherEntity getVoucher() {
		return voucher;
	}

	public void setVoucher(VoucherEntity voucher) {
		this.voucher = voucher;
	}

	public LocalDateTime getUsedAt() {
		return usedAt;
	}

	public void setUsedAt(LocalDateTime usedAt) {
		this.usedAt = usedAt;
	}

}
