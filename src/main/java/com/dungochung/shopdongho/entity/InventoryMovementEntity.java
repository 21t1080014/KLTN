package com.dungochung.shopdongho.entity;

import java.time.LocalDateTime;

import com.dungochung.shopdongho.enums.MovementType;

import jakarta.persistence.*;

/** Lịch sử biến động tồn kho: ai, khi nào, loại gì, trước/sau. Bảng chỉ ghi thêm, không sửa/xóa. */
@Entity
@Table(name = "inventory_movements", indexes = { @Index(name = "idx_movement_variant", columnList = "variant_id"),
		@Index(name = "idx_movement_product", columnList = "product_id") })
public class InventoryMovementEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "movement_id")
	private Long movementId;

	@Column(name = "variant_id")
	private Long variantId;

	@Column(name = "product_id", length = 250)
	private String productId;

	@Enumerated(EnumType.STRING)
	@Column(name = "movement_type", nullable = false, length = 30)
	private MovementType movementType;

	/** Thay đổi của số lượng tồn (quantity). Với RESERVE/RELEASE thì là thay đổi của reserved. */
	@Column(name = "quantity_change", nullable = false)
	private int quantityChange;

	@Column(name = "quantity_before", nullable = false)
	private int quantityBefore;

	@Column(name = "quantity_after", nullable = false)
	private int quantityAfter;

	@Column(name = "reference", length = 100)
	private String reference;

	@Column(name = "note", length = 255)
	private String note;

	@Column(name = "created_by", length = 100)
	private String createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}

	public Long getMovementId() { return movementId; }
	public Long getVariantId() { return variantId; }
	public void setVariantId(Long variantId) { this.variantId = variantId; }
	public String getProductId() { return productId; }
	public void setProductId(String productId) { this.productId = productId; }
	public MovementType getMovementType() { return movementType; }
	public void setMovementType(MovementType movementType) { this.movementType = movementType; }
	public int getQuantityChange() { return quantityChange; }
	public void setQuantityChange(int quantityChange) { this.quantityChange = quantityChange; }
	public int getQuantityBefore() { return quantityBefore; }
	public void setQuantityBefore(int quantityBefore) { this.quantityBefore = quantityBefore; }
	public int getQuantityAfter() { return quantityAfter; }
	public void setQuantityAfter(int quantityAfter) { this.quantityAfter = quantityAfter; }
	public String getReference() { return reference; }
	public void setReference(String reference) { this.reference = reference; }
	public String getNote() { return note; }
	public void setNote(String note) { this.note = note; }
	public String getCreatedBy() { return createdBy; }
	public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
