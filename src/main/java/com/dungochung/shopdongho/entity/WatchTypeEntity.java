package com.dungochung.shopdongho.entity;
import jakarta.persistence.*;
// phân loại đồng hồ 
@Entity
@Table(name = "watch_types")
public class WatchTypeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "type_id", nullable = false)
	private int typeId;

	@Column(name = "name", nullable = false, unique = true, length = 50)
	private String name;

	public WatchTypeEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public WatchTypeEntity(int typeId, String name) {
		super();
		this.typeId = typeId;
		this.name = name;
	}

	public int getTypeId() {
		return typeId;
	}

	public void setTypeId(int typeId) {
		this.typeId = typeId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
