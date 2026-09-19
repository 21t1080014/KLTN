package com.dungochung.shopdongho.common;

/** Ghi ô CSV an toàn: escape theo RFC4180 và vô hiệu hóa "CSV/formula injection" (ô bắt đầu bằng = + - @ sẽ bị Excel chạy như công thức). */
public final class CsvUtil {
	private CsvUtil() {
	}

	public static String cell(Object v) {
		String s = v == null ? "" : v.toString();
		if (!s.isEmpty()) {
			char c = s.charAt(0);
			if (c == '=' || c == '+' || c == '-' || c == '@' || c == '\t' || c == '\r') {
				s = "'" + s;
			}
		}
		if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
			return "\"" + s.replace("\"", "\"\"") + "\"";
		}
		return s;
	}
}
