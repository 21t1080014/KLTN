package com.dungochung.shopdongho.entity;

import jakarta.persistence.*;
//Danh mục chất liệu vỏ đồng hồ
@Entity
@Table(name = "case_materials")
public class CaseMaterialEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "case_material_id", nullable = false)
	private Integer caseMaterialId;

	@Column(name = "name", nullable = false, unique = true, length = 50)
	private String name;

	public CaseMaterialEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public CaseMaterialEntity(Integer caseMaterialId, String name) {
		super();
		this.caseMaterialId = caseMaterialId;
		this.name = name;
	}

	public Integer getCaseMaterialId() {
		return caseMaterialId;
	}

	public void setCaseMaterialId(Integer caseMaterialId) {
		this.caseMaterialId = caseMaterialId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
