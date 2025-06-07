package com.dungochung.shopdongho.enums;

public enum Gender {
	MALE("Nam"), FEMALE("Nữ"), UNISEX("Unisex");

	private final String dbValue;

	Gender(String dbValue) {
		this.dbValue = dbValue;
	}

	public String getDbValue() {
		return dbValue;
	}

	public static Gender fromDbValue(String dbValue) {
		for (Gender g : values()) {
			if (g.dbValue.equalsIgnoreCase(dbValue))
				return g;
		}
		throw new IllegalArgumentException("Unknown gender: " + dbValue);
	}
}
