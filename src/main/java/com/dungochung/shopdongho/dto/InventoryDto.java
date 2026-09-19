package com.dungochung.shopdongho.dto;

public class InventoryDto {
	private Long inventoryId;
	private String productId;
	private String productName;
	private Integer quantity;
	private Long variantId;
	private String sku;
	private String variantName;
	private Integer reservedQuantity;
	private Integer availableQuantity;
	private Integer lowStockThreshold;
	private boolean lowStock;

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

	public Long getVariantId() { return variantId; }
	public void setVariantId(Long variantId) { this.variantId = variantId; }
	public String getSku() { return sku; }
	public void setSku(String sku) { this.sku = sku; }
	public String getVariantName() { return variantName; }
	public void setVariantName(String variantName) { this.variantName = variantName; }
	public Integer getReservedQuantity() { return reservedQuantity; }
	public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
	public Integer getAvailableQuantity() { return availableQuantity; }
	public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
	public Integer getLowStockThreshold() { return lowStockThreshold; }
	public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
	public boolean isLowStock() { return lowStock; }
	public void setLowStock(boolean lowStock) { this.lowStock = lowStock; }

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

}
