package com.dungochung.shopdongho.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PromotionProductId implements Serializable {
	private static final long serialVersionUID = 1L;
	private Integer promotionId;
	private String productId;

	public PromotionProductId() {
	}

	public PromotionProductId(Integer promotionId, String productId) {
		this.promotionId = promotionId;
		this.productId = productId;
	}

	public Integer getPromotionId() {
		return promotionId;
	}

	public void setPromotionId(Integer promotionId) {
		this.promotionId = promotionId;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof PromotionProductId))
			return false;
		PromotionProductId that = (PromotionProductId) o;
		return Objects.equals(promotionId, that.promotionId) && Objects.equals(productId, that.productId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(promotionId, productId);
	}
}
