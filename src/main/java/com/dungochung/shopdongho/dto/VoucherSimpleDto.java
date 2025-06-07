package com.dungochung.shopdongho.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VoucherSimpleDto {
	private String voucherCode;
	private String description;
	private String discountType;
	private BigDecimal discountValue;
	private BigDecimal minOrderAmount;
	private LocalDate startAt;
	private LocalDate endAt;

	// Constructors
	public VoucherSimpleDto() {
	}

	public VoucherSimpleDto(String voucherCode, String description, String discountType, BigDecimal discountValue,
			BigDecimal minOrderAmount, LocalDate startAt, LocalDate endAt) {
		this.voucherCode = voucherCode;
		this.description = description;
		this.discountType = discountType;
		this.discountValue = discountValue;
		this.minOrderAmount = minOrderAmount;
		this.startAt = startAt;
		this.endAt = endAt;
	}

	// Getters & setters
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

	public String getDiscountType() {
		return discountType;
	}

	public void setDiscountType(String discountType) {
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
}
