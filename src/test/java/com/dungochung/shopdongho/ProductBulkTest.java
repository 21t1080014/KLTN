package com.dungochung.shopdongho;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.ProductVariantEntity;
import com.dungochung.shopdongho.enums.ProductStatus;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.repository.ProductVariantRepository;

import jakarta.persistence.EntityManager;

/** Import/export CSV và cập nhật hàng loạt (module 2). Rollback sau mỗi test. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductBulkTest {

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

	private MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder b) {
		return b.sessionAttr("roleName", "admin");
	}

	private ProductEntity anyProduct() {
		return productRepository.findAll().get(0);
	}

	@Test
	void exportListsEveryVariantWithSku() throws Exception {
		ProductEntity p = anyProduct();
		mockMvc.perform(admin(get("/admin/products/api/export"))).andExpect(status().isOk())
				.andExpect(content().string(containsString("sku,product_name,variant_name,price")))
				.andExpect(content().string(containsString(p.getSku())));
	}

	@Test
	void importUpdatesPriceStatusAndStockBySku() throws Exception {
		ProductEntity p = anyProduct();
		String csv = "sku,price,status,quantity\n" + p.getSku() + ",1234567,INACTIVE,9\n";
		mockMvc.perform(admin(multipart("/admin/products/api/import")
				.file(new MockMultipartFile("file", "p.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))))
				.andExpect(jsonPath("$.responseCode").value(1)).andExpect(jsonPath("$.data.updated").value(1));
		em.flush();
		em.clear();
		ProductEntity after = productRepository.findById(p.getProductId()).orElseThrow();
		assertEquals(0, after.getPrice().compareTo(new BigDecimal("1234567")));
		assertEquals(ProductStatus.INACTIVE, after.getStatus());
		ProductVariantEntity def = variantRepository.findByProduct_ProductIdAndIsDefaultTrue(p.getProductId()).orElseThrow();
		assertEquals(0, def.getPrice().compareTo(new BigDecimal("1234567")));
		assertEquals(9, inventoryRepository.findByVariant_VariantId(def.getVariantId()).orElseThrow().getQuantity());
	}

	@Test
	void importIsAllOrNothingWhenAnyRowIsInvalid() throws Exception {
		ProductEntity p = anyProduct();
		BigDecimal before = p.getPrice();
		String csv = "sku,price\n" + p.getSku() + ",999\nKHONG-TON-TAI,100\n";
		mockMvc.perform(admin(multipart("/admin/products/api/import")
				.file(new MockMultipartFile("file", "p.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))))
				.andExpect(jsonPath("$.responseCode").value(0)).andExpect(jsonPath("$.data.errors[0]").value(containsString("KHONG-TON-TAI")));
		em.flush();
		em.clear();
		assertEquals(0, productRepository.findById(p.getProductId()).orElseThrow().getPrice().compareTo(before));
	}

	@Test
	void bulkPricePercentAndStatus() throws Exception {
		ProductEntity p = anyProduct();
		BigDecimal before = p.getPrice();
		mockMvc.perform(admin(post("/admin/products/api/bulk-price").param("productIds", p.getProductId())
				.param("mode", "PERCENT").param("value", "10"))).andExpect(jsonPath("$.responseCode").value(1));
		mockMvc.perform(admin(post("/admin/products/api/bulk-status").param("productIds", p.getProductId())
				.param("status", "DRAFT"))).andExpect(jsonPath("$.responseCode").value(1));
		em.flush();
		em.clear();
		ProductEntity after = productRepository.findById(p.getProductId()).orElseThrow();
		assertEquals(0, after.getPrice().compareTo(before.multiply(new BigDecimal("1.10")).setScale(2, java.math.RoundingMode.HALF_UP)));
		assertEquals(ProductStatus.DRAFT, after.getStatus());
		// giá âm bị từ chối, không áp dụng
		mockMvc.perform(admin(post("/admin/products/api/bulk-price").param("productIds", p.getProductId())
				.param("mode", "AMOUNT").param("value", "-999999999"))).andExpect(jsonPath("$.responseCode").value(0));
	}
}
