package com.dungochung.shopdongho.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.UserVoucherEntity;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucherEntity, Long> {

	boolean existsByUser_UserIdAndVoucher_VoucherCodeAndUsedAtIsNull(String userId, String voucherCode);

	Optional<UserVoucherEntity> findByUserUserIdAndVoucherVoucherCode(String userId, String voucherCode);

	List<UserVoucherEntity> findByUser_UserIdAndUsedAtIsNull(String userId);
}
