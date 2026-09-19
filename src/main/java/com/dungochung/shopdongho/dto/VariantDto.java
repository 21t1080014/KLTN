package com.dungochung.shopdongho.dto;

import java.math.BigDecimal;
import java.util.List;

public class VariantDto {
	private Long variantId;
	private String productId;
	private String sku;
	private String variantName;
	private String color;
	private String strapOption;
	private String caseSize;
	private BigDecimal price;
	private boolean isDefault;
	private String status;
	private Integer sortOrder;
	private int quantity;
	private int reservedQuantity;
	private int availableQuantity;
	private int lowStockThreshold;
	private List<Integer> imageIds;

	public Long getVariantId() { return variantId; }
	public void setVariantId(Long variantId) { this.variantId = variantId; }
	public String getProductId() { return productId; }
	public void setProductId(String productId) { this.productId = productId; }
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
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public Integer getSortOrder() { return sortOrder; }
	public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
	public int getQuantity() { return quantity; }
	public void setQuantity(int quantity) { this.quantity = quantity; }
	public int getReservedQuantity() { return reservedQuantity; }
	public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }
	public int getAvailableQuantity() { return availableQuantity; }
	public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
	public int getLowStockThreshold() { return lowStockThreshold; }
	public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
	public List<Integer> getImageIds() { return imageIds; }
	public void setImageIds(List<Integer> imageIds) { this.imageIds = imageIds; }
}
