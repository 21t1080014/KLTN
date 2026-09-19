package com.dungochung.shopdongho.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.ProductVariantEntity;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {
	List<ProductVariantEntity> findByProduct_ProductIdOrderBySortOrderAscVariantIdAsc(String productId);

	Optional<ProductVariantEntity> findByProduct_ProductIdAndIsDefaultTrue(String productId);

	Optional<ProductVariantEntity> findBySku(String sku);

	boolean existsBySku(String sku);

	boolean existsBySkuAndVariantIdNot(String sku, Long variantId);

	long countByProduct_ProductId(String productId);
}
