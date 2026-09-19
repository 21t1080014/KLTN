package com.dungochung.shopdongho.common;

/** Người thực hiện thao tác trên đơn: tên, role (admin/support_staff/warehouse_staff/customer) và loại (ADMIN/CUSTOMER). */
public record OrderActor(String username, String role, String type) {
	public static OrderActor admin(String username, String role) {
		return new OrderActor(username == null ? "unknown" : username, role, "ADMIN");
	}

	public static OrderActor customer(String username) {
		return new OrderActor(username == null ? "customer" : username, "customer", "CUSTOMER");
	}
}
