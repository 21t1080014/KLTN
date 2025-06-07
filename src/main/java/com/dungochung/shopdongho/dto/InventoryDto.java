package com.dungochung.shopdongho.dto;

public class InventoryDto {
	private Long inventoryId;
	private String productId;
	private String productName;
	private Integer quantity;

	public InventoryDto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public InventoryDto(Long inventoryId, String productId, String productName, Integer quantity) {
		super();
		this.inventoryId = inventoryId;
		this.productId = productId;
		this.productName = productName;
		this.quantity = quantity;
	}

	public Long getInventoryId() {
		return inventoryId;
	}

	public void setInventoryId(Long inventoryId) {
		this.inventoryId = inventoryId;
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

}
