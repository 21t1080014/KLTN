package com.dungochung.shopdongho.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.entity.ProductEntity;

@Repository
public interface InventoryRepository
		extends JpaRepository<InventoryEntity, Long>, JpaSpecificationExecutor<InventoryEntity> {
	Optional<InventoryEntity> findByProduct(ProductEntity product);

	Optional<InventoryEntity> findByProduct_ProductId(String productId);

	List<InventoryEntity> findByQuantityLessThan(int threshold);

	@Query("SELECT COALESCE(SUM(i.quantity), 0) FROM InventoryEntity i")
	long getTotalInventory();
}
