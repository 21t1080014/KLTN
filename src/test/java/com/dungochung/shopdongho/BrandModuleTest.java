package com.dungochung.shopdongho;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Thương hiệu: thiếu logo (cột NOT NULL) phải báo lỗi rõ ràng chứ không nổ 500; file tĩnh không bị cache cứng. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BrandModuleTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void createWithoutLogoReturnsFriendlyError() throws Exception {
		mockMvc.perform(multipart("/admin/brands/api").param("brandName", "ZZ-NO-LOGO-" + System.nanoTime())
				.param("description", "x").with(AdminAuth.as("admin"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.responseCode").value(0))
				.andExpect(jsonPath("$.responseMsg").value("Vui lòng chọn ảnh logo cho thương hiệu"));
	}

	@Test
	void staticAssetsAreRevalidatedNotHardCached() throws Exception {
		mockMvc.perform(get("/css/output.css")).andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-cache")));
	}
}
