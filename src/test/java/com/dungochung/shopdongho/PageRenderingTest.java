package com.dungochung.shopdongho;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Kiểm tra mọi trang Thymeleaf (storefront + admin) render được HTTP 200 sau khi redesign Tailwind,
 * tức là template không có lỗi cú pháp/fragment (th:replace, th:insert, th:fragment...).
 * Trang admin được test bằng session giả lập roleName=admin để vượt qua RoleInterceptor, không cần
 * tài khoản thật trong DB (các trang chỉ trả về view shell, không gọi service tầng DB).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PageRenderingTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void storefrontPagesRender() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk());
		mockMvc.perform(get("/collections")).andExpect(status().isOk());
		mockMvc.perform(get("/search-shop").param("keyword", "a")).andExpect(status().isOk());
		mockMvc.perform(get("/detail-shop/test-id")).andExpect(status().isOk());
		mockMvc.perform(get("/cart")).andExpect(status().isOk());
		mockMvc.perform(get("/auth")).andExpect(status().isOk());
	}

	@Test
	void adminLoginPageRenders() throws Exception {
		mockMvc.perform(get("/admin/login")).andExpect(status().isOk());
	}

	@Test
	void adminPagesRenderWithAdminSession() throws Exception {
		String[] adminPaths = { "/admin/dashboard", "/admin/products", "/admin/brands", "/admin/inventory",
				"/admin/promotions", "/admin/purchases", "/admin/orders", "/admin/customers", "/admin/audit", "/admin/reports", "/admin/voucher", "/admin/users",
				"/admin/categories/case-materials", "/admin/categories/strap-materials",
				"/admin/categories/glass-materials", "/admin/categories/watch-types" };

		for (String path : adminPaths) {
			mockMvc.perform(get(path).with(AdminAuth.as("admin")).sessionAttr("username", "TestAdmin"))
					.andExpect(status().isOk());
		}
	}

	/** Regression: mục "Sản phẩm"/"Thuộc tính" là nút collapse phải có màu chữ sáng (trước đây thừa kế màu link tối => chìm vào nền sidebar). */
	@Test
	void adminSidebarShowsProductAndAttributeMenusForAdminWithVisibleColor() throws Exception {
		String html = mockMvc.perform(get("/admin/dashboard").with(AdminAuth.as("admin"))).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		for (String target : new String[] { "#productMenu", "#categoryMenu" }) {
			int i = html.indexOf("href=\"" + target + "\"");
			org.junit.jupiter.api.Assertions.assertTrue(i > 0, target + " phải có trong menu của admin");
			String tag = html.substring(html.lastIndexOf("<a ", i), i);
			org.junit.jupiter.api.Assertions.assertTrue(tag.contains("text-white"), target + " thiếu lớp màu chữ sáng: " + tag);
		}
		org.junit.jupiter.api.Assertions.assertTrue(html.contains("/admin/products") && html.contains("/admin/brands")
				&& html.contains("/admin/categories/watch-types"));
	}

	/** Regression: utility `.collapse` của Tailwind (visibility:collapse) trùng tên với Bootstrap collapse; phải có luật ép hiện khi .show. */
	@Test
	void compiledCssRestoresVisibilityForOpenCollapse() throws Exception {
		String css = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/static/css/output.css"));
		org.junit.jupiter.api.Assertions.assertTrue(css.contains(".collapse.show,.collapsing{visibility:visible}"),
				"output.css thiếu luật hiển thị .collapse.show - chạy npm run css:build");
	}
}
