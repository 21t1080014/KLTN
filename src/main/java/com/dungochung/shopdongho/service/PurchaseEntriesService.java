package com.dungochung.shopdongho.service;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.PurchaseEntriesEntity;

public interface PurchaseEntriesService {
	ResponseDataDto getAllEntries(int page, int size);

	ResponseDataDto getEntriesId(Long id);

	ResponseDataDto deleteEntry(Long id);

	ResponseDataDto createEntry(PurchaseEntriesEntity entry);

	ResponseDataDto updateEntry(PurchaseEntriesEntity entry, Long id);

	ResponseDataDto deleteOldEntry(Long id);

	ResponseDataDto searchEntries(String productName, Timestamp fromDate, Timestamp toDate, BigDecimal minPrice,
			BigDecimal maxPrice, int page, int size);
}
