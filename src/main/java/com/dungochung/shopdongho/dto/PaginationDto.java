package com.dungochung.shopdongho.dto;

public class PaginationDto {
	private int currentPage;
	private int totalPages;
	private int startPage;
	private int endPage;
	public PaginationDto() {
		super();
		// TODO Auto-generated constructor stub
	}
	public PaginationDto(int currentPage, int totalPages, int startPage, int endPage) {
		super();
		this.currentPage = currentPage;
		this.totalPages = totalPages;
		this.startPage = startPage;
		this.endPage = endPage;
	}
	public int getCurrentPage() {
		return currentPage;
	}
	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}
	public int getTotalPages() {
		return totalPages;
	}
	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}
	public int getStartPage() {
		return startPage;
	}
	public void setStartPage(int startPage) {
		this.startPage = startPage;
	}
	public int getEndPage() {
		return endPage;
	}
	public void setEndPage(int endPage) {
		this.endPage = endPage;
	}
	
}
