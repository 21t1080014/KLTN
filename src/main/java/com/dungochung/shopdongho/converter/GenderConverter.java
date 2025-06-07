package com.dungochung.shopdongho.converter;

import com.dungochung.shopdongho.enums.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GenderConverter implements AttributeConverter<Gender, String> {
	@Override
	public String convertToDatabaseColumn(Gender attribute) {
		return attribute == null ? null : attribute.getDbValue();
	}

	@Override
	public Gender convertToEntityAttribute(String dbData) {
		return dbData == null ? null : Gender.fromDbValue(dbData);
	}
}
