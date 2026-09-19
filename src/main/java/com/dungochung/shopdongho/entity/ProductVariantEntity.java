package com.dungochung.shopdongho.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

import com.dungochung.shopdongho.enums.ProductStatus;

import jakarta.persistence.*;

/** Biến thể của sản phẩm (model/màu/dây/size), có SKU, giá và tồn kho riêng. */
@Entity
@Table(name = "product_variants")
public class ProductVariantEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "variant_id")
	private Long variantId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_variant_product"))
	private ProductEntity product;

	@Column(name = "sku", nullable = false, unique = true, length = 60)
	private String sku;

	@Column(name = "variant_name", nullable = false, length = 150)
	private String variantName;

	@Column(name = "color", length = 50)
	private String color;

	@Column(name = "strap_option", length = 50)
	private String strapOption;

	@Column(name = "case_size", length = 50)
	private String caseSize;

	@Column(name = "price", nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "is_default", nullable = false)
	private boolean isDefault;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@ColumnDefault("'ACTIVE'")
	private ProductStatus status = ProductStatus.ACTIVE;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder = 0;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public Long getVariantId() { return variantId; }
	public void setVariantId(Long variantId) { this.variantId = variantId; }
	public ProductEntity getProduct() { return product; }
	public void setProduct(ProductEntity product) { this.product = product; }
	public String getSku() { return sku; }
	public void setSku(String sku) { this.sku = sku; }
	public String getVariantName() { return variantName; }
	public void setVariantName(String variantName) { this.variantName = variantName; }
	public String getColor() { return color; }
	public void setColor(String color) { this.color = color; }
	public String getStrapOption() { return strapOption; }
	public void setStrapOption(String strapOption) { this.strapOption = strapOption; }
	public String getCaseSize() { return caseSize; }
	public void setCaseSize(String caseSize) { this.caseSize = caseSize; }
	public BigDecimal getPrice() { return price; }
	public void setPrice(BigDecimal price) { this.price = price; }
	public boolean isDefault() { return isDefault; }
	public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
	public ProductStatus getStatus() { return status; }
	public void setStatus(ProductStatus status) { this.status = status; }
	public Integer getSortOrder() { return sortOrder; }
	public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}
