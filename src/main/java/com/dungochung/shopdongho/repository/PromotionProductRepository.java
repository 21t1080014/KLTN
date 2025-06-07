package com.dungochung.shopdongho.repository;

import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.PromotionProductEntity;
import com.dungochung.shopdongho.entity.PromotionProductId;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionProductRepository extends JpaRepository<PromotionProductEntity, PromotionProductId> {
	Page<PromotionProductEntity> findByPromotion_PromotionId(Integer promotionId, Pageable pageable);

	Page<PromotionProductEntity> findByPromotion_PromotionIdAndProduct_NameContainingIgnoreCase(Integer promotionId,
			String productName, Pageable pageable);

//	@EntityGraph(attributePaths = { "promotionProducts", "promotionProducts.promotion", "brand", "type", "inventory",
//			"images", "caseMaterial", "strapMaterial", "glassMaterial" })
//	@Query("SELECT ppe.product FROM PromotionProductEntity ppe " + "WHERE ppe.promotion.isActive = true "
//			+ "AND ppe.promotion.startAt <= :now " + "AND ppe.promotion.endAt >= :now")
//	List<ProductEntity> findPromotionalProducts(@Param("now") LocalDateTime now);
}
