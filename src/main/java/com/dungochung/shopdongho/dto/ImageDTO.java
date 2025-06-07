package com.dungochung.shopdongho.dto;

public class ImageDTO {
	private Integer imageId;
	private String url;
	private String altText;
	private Integer sortOrder;
	public ImageDTO() {
		super();
		// TODO Auto-generated constructor stub
	}
	public ImageDTO(Integer imageId, String url, String altText, Integer sortOrder) {
		super();
		this.imageId = imageId;
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
