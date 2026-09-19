package com.dungochung.shopdongho.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.UserStatusHistoryEntity;

@Repository
public interface UserStatusHistoryRepository extends JpaRepository<UserStatusHistoryEntity, Long> {
	List<UserStatusHistoryEntity> findByUserIdOrderByHistoryIdDesc(String userId);
}
