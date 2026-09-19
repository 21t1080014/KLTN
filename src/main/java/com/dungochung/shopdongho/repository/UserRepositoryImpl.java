package com.dungochung.shopdongho.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.dungochung.shopdongho.common.CustomerTier;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.CustomerSummaryDto;
import com.dungochung.shopdongho.enums.UserStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

public class UserRepositoryImpl implements UserRepositoryCustom {
	// Sắp xếp theo whitelist (không ghép chuỗi từ input) để tránh JPQL injection
	private static final Map<String, String> ORDER_BY = Map.of(
			"newest", "u.createdAt DESC",
			"spent", "SUM(CASE WHEN o.orderStatus IN :spendStatuses AND o.paymentStatus = :spendPayment THEN o.totalPrice ELSE 0 END) DESC, u.createdAt DESC",
			"orders", "COUNT(o.orderId) DESC, u.createdAt DESC",
			"recent", "MAX(o.createdAt) DESC NULLS LAST, u.createdAt DESC",
			"name", "u.fullName ASC, u.username ASC");

	@PersistenceContext
	private EntityManager em;

	@Override
	public Page<CustomerSummaryDto> searchCustomers(String keyword, UserStatus status, String sort,
			Pageable pageable) {
		String kw = keyword == null ? "" : keyword.trim().toLowerCase();
		boolean hasKw = !kw.isEmpty();
		String where = " WHERE u.role.roleName = :customerRole"
				+ (status != null ? " AND u.status = :status" : "")
				+ (hasKw ? " AND (LOWER(u.username) LIKE :kw OR LOWER(u.email) LIKE :kw OR LOWER(u.fullName) LIKE :kw OR u.phone LIKE :kw)" : "");

		String jpql = "SELECT new com.dungochung.shopdongho.dto.CustomerSummaryDto(u.userId, u.username, u.fullName, u.email, u.phone, u.status, u.createdAt, "
				+ "COUNT(o.orderId), "
				+ "SUM(CASE WHEN o.orderStatus IN :spendStatuses AND o.paymentStatus = :spendPayment THEN o.totalPrice ELSE 0 END), "
				+ "MAX(o.createdAt)) "
				+ "FROM UserEntity u LEFT JOIN OrderEntity o ON o.userId = u.userId" + where
				+ " GROUP BY u.userId, u.username, u.fullName, u.email, u.phone, u.status, u.createdAt ORDER BY "
				+ ORDER_BY.getOrDefault(sort == null ? "newest" : sort, ORDER_BY.get("newest"));
		TypedQuery<CustomerSummaryDto> q = em.createQuery(jpql, CustomerSummaryDto.class);
		q.setParameter("spendStatuses", CustomerTier.SPEND_STATUSES);
		q.setParameter("spendPayment", CustomerTier.SPEND_PAYMENT);
		bind(q, status, hasKw, kw);
		q.setFirstResult((int) pageable.getOffset());
		q.setMaxResults(pageable.getPageSize());
		List<CustomerSummaryDto> rows = q.getResultList();

		TypedQuery<Long> count = em.createQuery("SELECT COUNT(u) FROM UserEntity u" + where, Long.class);
		bind(count, status, hasKw, kw);
		return new PageImpl<>(rows, pageable, count.getSingleResult());
	}

	private void bind(TypedQuery<?> q, UserStatus status, boolean hasKw, String kw) {
		q.setParameter("customerRole", Constant.ROLE_CUSTOMER);
		if (status != null) {
			q.setParameter("status", status);
		}
		if (hasKw) {
			q.setParameter("kw", "%" + kw.replace("%", "\\%").replace("_", "\\_") + "%");
		}
	}
}
