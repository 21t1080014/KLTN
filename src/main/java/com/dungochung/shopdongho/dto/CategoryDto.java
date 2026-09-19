package com.dungochung.shopdongho.dto;

import java.util.List;

public class CategoryDto {
	private Integer categoryId;
	private String name;
	private Integer parentId;
	private String parentName;
	private String description;
	private List<CategoryDto> children;

	public CategoryDto() {
		super();
	}

	public CategoryDto(Integer categoryId, String name, Integer parentId, String parentName, String description) {
		this.categoryId = categoryId;
		this.name = name;
		this.parentId = parentId;
		this.parentName = parentName;
		this.description = description;
	}

	public Integer getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getParentId() {
		return parentId;
	}

	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}

	public String getParentName() {
		return parentName;
	}

	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<CategoryDto> getChildren() {
		return children;
	}

	public void setChildren(List<CategoryDto> children) {
		this.children = children;
	}
}
