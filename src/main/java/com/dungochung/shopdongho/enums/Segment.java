package com.dungochung.shopdongho.enums;

public enum Segment {
	CASUAL("Thông thường"), SPORT("Thể thao"), LUXURY("Cao cấp");

	private final String dbValue;

	Segment(String dbValue) {
		this.dbValue = dbValue;
	}

	public String getDbValue() {
		return dbValue;
	}

	public static Segment fromDbValue(String dbValue) {
		for (Segment s : values()) {
			if (s.dbValue.equalsIgnoreCase(dbValue))
				return s;
		}
		throw new IllegalArgumentException("Unknown segment: " + dbValue);
	}
}
