package com.dungochung.shopdongho.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.entity.InventoryMovementEntity;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.ProductVariantEntity;
import com.dungochung.shopdongho.enums.MovementType;
import com.dungochung.shopdongho.repository.InventoryMovementRepository;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.service.ProductVariantService;
import com.dungochung.shopdongho.service.StockService;

/**
 * Migrate dữ liệu cũ sang mô hình biến thể, chạy mỗi lần khởi động và IDEMPOTENT (chạy lại không đổi gì
 * nếu đã migrate xong):
 * 1. Bỏ ràng buộc unique inventories.product_id (1 sản phẩm giờ có nhiều dòng tồn kho, mỗi biến thể 1 dòng).
 * 2. Mỗi sản phẩm chưa có biến thể được tạo 1 biến thể mặc định (sku/giá lấy từ sản phẩm).
 * 3. Dòng tồn kho cũ chưa gắn biến thể được gắn vào biến thể mặc định; sản phẩm chưa có tồn kho được tạo dòng 0.
 * 4. Ghi 1 dòng lịch sử INITIAL cho số dư đầu kỳ của mỗi biến thể chưa có lịch sử.
 */
@Component
public class VariantBackfillRunner implements ApplicationRunner {
	private static final Logger log = LoggerFactory.getLogger(VariantBackfillRunner.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private TransactionTemplate transactionTemplate;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private InventoryRepository inventoryRepository;
	@Autowired
	private InventoryMovementRepository movementRepository;
	@Autowired
	private ProductVariantService variantService;
	@Autowired
	private StockService stockService;
	@Autowired
	private com.dungochung.shopdongho.repository.ProductVariantRepository variantRepository;

	@Override
	public void run(ApplicationArguments args) {
		dropUniqueIndexOnInventoryProduct();
		transactionTemplate.executeWithoutResult(status -> backfill());
	}

	private void dropUniqueIndexOnInventoryProduct() {
		try {
			List<String> uniqueIdx = jdbcTemplate.queryForList(
					"SELECT DISTINCT index_name FROM information_schema.statistics WHERE table_schema = DATABASE() "
							+ "AND table_name = 'inventories' AND column_name = 'product_id' AND non_unique = 0 "
							+ "AND index_name <> 'PRIMARY'",
					String.class);
			if (uniqueIdx.isEmpty()) {
				return;
			}
			Integer plain = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() "
							+ "AND table_name = 'inventories' AND index_name = 'idx_inventories_product'",
					Integer.class);
			if (plain == null || plain == 0) {
				// FK cần 1 index thường thay thế trước khi bỏ index unique
				jdbcTemplate.execute("CREATE INDEX idx_inventories_product ON inventories (product_id)");
			}
			for (String name : uniqueIdx) {
				jdbcTemplate.execute("ALTER TABLE inventories DROP INDEX `" + name + "`");
				log.info("Đã bỏ unique index {} trên inventories.product_id", name);
			}
		} catch (Exception e) {
			log.warn("Không bỏ được unique index inventories.product_id (có thể đã bỏ trước đó): {}", e.getMessage());
		}
	}

	private void backfill() {
		int variants = 0, linked = 0, initial = 0;
		// 0. Chuẩn hoá SKU: dữ liệu cũ có SKU dính khoảng trắng đầu/cuối làm import/tra cứu theo SKU không khớp
		java.util.Set<String> used = new java.util.HashSet<>();
		for (ProductEntity p : productRepository.findAll()) {
			used.add(p.getSku());
		}
		for (ProductEntity p : productRepository.findAll()) {
			String trimmed = p.getSku() == null ? null : p.getSku().trim();
			if (trimmed != null && !trimmed.equals(p.getSku())) {
				if (used.contains(trimmed)) {
					log.warn("Không trim được SKU '{}' vì trùng SKU khác", p.getSku());
					continue;
				}
				used.remove(p.getSku());
				used.add(trimmed);
				p.setSku(trimmed);
				productRepository.save(p);
				variantRepository.findByProduct_ProductIdAndIsDefaultTrue(p.getProductId()).ifPresent(v -> {
					v.setSku(trimmed);
					variantRepository.save(v);
				});
				log.info("Đã chuẩn hoá SKU sản phẩm -> '{}'", trimmed);
			}
		}
		// 1. Mỗi sản phẩm có ít nhất 1 biến thể (mặc định)
		for (ProductEntity p : productRepository.findAll()) {
			variantService.ensureDefaultVariant(p);
		}
		// 2. Gắn các dòng tồn kho cũ chưa có variant vào biến thể mặc định của sản phẩm
		for (InventoryEntity inv : inventoryRepository.findByVariantIsNull()) {
			ProductVariantEntity def = variantService.ensureDefaultVariant(inv.getProduct());
			if (inventoryRepository.findByVariant_VariantId(def.getVariantId()).isEmpty()) {
				inv.setVariant(def);
				inventoryRepository.save(inv);
				linked++;
			}
		}
		// 3. Sản phẩm chưa có tồn kho nào thì tạo dòng tồn kho 0 cho biến thể mặc định
		for (ProductEntity p : productRepository.findAll()) {
			ProductVariantEntity def = variantService.ensureDefaultVariant(p);
			if (inventoryRepository.findByVariant_VariantId(def.getVariantId()).isEmpty()) {
				stockService.ensureInventory(def);
				variants++;
			}
		}
		for (InventoryEntity inv : inventoryRepository.findAll()) {
			if (inv.getVariant() != null && inv.getQuantity() != null && inv.getQuantity() > 0
					&& !movementRepository.existsByVariantId(inv.getVariant().getVariantId())) {
				InventoryMovementEntity m = new InventoryMovementEntity();
				m.setVariantId(inv.getVariant().getVariantId());
				m.setProductId(inv.getProduct().getProductId());
				m.setMovementType(MovementType.INITIAL);
				m.setQuantityChange(inv.getQuantity());
				m.setQuantityBefore(0);
				m.setQuantityAfter(inv.getQuantity());
				m.setNote("Số dư đầu kỳ khi nâng cấp mô hình biến thể");
				m.setCreatedBy("system");
				movementRepository.save(m);
				initial++;
			}
		}
		if (variants + linked + initial > 0) {
			log.info("Backfill biến thể: tạo tồn kho cho {} biến thể, gắn {} dòng tồn kho cũ, ghi {} dòng lịch sử đầu kỳ",
					variants, linked, initial);
		}
	}
}
