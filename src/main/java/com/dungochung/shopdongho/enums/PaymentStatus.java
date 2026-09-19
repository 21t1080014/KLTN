package com.dungochung.shopdongho.enums;

/** pending -> paid -> refund_pending -> refunded; failed = thanh toán thất bại/hủy trước khi trả tiền. */
public enum PaymentStatus {
	pending, paid, failed, refund_pending, refunded
}
