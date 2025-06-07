package com.dungochung.shopdongho.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "promotions")
public class PromotionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "promotion_id")
	private Integer promotionId;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "discount_type", nullable = false)
	private DiscountType discountType;

	@Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountValue;

	@Column(name = "start_at", nullable = false)
	private LocalDateTime startAt;

	@Column(name = "end_at", nullable = false)
	private LocalDateTime endAt;

	@Column(name = "is_active")
	private Boolean isActive = true;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "created_at", updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	private Set<PromotionProductEntity> promotionProducts;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}

	public PromotionEntity() {
		super();
		// TODO Auto-generated constructor stub
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

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Set<PromotionProductEntity> getPromotionProducts() {
		return promotionProducts;
	}

	public void setPromotionProducts(Set<PromotionProductEntity> promotionProducts) {
		this.promotionProducts = promotionProducts;
	}

	public enum DiscountType {
		percent, amount
	}
}
