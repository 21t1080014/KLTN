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

	// ---- Báo cáo. "Doanh thu" = đơn ĐÃ GIAO/HOÀN THÀNH và ĐÃ THANH TOÁN (khớp CustomerTier.SPEND_*), tính theo ngày đặt ----
	String REVENUE_FILTER = "o.order_status IN ('delivered','completed') AND o.payment_status = 'paid'";

	@Query(value = "SELECT DATE(o.created_at) AS bucket, COUNT(*) AS cnt, COALESCE(SUM(o.total_price),0) AS revenue FROM orders o "
			+ "WHERE o.created_at >= :from AND o.created_at < :to AND " + REVENUE_FILTER + " GROUP BY bucket ORDER BY bucket", nativeQuery = true)
	List<Object[]> revenueByDay(@org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
			@org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

	/** Bucket = ngày thứ Hai của tuần (tuần ISO). */
	@Query(value = "SELECT DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY) AS bucket, COUNT(*) AS cnt, COALESCE(SUM(o.total_price),0) AS revenue FROM orders o "
			+ "WHERE o.created_at >= :from AND o.created_at < :to AND " + REVENUE_FILTER + " GROUP BY bucket ORDER BY bucket", nativeQuery = true)
	List<Object[]> revenueByWeek(@org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
			@org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

	/** Bucket = ngày đầu tháng. */
	@Query(value = "SELECT DATE_FORMAT(o.created_at, '%Y-%m-01') AS bucket, COUNT(*) AS cnt, COALESCE(SUM(o.total_price),0) AS revenue FROM orders o "
			+ "WHERE o.created_at >= :from AND o.created_at < :to AND " + REVENUE_FILTER + " GROUP BY bucket ORDER BY bucket", nativeQuery = true)
	List<Object[]> revenueByMonth(@org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
			@org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

	@Query(value = "SELECT COALESCE(SUM(o.total_price),0) FROM orders o WHERE o.created_at >= :from AND o.created_at < :to AND " + REVENUE_FILTER, nativeQuery = true)
	BigDecimal sumRevenue(@org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
			@org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

	long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(java.time.LocalDateTime from, java.time.LocalDateTime to);

	long countByOrderStatus(com.dungochung.shopdongho.enums.OrderStatus status);

	/** Số đơn và tổng tiền đang chờ hoàn tiền cho khách. */
	@Query("SELECT COUNT(o), COALESCE(SUM(o.totalPrice), 0) FROM OrderEntity o WHERE o.paymentStatus = com.dungochung.shopdongho.enums.PaymentStatus.refund_pending")
	List<Object[]> refundPendingSummary();

	@EntityGraph(attributePaths = { "user" })
	List<OrderEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(java.time.LocalDateTime from,
			java.time.LocalDateTime to);

	@Query("SELECT COUNT(o) FROM OrderEntity o")
	long countAllOrders();

	@Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM OrderEntity o WHERE o.paymentStatus = 'paid'")
	BigDecimal getTotalRevenue();

	@Query("SELECT o.orderStatus, COUNT(o) FROM OrderEntity o GROUP BY o.orderStatus")
	List<Object[]> countOrdersByStatus();
}
