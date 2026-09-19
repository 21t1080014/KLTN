package com.dungochung.shopdongho.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migrate dữ liệu đơn hàng cho luồng trạng thái mới (Module 3). IDEMPOTENT, chạy mỗi lần khởi động, sau
 * {@link VariantBackfillRunner} (cần biến thể mặc định):
 * 1. Mở rộng cột ENUM orders.order_status / payment_status (+ payment_method thêm TRANSFER cho chuyển khoản) (ddl-auto=update KHÔNG tự thêm giá trị vào ENUM có sẵn).
 * 2. Đơn cũ trạng thái "processing" chuyển thành "confirmed" (giữ nguyên nghĩa: đã xác nhận).
 * 3. order_items cũ chưa có variant_id được gắn vào biến thể mặc định của sản phẩm.
 */
@Component
@Order(2)
public class OrderFlowMigrationRunner implements ApplicationRunner {
	private static final Logger log = LoggerFactory.getLogger(OrderFlowMigrationRunner.class);

	private static final String ORDER_STATUS_ENUM = "enum('pending','processing','confirmed','packing','shipping','delivered','completed','canceled','refunded')";
	private static final String PAYMENT_METHOD_ENUM = "enum('COD','VNPay','CreditCard','TRANSFER')";
	private static final String PAYMENT_STATUS_ENUM = "enum('pending','paid','failed','refund_pending','refunded')";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public void run(ApplicationArguments args) {
		widenEnum("order_status", ORDER_STATUS_ENUM, "'pending'");
		widenEnum("payment_status", PAYMENT_STATUS_ENUM, "'pending'");
		widenEnum("payment_method", PAYMENT_METHOD_ENUM, null);
		int moved = jdbcTemplate.update("UPDATE orders SET order_status = 'confirmed' WHERE order_status = 'processing'");
		if (moved > 0) {
			log.info("Đã chuyển {} đơn 'processing' sang 'confirmed'", moved);
		}
		int linked = jdbcTemplate.update("UPDATE order_items oi JOIN product_variants v ON v.product_id = oi.product_id "
				+ "AND v.is_default = 1 SET oi.variant_id = v.variant_id WHERE oi.variant_id IS NULL");
		if (linked > 0) {
			log.info("Đã gắn biến thể mặc định cho {} dòng order_items cũ", linked);
		}
	}

	private static final java.util.Map<String, String> COMMENTS = java.util.Map.of("order_status", "TT đơn hàng",
			"payment_status", "TT thanh toán", "payment_method", "PT thanh toán");

	private void widenEnum(String column, String target, String defaultValue) {
		var row = jdbcTemplate.queryForMap("SELECT column_type AS t, column_comment AS c FROM information_schema.columns "
				+ "WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = ?", column);
		String current = (String) row.get("t");
		String comment = (String) row.get("c");
		boolean sameType = current != null && current.equalsIgnoreCase(target);
		boolean hasComment = comment != null && !comment.isEmpty();
		if (sameType && hasComment) {
			return;
		}
		// MODIFY thay toàn bộ định nghĩa cột nên phải giữ lại COMMENT (khôi phục nếu lần chạy trước đã làm mất)
		String keep = hasComment ? comment : COMMENTS.get(column);
		jdbcTemplate.execute("ALTER TABLE orders MODIFY COLUMN " + column + " " + target + " NOT NULL"
				+ (defaultValue == null ? "" : " DEFAULT " + defaultValue) + " COMMENT '" + keep + "'");
		log.info("Đã mở rộng ENUM orders.{}", column);
	}
}
