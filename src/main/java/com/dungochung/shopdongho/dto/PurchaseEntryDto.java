package com.dungochung.shopdongho.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class PurchaseEntryDto {
	private Long entryId;
	private String productId;
	private String productName;
	private Integer quantity;
	private BigDecimal importPrice;
	private Timestamp importedAt;
	private String note;
	public PurchaseEntryDto() {
		super();
		// TODO Auto-generated constructor stub
	}
	public PurchaseEntryDto(Long entryId, String productId, String productName, Integer quantity,
			BigDecimal importPrice, Timestamp importedAt, String note) {
		super();
		this.entryId = entryId;
		this.productId = productId;
		this.productName = productName;
		this.quantity = quantity;
		this.importPrice = importPrice;
		this.importedAt = importedAt;
		this.note = note;
	}
	public Long getEntryId() {
		return entryId;
	}
	public void setEntryId(Long entryId) {
		this.entryId = entryId;
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
