package com.dungochung.shopdongho.entity;

import java.sql.Timestamp;

import jakarta.persistence.*;

@Entity
@Table(name = "inventories")
public class InventoryEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "inventory_id")
	private Long inventoryId;

	@OneToOne
	@JoinColumn(name = "product_id", referencedColumnName = "product_id", unique = true, foreignKey = @ForeignKey(name = "fk_inventory_product"))
	private ProductEntity product;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

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
