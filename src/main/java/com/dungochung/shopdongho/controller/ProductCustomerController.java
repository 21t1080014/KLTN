package com.dungochung.shopdongho.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dungochung.shopdongho.common.FileStorageService;
import com.dungochung.shopdongho.dto.ApplyVoucherRequestDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.OrderItemEntity;
import com.dungochung.shopdongho.entity.UserEntity;
import com.dungochung.shopdongho.enums.PaymentMethod;
import com.dungochung.shopdongho.service.BrandService;
import com.dungochung.shopdongho.service.OrderService;
import com.dungochung.shopdongho.service.ProductCustomerService;
import com.dungochung.shopdongho.service.UserService;
import com.dungochung.shopdongho.service.VoucherService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class ProductCustomerController {
	@Autowired
	private FileStorageService fileStorageService;
	@Autowired
	private ProductCustomerService productCustomerService;
	@Autowired
	private OrderService orderService;
	@Autowired
	private BrandService brandService;
	@Autowired
	private VoucherService voucherService;
	@Autowired
	private UserService userService;

	@GetMapping("/user-customer")
	public ResponseDataDto getUserCustomer(HttpSession session) {
		UserEntity currentUser = (UserEntity) session.getAttribute("currentUser");
		if (currentUser == null) {
			return new ResponseDataDto(401, "Bạn chưa đăng nhập", null);
		}
		return userService.getUserById(currentUser.getUserId());
	}

	@PutMapping("/user-customer/avatar")
	public ResponseDataDto updateAvatar(@RequestParam("file") MultipartFile file, HttpSession session) {
		UserEntity currentUser = (UserEntity) session.getAttribute("currentUser");
		if (currentUser == null) {
			return new ResponseDataDto(401, "Bạn chưa đăng nhập", null);
		}
		return userService.updateCustomerAvatar(currentUser.getUserId(), file);
	}

	@PutMapping("/user-customer")
	public ResponseDataDto updateUserCustomer(@RequestBody UserEntity updateUser, HttpSession session) {
		UserEntity currentUser = (UserEntity) session.getAttribute("currentUser");
		if (currentUser == null) {
			return new ResponseDataDto(401, "Bạn chưa đăng nhập", null);
		}

		return userService.updateUserInfo(currentUser.getUserId(), updateUser);
	}

	@GetMapping("/voucher")
	public ResponseDataDto getVoucherCustomer(HttpSession session) {
		UserEntity currentUser = (UserEntity) session.getAttribute("currentUser");
		if (currentUser == null) {
			return new ResponseDataDto(401, "Bạn chưa đăng nhập", null);
		}
		return voucherService.getAvailableVouchersForUser(currentUser.getUserId());
	}

	@GetMapping("/featured/{brandId}")
	public ResponseDataDto getFeaturedProducts(@PathVariable int brandId) {
		return productCustomerService.getFeaturedProducts(brandId);
	}

	@GetMapping("/promotions/active-products")
	public ResponseDataDto getActivePromotionalProducts() {
		return productCustomerService.getActivePromotionalProducts();
	}

	@GetMapping("/orders")
	public ResponseDataDto getOrders(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size, HttpSession session) {
		UserEntity currentUser = (UserEntity) session.getAttribute("currentUser");
		if (currentUser == null) {
			return new ResponseDataDto(401, "Bạn chưa đăng nhập", null);
		}
		return orderService.getOrdersByUserId(currentUser.getUserId(), page, size);
	}

	@GetMapping("/collections/all")
	public ResponseDataDto getProducts(@RequestParam(required = false) List<String> genders,
			@RequestParam(required = false) List<String> segments,
			@RequestParam(required = false) List<String> brandNames, @RequestParam(required = false) Double priceMin,
			@RequestParam(required = false) Double priceMax, @RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String sortDir, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		boolean noFilters = (genders == null || genders.isEmpty()) && (segments == null || segments.isEmpty())
				&& (brandNames == null || brandNames.isEmpty()) && priceMin == null && priceMax == null;

		if (noFilters && sortBy == null && sortDir == null) {
			return productCustomerService.getAllProductCustomer(page, size);
		}

		return productCustomerService.searchProducts(genders, segments, brandNames, priceMin, priceMax, sortBy, sortDir,
				page, size);
	}

	@GetMapping("/newest")
	public ResponseDataDto getNewestProducts() {
		return productCustomerService.getNewestProducts();
	}

	@GetMapping("/brand-name")
	public ResponseDataDto getBrandNameAndId() {
		return brandService.getBrandNameAndId();
	}

	@GetMapping("/gender")
	public ResponseDataDto getGender() {
		return productCustomerService.getGenders();
	}

	@GetMapping("/segment")
	public ResponseDataDto getSegment() {
		return productCustomerService.getSegment();
	}

	@GetMapping("/img/{fileName:.+}")
	@ResponseBody
	public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
		return fileStorageService.getImageAsResponse("products/" + fileName);
	}

	@GetMapping("/imgUser/{fileName:.+}")
	@ResponseBody
	public ResponseEntity<Resource> getImageUsr(@PathVariable String fileName) {
		return fileStorageService.getImageAsResponse("users/" + fileName);
	}

	@GetMapping("/product-detail/{id}")
	public ResponseDataDto getProductDetail(@PathVariable String id) {
		return productCustomerService.getProductById(id);
	}

	@GetMapping("/product-search")
	public ResponseDataDto searchProductShop(@RequestParam String keyword, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		return productCustomerService.searchProducts(keyword, page, size);
	}

	@GetMapping("/products-suggest")
	public ResponseDataDto suggestProducts(@RequestParam String keyword) {
		return productCustomerService.suggestProducts(keyword);
	}

	@PostMapping("/create-orders")
	public ResponseDataDto createOrder(@RequestParam BigDecimal totalPrice,
			@RequestParam PaymentMethod paymentMethod, @RequestBody List<OrderItemEntity> items, HttpSession session) {
		UserEntity currentUser = (UserEntity) session.getAttribute("currentUser");
		if (currentUser == null) {
			return new ResponseDataDto(401, "Bạn chưa đăng nhập", null);
		}
		// Chủ sở hữu đơn hàng luôn lấy từ session, không tin userId do client gửi lên (chống IDOR)
		return orderService.createOrder(currentUser.getUserId(), items, totalPrice, paymentMethod);
	}

	/** Khách tự hủy đơn của chính mình (chỉ khi chưa vào đóng gói). Chủ đơn lấy từ session, không tin client. */
	@PostMapping("/orders/{orderId}/cancel")
	public ResponseDataDto cancelOrder(@PathVariable Integer orderId, @RequestParam(required = false) String reason,
			HttpSession session) {
		UserEntity currentUser = (UserEntity) session.getAttribute("currentUser");
		if (currentUser == null) {
			return new ResponseDataDto(401, "Bạn chưa đăng nhập", null);
		}
		return orderService.cancelByCustomer(currentUser.getUserId(), orderId, reason, currentUser.getUsername());
	}

	@PostMapping("/apply-voucher")
	public ResponseDataDto applyVoucher(@RequestBody ApplyVoucherRequestDto request, HttpSession session) {
		UserEntity currentUser = (UserEntity) session.getAttribute("currentUser");
		if (currentUser == null) {
			return new ResponseDataDto(401, "Bạn chưa đăng nhập", null);
		}
		// Chủ sở hữu voucher luôn lấy từ session, không tin userId do client gửi lên (chống IDOR)
		return orderService.applyVoucher(currentUser.getUserId(), request.getTotalPrice(), request.getVoucherCode());
	}

}
