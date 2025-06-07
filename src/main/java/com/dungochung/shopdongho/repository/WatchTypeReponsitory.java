package com.dungochung.shopdongho.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.WatchTypeEntity;

@Repository
public interface WatchTypeReponsitory extends JpaRepository<WatchTypeEntity, Integer> {
	boolean existsByName(String name);

	boolean existsByNameAndTypeIdNot(String name, Integer typeId);

	Page<WatchTypeEntity> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
