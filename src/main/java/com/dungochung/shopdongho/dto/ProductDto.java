package com.dungochung.shopdongho.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.dungochung.shopdongho.enums.Gender;
import com.dungochung.shopdongho.enums.ProductCondition;
import com.dungochung.shopdongho.enums.Segment;

public class ProductDto {
	private String productId;
	private String sku;
	private String name;
	private String origin;
	private ProductCondition condition;/* ProductCondition enum */
	private String warrantyPeriod;
	private BigDecimal price;
	private Gender gender; /* Gender enum */
	private Segment segment;/* Segment enum */
	private String description;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private String brandName;
	private String typeName;
	private String caseMaterialName;
	private String strapMaterialName;
	private String glassMaterialName;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	private List<ImageDTO> images;

	public ProductDto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ProductDto(String productId, String sku, String name, String origin, ProductCondition condition,
			String warrantyPeriod, BigDecimal price, Gender gender, Segment segment, String description,
			String brandName, String typeName, String caseMaterialName, String strapMaterialName,
			String glassMaterialName, LocalDateTime createdAt, LocalDateTime updatedAt, List<ImageDTO> images) {
		super();
		this.productId = productId;
		this.sku = sku;
		this.name = name;
		this.origin = origin;
		this.condition = condition;
		this.warrantyPeriod = warrantyPeriod;
		this.price = price;
		this.gender = gender;
		this.segment = segment;
		this.description = description;
		this.brandName = brandName;
		this.typeName = typeName;
		this.caseMaterialName = caseMaterialName;
		this.strapMaterialName = strapMaterialName;
		this.glassMaterialName = glassMaterialName;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.images = images;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public ProductCondition getCondition() {
		return condition;
	}

	public void setCondition(ProductCondition condition) {
		this.condition = condition;
	}

	public String getWarrantyPeriod() {
		return warrantyPeriod;
	}

	public void setWarrantyPeriod(String warrantyPeriod) {
		this.warrantyPeriod = warrantyPeriod;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public Segment getSegment() {
		return segment;
	}

	public void setSegment(Segment segment) {
		this.segment = segment;
	}

	public String getBrandName() {
		return brandName;
	}

	public void setBrandName(String brandName) {
		this.brandName = brandName;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public String getCaseMaterialName() {
		return caseMaterialName;
	}

	public void setCaseMaterialName(String caseMaterialName) {
		this.caseMaterialName = caseMaterialName;
	}

	public String getStrapMaterialName() {
		return strapMaterialName;
	}

	public void setStrapMaterialName(String strapMaterialName) {
		this.strapMaterialName = strapMaterialName;
	}

	public String getGlassMaterialName() {
		return glassMaterialName;
	}

	public void setGlassMaterialName(String glassMaterialName) {
		this.glassMaterialName = glassMaterialName;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public List<ImageDTO> getImages() {
		return images;
	}

	public void setImages(List<ImageDTO> images) {
		this.images = images;
	}

}
