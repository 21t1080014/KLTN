package com.dungochung.shopdongho.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.OrderEntity;
import com.dungochung.shopdongho.entity.OrderItemEntity;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Integer> {
	@Query("""
			    SELECT oi.product.id, SUM(oi.quantity) as total
			    FROM OrderItemEntity oi
			    WHERE oi.product.brand.brandId = :brandId
			    GROUP BY oi.product.id
			    ORDER BY total DESC
			""")
	List<Object[]> findTopSellingProductIdsByBrandId(@Param("brandId") Integer brandId);

	List<OrderItemEntity> findByOrder(OrderEntity order);

	boolean existsByProduct_ProductId(String productId);

}
