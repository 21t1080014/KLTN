package com.dungochung.shopdongho.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.CategoryEntity;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Integer> {
	boolean existsByNameAndParent_CategoryId(String name, Integer parentId);

	boolean existsByNameAndParentIsNull(String name);

	List<CategoryEntity> findByParentIsNull();

	List<CategoryEntity> findByParent_CategoryId(Integer parentId);

	Page<CategoryEntity> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

	Optional<CategoryEntity> findByNameIgnoreCase(String name);
}
