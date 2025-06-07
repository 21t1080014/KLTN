package com.dungochung.shopdongho.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

import com.dungochung.shopdongho.entity.PromotionEntity.DiscountType;

import jakarta.persistence.*;

@Entity
@Table(name = "vouchers")
public class VoucherEntity {
	@Id
	@Column(name = "voucher_code", length = 50)
	private String voucherCode;

	@Column(length = 255)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "discount_type")
	private DiscountType discountType;

	@Column(name = "discount_value", precision = 38, scale = 2)
	private BigDecimal discountValue;

	@Column(name = "min_order_amount", precision = 38, scale = 2)
	private BigDecimal minOrderAmount;

	@Column(name = "start_at")
	private LocalDate startAt;

	@Column(name = "end_at")
	private LocalDate endAt;

	@Column(name = "usage_limit")
	private int usageLimit;

	@Column(name = "created_at", updatable = false, insertable = false)
	private Timestamp createdAt;

	public VoucherEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public String getVoucherCode() {
		return voucherCode;
	}

	public void setVoucherCode(String voucherCode) {
		this.voucherCode = voucherCode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public DiscountType getDiscountType() {
		return discountType;
	}

	public void setDiscountType(DiscountType discountType) {
		this.discountType = discountType;
	}

	public BigDecimal getDiscountValue() {
		return discountValue;
	}

	public void setDiscountValue(BigDecimal discountValue) {
		this.discountValue = discountValue;
	}

	public BigDecimal getMinOrderAmount() {
		return minOrderAmount;
	}

	public void setMinOrderAmount(BigDecimal minOrderAmount) {
		this.minOrderAmount = minOrderAmount;
	}

	public LocalDate getStartAt() {
		return startAt;
	}

	public void setStartAt(LocalDate startAt) {
		this.startAt = startAt;
	}

	public LocalDate getEndAt() {
		return endAt;
	}

	public void setEndAt(LocalDate endAt) {
		this.endAt = endAt;
	}

	public int getUsageLimit() {
		return usageLimit;
	}

	public void setUsageLimit(int usageLimit) {
		this.usageLimit = usageLimit;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}
	

}
