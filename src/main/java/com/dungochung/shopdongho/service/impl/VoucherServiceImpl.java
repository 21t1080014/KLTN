package com.dungochung.shopdongho.service.impl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.dto.VoucherSimpleDto;
import com.dungochung.shopdongho.entity.UserVoucherEntity;
import com.dungochung.shopdongho.entity.VoucherEntity;
import com.dungochung.shopdongho.repository.UserVoucherRepository;
import com.dungochung.shopdongho.repository.VoucherRepository;
import com.dungochung.shopdongho.service.VoucherService;

@Service
public class VoucherServiceImpl implements VoucherService {
	@Autowired
	private UserVoucherRepository userVoucherRepository;
	@Autowired
	private VoucherRepository voucherRepository;

	@Override
	public ResponseDataDto getAvailableVouchersForUser(String userId) {
		LocalDate today = LocalDate.now();

		List<UserVoucherEntity> userVouchers = userVoucherRepository.findByUser_UserIdAndUsedAtIsNull(userId);

		List<VoucherSimpleDto> availableVouchers = userVouchers.stream().map(UserVoucherEntity::getVoucher)
				.filter(v -> (v.getStartAt() == null || !today.isBefore(v.getStartAt()))
						&& (v.getEndAt() == null || !today.isAfter(v.getEndAt())))
				.map(v -> new VoucherSimpleDto(v.getVoucherCode(), v.getDescription(), v.getDiscountType().toString(),
						v.getDiscountValue(), v.getMinOrderAmount(), v.getStartAt(), v.getEndAt()))
				.collect(Collectors.toList());

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", availableVouchers);
	}

	@Override
	public ResponseDataDto getAllVoucher(int page, int size, String keyword) {
		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
		Page<VoucherEntity> vouchers;

		if (keyword != null && !keyword.trim().isEmpty()) {
			vouchers = voucherRepository.findByVoucherCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
					keyword.trim(), keyword.trim(), pageable);
		} else {
			vouchers = voucherRepository.findAll(pageable);
		}
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("vouchers", vouchers.getContent());
		data.put("pagination", PaginationCommon.getPaginationInfo(page, vouchers.getTotalPages(), 3));
		data.put("totalCount", vouchers.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto creatVoucher(VoucherEntity voucherEntity) {
		try {
			if (voucherRepository.existsById(voucherEntity.getVoucherCode())) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Mã voucher đã tồn tại");
			}
			VoucherEntity saved = voucherRepository.save(voucherEntity);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Tạo voucher thành công", saved);
		} catch (Exception e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Lỗi hệ thống: " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto updateVoucher(String voucherCode, VoucherEntity voucher) {
		try {
			Optional<VoucherEntity> existingOpt = voucherRepository.findById(voucherCode);
			if (existingOpt.isEmpty()) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy voucher");
			}
			VoucherEntity existing = existingOpt.get();

			// Update fields
			existing.setDescription(voucher.getDescription());
			existing.setDiscountType(voucher.getDiscountType());
			existing.setDiscountValue(voucher.getDiscountValue());
			existing.setMinOrderAmount(voucher.getMinOrderAmount());
			existing.setStartAt(voucher.getStartAt());
			existing.setEndAt(voucher.getEndAt());
			existing.setUsageLimit(voucher.getUsageLimit());

			VoucherEntity updated = voucherRepository.save(existing);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Cập nhật thành công", updated);
		} catch (Exception e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Lỗi hệ thống: " + e.getMessage());
		}
	}

	@Override
	public ResponseDataDto deleteVoucher(String voucherCode) {
		try {
			if (!voucherRepository.existsById(voucherCode)) {
				return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Voucher không tồn tại");
			}
			voucherRepository.deleteById(voucherCode);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Xoá thành công");
		} catch (Exception e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Lỗi hệ thống: " + e.getMessage());
		}
	}

}
