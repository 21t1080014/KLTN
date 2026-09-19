package com.dungochung.shopdongho;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.ProductVariantEntity;
import com.dungochung.shopdongho.enums.MovementType;
import com.dungochung.shopdongho.repository.BrandReponsitory;
import com.dungochung.shopdongho.repository.CaseMaterialReponsitory;
import com.dungochung.shopdongho.repository.GlassMaterialReponsitory;
import com.dungochung.shopdongho.repository.InventoryMovementRepository;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.repository.ProductVariantRepository;
import com.dungochung.shopdongho.repository.StraMaterialReponsitory;
import com.dungochung.shopdongho.repository.WatchTypeReponsitory;

import jakarta.persistence.EntityManager;

/** Module 2 - biến thể, trạng thái sản phẩm, tồn kho + lịch sử. Rollback sau mỗi test. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductVariantInventoryTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private EntityManager em;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private ProductVariantRepository variantRepository;
	@Autowired
	private InventoryRepository inventoryRepository;
	@Autowired
	private InventoryMovementRepository movementRepository;
	@Autowired
	private BrandReponsitory brandRepo;
	@Autowired
	private WatchTypeReponsitory typeRepo;
	@Autowired
	private CaseMaterialReponsitory caseRepo;
	@Autowired
	private StraMaterialReponsitory strapRepo;
	@Autowired
	private GlassMaterialReponsitory glassRepo;

	private MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder b) {
		return b.sessionAttr("roleName", "admin");
	}

	private String createProduct(String sku, String status, String price) throws Exception {
		MockHttpServletRequestBuilder req = post("/admin/products/api").param("sku", sku).param("name", "TEST " + sku)
				.param("brandId", String.valueOf(brandRepo.findAll().get(0).getBrandId()))
				.param("typeId", String.valueOf(typeRepo.findAll().get(0).getTypeId()))
				.param("caseMaterialId", String.valueOf(caseRepo.findAll().get(0).getCaseMaterialId()))
				.param("strapMaterialId", String.valueOf(strapRepo.findAll().get(0).getStrapMaterialId()))
				.param("glassMaterialId", String.valueOf(glassRepo.findAll().get(0).getGlassMaterialId()))
				.param("origin", "Test").param("condition", "MOI_100").param("warrantyPeriod", "12")
				.param("price", price).param("gender", "MALE").param("segment", "CASUAL").param("description", "d");
		if (status != null) {
			req.param("status", status);
		}
		mockMvc.perform(admin(req)).andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value(1));
		em.flush();
		em.clear();
		return productRepository.findAll().stream().filter(p -> sku.equals(p.getSku())).findFirst().orElseThrow()
				.getProductId();
	}

	@Test
	void newProductGetsDefaultVariantAndInventory() throws Exception {
		String pid = createProduct("T2-A", null, "1000000");
		ProductVariantEntity def = variantRepository.findByProduct_ProductIdAndIsDefaultTrue(pid).orElseThrow();
		assertEquals("T2-A", def.getSku());
		assertEquals(0, def.getPrice().compareTo(new java.math.BigDecimal("1000000")));
		InventoryEntity inv = inventoryRepository.findByVariant_VariantId(def.getVariantId()).orElseThrow();
		assertEquals(0, inv.getQuantity());
		assertEquals(0, inv.getReservedQuantity());
	}

	@Test
	void statusControlsStorefrontVisibility() throws Exception {
		String draft = createProduct("T2-D", "DRAFT", "500000");
		String active = createProduct("T2-E", "ACTIVE", "500000");
		mockMvc.perform(get("/api/product-detail/" + draft)).andExpect(jsonPath("$.responseCode").value(0));
		mockMvc.perform(get("/api/product-detail/" + active)).andExpect(jsonPath("$.responseCode").value(1));
	}

	@Test
	void variantLifecycleAndStockHistory() throws Exception {
		String pid = createProduct("T2-V", null, "2000000");

		// SKU trùng với SKU sản phẩm (biến thể mặc định) bị từ chối
		mockMvc.perform(admin(post("/admin/products/api/" + pid + "/variants").param("sku", "T2-V")
				.param("variantName", "x").param("price", "1"))).andExpect(jsonPath("$.responseCode").value(0));

		// tạo biến thể phụ có tồn ban đầu 7
		mockMvc.perform(admin(post("/admin/products/api/" + pid + "/variants").param("sku", "T2-V-BLK")
				.param("variantName", "Dây da đen").param("color", "Đen").param("price", "2200000")
				.param("initialQuantity", "7").param("lowStockThreshold", "3")))
				.andExpect(jsonPath("$.responseCode").value(1)).andExpect(jsonPath("$.data.quantity").value(7));
		em.flush();
		em.clear();
		ProductVariantEntity black = variantRepository.findBySku("T2-V-BLK").orElseThrow();
		assertEquals(false, black.isDefault());

		// lịch sử: 1 dòng INITIAL +7
		var moves = movementRepository.findByVariantId(black.getVariantId(), org.springframework.data.domain.PageRequest.of(0, 10));
		assertEquals(1, moves.getTotalElements());
		assertEquals(MovementType.INITIAL, moves.getContent().get(0).getMovementType());

		// kiểm kê xuống 2 => ADJUSTMENT -5 và vào danh sách tồn thấp (ngưỡng 3)
		mockMvc.perform(admin(put("/admin/inventory/api/variant/" + black.getVariantId()).param("quantity", "2")
				.param("note", "kiểm kê")))
				.andExpect(jsonPath("$.responseCode").value(1));
		mockMvc.perform(admin(get("/admin/inventory/api/low-stock"))).andExpect(content().string(containsString("T2-V-BLK")));
		mockMvc.perform(admin(get("/admin/inventory/api/movements").param("variantId", String.valueOf(black.getVariantId()))))
				.andExpect(jsonPath("$.data.totalCount").value(2)).andExpect(jsonPath("$.data.movements[0].type").value("ADJUSTMENT"))
				.andExpect(jsonPath("$.data.movements[0].change").value(-5));

		// không xóa được biến thể còn tồn; không xóa được biến thể mặc định
		mockMvc.perform(admin(delete("/admin/products/api/" + pid + "/variants/" + black.getVariantId())))
				.andExpect(jsonPath("$.responseCode").value(0));
		ProductVariantEntity def = variantRepository.findByProduct_ProductIdAndIsDefaultTrue(pid).orElseThrow();
		mockMvc.perform(admin(delete("/admin/products/api/" + pid + "/variants/" + def.getVariantId())))
				.andExpect(jsonPath("$.responseCode").value(0));

		// về 0 rồi xóa được
		mockMvc.perform(admin(put("/admin/inventory/api/variant/" + black.getVariantId()).param("quantity", "0")))
				.andExpect(jsonPath("$.responseCode").value(1));
		mockMvc.perform(admin(delete("/admin/products/api/" + pid + "/variants/" + black.getVariantId())))
				.andExpect(jsonPath("$.responseCode").value(1));
	}

	@Test
	void reservedStockCannotBeAdjustedBelowReservedAndAffectsAvailable() throws Exception {
		String pid = createProduct("T2-R", null, "900000");
		ProductVariantEntity def = variantRepository.findByProduct_ProductIdAndIsDefaultTrue(pid).orElseThrow();
		InventoryEntity inv = inventoryRepository.findByVariant_VariantId(def.getVariantId()).orElseThrow();
		inv.setQuantity(10);
		inv.setReservedQuantity(4);
		inventoryRepository.saveAndFlush(inv);
		em.clear();

		// tồn thực không được xuống thấp hơn số đang giữ (4)
		mockMvc.perform(admin(put("/admin/inventory/api/variant/" + def.getVariantId()).param("quantity", "3")))
				.andExpect(jsonPath("$.responseCode").value(0));
		// storefront thấy số có thể bán = 10 - 4 = 6
		mockMvc.perform(get("/api/product-detail/" + pid)).andExpect(jsonPath("$.data.quantity").value(6));
	}

	@Test
	void purchaseEntryFeedsStockAndHistory() throws Exception {
		String pid = createProduct("T2-P", null, "1500000");
		String body = "{\"product\":{\"productId\":\"" + pid + "\"},\"quantity\":5,\"importPrice\":1000000,\"note\":\"nhập test\"}";
		mockMvc.perform(admin(post("/admin/purchases/api").contentType("application/json").content(body)))
				.andExpect(jsonPath("$.responseCode").value(1));
		em.flush();
		em.clear();
		ProductVariantEntity def = variantRepository.findByProduct_ProductIdAndIsDefaultTrue(pid).orElseThrow();
		InventoryEntity inv = inventoryRepository.findByVariant_VariantId(def.getVariantId()).orElseThrow();
		assertEquals(5, inv.getQuantity());
		var moves = movementRepository.findByVariantId(def.getVariantId(), org.springframework.data.domain.PageRequest.of(0, 10));
		assertTrue(moves.getContent().stream().anyMatch(m -> m.getMovementType() == MovementType.PURCHASE_IN && m.getQuantityChange() == 5));
	}

	@Test
	void productPagesStillRender() throws Exception {
		mockMvc.perform(admin(get("/admin/products"))).andExpect(status().isOk());
		mockMvc.perform(admin(get("/admin/inventory"))).andExpect(status().isOk());
	}
}
