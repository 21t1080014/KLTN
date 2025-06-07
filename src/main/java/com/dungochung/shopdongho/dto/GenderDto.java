package com.dungochung.shopdongho.dto;

public class GenderDto {
	private String code; // MALE, FEMALE, UNISEX
	private String display; // "Nam", "Nữ", "Unisex"

	public GenderDto(String code, String display) {
		this.code = code;
		this.display = display;
	}

	// getters/setters
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDisplay() {
		return display;
	}

	public void setDisplay(String display) {
		this.display = display;
	}
}
