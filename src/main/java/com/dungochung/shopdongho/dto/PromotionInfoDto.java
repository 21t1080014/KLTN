package com.dungochung.shopdongho.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PromotionInfoDto {
	private Integer promotionId;
	private String name;
	private String discountType;
	private BigDecimal discountValue;
	private String description;
	private LocalDateTime startAt;
	private LocalDateTime endAt;

	public PromotionInfoDto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public PromotionInfoDto(Integer promotionId, String name, String discountType, BigDecimal discountValue,
			String description, LocalDateTime startAt, LocalDateTime endAt) {
		super();
		this.promotionId = promotionId;
		this.name = name;
		this.discountType = discountType;
		this.discountValue = discountValue;
		this.description = description;
		this.startAt = startAt;
		this.endAt = endAt;
	}

	public Integer getPromotionId() {
		return promotionId;
	}

	public void setPromotionId(Integer promotionId) {
		this.promotionId = promotionId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public LocalDateTime getStartAt() {
		return startAt;
	}

	public void setStartAt(LocalDateTime startAt) {
		this.startAt = startAt;
	}

	public LocalDateTime getEndAt() {
		return endAt;
	}

	public void setEndAt(LocalDateTime endAt) {
		this.endAt = endAt;
	}

}
