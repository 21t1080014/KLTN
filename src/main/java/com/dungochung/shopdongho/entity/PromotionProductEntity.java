package com.dungochung.shopdongho.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "promotion_products")
public class PromotionProductEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@EmbeddedId
	private PromotionProductId id;

	@ManyToOne
	@MapsId("promotionId")
	@JoinColumn(name = "promotion_id", nullable = false)
	private PromotionEntity promotion;

	@ManyToOne
	@MapsId("productId")
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEntity product;

	// Constructors
	public PromotionProductEntity() {
	}

	public PromotionProductEntity(PromotionProductId id, PromotionEntity promotion, ProductEntity product) {
		this.id = id;
		this.promotion = promotion;
		this.product = product;
	}

	// Getters and Setters
	public PromotionProductId getId() {
		return id;
	}

	public void setId(PromotionProductId id) {
		this.id = id;
	}

	public PromotionEntity getPromotion() {
		return promotion;
	}

	public void setPromotion(PromotionEntity promotion) {
		this.promotion = promotion;
	}

	public ProductEntity getProduct() {
		return product;
	}

	public void setProduct(ProductEntity product) {
		this.product = product;
	}

	// equals and hashCode - based on ID
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof PromotionProductEntity))
			return false;
		PromotionProductEntity that = (PromotionProductEntity) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
