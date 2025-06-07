package com.dungochung.shopdongho.converter;

import com.dungochung.shopdongho.enums.ProductCondition;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProductConditionConverter implements AttributeConverter<ProductCondition, String> {
	@Override
	public String convertToDatabaseColumn(ProductCondition attribute) {
		return attribute == null ? null : attribute.getDbValue();
	}

	@Override
	public ProductCondition convertToEntityAttribute(String dbData) {
		return dbData == null ? null : ProductCondition.fromDbValue(dbData);
	}
}
