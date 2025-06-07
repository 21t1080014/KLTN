package com.dungochung.shopdongho.dto;

public class SegmentDto {
	private String code;
	private String display;

	public SegmentDto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public SegmentDto(String code, String display) {
		super();
		this.code = code;
		this.display = display;
	}

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
