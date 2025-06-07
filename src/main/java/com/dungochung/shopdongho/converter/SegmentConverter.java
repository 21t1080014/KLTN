package com.dungochung.shopdongho.converter;

import com.dungochung.shopdongho.enums.Segment;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SegmentConverter implements AttributeConverter<Segment, String> {
	@Override
	public String convertToDatabaseColumn(Segment attribute) {
		return attribute == null ? null : attribute.getDbValue();
	}

	@Override
	public Segment convertToEntityAttribute(String dbData) {
		return dbData == null ? null : Segment.fromDbValue(dbData);
	}
}
