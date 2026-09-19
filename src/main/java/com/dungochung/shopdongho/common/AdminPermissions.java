package com.dungochung.shopdongho.common;

import java.util.List;
import java.util.Set;

import com.dungochung.shopdongho.common.constant.Constant;

/**
 * Ma trận phân quyền khu vực /admin — MỘT nguồn sự thật cho {@code RoleInterceptor} (và cho test).
 * Mỗi module có: role được ĐỌC (GET/HEAD), role được GHI (POST/PUT/PATCH...), role được XÓA (DELETE).
 * Đường dẫn không khớp module nào chỉ dành cho admin (deny-by-default: endpoint mới không bao giờ tự động mở cho nhân viên).
 * Quyền chi tiết hơn (vd đơn hàng: ai được chuyển sang trạng thái nào) nằm ở tầng service ({@link OrderWorkflow}).
 */
public final class AdminPermissions {
	private AdminPermissions() {
	}

	private static final String ADMIN = Constant.ROLE_ADMIN;
	private static final String PRODUCT = Constant.ROLE_PRODUCT_STAFF;
	private static final String SUPPORT = Constant.ROLE_SUPPORT_STAFF;
	private static final String WAREHOUSE = Constant.ROLE_WAREHOUSE_STAFF;

	/** Các role được vào khu vực admin. */
	public static final Set<String> STAFF_ROLES = Set.of(ADMIN, PRODUCT, SUPPORT, WAREHOUSE);

	public record Rule(String prefix, Set<String> read, Set<String> write, Set<String> delete) {
	}

	private static Rule rule(String prefix, Set<String> read, Set<String> write, Set<String> delete) {
		return new Rule(prefix, read, write, delete);
	}

	private static Rule adminOnly(String prefix) {
		return rule(prefix, Set.of(ADMIN), Set.of(ADMIN), Set.of(ADMIN));
	}

	public static final List<Rule> RULES = List.of(
			// Sản phẩm/biến thể: nhân viên sản phẩm quản lý, nhưng chỉ admin được xóa (nhân viên dùng trạng thái Ngừng bán)
			rule("/admin/products", Set.of(ADMIN, PRODUCT), Set.of(ADMIN, PRODUCT), Set.of(ADMIN)),
			// Tồn kho: nhân viên sản phẩm chỉ XEM (biết còn hàng hay không), chỉ kho/admin được kiểm kê/điều chỉnh.
			// Xóa dòng tồn kho chỉ được khi số lượng = 0 (service kiểm tra) nên kho được phép dọn dẹp.
			rule("/admin/inventory", Set.of(ADMIN, WAREHOUSE, PRODUCT), Set.of(ADMIN, WAREHOUSE), Set.of(ADMIN, WAREHOUSE)),
			// Phiếu nhập kho: kho tạo/sửa/xóa (xóa chỉ trong ngày và phải đủ tồn để đảo — service kiểm tra, có ghi lịch sử kho + audit)
			rule("/admin/purchases", Set.of(ADMIN, WAREHOUSE), Set.of(ADMIN, WAREHOUSE), Set.of(ADMIN, WAREHOUSE)),
			// Đơn hàng: CSKH + kho + admin; từng bước chuyển trạng thái còn bị giới hạn theo role ở OrderWorkflow; chỉ admin xóa đơn
			rule("/admin/orders", Set.of(ADMIN, SUPPORT, WAREHOUSE), Set.of(ADMIN, SUPPORT, WAREHOUSE), Set.of(ADMIN)),
			// Khách hàng: CSKH + admin
			rule("/admin/customers", Set.of(ADMIN, SUPPORT), Set.of(ADMIN, SUPPORT), Set.of(ADMIN)),
			// Dashboard: mọi nhân viên vào được trang; dữ liệu nhạy cảm (doanh thu...) do DashboardService lọc theo role
			rule("/admin/dashboard", STAFF_ROLES, STAFF_ROLES, Set.of(ADMIN)),
			adminOnly("/admin/users"), adminOnly("/admin/brands"), adminOnly("/admin/categories"),
			adminOnly("/admin/promotions"), adminOnly("/admin/voucher"), adminOnly("/admin/reports"),
			adminOnly("/admin/audit"));

	public static boolean isRead(String httpMethod) {
		return "GET".equalsIgnoreCase(httpMethod) || "HEAD".equalsIgnoreCase(httpMethod)
				|| "OPTIONS".equalsIgnoreCase(httpMethod);
	}

	public static boolean isAllowed(String role, String httpMethod, String uri) {
		if (role == null || !STAFF_ROLES.contains(role)) {
			return false;
		}
		for (Rule r : RULES) {
			if (uri.equals(r.prefix()) || uri.startsWith(r.prefix() + "/")) {
				Set<String> roles = isRead(httpMethod) ? r.read()
						: "DELETE".equalsIgnoreCase(httpMethod) ? r.delete() : r.write();
				return roles.contains(role);
			}
		}
		return ADMIN.equals(role);
	}
}
