package com.dungochung.shopdongho.service;

import org.springframework.web.multipart.MultipartFile;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.UserEntity;

public interface UserService {
	// User registration
	ResponseDataDto registerCustomer(UserEntity user, String rawPassword, MultipartFile profileImage);

	ResponseDataDto loginCustomer(String usernameOrEmail, String rawPassword);

	// User login returns JWT or session token in ResponseDataDto
	ResponseDataDto login(String usernameOrEmail, String rawPassword);

	ResponseDataDto searchByName(String keyword, int page, int size);

	// Admin: list all users
	ResponseDataDto getAllUsers(int page, int size);

	// Admin: create user
	ResponseDataDto createUser(UserEntity user, String rawPassword, MultipartFile profileImage);

	// Admin: update user
	ResponseDataDto updateUser(String userId, UserEntity user, MultipartFile profileImage);

	// Admin: delete user
	ResponseDataDto deleteUser(String userId);

	// View own info (for user)
	ResponseDataDto getUserById(String userId);

	// Update for user
	ResponseDataDto updateUserInfo(String userId, UserEntity updateUser);

	ResponseDataDto updateCustomerAvatar(String userId, MultipartFile profileImage);
}
