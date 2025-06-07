package com.dungochung.shopdongho.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "glass_materials")
public class GlassMaterialEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "glass_material_id", nullable = false)
	private Integer glassMaterialId;

	@Column(name = "name", nullable = false, unique = true, length = 50)
	private String name;

	public GlassMaterialEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public GlassMaterialEntity(Integer glassMaterialId, String name) {
		super();
		this.glassMaterialId = glassMaterialId;
		this.name = name;
	}

	public Integer getGlassMaterialId() {
		return glassMaterialId;
	}

	public void setGlassMaterialId(Integer glassMaterialId) {
		this.glassMaterialId = glassMaterialId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
