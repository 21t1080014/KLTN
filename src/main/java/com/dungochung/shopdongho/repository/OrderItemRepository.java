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

	/** Sản phẩm bán chạy trong kỳ: chỉ tính đơn đã giao/hoàn thành và đã thanh toán (khớp định nghĩa doanh thu). Trả [id, tên, sku, số lượng, doanh thu]. */
	@Query("""
			SELECT oi.product.productId, oi.product.name, oi.product.sku, SUM(oi.quantity), SUM(oi.quantity * oi.priceEach)
			FROM OrderItemEntity oi
			WHERE oi.order.createdAt >= :from AND oi.order.createdAt < :to
			  AND oi.order.orderStatus IN (com.dungochung.shopdongho.enums.OrderStatus.delivered, com.dungochung.shopdongho.enums.OrderStatus.completed)
			  AND oi.order.paymentStatus = com.dungochung.shopdongho.enums.PaymentStatus.paid
			GROUP BY oi.product.productId, oi.product.name, oi.product.sku
			ORDER BY SUM(oi.quantity) DESC, SUM(oi.quantity * oi.priceEach) DESC
			""")
	List<Object[]> topProducts(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to,
			org.springframework.data.domain.Pageable pageable);

	boolean existsByProduct_ProductId(String productId);

}
