package com.dungochung.shopdongho.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.CaseMaterialEntity;

@Repository
public interface CaseMaterialReponsitory extends JpaRepository<CaseMaterialEntity, Integer> {
	Page<CaseMaterialEntity> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

	boolean existsByNameAndCaseMaterialIdNot(String name, Integer strapId);

	boolean existsByName(String name);
}
