package com.dungochung.shopdongho.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.RoleEntity;

@Repository
public interface RoleReponsitory extends JpaRepository<RoleEntity, Integer> {
	Optional<RoleEntity> findByRoleName(String roleName);
}
