package com.dungochung.shopdongho;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.dungochung.shopdongho.repository.CategoryRepository;

/** Module 1 - danh mục đa cấp. Chạy trong transaction và rollback nên không làm bẩn DB dev. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryModuleTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private CategoryRepository categoryRepository;

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder b) {
		return b.with(AdminAuth.as("admin"));
	}

	private int createCategory(String name, Integer parentId) throws Exception {
		MockHttpServletRequestBuilder req = post("/admin/categories/api").param("name", name);
		if (parentId != null) {
			req.param("parentId", String.valueOf(parentId));
		}
		String body = mockMvc.perform(asAdmin(req)).andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value(1))
				.andReturn().getResponse().getContentAsString();
		return categoryRepository.findByNameIgnoreCase(name).orElseThrow().getCategoryId();
	}

	@Test
	void pageRendersForAdmin() throws Exception {
		mockMvc.perform(asAdmin(get("/admin/categories"))).andExpect(status().isOk())
				.andExpect(content().string(containsString("Danh mục sản phẩm")));
	}

	@Test
	void nonAdminCannotAccess() throws Exception {
		mockMvc.perform(get("/admin/categories").with(AdminAuth.as("product_staff"))).andExpect(status().is3xxRedirection());
	}

	@Test
	void createTreeUpdateAndDeleteRules() throws Exception {
		int root = createCategory("TEST-Nam", null);
		int child = createCategory("TEST-Thể thao", root);

		// tên trùng trong cùng cấp cha bị từ chối
		mockMvc.perform(asAdmin(post("/admin/categories/api").param("name", "TEST-Thể thao").param("parentId", String.valueOf(root))))
				.andExpect(jsonPath("$.responseCode").value(0));

		// cây trả về đúng quan hệ cha-con
		mockMvc.perform(asAdmin(get("/admin/categories/api/tree"))).andExpect(status().isOk())
				.andExpect(content().string(containsString("TEST-Thể thao")));

		// không được đặt cha là chính nó
		mockMvc.perform(asAdmin(put("/admin/categories/api/" + root).param("name", "TEST-Nam").param("parentId", String.valueOf(root))))
				.andExpect(jsonPath("$.responseCode").value(0));

		// không được đặt cha là con/cháu của mình (vòng lặp)
		mockMvc.perform(asAdmin(put("/admin/categories/api/" + root).param("name", "TEST-Nam").param("parentId", String.valueOf(child))))
				.andExpect(jsonPath("$.responseCode").value(0)).andExpect(jsonPath("$.responseMsg", containsString("con của chính nó")));

		// không xóa được danh mục còn con
		mockMvc.perform(asAdmin(delete("/admin/categories/api/" + root))).andExpect(jsonPath("$.responseCode").value(0));

		// xóa con rồi xóa cha thì được
		mockMvc.perform(asAdmin(delete("/admin/categories/api/" + child))).andExpect(jsonPath("$.responseCode").value(1));
		mockMvc.perform(asAdmin(delete("/admin/categories/api/" + root))).andExpect(jsonPath("$.responseCode").value(1));
	}

	@Test
	void productFormOptionsIncludeCategories() throws Exception {
		createCategory("TEST-Option", null);
		mockMvc.perform(get("/admin/products/api/form-options").with(AdminAuth.as("admin"))).andExpect(status().isOk())
				.andExpect(content().string(containsString("TEST-Option")));
	}
}
