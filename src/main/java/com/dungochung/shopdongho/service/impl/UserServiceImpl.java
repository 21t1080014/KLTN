package com.dungochung.shopdongho.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dungochung.shopdongho.common.CurrentActor;
import com.dungochung.shopdongho.common.FileStorageService;
import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.RoleEntity;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.entity.UserStatusHistoryEntity;
import com.dungochung.shopdongho.enums.UserStatus;
import com.dungochung.shopdongho.repository.OrderRepository;
import com.dungochung.shopdongho.repository.RoleReponsitory;
import com.dungochung.shopdongho.repository.UserRepository;
import com.dungochung.shopdongho.repository.UserStatusHistoryRepository;
import com.dungochung.shopdongho.service.UserService;

@Service
public class UserServiceImpl implements UserService {
	@org.springframework.beans.factory.annotation.Autowired
	private com.dungochung.shopdongho.service.AuditService auditService;

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RoleReponsitory roleReponsitory;
	@Autowired
	private FileStorageService fileStorageService;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private UserStatusHistoryRepository statusHistoryRepository;

	@Override
	public ResponseDataDto loginCustomer(String usernameOrEmail, String rawPassword) {
		Optional<UserEntity> optionalUser = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
		if (optionalUser.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Tài khoản không tồn tại", 404);
		}

		UserEntity user = optionalUser.get();

		// Kiểm tra mật khẩu
		if (!BCrypt.checkpw(rawPassword, user.getPasswordHash())) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Mật khẩu không đúng", 401);
		}

		if (!user.getRole().getRoleName().equals(Constant.ROLE_CUSTOMER)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Chỉ khách hàng được phép đăng nhập", 403);
		}

		// Kiểm tra trạng thái tài khoản
		if (!user.getStatus().equals(UserStatus.ACTIVE)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Tài khoản chưa được kích hoạt hoặc đã bị khóa", 403);
		}

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Đăng nhập thành công", user);
	}

	@Override
	public ResponseDataDto registerCustomer(UserEntity user, String rawPassword, MultipartFile profileImage) {
		if (userRepository.existsByUsername(user.getUsername())) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Tên đăng nhập đã tồn tại", 400);
		}
		if (userRepository.existsByEmail(user.getEmail())) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Email đã tồn tại", 400);
		}

		// Mã hóa mật khẩu
		String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
		user.setPasswordHash(hashedPassword);

		// Gán role là customer
		Optional<RoleEntity> roleOpt = roleReponsitory.findByRoleName(Constant.ROLE_CUSTOMER);
		if (roleOpt.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy vai trò customer", 500);
		}
		user.setRole(roleOpt.get());
		user.setStatus(UserStatus.ACTIVE);

		try {
			if (profileImage != null && !profileImage.isEmpty()) {
				String savedFileName = fileStorageService.saveFile(profileImage, "users");
				user.setUserImage(savedFileName);
			}
		} catch (IOException e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Tải ảnh đại diện thất bại", 500);
		}

		userRepository.save(user);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Đăng ký thành công", 200);
	}

	@Override
	public ResponseDataDto login(String usernameOrEmail, String rawPassword) {
		Optional<UserEntity> optionalUser = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
		if (optionalUser.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "User not found", 404);
		}
		UserEntity user = optionalUser.get();

		if (BCrypt.checkpw(rawPassword, user.getPasswordHash())) {
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Login successful", user);
		} else {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Invalid password", 401);
		}
	}

	@Override
	public ResponseDataDto getAllUsers(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<UserEntity> userList = userRepository.findAll(pageable);
		Map<String, Object> data = new HashMap<>();
		data.put("user", userList.getContent());
		data.put("pagination", PaginationCommon.getPaginationInfo(page, userList.getTotalPages(), 3));
		data.put("totalCount", userList.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto createUser(UserEntity user, String rawPassword, MultipartFile profileImage) {
		if (userRepository.existsByUsername(user.getUsername())) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Username already exists", 400);
		}
		if (userRepository.existsByEmail(user.getEmail())) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Email already exists", 400);
		}

		String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
		user.setPasswordHash(hashedPassword);

		// user.setStatus(UserStatus.ACTIVE);
		try {
			if (profileImage != null && !profileImage.isEmpty()) {
				String savedFileName = fileStorageService.saveFile(profileImage, "users");
				user.setUserImage(savedFileName);
			}
		} catch (IOException e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Image upload failed", 500);
		}
		userRepository.save(user);
		auditService.log("USER_CREATE", "USER", user.getUserId(), "username=" + user.getUsername() + ", role="
				+ (user.getRole() == null ? null : user.getRole().getRoleName()) + ", status=" + user.getStatus());

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "User created successfully", user);
	}

	@Override
	public ResponseDataDto updateUser(String userId, UserEntity updatedUser, MultipartFile profileImage) {
		Optional<UserEntity> optionalUser = userRepository.findById(userId);
		if (optionalUser.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "User not found", 404);
		}

		UserEntity user = optionalUser.get();
		if (updatedUser.getRole() == null || updatedUser.getStatus() == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Vai trò hoặc trạng thái không hợp lệ", 400);
		}
		boolean isSelf = user.getUsername().equals(CurrentActor.username());
		boolean roleChanged = user.getRole() == null || !user.getRole().getRoleId().equals(updatedUser.getRole().getRoleId());
		boolean statusChanged = user.getStatus() != updatedUser.getStatus();
		if (isSelf && (roleChanged || statusChanged)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Không thể tự đổi vai trò hoặc trạng thái của tài khoản đang đăng nhập", 400);
		}
		if ((roleChanged || statusChanged) && isLastActiveAdmin(user)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Đây là quản trị viên hoạt động cuối cùng, không thể đổi vai trò hoặc khóa", 400);
		}
		UserStatus oldStatus = user.getStatus();
		String oldRoleName = user.getRole() == null ? null : user.getRole().getRoleName();
		boolean passwordChanged = updatedUser.getPasswordHash() != null && !updatedUser.getPasswordHash().isEmpty();
		user.setFullName(updatedUser.getFullName());
		user.setPhone(updatedUser.getPhone());
		if (updatedUser.getPasswordHash() != null && !updatedUser.getPasswordHash().isEmpty()) {
			String newHashed = BCrypt.hashpw(updatedUser.getPasswordHash(), BCrypt.gensalt());
			user.setPasswordHash(newHashed);
		}
		user.setAddress(updatedUser.getAddress());
		user.setRole(updatedUser.getRole());
		user.setSeller(updatedUser.isSeller());
		user.setStatus(updatedUser.getStatus());
		if (profileImage != null && !profileImage.isEmpty()) {
			try {
				if (profileImage != null && !profileImage.isEmpty()) {
					String updatedFile = fileStorageService.updateFile(user.getUserImage(), profileImage, "users");
					user.setUserImage(updatedFile);
				}
			} catch (IOException e) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Image update failed", 500);
			}
		}
		if (statusChanged) {
			// Đồng bộ thông tin khóa + ghi lịch sử giống trang Khách hàng
			if (user.getStatus() == UserStatus.LOCKED) {
				user.setLockReason("Khóa từ trang Người dùng");
				user.setLockedAt(LocalDateTime.now());
				user.setLockedBy(CurrentActor.username());
			} else {
				user.setLockReason(null);
				user.setLockedAt(null);
				user.setLockedBy(null);
			}
			UserStatusHistoryEntity h = new UserStatusHistoryEntity();
			h.setUserId(userId);
			h.setFromStatus(oldStatus.name());
			h.setToStatus(user.getStatus().name());
			h.setReason("Đổi trạng thái từ trang Người dùng");
			h.setChangedBy(CurrentActor.username());
			statusHistoryRepository.save(h);
		}
		userRepository.save(user);
		StringBuilder diff = new StringBuilder("username=").append(user.getUsername());
		if (roleChanged) {
			diff.append(", role: ").append(oldRoleName).append(" -> ").append(user.getRole().getRoleName());
		}
		if (statusChanged) {
			diff.append(", status: ").append(oldStatus).append(" -> ").append(user.getStatus());
		}
		if (passwordChanged) {
			diff.append(", đổi mật khẩu");
		}
		auditService.log("USER_UPDATE", "USER", userId, diff.toString());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "User updated successfully", user);
	}

	@Override
	public ResponseDataDto deleteUser(String userId) {
		UserEntity target = userRepository.findById(userId).orElse(null);
		if (target == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "User not found", 404);
		}
		if (target.getUsername().equals(CurrentActor.username())) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không thể tự xóa tài khoản đang đăng nhập", 400);
		}
		if (isLastActiveAdmin(target)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không thể xóa quản trị viên hoạt động cuối cùng", 400);
		}
		// Khách đã có đơn hàng thì giữ lại để bảo toàn lịch sử/doanh thu; dùng chức năng khóa thay vì xóa
		if (orderRepository.existsByUserId(userId)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Tài khoản đã có đơn hàng nên không thể xóa. Hãy khóa tài khoản thay vì xóa.", 400);
		}
		try {
			userRepository.deleteById(userId);
			userRepository.flush();
			auditService.log("USER_DELETE", "USER", userId, "username=" + target.getUsername() + ", role="
					+ (target.getRole() == null ? null : target.getRole().getRoleName()));
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Tài khoản còn dữ liệu liên quan (đấu giá, ký gửi...) nên không thể xóa. Hãy khóa tài khoản.", 400);
		}
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "User deleted successfully", 200);
	}

	@Override
	public ResponseDataDto getUserById(String userId) {
		Optional<UserEntity> optionalUser = userRepository.findById(userId);
		if (optionalUser.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "User not found", 404);
		}
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "User retrieved", optionalUser.get());
	}

	@Override
	public ResponseDataDto searchByName(String keyword, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<UserEntity> result = userRepository
				.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(keyword, keyword, pageable);

		Map<String, Object> data = new HashMap<>();
		data.put("user", result.getContent());
		data.put("pagination", PaginationCommon.getPaginationInfo(page, result.getTotalPages(), 3));
		data.put("totalCount", result.getTotalElements());

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Search results for '" + keyword + "'", data);
	}

	@Override
	public ResponseDataDto updateUserInfo(String userId, UserEntity updateUser) {
		Optional<UserEntity> optionalUser = userRepository.findById(userId);
		if (optionalUser.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy người dùng", null);
		}

		UserEntity user = optionalUser.get();
		user.setFullName(updateUser.getFullName());
		user.setEmail(updateUser.getEmail());
		user.setPhone(updateUser.getPhone());
		user.setAddress(updateUser.getAddress());
		user.setUpdatedAt(LocalDateTime.now());

		userRepository.save(user);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Cập nhật thành công", user);
	}

	@Override
	public ResponseDataDto updateCustomerAvatar(String userId, MultipartFile profileImage) {
		Optional<UserEntity> optionalUser = userRepository.findById(userId);
		if (optionalUser.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy người dùng", null);
		}

		UserEntity user = optionalUser.get();
		try {
			String updatedImage = fileStorageService.updateFile(user.getUserImage(), profileImage, "users");
			user.setUserImage(updatedImage);
			user.setUpdatedAt(LocalDateTime.now());
			userRepository.save(user);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Cập nhật ảnh đại diện thành công", user);
		} catch (IOException e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Cập nhật ảnh đại diện thất bại", 500);
		}
	}


	/** true nếu user là admin đang ACTIVE và là người duy nhất còn lại (tránh khóa/xóa hết quyền quản trị). */
	private boolean isLastActiveAdmin(UserEntity user) {
		return user.getRole() != null && Constant.ROLE_ADMIN.equals(user.getRole().getRoleName())
				&& user.getStatus() == UserStatus.ACTIVE
				&& userRepository.countByRole_RoleNameAndStatus(Constant.ROLE_ADMIN, UserStatus.ACTIVE) <= 1;
	}
}
