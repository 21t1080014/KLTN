package com.dungochung.shopdongho.entity;

import com.dungochung.shopdongho.enums.Gender;
import com.dungochung.shopdongho.enums.ProductCondition;
import com.dungochung.shopdongho.enums.Segment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.dungochung.shopdongho.converter.GenderConverter;
import com.dungochung.shopdongho.converter.ProductConditionConverter;
import com.dungochung.shopdongho.converter.SegmentConverter;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UuidGenerator;

import com.dungochung.shopdongho.enums.ProductStatus;

@Entity
@Table(name = "products")
public class ProductEntity {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.AUTO)
	@Column(name = "product_id", nullable = false, length = 250)
	private String productId;

	@Column(name = "sku", nullable = false, unique = true, length = 50)
	private String sku;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "brand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_brand"))
	private BrandEntity brand;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "type_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_type"))
	private WatchTypeEntity type;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "case_material_id", foreignKey = @ForeignKey(name = "fk_products_case_material"))
	private CaseMaterialEntity caseMaterial;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "strap_material_id", foreignKey = @ForeignKey(name = "fk_products_strap_material"))
	private StrapMaterialEntity strapMaterial;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "glass_material_id", foreignKey = @ForeignKey(name = "fk_products_glass_material"))
	private GlassMaterialEntity glassMaterial;

	// Danh mục đa cấp (nullable - sản phẩm cũ chưa gán danh mục vẫn hợp lệ)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_products_category"))
	private CategoryEntity category;

	@Column(name = "origin", length = 100)
	private String origin;

	@Convert(converter = ProductConditionConverter.class)
	@Column(name = "product_condition", nullable = false)
	private ProductCondition condition = ProductCondition.MOI_100;

	@Column(name = "warranty_period", length = 50)
	private String warrantyPeriod;

	@Column(name = "price", nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Convert(converter = GenderConverter.class)
	@Column(name = "gender")
	private Gender gender;

	@Convert(converter = SegmentConverter.class)
	@Column(name = "segment")
	private Segment segment;
	@Column(name = "description", columnDefinition = "TEXT")
	private String description;
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	private Set<PromotionProductEntity> promotionProducts;

	/** Trạng thái vòng đời: chỉ ACTIVE mới hiển thị/bán ở storefront. */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@ColumnDefault("'ACTIVE'")
	private ProductStatus status = ProductStatus.ACTIVE;

	/** Tổng hàng có thể bán của mọi biến thể (quantity - reserved). Tính bằng subquery, không cần join. */
	@Formula("(select coalesce(sum(i.quantity - i.reserved_quantity), 0) from inventories i where i.product_id = product_id)")
	private Integer availableQuantity;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@OrderBy("sortOrder ASC")
	private List<ProductImageEntity> images;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public ProductEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ProductEntity(String productId, String sku, String name, BrandEntity brand, WatchTypeEntity type,
			CaseMaterialEntity caseMaterial, StrapMaterialEntity strapMaterial, GlassMaterialEntity glassMaterial,
			String origin, ProductCondition condition, String warrantyPeriod, BigDecimal price, Gender gender,
			Segment segment, String description, List<ProductImageEntity> images, LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		super();
		this.productId = productId;
		this.sku = sku;
		this.name = name;
		this.brand = brand;
		this.type = type;
		this.caseMaterial = caseMaterial;
		this.strapMaterial = strapMaterial;
		this.glassMaterial = glassMaterial;
		this.origin = origin;
		this.condition = condition;
		this.warrantyPeriod = warrantyPeriod;
		this.price = price;
		this.gender = gender;
		this.segment = segment;
		this.description = description;
		this.images = images;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public String getProductId() {
		return productId;
	}

	public Set<PromotionProductEntity> getPromotionProducts() {
		return promotionProducts;
	}

	public ProductStatus getStatus() {
		return status;
	}

	public void setStatus(ProductStatus status) {
		this.status = status;
	}

	public int getAvailableQuantity() {
		return availableQuantity == null ? 0 : Math.max(availableQuantity, 0);
	}

	public void setPromotionProducts(Set<PromotionProductEntity> promotionProducts) {
		this.promotionProducts = promotionProducts;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BrandEntity getBrand() {
		return brand;
	}

	public void setBrand(BrandEntity brand) {
		this.brand = brand;
	}

	public WatchTypeEntity getType() {
		return type;
	}

	public void setType(WatchTypeEntity type) {
		this.type = type;
	}

	public CaseMaterialEntity getCaseMaterial() {
		return caseMaterial;
	}

	public void setCaseMaterial(CaseMaterialEntity caseMaterial) {
		this.caseMaterial = caseMaterial;
	}

	public StrapMaterialEntity getStrapMaterial() {
		return strapMaterial;
	}

	public void setStrapMaterial(StrapMaterialEntity strapMaterial) {
		this.strapMaterial = strapMaterial;
	}

	public GlassMaterialEntity getGlassMaterial() {
		return glassMaterial;
	}

	public void setGlassMaterial(GlassMaterialEntity glassMaterial) {
		this.glassMaterial = glassMaterial;
	}

	public CategoryEntity getCategory() {
		return category;
	}

	public void setCategory(CategoryEntity category) {
		this.category = category;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public ProductCondition getCondition() {
		return condition;
	}

	public void setCondition(ProductCondition condition) {
		this.condition = condition;
	}

	public String getWarrantyPeriod() {
		return warrantyPeriod;
	}

	public void setWarrantyPeriod(String warrantyPeriod) {
		this.warrantyPeriod = warrantyPeriod;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public Segment getSegment() {
		return segment;
	}

	public void setSegment(Segment segment) {
		this.segment = segment;
	}

	public List<ProductImageEntity> getImages() {
		return images;
	}

	public void setImages(List<ProductImageEntity> images) {
		this.images = images;
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

}
