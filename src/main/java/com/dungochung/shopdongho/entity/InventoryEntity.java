package com.dungochung.shopdongho.entity;

import java.sql.Timestamp;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;

@Entity
@Table(name = "inventories")
public class InventoryEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "inventory_id")
	private Long inventoryId;

	// Một sản phẩm có thể có nhiều dòng tồn kho (mỗi biến thể 1 dòng) nên không còn unique theo product_id
	@ManyToOne
	@JoinColumn(name = "product_id", referencedColumnName = "product_id", foreignKey = @ForeignKey(name = "fk_inventory_product"))
	private ProductEntity product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "variant_id", foreignKey = @ForeignKey(name = "fk_inventory_variant"))
	private ProductVariantEntity variant;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	/** Số lượng đang giữ cho đơn chưa xuất kho. Hàng có thể bán = quantity - reservedQuantity. */
	@Column(name = "reserved_quantity", nullable = false)
	@ColumnDefault("0")
	private Integer reservedQuantity = 0;

	/** Cảnh báo tồn thấp khi hàng có thể bán <= ngưỡng này. */
	@Column(name = "low_stock_threshold", nullable = false)
	@ColumnDefault("5")
	private Integer lowStockThreshold = 5;

	@Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	private Timestamp updatedAt;

	public InventoryEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public InventoryEntity(ProductEntity product, Integer quantity) {
		super();
		this.product = product;
		this.quantity = quantity;
	}

	public Long getInventoryId() {
		return inventoryId;
	}

	public void setInventoryId(Long inventoryId) {
		this.inventoryId = inventoryId;
	}

	public ProductEntity getProduct() {
		return product;
	}

	public void setProduct(ProductEntity product) {
		this.product = product;
	}

	public ProductVariantEntity getVariant() {
		return variant;
	}

	public void setVariant(ProductVariantEntity variant) {
		this.variant = variant;
	}

	public Integer getReservedQuantity() {
		return reservedQuantity;
	}

	public void setReservedQuantity(Integer reservedQuantity) {
		this.reservedQuantity = reservedQuantity;
	}

	public Integer getLowStockThreshold() {
		return lowStockThreshold;
	}

	public void setLowStockThreshold(Integer lowStockThreshold) {
		this.lowStockThreshold = lowStockThreshold;
	}

	public int getAvailableQuantity() {
		return (quantity == null ? 0 : quantity) - (reservedQuantity == null ? 0 : reservedQuantity);
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Timestamp getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}

	@PrePersist
	protected void onCreate() {
		this.updatedAt = new Timestamp(System.currentTimeMillis());
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = new Timestamp(System.currentTimeMillis());
	}
}
