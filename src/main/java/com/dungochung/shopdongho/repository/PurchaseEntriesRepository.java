package com.dungochung.shopdongho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.PurchaseEntriesEntity;

@Repository
public interface PurchaseEntriesRepository
		extends JpaRepository<PurchaseEntriesEntity, Long>, JpaSpecificationExecutor<PurchaseEntriesEntity> {

}
