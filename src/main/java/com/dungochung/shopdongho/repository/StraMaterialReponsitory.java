package com.dungochung.shopdongho.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.StrapMaterialEntity;

@Repository
public interface StraMaterialReponsitory extends JpaRepository<StrapMaterialEntity, Integer> {

	Page<StrapMaterialEntity> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

	boolean existsByNameAndStrapMaterialIdNot(String name, Integer strapId);

	boolean existsByName(String name);

}
