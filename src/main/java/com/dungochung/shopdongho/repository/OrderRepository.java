package com.dungochung.shopdongho.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.OrderEntity;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Integer> {
	Page<OrderEntity> findByUserId(String userId, Pageable pageable);

	@EntityGraph(attributePaths = { "user" })
	Page<OrderEntity> findAll(Pageable pageable);

	@EntityGraph(attributePaths = { "user" })
	Page<OrderEntity> findByOrderStatus(com.dungochung.shopdongho.enums.OrderStatus orderStatus, Pageable pageable);

	boolean existsByUserId(String userId);

	@Query("SELECT o.orderStatus, o.paymentStatus, COUNT(o), COALESCE(SUM(o.totalPrice), 0), MIN(o.createdAt), MAX(o.createdAt) "
			+ "FROM OrderEntity o WHERE o.userId = :userId GROUP BY o.orderStatus, o.paymentStatus")
	List<Object[]> summarizeByUser(@org.springframework.data.repository.query.Param("userId") String userId);

	@Query("SELECT COUNT(o) FROM OrderEntity o")
	long countAllOrders();

	@Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM OrderEntity o WHERE o.paymentStatus = 'paid'")
	BigDecimal getTotalRevenue();

	@Query("SELECT o.orderStatus, COUNT(o) FROM OrderEntity o GROUP BY o.orderStatus")
	List<Object[]> countOrdersByStatus();
}
