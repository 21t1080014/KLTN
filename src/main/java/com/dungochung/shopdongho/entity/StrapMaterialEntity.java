package com.dungochung.shopdongho.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "strap_materials")
public class StrapMaterialEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "strap_material_id", nullable = false)
	private Integer strapMaterialId;

	@Column(name = "name", nullable = false, unique = true, length = 50)
	private String name;

	public StrapMaterialEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public StrapMaterialEntity(Integer strapMaterialId, String name) {
		super();
		this.strapMaterialId = strapMaterialId;
		this.name = name;
	}

	public Integer getStrapMaterialId() {
		return strapMaterialId;
	}

	public void setStrapMaterialId(Integer strapMaterialId) {
		this.strapMaterialId = strapMaterialId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
