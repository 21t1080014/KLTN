package com.dungochung.shopdongho.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
	@Value("${upload.dir}")
	private String uploadDir;

	@Autowired
	private RoleInterceptor roleInterceptor;

	/*
	 * @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
	 * registry.addResourceHandler("/api/brands/image/**").addResourceLocations(
	 * "file:" + uploadDir + "/brands/");
	 * registry.addResourceHandler("/api/products/image/**").addResourceLocations(
	 * "file:" + uploadDir + "/products/"); }
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploadImgshop/**").addResourceLocations("file:" + uploadDir + "/");
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**").allowedOrigins("http://localhost:8080")
				.allowedMethods("GET", "POST", "PUT", "DELETE") // Allow these HTTP methods
				.allowedHeaders("*"); // Allow all headers
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(roleInterceptor).addPathPatterns("/admin/**") // áp dụng tất cả đường dẫn /admin/*
				.excludePathPatterns("/admin/login", "/admin/login/**"); // không chặn trang đăng nhập
	}

}
