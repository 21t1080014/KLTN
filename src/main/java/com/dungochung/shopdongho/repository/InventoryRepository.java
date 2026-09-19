package com.dungochung.shopdongho.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.entity.ProductEntity;

@Repository
public interface InventoryRepository
		extends JpaRepository<InventoryEntity, Long>, JpaSpecificationExecutor<InventoryEntity> {

	/** Tồn kho của biến thể mặc định — giữ nguyên ngữ nghĩa "1 sản phẩm = 1 tồn kho" cho code cũ. */
	@Query("SELECT i FROM InventoryEntity i WHERE i.product = :product AND i.variant.isDefault = true")
	Optional<InventoryEntity> findByProduct(@Param("product") ProductEntity product);

	@Query("SELECT i FROM InventoryEntity i WHERE i.product.productId = :productId AND i.variant.isDefault = true")
	Optional<InventoryEntity> findByProduct_ProductId(@Param("productId") String productId);

	Optional<InventoryEntity> findByVariant_VariantId(Long variantId);

	List<InventoryEntity> findByVariantIsNull();

	@Query("SELECT i FROM InventoryEntity i WHERE (i.quantity - i.reservedQuantity) < :threshold")
	List<InventoryEntity> findByQuantityLessThan(@Param("threshold") int threshold);

	/** Cảnh báo tồn thấp theo ngưỡng riêng của từng biến thể. */
	@Query("SELECT i FROM InventoryEntity i WHERE (i.quantity - i.reservedQuantity) <= i.lowStockThreshold")
	List<InventoryEntity> findLowStock();

	@Query("SELECT COALESCE(SUM(i.quantity), 0) FROM InventoryEntity i")
	long getTotalInventory();
}
