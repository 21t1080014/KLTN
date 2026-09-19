package com.dungochung.shopdongho.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.InventoryMovementEntity;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovementEntity, Long> {
	Page<InventoryMovementEntity> findByVariantId(Long variantId, Pageable pageable);

	Page<InventoryMovementEntity> findByProductId(String productId, Pageable pageable);

	boolean existsByVariantId(Long variantId);
}
