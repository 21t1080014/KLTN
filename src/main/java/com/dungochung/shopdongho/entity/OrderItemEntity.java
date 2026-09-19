package com.dungochung.shopdongho.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_item_id")
	private Integer orderItemId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private OrderEntity order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEntity product;

	// Biến thể đã bán (null với đơn cũ trước khi có biến thể; được backfill về biến thể mặc định)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "variant_id", foreignKey = @ForeignKey(name = "fk_order_item_variant"))
	@com.fasterxml.jackson.annotation.JsonIgnore
	private ProductVariantEntity variant;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	@Column(name = "price_each", nullable = false)
	private BigDecimal priceEach;

	public OrderItemEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public OrderItemEntity(Integer orderItemId, OrderEntity order, ProductEntity product, int quantity,
			BigDecimal priceEach) {
		super();
		this.orderItemId = orderItemId;
		this.order = order;
		this.product = product;
		this.quantity = quantity;
		this.priceEach = priceEach;
	}

	public Integer getOrderItemId() {
		return orderItemId;
	}

	public void setOrderItemId(Integer orderItemId) {
		this.orderItemId = orderItemId;
	}

	public OrderEntity getOrder() {
		return order;
	}

	public void setOrder(OrderEntity order) {
		this.order = order;
	}

	public ProductEntity getProduct() {
		return product;
	}

	public void setProduct(ProductEntity product) {
		this.product = product;
	}

	public ProductVariantEntity getVariant() {
		return variant;
	}

	public void setVariant(ProductVariantEntity variant) {
		this.variant = variant;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getPriceEach() {
		return priceEach;
	}

	public void setPriceEach(BigDecimal priceEach) {
		this.priceEach = priceEach;
	}

	
}
