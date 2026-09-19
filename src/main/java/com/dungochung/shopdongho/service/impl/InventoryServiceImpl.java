package com.dungochung.shopdongho.service.impl;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.InventoryDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.entity.InventoryMovementEntity;
import com.dungochung.shopdongho.entity.ProductVariantEntity;
import com.dungochung.shopdongho.repository.InventoryMovementRepository;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.service.InventoryService;
import com.dungochung.shopdongho.service.ProductVariantService;
import com.dungochung.shopdongho.service.StockService;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {
	@Autowired
	private InventoryRepository inventoryRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private InventoryMovementRepository movementRepository;
	@Autowired
	private StockService stockService;
	@Autowired
	private ProductVariantService variantService;

	private InventoryDto toDto(InventoryEntity e) {
		InventoryDto dto = new InventoryDto(e.getInventoryId(), e.getProduct() != null ? e.getProduct().getProductId() : null,
				e.getProduct() != null ? e.getProduct().getName() : "Không rõ", e.getQuantity());
		ProductVariantEntity v = e.getVariant();
		if (v != null) {
			dto.setVariantId(v.getVariantId());
			dto.setSku(v.getSku());
			dto.setVariantName(v.getVariantName());
		}
		dto.setReservedQuantity(e.getReservedQuantity());
		dto.setAvailableQuantity(e.getAvailableQuantity());
		dto.setLowStockThreshold(e.getLowStockThreshold());
		dto.setLowStock(e.getAvailableQuantity() <= e.getLowStockThreshold());
		return dto;
	}

	private Map<String, Object> pageData(Page<InventoryEntity> page, int pageNo) {
		List<InventoryDto> dtoList = page.stream().map(this::toDto).toList();
		Map<String, Object> data = new HashMap<>();
		data.put("inventory", dtoList);
		data.put("pagination", PaginationCommon.getPaginationInfo(pageNo, page.getTotalPages(), 3));
		data.put("totalCount", page.getTotalElements());
		return data;
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto getAllInventory(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "updatedAt");
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", pageData(inventoryRepository.findAll(pageable), page));
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto searchInventory(String productName, Timestamp fromDate, Timestamp toDate, int page,
			int size) {
		Specification<InventoryEntity> specification = Specification.where(null);
		if (productName != null && !productName.isBlank()) {
			String like = "%" + productName.toLowerCase() + "%";
			specification = specification.and((root, query, cb) -> cb.or(
					cb.like(cb.lower(root.join("product").get("name")), like),
					cb.like(cb.lower(root.join("variant", jakarta.persistence.criteria.JoinType.LEFT).get("sku")), like)));
		}
		// lọc theo ngày cập nhật kho (trước đây lọc theo cột importedAt không tồn tại trên bảng inventories)
		if (fromDate != null) {
			specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("updatedAt"), fromDate));
		}
		if (toDate != null) {
			specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("updatedAt"), toDate));
		}
		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "updatedAt");
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Search results",
				pageData(inventoryRepository.findAll(specification, pageable), page));
	}

	@Override
	public ResponseDataDto updateInventoryQuantity(String productId, int quantity) {
		if (productId == null || productId.isBlank()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Mã sản phẩm không được để trống");
		}
		var productOpt = productRepository.findById(productId);
		if (productOpt.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Sản phẩm không tồn tại");
		}
		if (quantity < 0) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Số lượng không được âm");
		}
		InventoryEntity inv = inventoryRepository.findByProduct_ProductId(productId)
				.orElseGet(() -> stockService.ensureInventory(variantService.ensureDefaultVariant(productOpt.get())));
		try {
			stockService.setQuantity(inv, quantity, "Cập nhật tồn kho");
		} catch (IllegalArgumentException e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, e.getMessage());
		}
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Cập nhật tồn kho thành công", inv.getInventoryId());
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto getInventoryByProductId(String productId) {
		var inventoryOpt = inventoryRepository.findByProduct_ProductId(productId);
		if (inventoryOpt.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy tồn kho cho sản phẩm");
		}
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Thông tin tồn kho", toDto(inventoryOpt.get()));
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto getLowStockInventories(Integer threshold) {
		// không truyền ngưỡng => dùng ngưỡng riêng của từng biến thể
		List<InventoryEntity> lowStockList = threshold == null ? inventoryRepository.findLowStock()
				: inventoryRepository.findByQuantityLessThan(threshold);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Danh sách sản phẩm tồn kho thấp",
				lowStockList.stream().map(this::toDto).toList());
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto getInventoryChangeHistory(String productId, int page, int size) {
		return getMovements(null, productId, page, size);
	}

	@Override
	public ResponseDataDto deleteInventory(String productId) {
		var inventoryOpt = inventoryRepository.findByProduct_ProductId(productId);
		if (inventoryOpt.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy tồn kho để xóa");
		}
		return deleteRow(inventoryOpt.get());
	}

	private ResponseDataDto deleteRow(InventoryEntity inv) {
		if (inv.getQuantity() > 0 || inv.getReservedQuantity() > 0) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Chỉ xóa được dòng tồn kho khi số lượng = 0 và không giữ hàng cho đơn nào");
		}
		inventoryRepository.delete(inv);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Xóa tồn kho thành công");
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto getVariantInventory(Long variantId) {
		return inventoryRepository.findByVariant_VariantId(variantId)
				.map(i -> new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Thông tin tồn kho", toDto(i)))
				.orElseGet(() -> new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy tồn kho của biến thể"));
	}

	@Override
	public ResponseDataDto updateVariantStock(Long variantId, int quantity, String note, Integer lowStockThreshold) {
		if (quantity < 0) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Số lượng không được âm");
		}
		var invOpt = inventoryRepository.findByVariant_VariantId(variantId);
		if (invOpt.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy tồn kho của biến thể");
		}
		InventoryEntity inv = invOpt.get();
		try {
			if (quantity != inv.getQuantity()) {
				stockService.setQuantity(inv, quantity, note == null || note.isBlank() ? "Kiểm kê" : note);
			}
		} catch (IllegalArgumentException e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, e.getMessage());
		}
		if (lowStockThreshold != null && lowStockThreshold >= 0) {
			inv.setLowStockThreshold(lowStockThreshold);
			inventoryRepository.save(inv);
		}
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Cập nhật tồn kho thành công", toDto(inv));
	}

	@Override
	public ResponseDataDto deleteVariantInventory(Long variantId) {
		var invOpt = inventoryRepository.findByVariant_VariantId(variantId);
		if (invOpt.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy tồn kho để xóa");
		}
		return deleteRow(invOpt.get());
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseDataDto getMovements(Long variantId, String productId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "movementId"));
		Page<InventoryMovementEntity> result;
		if (variantId != null) {
			result = movementRepository.findByVariantId(variantId, pageable);
		} else if (productId != null && !productId.isBlank()) {
			result = movementRepository.findByProductId(productId, pageable);
		} else {
			result = movementRepository.findAll(pageable);
		}
		List<Map<String, Object>> rows = result.stream().map(m -> {
			Map<String, Object> row = new HashMap<>();
			row.put("movementId", m.getMovementId());
			row.put("variantId", m.getVariantId());
			row.put("productId", m.getProductId());
			row.put("type", m.getMovementType().name());
			row.put("change", m.getQuantityChange());
			row.put("before", m.getQuantityBefore());
			row.put("after", m.getQuantityAfter());
			row.put("reference", m.getReference());
			row.put("note", m.getNote());
			row.put("createdBy", m.getCreatedBy());
			row.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString().replace('T', ' ') : null);
			return row;
		}).toList();
		Map<String, Object> data = new HashMap<>();
		data.put("movements", rows);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, result.getTotalPages(), 3));
		data.put("totalCount", result.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Lịch sử tồn kho", data);
	}
}
