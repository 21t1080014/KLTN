package com.dungochung.shopdongho.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.dto.BrandIdNameDto;
import com.dungochung.shopdongho.entity.BrandEntity;

@Repository
public interface BrandReponsitory extends JpaRepository<BrandEntity, Integer> {
	boolean existsByName(String name);

	boolean existsByNameAndBrandIdNot(String name, Integer brandId);

	Page<BrandEntity> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

	@Query("SELECT new com.dungochung.shopdongho.dto.BrandIdNameDto(b.brandId, b.name) FROM BrandEntity b")
	List<BrandIdNameDto> findAllBrandIdAndName();
}
