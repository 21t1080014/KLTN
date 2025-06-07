package com.dungochung.shopdongho.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

import jakarta.persistence.*;

@Entity
@Table(name = "purchase_entries")
public class PurchaseEntriesEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "entry_id", nullable = false, updatable = false)
	private Long entryId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_purchase_product"))
	private ProductEntity product;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "import_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal importPrice;

	@Column(name = "imported_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private Timestamp importedAt;

	@Column(name = "note", columnDefinition = "TEXT")
	private String note;

	public PurchaseEntriesEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Long getEntryId() {
		return entryId;
	}

	public ProductEntity getProduct() {
		return product;
	}

	public void setProduct(ProductEntity product) {
		this.product = product;
	}

	public void setEntryId(Long entryId) {
		this.entryId = entryId;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getImportPrice() {
		return importPrice;
	}

	public void setImportPrice(BigDecimal importPrice) {
		this.importPrice = importPrice;
	}

	public Timestamp getImportedAt() {
		return importedAt;
	}

	public void setImportedAt(Timestamp importedAt) {
		this.importedAt = importedAt;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

}
