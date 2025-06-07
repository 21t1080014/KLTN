package com.dungochung.shopdongho.common;

import com.dungochung.shopdongho.dto.PaginationDto;

public class PaginationCommon {
	/**
	 * Calculates pagination information.
	 * 
	 * @param currentPage     the current page (starting from 0)
	 * @param totalPages      the total number of pages
	 * @param maxDisplayPages the maximum number of pages to display (e.g. 3)
	 * @return a PaginationInfo object containing startPage and endPage
	 */

	public static PaginationDto getPaginationInfo(int currentPage, int totalPages, int maxDisplayPages) {
		int startPage = Math.max(0, currentPage - maxDisplayPages / 2);
		int endPage = Math.min(totalPages - 1, startPage + maxDisplayPages - 1);
		if (endPage - startPage + 1 < maxDisplayPages) {
			startPage = Math.max(0, endPage - maxDisplayPages + 1);
		}
		return new PaginationDto(currentPage, totalPages, startPage, endPage);
	}
}
