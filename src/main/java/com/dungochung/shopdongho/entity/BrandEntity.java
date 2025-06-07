package com.dungochung.shopdongho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "brands")
public class BrandEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "brand_id", nullable = false)
	private Integer brandId;

	@Column(name = "name", nullable = false, unique = true, length = 100)
	private String name;
	@Column(name = "logo_image", nullable = false, length = 255)
	private String logoImage;
	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	public BrandEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public BrandEntity(Integer brandId, String name, String logoImage, String description) {
		super();
		this.brandId = brandId;
		this.name = name;
		this.logoImage = logoImage;
		this.description = description;
	}

	public Integer getBrandId() {
		return brandId;
	}

	public void setBrandId(Integer brandId) {
		this.brandId = brandId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLogoImage() {
		return logoImage;
	}

	public void setLogoImage(String logoImage) {
		this.logoImage = logoImage;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
