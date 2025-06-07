package com.dungochung.shopdongho.entity;

import com.dungochung.shopdongho.enums.OrderStatus;
import com.dungochung.shopdongho.enums.PaymentMethod;
import com.dungochung.shopdongho.enums.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_id")
	private Integer orderId;

	@Column(name = "user_id", nullable = false)
	private String userId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private UserEntity user;
	@Column(name = "total_price", nullable = false)
	private BigDecimal totalPrice;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false)
	private PaymentStatus paymentStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "order_status", nullable = false)
	private OrderStatus orderStatus;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<OrderItemEntity> orderItems;

	@PrePersist
	protected void onCreate() {
		createdAt = updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public OrderEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public OrderEntity(Integer orderId, String userId, BigDecimal totalPrice, PaymentMethod paymentMethod,
			PaymentStatus paymentStatus, OrderStatus orderStatus, LocalDateTime createdAt, LocalDateTime updatedAt,
			List<OrderItemEntity> orderItems) {
		super();
		this.orderId = orderId;
		this.userId = userId;
		this.totalPrice = totalPrice;
		this.paymentMethod = paymentMethod;
		this.paymentStatus = paymentStatus;
		this.orderStatus = orderStatus;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.orderItems = orderItems;
	}

	public Integer getOrderId() {
		return orderId;
	}

	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}

	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(BigDecimal totalPrice) {
		this.totalPrice = totalPrice;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public PaymentStatus getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(PaymentStatus paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	public OrderStatus getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(OrderStatus orderStatus) {
		this.orderStatus = orderStatus;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public List<OrderItemEntity> getOrderItems() {
		return orderItems;
	}

	public void setOrderItems(List<OrderItemEntity> orderItems) {
		this.orderItems = orderItems;
	}

}
