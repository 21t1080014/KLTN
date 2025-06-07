package com.dungochung.shopdongho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.ProductImageEntity;


@Repository
public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Integer> {

}
