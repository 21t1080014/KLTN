package com.dungochung.shopdongho.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.GlassMaterialEntity;

@Repository
public interface GlassMaterialReponsitory extends JpaRepository<GlassMaterialEntity, Integer> {
	boolean existsByName(String name);
	boolean existsByNameAndGlassMaterialIdNot(String name , Integer glassMaterialId);
	Page<GlassMaterialEntity> findByNameContainingIgnoreCase(String keyword,Pageable pageable);
}
