package com.dungochung.shopdongho.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_images")
public class ProductImageEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "image_id")
	private Integer imageId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEntity product;

	@Column(name = "url", nullable = false, length = 255)
	private String url;

	@Column(name = "alt_text", length = 150)
	private String altText;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder = 0;

	public ProductImageEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ProductImageEntity(Integer imageId, ProductEntity product, String url, String altText, Integer sortOrder) {
		super();
		this.imageId = imageId;
		this.product = product;
		this.url = url;
		this.altText = altText;
		this.sortOrder = sortOrder;
	}

	public Integer getImageId() {
		return imageId;
	}

	public void setImageId(Integer imageId) {
		this.imageId = imageId;
	}

	public ProductEntity getProduct() {
		return product;
	}

	public void setProduct(ProductEntity product) {
		this.product = product;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getAltText() {
		return altText;
	}

	public void setAltText(String altText) {
		this.altText = altText;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	
}
