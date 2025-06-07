package com.dungochung.shopdongho.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.BrandEntity;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.enums.Segment;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, String> {
	boolean existsByName(String name);

	boolean existsByNameAndProductIdNot(String name, String productId);

	@Query("""
			  SELECT DISTINCT p
			  FROM ProductEntity p
			    LEFT JOIN FETCH p.promotionProducts pp
			    LEFT JOIN FETCH pp.promotion promo
			  WHERE (:genders   IS NULL OR p.gender   IN :genders)
			    AND (:segments  IS NULL OR p.segment  IN :segments)
			    AND (:brandNames IS NULL OR p.brand.name IN :brandNames)
			    AND (:priceMin  IS NULL OR p.price >= :priceMin)
			    AND (:priceMax  IS NULL OR p.price <= :priceMax)
			""")
	Page<ProductEntity> advancedSearch(@Param("genders") Object genders, @Param("segments") Object segments,
			@Param("brandNames") Object brandNames, @Param("priceMin") Double priceMin,
			@Param("priceMax") Double priceMax, Pageable pageable);

	@EntityGraph(attributePaths = { "promotionProducts", "promotionProducts.promotion", "images", "inventory", "brand",
			"type", "caseMaterial", "strapMaterial", "glassMaterial" })
	List<ProductEntity> findTop10ByOrderByCreatedAtDesc();

	@EntityGraph(attributePaths = { "promotionProducts", "promotionProducts.promotion", "brand", "type", "inventory",
			"images", "caseMaterial", "strapMaterial", "glassMaterial" })
	@Query("SELECT DISTINCT p FROM ProductEntity p " + "JOIN p.promotionProducts pp "
			+ "WHERE pp.promotion.isActive = true " + "AND pp.promotion.startAt <= :now "
			+ "AND pp.promotion.endAt >= :now")
	List<ProductEntity> findPromotionalProducts(@Param("now") LocalDateTime now);

	@Query("SELECT p FROM ProductEntity p WHERE "
			+ "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
			+ "(:priceStart IS NULL OR p.price >= :priceStart) AND " + "(:priceEnd IS NULL OR p.price <= :priceEnd)")
	Page<ProductEntity> searchProducts(@Param("keyword") String keyword, @Param("priceStart") BigDecimal priceStart,
			@Param("priceEnd") BigDecimal priceEnd, Pageable pageable);

	Page<ProductEntity> findByNameContainingIgnoreCaseAndPriceBetween(String name, Double min, Double max,
			Pageable pageable);

	@Query("SELECT MAX(p.price) FROM ProductEntity p")
	BigDecimal findMaxPrice();

	List<ProductEntity> findTop10ByNameContainingIgnoreCase(String name);

	Page<BrandEntity> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

	List<ProductEntity> findByBrandBrandIdAndSegmentOrderByCreatedAtDesc(Integer brandId, Segment segment);

	@Query("SELECT DISTINCT p FROM ProductEntity p " + "LEFT JOIN FETCH p.promotionProducts pp "
			+ "LEFT JOIN FETCH pp.promotion promo " + "WHERE p.productId = :productId")
	Optional<ProductEntity> findByIdWithPromotionProducts(@Param("productId") String productId);

	@Query("SELECT DISTINCT p FROM ProductEntity p " + "LEFT JOIN FETCH p.promotionProducts pp "
			+ "LEFT JOIN FETCH pp.promotion promo " + "WHERE p.brand.brandId = :brandId AND p.segment = :segment "
			+ "ORDER BY p.createdAt DESC")
	List<ProductEntity> findByBrandIdAndSegmentWithPromotionProducts(@Param("brandId") int brandId,
			@Param("segment") Segment segment);

	/** Tải luôn promotionProducts + promotion */
	@Query("""
			SELECT DISTINCT p
			FROM ProductEntity p
			LEFT JOIN FETCH p.promotionProducts pp
			LEFT JOIN FETCH pp.promotion promo
			LEFT JOIN FETCH p.images img
			WHERE p.productId = :productId
			""")
	Optional<ProductEntity> findWithPromotionsAndImagesById(@Param("productId") String productId);

	/*
	 * @Query(""" SELECT p FROM ProductEntity p LEFT JOIN FETCH p.images LEFT JOIN
	 * FETCH p.promotionProducts pp LEFT JOIN FETCH pp.promotion WHERE LOWER(p.name)
	 * LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%',
	 * :sku, '%')) """) List<ProductEntity>
	 * findTop7WithPromotionsAndImagesByNameOrSku(@Param("name") String name,
	 * 
	 * @Param("sku") String sku);
	 */
	// Truy vấn chỉ lấy ID sản phẩm theo keyword, có phân trang
	@Query("""
			    SELECT p.id FROM ProductEntity p
			    WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
			       OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :sku, '%'))
			""")
	Page<Long> findIdsByNameOrSku(@Param("name") String name, @Param("sku") String sku, Pageable pageable);

	// Truy vấn đầy đủ sản phẩm theo danh sách ID đã có, dùng EntityGraph để fetch
	// các quan hệ
	@EntityGraph(attributePaths = { "images", "promotionProducts", "promotionProducts.promotion", "brand", "type",
			"strapMaterial", "glassMaterial", "caseMaterial" })
	List<ProductEntity> findByProductIdIn(List<Long> ids);

	@Query("SELECT COUNT(p) FROM ProductEntity p " + "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ "   OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	long countByNameOrSku(@Param("keyword") String keyword);

	@EntityGraph(attributePaths = { "promotionProducts", "promotionProducts.promotion", "images", "brand", "type",
			"strapMaterial", "glassMaterial", "caseMaterial" })
	Page<ProductEntity> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(String name, String sku,
			Pageable pageable);

	/** Tải luôn theo brandId + segment */
	@Query("""
			SELECT DISTINCT p
			FROM ProductEntity p
			LEFT JOIN FETCH p.promotionProducts pp
			LEFT JOIN FETCH pp.promotion promo
			LEFT JOIN FETCH p.images img
			WHERE p.brand.brandId = :brandId AND p.segment = :segment
			ORDER BY p.createdAt DESC
			""")
	List<ProductEntity> findWithPromotionsAndImagesByBrandAndSegment(@Param("brandId") int brandId,
			@Param("segment") Segment segment);
}
