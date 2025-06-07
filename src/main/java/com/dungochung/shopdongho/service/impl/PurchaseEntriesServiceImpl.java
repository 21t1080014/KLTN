package com.dungochung.shopdongho.service.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.PurchaseEntryDto;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.PurchaseEntriesEntity;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.repository.PurchaseEntriesRepository;
import com.dungochung.shopdongho.service.PurchaseEntriesService;

@Service
public class PurchaseEntriesServiceImpl implements PurchaseEntriesService {
	@Autowired
	private PurchaseEntriesRepository entriesRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private InventoryRepository inventoryRepository;

	@Override
	public ResponseDataDto getAllEntries(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "importedAt");
		Page<PurchaseEntriesEntity> entry = entriesRepository.findAll(pageable);
		List<PurchaseEntryDto> dtoList = entry.stream().map(e -> {
			String productName = productRepository.findById(e.getProduct().getProductId()).map(p -> p.getName())
					.orElse(null);

			return new PurchaseEntryDto(e.getEntryId(), e.getProduct().getProductId(), productName, e.getQuantity(),
					e.getImportPrice(), e.getImportedAt(), e.getNote());
		}).toList();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("entry", dtoList);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, entry.getTotalPages(), 3));
		data.put("totalCount", entry.getTotalElements());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Success", data);
	}

	@Override
	public ResponseDataDto deleteEntry(Long id) {
		Optional<PurchaseEntriesEntity> optional = entriesRepository.findById(id);
		if (optional.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Purchase Entry not found", 404);
		}
		PurchaseEntriesEntity entry = optional.get();

		// Only allow deletion within same day
		Duration duration = Duration.between(entry.getImportedAt().toInstant(), Instant.now());
		if (duration.toDays() >= 1) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Cannot delete entries older than 1 day");
		}

		InventoryEntity inventory = inventoryRepository.findByProduct(entry.getProduct())
				.orElseThrow(() -> new RuntimeException("Inventory record not found"));
		if (inventory.getQuantity() < entry.getQuantity()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Cannot delete: current stock less than entry quantity");
		}
		inventory.setQuantity(inventory.getQuantity() - entry.getQuantity());
		inventoryRepository.save(inventory);

		entriesRepository.delete(entry);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Purchase Entry deleted successfully");
	}

	@Override
	public ResponseDataDto createEntry(PurchaseEntriesEntity entry) {
		// Validate product
		if (entry.getProduct() == null || entry.getProduct().getProductId() == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Product is required");
		}
		Optional<ProductEntity> productOpt = productRepository.findById(entry.getProduct().getProductId());
		if (productOpt.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Product not found");
		}
		ProductEntity product = productOpt.get();
		entry.setProduct(product);

		// Save entry
		PurchaseEntriesEntity savedEntry = entriesRepository.save(entry);

		// Update inventory
		InventoryEntity inventory = inventoryRepository.findByProduct(product).orElseGet(() -> {
			InventoryEntity inv = new InventoryEntity();
			inv.setProduct(product);
			inv.setQuantity(0);
			return inv;
		});
		inventory.setQuantity(inventory.getQuantity() + savedEntry.getQuantity());
		inventoryRepository.save(inventory);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Purchase Entry created successfully",
				savedEntry.getEntryId());
	}

	@Override
	public ResponseDataDto updateEntry(PurchaseEntriesEntity entry, Long id) {
		Optional<PurchaseEntriesEntity> optional = entriesRepository.findById(id);
		if (optional.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Purchase Entry not found", 404);
		}

		PurchaseEntriesEntity existing = optional.get();

		// Check if within allowable modification window (24h)
		Duration duration = Duration.between(existing.getImportedAt().toInstant(), Instant.now());
		if (duration.toHours() > 24) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Cannot update entries older than 24 hours");
		}

		ProductEntity oldProduct = existing.getProduct();
		int oldQty = existing.getQuantity();
		int newQty = entry.getQuantity();

		// Nếu người dùng chọn sản phẩm mới (khác với cũ)
		if (entry.getProduct() != null && entry.getProduct().getProductId() != null) {
			String newProductId = entry.getProduct().getProductId();
			if (!oldProduct.getProductId().equals(newProductId)) {
				Optional<ProductEntity> productOpt = productRepository.findById(newProductId);
				if (productOpt.isEmpty()) {
					return new ResponseDataDto(Constant.RESULT_CD_FAIL, "New product does not exist", 400);
				}
				ProductEntity newProduct = productOpt.get();

				// Giảm tồn kho sản phẩm cũ
				InventoryEntity oldInventory = inventoryRepository.findByProduct(oldProduct).orElse(null);
				if (oldInventory != null) {
					oldInventory.setQuantity(oldInventory.getQuantity() - oldQty);
					inventoryRepository.save(oldInventory);
				}

				// Tăng tồn kho sản phẩm mới
				InventoryEntity newInventory = inventoryRepository.findByProduct(newProduct).orElseGet(() -> {
					InventoryEntity newInv = new InventoryEntity();
					newInv.setProduct(newProduct);
					newInv.setQuantity(0);
					return inventoryRepository.save(newInv);
				});

				newInventory.setQuantity(newInventory.getQuantity() + newQty);
				inventoryRepository.save(newInventory);

				// Cập nhật product
				existing.setProduct(newProduct);
			} else {
				// Cùng sản phẩm, chỉ cập nhật số lượng tồn kho chênh lệch
				InventoryEntity inventory = inventoryRepository.findByProduct(oldProduct).orElse(null);
				if (inventory != null) {
					int diff = newQty - oldQty;
					inventory.setQuantity(inventory.getQuantity() + diff);
					inventoryRepository.save(inventory);
				}
			}
		}

		// Cập nhật các trường còn lại
		existing.setQuantity(newQty);
		existing.setImportPrice(entry.getImportPrice());
		existing.setImportedAt(entry.getImportedAt());
		existing.setNote(entry.getNote());

		entriesRepository.save(existing);

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Purchase Entry updated successfully", id);
	}

	@Override
	public ResponseDataDto getEntriesId(Long id) {
		Optional<PurchaseEntriesEntity> optional = entriesRepository.findById(id);
		if (optional.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Purchase Entry not found", 404);
		}
		PurchaseEntriesEntity entry = optional.get();
		ProductEntity product = entry.getProduct();
		PurchaseEntryDto dto = new PurchaseEntryDto(entry.getEntryId(), product != null ? product.getProductId() : null,
				product != null ? product.getName() : "Không rõ tên sản phẩm", entry.getQuantity(),
				entry.getImportPrice(), entry.getImportedAt(), entry.getNote());
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Purchase Entry retrieved", dto);
	}

	@Override
	public ResponseDataDto deleteOldEntry(Long id) {
		Optional<PurchaseEntriesEntity> optional = entriesRepository.findById(id);
		if (optional.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Purchase Entry not found", 404);
		}
		PurchaseEntriesEntity entry = optional.get();

		// Tính khoảng cách thời gian
		Duration duration = Duration.between(entry.getImportedAt().toInstant(), Instant.now());
		if (duration.toDays() < 365) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Chỉ có thể xóa phiếu nhập đã hơn 1 năm", 400);
		}

		// Xóa thẳng, không cập nhật tồn kho
		entriesRepository.delete(entry);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Đã xóa phiếu nhập cũ (hơn 1 năm) thành công");
	}

	@Override
	public ResponseDataDto searchEntries(String productName, Timestamp fromDate, Timestamp toDate, BigDecimal minPrice,
			BigDecimal maxPrice, int page, int size) {
		// 1. Build Specification động
		Specification<PurchaseEntriesEntity> spec = Specification.where(null);

		if (productName != null && !productName.isBlank()) {
			spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.join("product").get("name")),
					"%" + productName.toLowerCase() + "%"));
		}
		if (fromDate != null) {
			spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("importedAt"), fromDate));
		}
		if (toDate != null) {
			spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("importedAt"), toDate));
		}
		if (minPrice != null) {
			spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("importPrice"), minPrice));
		}
		if (maxPrice != null) {
			spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("importPrice"), maxPrice));
		}

		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "importedAt");
		Page<PurchaseEntriesEntity> pageResult = entriesRepository.findAll(spec, pageable);

		List<PurchaseEntryDto> dtoList = pageResult.stream().map(e -> {
			var prod = e.getProduct();
			return new PurchaseEntryDto(e.getEntryId(), prod != null ? prod.getProductId() : null,
					prod != null ? prod.getName() : "Không rõ", e.getQuantity(), e.getImportPrice(), e.getImportedAt(),
					e.getNote());
		}).collect(Collectors.toList());

		// 4. Build response
		Map<String, Object> data = new HashMap<>();
		data.put("entry", dtoList);
		data.put("pagination", PaginationCommon.getPaginationInfo(page, pageResult.getTotalPages(), 3));
		data.put("totalCount", pageResult.getTotalElements());

		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Search results", data);
	}

}
