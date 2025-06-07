package com.dungochung.shopdongho.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dungochung.shopdongho.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	Optional<UserEntity> findByUsernameOrEmail(String username, String email);
	Page<UserEntity> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String usernamePart,
            String fullNamePart,
            Pageable pageable);
}
