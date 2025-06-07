package com.dungochung.shopdongho.dto;

public class BrandIdNameDto {
	private Integer brandId;
	private String name;

	public BrandIdNameDto(Integer brandId, String name) {
		this.brandId = brandId;
		this.name = name;
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
}
