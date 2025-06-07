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

import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.InventoryDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.service.InventoryService;

@Service
public class InventoryServiceImpl implements InventoryService {
	@Autowired
	private InventoryRepository inventoryRepository;
	@Autowired
	private ProductRepository productRepository;

	@Override
	public ResponseDataDto getAllInventory(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "updatedAt");
		Page<InventoryEntity> inventory = inventoryRepository.findAll(pageable);
		List<InventoryDto> dtoList = inventory.stream().map(e -> {
			String productName = productRepository.findById(e.getProduct().getProductId()).map(p -> p.getName())
					.orElse(null);
			return new InventoryDto(e.getInventoryId(), e.getProduct().getProductId(), productName, e.getQuantity());
		}).toList();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("inventory", dtoList);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, inventory.getTotalPages(), 3));
		data.put("totalCount", inventory.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto searchInventory(String productName, Timestamp fromDate, Timestamp toDate, int page,
			int size) {
		Specification<InventoryEntity> specification = Specification.where(null);
		if (productName != null && !productName.isBlank()) {
			specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.join("product").get("name")),
					"%" + productName.toLowerCase() + "%"));
		}
		if (fromDate != null) {
			specification = specification
					.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("importedAt"), fromDate));
		}
		if (toDate != null) {
			specification = specification
					.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("importedAt"), toDate));
		}
		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "updatedAt");
		Page<InventoryEntity> inventory = inventoryRepository.findAll(specification, pageable);
		List<InventoryDto> dtoList = inventory.stream().map(e -> {
			var prod = e.getProduct();
			return new InventoryDto(e.getInventoryId(), prod != null ? prod.getProductId() : null,
					prod != null ? prod.getName() : "Không rõ", e.getQuantity());
		}).toList();
		Map<String, Object> data = new HashMap<>();
		data.put("inventory", dtoList);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, inventory.getTotalPages(), 3));
		data.put("totalCount", inventory.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Search results", data);
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

		var inventoryOpt = inventoryRepository.findByProduct_ProductId(productId);
		if (inventoryOpt.isEmpty()) {
			// Nếu chưa có tồn kho, tạo mới
			InventoryEntity newInventory = new InventoryEntity();
			newInventory.setProduct(productOpt.get());
			newInventory.setQuantity(quantity);
			newInventory.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
			inventoryRepository.save(newInventory);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Tạo mới tồn kho thành công",
					newInventory.getInventoryId());
		} else {
			// Nếu đã có, cập nhật
			var inventory = inventoryOpt.get();
			inventory.setQuantity(quantity);
			inventory.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
			inventoryRepository.save(inventory);
			return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Cập nhật tồn kho thành công",
					inventory.getInventoryId());
		}
	}

	@Override
	public ResponseDataDto getInventoryByProductId(String productId) {
		var inventoryOpt = inventoryRepository.findByProduct_ProductId(productId);
		if (inventoryOpt.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy tồn kho cho sản phẩm");
		}
		var e = inventoryOpt.get();
		var productName = productRepository.findById(productId).map(p -> p.getName()).orElse("Không rõ");
		var dto = new InventoryDto(e.getInventoryId(), e.getProduct().getProductId(), productName, e.getQuantity());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Thông tin tồn kho", dto);
	}

	@Override
	public ResponseDataDto getLowStockInventories(int threshold) {
		List<InventoryEntity> lowStockList = inventoryRepository.findByQuantityLessThan(threshold);
		List<InventoryDto> dtoList = lowStockList.stream().map(e -> {
			var productName = productRepository.findById(e.getProduct().getProductId()).map(p -> p.getName())
					.orElse("Không rõ");
			return new InventoryDto(e.getInventoryId(), e.getProduct().getProductId(), productName, e.getQuantity());
		}).toList();
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Danh sách sản phẩm tồn kho thấp", dtoList);
	}

	@Override
	public ResponseDataDto getInventoryChangeHistory(String productId, int page, int size) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseDataDto deleteInventory(String productId) {
		var inventoryOpt = inventoryRepository.findByProduct_ProductId(productId);
		if (inventoryOpt.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không tìm thấy tồn kho để xóa");
		}
		inventoryRepository.delete(inventoryOpt.get());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Xóa tồn kho thành công");
	}

}
