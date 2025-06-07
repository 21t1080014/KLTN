package com.dungochung.shopdongho.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductPromotionInfoDto {
	private String productId;
	private String productName;
	private Integer promotionId;
	private String promotionName;
	private String discountType;
	private BigDecimal discountValue;
	private LocalDateTime startAt;
	private LocalDateTime endAt;

	public ProductPromotionInfoDto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ProductPromotionInfoDto(String productId, String productName, Integer promotionId, String promotionName,
			String discountType, BigDecimal discountValue, LocalDateTime startAt, LocalDateTime endAt) {
		super();
		this.productId = productId;
		this.productName = productName;
		this.promotionId = promotionId;
		this.promotionName = promotionName;
		this.discountType = discountType;
		this.discountValue = discountValue;
		this.startAt = startAt;
		this.endAt = endAt;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public Integer getPromotionId() {
		return promotionId;
	}

	public void setPromotionId(Integer promotionId) {
		this.promotionId = promotionId;
	}

	public String getPromotionName() {
		return promotionName;
	}

	public void setPromotionName(String promotionName) {
		this.promotionName = promotionName;
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
