package com.dungochung.shopdongho.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.OrderStatusHistoryEntity;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistoryEntity, Long> {
	List<OrderStatusHistoryEntity> findByOrderIdOrderByHistoryIdAsc(Integer orderId);
}
