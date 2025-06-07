package com.dungochung.shopdongho.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dungochung.shopdongho.entity.PromotionEntity;

public interface PromotionRepository extends JpaRepository<PromotionEntity, Integer> {
	@Query("""
			SELECT p FROM PromotionEntity p
			WHERE (:keyword     IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
			  AND (:discountType IS NULL OR p.discountType = :discountType)
			  AND (:discountValue IS NULL OR p.discountValue = CAST(:discountValue AS java.math.BigDecimal))
			""")
	Page<PromotionEntity> searchPromotions(@Param("keyword") String keyword,
			@Param("discountType") PromotionEntity.DiscountType discountType,
			@Param("discountValue") String discountValue, Pageable pageable);
}
