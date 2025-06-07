/*
 * package com.dungochung.shopdongho.config;
 * 
 * import org.springframework.context.annotation.Bean; import
 * org.springframework.context.annotation.Configuration; import
 * org.springframework.security.config.annotation.method.configuration.
 * EnableMethodSecurity; import
 * org.springframework.security.config.annotation.web.builders.HttpSecurity;
 * import org.springframework.security.config.annotation.web.configuration.
 * EnableWebSecurity; import
 * org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import
 * org.springframework.security.crypto.password.PasswordEncoder; import
 * org.springframework.security.web.SecurityFilterChain;
 * 
 * @Configuration
 * 
 * @EnableWebSecurity
 * 
 * @EnableMethodSecurity(prePostEnabled = true) public class SecurityConfig {
 * 
 * @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws
 * Exception { http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(authz ->
 * authz // Public endpoints .requestMatchers("/auth/register",
 * "/auth/login","/shop/**").permitAll()
 * 
 * // Admin pages - require ADMIN role
 * .requestMatchers("/admin/**").hasRole("ADMIN")
 * 
 * // Staff pages .requestMatchers( "/admin/products/**",
 * "/admin/categories/**", "/admin/brands/**" ).hasAnyRole("PRODUCT_STAFF",
 * "SUPPORT_STAFF")
 * 
 * // Warehouse staff .requestMatchers("/admin/**").hasRole("WAREHOUSE_STAFF")
 * 
 * // Customer pages .requestMatchers("/auth/**").hasRole("CUSTOMER")
 * 
 * // All other requests need to be authenticated
 * .anyRequest().authenticated()).httpBasic(httpBasic -> httpBasic.disable());
 * return http.build(); }
 * 
 * @Bean public PasswordEncoder passwordEncoder() { return new
 * BCryptPasswordEncoder(); } }
 */
