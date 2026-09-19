package com.dungochung.shopdongho.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.InventoryEntity;
import com.dungochung.shopdongho.entity.ProductEntity;
import com.dungochung.shopdongho.entity.ProductVariantEntity;
import com.dungochung.shopdongho.enums.ProductStatus;
import com.dungochung.shopdongho.repository.InventoryRepository;
import com.dungochung.shopdongho.repository.ProductRepository;
import com.dungochung.shopdongho.repository.ProductVariantRepository;
import com.dungochung.shopdongho.service.ProductBulkService;
import com.dungochung.shopdongho.service.StockService;

@Service
@Transactional
public class ProductBulkServiceImpl implements ProductBulkService {

	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private ProductVariantRepository variantRepository;
	@Autowired
	private InventoryRepository inventoryRepository;
	@Autowired
	private StockService stockService;

	private static String csv(Object v) {
		String s = v == null ? "" : v.toString();
		if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
			return "\"" + s.replace("\"", "\"\"") + "\"";
		}
		return s;
	}

	@Override
	@Transactional(readOnly = true)
	public String exportCsv() {
		StringBuilder sb = new StringBuilder("﻿");
		sb.append("sku,product_name,variant_name,price,product_status,variant_status,quantity,reserved,low_stock_threshold\n");
		for (ProductVariantEntity v : variantRepository.findAll()) {
			InventoryEntity inv = inventoryRepository.findByVariant_VariantId(v.getVariantId()).orElse(null);
			sb.append(csv(v.getSku())).append(',').append(csv(v.getProduct().getName())).append(',')
					.append(csv(v.getVariantName())).append(',').append(v.getPrice().toPlainString()).append(',')
					.append(v.getProduct().getStatus()).append(',').append(v.getStatus()).append(',')
					.append(inv != null ? inv.getQuantity() : 0).append(',').append(inv != null ? inv.getReservedQuantity() : 0)
					.append(',').append(inv != null ? inv.getLowStockThreshold() : 5).append('\n');
		}
		return sb.toString();
	}

	/** Parser CSV RFC4180 tối giản: hỗ trợ dấu ngoặc kép, dấu phẩy và xuống dòng trong ô. */
	static List<List<String>> parseCsv(String text) {
		List<List<String>> rows = new ArrayList<>();
		List<String> row = new ArrayList<>();
		StringBuilder cell = new StringBuilder();
		boolean inQuotes = false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (inQuotes) {
				if (c == '"') {
					if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
						cell.append('"');
						i++;
					} else {
						inQuotes = false;
					}
				} else {
					cell.append(c);
				}
			} else if (c == '"') {
				inQuotes = true;
			} else if (c == ',') {
				row.add(cell.toString());
				cell.setLength(0);
			} else if (c == '\n' || c == '\r') {
				if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
					i++;
				}
				row.add(cell.toString());
				cell.setLength(0);
				if (!(row.size() == 1 && row.get(0).isBlank())) {
					rows.add(row);
				}
				row = new ArrayList<>();
			} else {
				cell.append(c);
			}
		}
		if (cell.length() > 0 || !row.isEmpty()) {
			row.add(cell.toString());
			rows.add(row);
		}
		return rows;
	}

	private record ImportRow(int line, ProductVariantEntity variant, BigDecimal price, ProductStatus status,
			Integer quantity) {
	}

	@Override
	public ResponseDataDto importCsv(MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Chưa chọn file CSV", null);
		}
		String text = new String(file.getBytes(), StandardCharsets.UTF_8);
		if (text.startsWith("﻿")) {
			text = text.substring(1);
		}
		List<List<String>> rows = parseCsv(text);
		if (rows.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "File CSV rỗng", null);
		}
		Map<String, Integer> col = new HashMap<>();
		List<String> header = rows.get(0);
		for (int i = 0; i < header.size(); i++) {
			col.put(header.get(i).trim().toLowerCase(), i);
		}
		if (!col.containsKey("sku")) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Thiếu cột bắt buộc: sku", null);
		}
		if (!col.containsKey("price") && !col.containsKey("status") && !col.containsKey("quantity")) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL,
					"Cần ít nhất 1 cột để cập nhật: price, status hoặc quantity", null);
		}

		List<ImportRow> valid = new ArrayList<>();
		List<String> errors = new ArrayList<>();
		java.util.Set<String> seen = new java.util.HashSet<>();
		for (int r = 1; r < rows.size(); r++) {
			List<String> row = rows.get(r);
			int line = r + 1;
			String sku = cell(row, col.get("sku"));
			if (sku.isEmpty()) {
				errors.add("Dòng " + line + ": thiếu SKU");
				continue;
			}
			if (!seen.add(sku)) {
				errors.add("Dòng " + line + ": SKU " + sku + " bị lặp trong file");
				continue;
			}
			var variantOpt = variantRepository.findBySku(sku);
			if (variantOpt.isEmpty()) {
				errors.add("Dòng " + line + ": không tìm thấy SKU " + sku);
				continue;
			}
			BigDecimal price = null;
			ProductStatus status = null;
			Integer qty = null;
			try {
				String p = cell(row, col.get("price"));
				if (!p.isEmpty()) {
					price = new BigDecimal(p);
					if (price.signum() <= 0) {
						throw new IllegalArgumentException("giá phải > 0");
					}
				}
				String st = cell(row, col.get("status"));
				if (!st.isEmpty()) {
					status = ProductStatus.valueOf(st.toUpperCase());
				}
				String q = cell(row, col.get("quantity"));
				if (!q.isEmpty()) {
					qty = Integer.parseInt(q);
					if (qty < 0) {
						throw new IllegalArgumentException("số lượng không được âm");
					}
				}
			} catch (IllegalArgumentException e) {
				errors.add("Dòng " + line + " (" + sku + "): giá trị không hợp lệ - " + e.getMessage());
				continue;
			}
			valid.add(new ImportRow(line, variantOpt.get(), price, status, qty));
		}
		if (!errors.isEmpty()) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("errors", errors);
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Import bị hủy, không có dòng nào được áp dụng. Sửa file rồi thử lại.", data);
		}

		int updated = 0;
		for (ImportRow r : valid) {
			ProductVariantEntity v = r.variant();
			ProductEntity product = v.getProduct();
			try {
				if (r.price() != null) {
					v.setPrice(r.price());
					if (v.isDefault()) {
						product.setPrice(r.price());
						productRepository.save(product);
					}
				}
				if (r.status() != null) {
					if (v.isDefault()) {
						product.setStatus(r.status());
						productRepository.save(product);
					} else {
						v.setStatus(r.status());
					}
				}
				variantRepository.save(v);
				if (r.quantity() != null) {
					InventoryEntity inv = stockService.ensureInventory(v);
					if (inv.getQuantity() != r.quantity()) {
						stockService.setQuantity(inv, r.quantity(), "Import CSV");
					}
				}
				updated++;
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException("Dòng " + r.line() + " (" + v.getSku() + "): " + e.getMessage(), e);
			}
		}
		Map<String, Object> data = new HashMap<>();
		data.put("updated", updated);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Đã cập nhật " + updated + " biến thể", data);
	}

	private static String cell(List<String> row, Integer idx) {
		if (idx == null || idx >= row.size()) {
			return "";
		}
		return row.get(idx).trim();
	}

	@Override
	public ResponseDataDto bulkStatus(List<String> productIds, String status) {
		if (productIds == null || productIds.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Chưa chọn sản phẩm nào", null);
		}
		ProductStatus st;
		try {
			st = ProductStatus.valueOf(status == null ? "" : status.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Trạng thái không hợp lệ", null);
		}
		List<ProductEntity> products = productRepository.findAllById(productIds);
		if (products.size() != productIds.size()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Có sản phẩm không tồn tại", null);
		}
		products.forEach(p -> p.setStatus(st));
		productRepository.saveAll(products);
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Đã cập nhật trạng thái " + products.size() + " sản phẩm", products.size());
	}

	@Override
	public ResponseDataDto bulkPrice(List<String> productIds, String mode, BigDecimal value) {
		if (productIds == null || productIds.isEmpty()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Chưa chọn sản phẩm nào", null);
		}
		if (value == null || mode == null) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Thiếu chế độ hoặc giá trị", null);
		}
		String m = mode.trim().toUpperCase();
		if (!List.of("PERCENT", "AMOUNT", "SET").contains(m)) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Chế độ phải là PERCENT, AMOUNT hoặc SET", null);
		}
		List<ProductEntity> products = productRepository.findAllById(productIds);
		if (products.size() != productIds.size()) {
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Có sản phẩm không tồn tại", null);
		}
		List<ProductVariantEntity> all = new ArrayList<>();
		for (ProductEntity p : products) {
			all.addAll(variantRepository.findByProduct_ProductIdOrderBySortOrderAscVariantIdAsc(p.getProductId()));
		}
		List<String> errors = new ArrayList<>();
		for (ProductVariantEntity v : all) {
			BigDecimal np = newPrice(v.getPrice(), m, value);
			if (np.signum() <= 0) {
				errors.add(v.getSku() + ": giá mới " + np.toPlainString() + " không hợp lệ (phải > 0)");
			}
		}
		if (!errors.isEmpty()) {
			Map<String, Object> data = new HashMap<>();
			data.put("errors", errors);
			return new ResponseDataDto(Constant.RESULT_CD_FAIL, "Không áp dụng vì có giá mới không hợp lệ", data);
		}
		for (ProductVariantEntity v : all) {
			v.setPrice(newPrice(v.getPrice(), m, value));
			variantRepository.save(v);
			if (v.isDefault()) {
				v.getProduct().setPrice(v.getPrice());
				productRepository.save(v.getProduct());
			}
		}
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "Đã đổi giá " + all.size() + " biến thể của " + products.size() + " sản phẩm", all.size());
	}

	private static BigDecimal newPrice(BigDecimal old, String mode, BigDecimal value) {
		BigDecimal result = switch (mode) {
		case "PERCENT" -> old.add(old.multiply(value).divide(BigDecimal.valueOf(100)));
		case "AMOUNT" -> old.add(value);
		default -> value;
		};
		return result.setScale(2, RoundingMode.HALF_UP);
	}
}
