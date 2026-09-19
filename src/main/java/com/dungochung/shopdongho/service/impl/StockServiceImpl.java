package com.dungochung.shopdongho.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dungochung.shopdongho.common.CurrentActor;
import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.entity.InventoryMovementEntity;
import com.dungochung.shopdongho.entity.ProductVariantEntity;
import com.dungochung.shopdongho.enums.MovementType;
import com.dungochung.shopdongho.repository.InventoryMovementRepository;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.service.StockService;

@Service
@Transactional
public class StockServiceImpl implements StockService {

	@Autowired
	private InventoryRepository inventoryRepository;
	@Autowired
	private InventoryMovementRepository movementRepository;

	private void log(InventoryEntity inv, MovementType type, int change, int before, int after, String reference,
			String note) {
		InventoryMovementEntity m = new InventoryMovementEntity();
		m.setVariantId(inv.getVariant() != null ? inv.getVariant().getVariantId() : null);
		m.setProductId(inv.getProduct() != null ? inv.getProduct().getProductId() : null);
		m.setMovementType(type);
		m.setQuantityChange(change);
		m.setQuantityBefore(before);
		m.setQuantityAfter(after);
		m.setReference(reference);
		m.setNote(note);
		m.setCreatedBy(CurrentActor.username());
		movementRepository.save(m);
	}

	@Override
	public InventoryEntity ensureInventory(ProductVariantEntity variant) {
		return inventoryRepository.findByVariant_VariantId(variant.getVariantId()).orElseGet(() -> {
			InventoryEntity inv = new InventoryEntity();
			inv.setProduct(variant.getProduct());
			inv.setVariant(variant);
			inv.setQuantity(0);
			inv.setReservedQuantity(0);
			return inventoryRepository.save(inv);
		});
	}

	@Override
	public InventoryEntity adjust(InventoryEntity inv, int delta, MovementType type, String reference, String note) {
		int before = inv.getQuantity() == null ? 0 : inv.getQuantity();
		int after = before + delta;
		if (after < 0) {
			throw new IllegalArgumentException("Tồn kho không đủ: hiện có " + before + ", cần trừ " + (-delta));
		}
		if (after < inv.getReservedQuantity()) {
			throw new IllegalArgumentException(
					"Không thể giảm tồn kho xuống " + after + " vì đang giữ " + inv.getReservedQuantity() + " cho đơn hàng");
		}
		inv.setQuantity(after);
		inventoryRepository.save(inv);
		log(inv, type, delta, before, after, reference, note);
		return inv;
	}

	@Override
	public InventoryEntity setQuantity(InventoryEntity inv, int newQuantity, String note) {
		int before = inv.getQuantity() == null ? 0 : inv.getQuantity();
		return adjust(inv, newQuantity - before, MovementType.ADJUSTMENT, null, note);
	}

	@Override
	public InventoryEntity reserve(InventoryEntity inv, int quantity, String reference) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Số lượng giữ hàng phải > 0");
		}
		if (inv.getAvailableQuantity() < quantity) {
			throw new IllegalArgumentException("Không đủ hàng: còn " + inv.getAvailableQuantity() + ", cần " + quantity);
		}
		int before = inv.getReservedQuantity();
		inv.setReservedQuantity(before + quantity);
		inventoryRepository.save(inv);
		log(inv, MovementType.ORDER_RESERVE, quantity, before, before + quantity, reference, null);
		return inv;
	}

	@Override
	public InventoryEntity release(InventoryEntity inv, int quantity, String reference) {
		int before = inv.getReservedQuantity();
		int after = Math.max(0, before - quantity);
		inv.setReservedQuantity(after);
		inventoryRepository.save(inv);
		log(inv, MovementType.ORDER_RELEASE, after - before, before, after, reference, null);
		return inv;
	}

	@Override
	public InventoryEntity ship(InventoryEntity inv, int quantity, String reference) {
		int qBefore = inv.getQuantity();
		int rBefore = inv.getReservedQuantity();
		if (qBefore < quantity) {
			throw new IllegalArgumentException("Tồn kho không đủ để xuất: hiện có " + qBefore + ", cần " + quantity);
		}
		inv.setQuantity(qBefore - quantity);
		inv.setReservedQuantity(Math.max(0, rBefore - quantity));
		inventoryRepository.save(inv);
		log(inv, MovementType.ORDER_SHIPPED, -quantity, qBefore, qBefore - quantity, reference, null);
		return inv;
	}

	@Override
	public InventoryEntity restock(InventoryEntity inv, int quantity, String reference) {
		return adjust(inv, quantity, MovementType.ORDER_RETURN, reference, null);
	}
}
