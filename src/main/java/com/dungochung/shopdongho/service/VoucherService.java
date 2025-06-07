package com.dungochung.shopdongho.service;

import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.VoucherEntity;

public interface VoucherService {
	ResponseDataDto getAvailableVouchersForUser(String userId);
	ResponseDataDto getAllVoucher(int page,int size,String keyword);
	ResponseDataDto creatVoucher(VoucherEntity voucherEntity);
	ResponseDataDto updateVoucher(String voucherCode, VoucherEntity voucher);
	ResponseDataDto deleteVoucher(String voucherCode);
}
