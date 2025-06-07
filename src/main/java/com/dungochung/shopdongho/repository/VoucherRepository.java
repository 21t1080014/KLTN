package com.dungochung.shopdongho.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.VoucherEntity;

@Repository
public interface VoucherRepository extends JpaRepository<VoucherEntity, String> {
	Optional<VoucherEntity> findFirstByStartAtBeforeAndEndAtAfterAndUsageLimitGreaterThanAndMinOrderAmountLessThanEqual(
			LocalDate startAt, LocalDate endAt, int usageLimit, BigDecimal minOrderAmount);

	Optional<VoucherEntity> findByVoucherCode(String voucherCode);

	Page<VoucherEntity> findByVoucherCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String code,
			String description, Pageable pageable);
}
