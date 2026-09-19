package com.dungochung.shopdongho.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.dungochung.shopdongho.dto.CustomerSummaryDto;
import com.dungochung.shopdongho.enums.UserStatus;

public interface UserRepositoryCustom {
	/**
	 * Danh sách khách hàng (role = customer) kèm số đơn / tổng chi tiêu / đơn gần nhất.
	 * @param sort newest (mặc định) | spent | orders | recent (đơn gần nhất) | name
	 */
	Page<CustomerSummaryDto> searchCustomers(String keyword, UserStatus status, String sort, Pageable pageable);
}
