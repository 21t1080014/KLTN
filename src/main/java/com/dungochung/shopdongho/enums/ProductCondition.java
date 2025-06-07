package com.dungochung.shopdongho.enums;

public enum ProductCondition {
	MOI_100("Mới 100%"), LIKE_NEW("Like New"), DA_QUA_SU_DUNG("Đã qua sử dụng");

	private final String dbValue;

	ProductCondition(String dbValue) {
		this.dbValue = dbValue;
	}

	public String getDbValue() {
		return dbValue;
	}

	public static ProductCondition fromDbValue(String dbValue) {
		for (ProductCondition c : values()) {
			if (c.dbValue.equalsIgnoreCase(dbValue))
				return c;
		}
		throw new IllegalArgumentException("Unknown condition: " + dbValue);
	}
}
