package com.hcmute.careergraph.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JsonUtils {

    @Converter
    public static class StringListConverter implements AttributeConverter<List<String>, String> {

        private final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * Converts the List<String> into a JSON string for database storage.
         * @param attribute The list of Strings from the entity.
         * @return A JSON string representation of the list, e.g., ["item1", "item2"].
         */
        @Override
        public String convertToDatabaseColumn(List<String> attribute) {
            if (attribute == null || attribute.isEmpty()) {
                return null; // or "[]" if you prefer an empty JSON array
            }
            try {
                // Chuyển đổi List<String> thành chuỗi JSON
                return objectMapper.writeValueAsString(attribute);
            } catch (IOException e) {
                // In a real application, you should use a proper logger
                throw new IllegalArgumentException("Error converting List<String> to JSON", e);
            }
        }

        /**
         * Converts the JSON string from the database back into a List<String>.
         * @param dbData The JSON string from the database column.
         * @return A List of Strings.
         */
        @Override
        public List<String> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) {
                return Collections.emptyList();
            }
            try {
                // Chuyển đổi chuỗi JSON trở lại thành List<String>
                return objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
            } catch (IOException e) {
                throw new IllegalArgumentException("Error converting JSON to List<String>", e);
            }
        }
    }

    @Converter
    public static class LongListConverter implements AttributeConverter<List<Long>, String> {

        private final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * Converts the List<Long> into a JSON string for database storage.
         * @param attribute The list of Longs from the entity.
         * @return A JSON string representation of the list.
         */
        @Override
        public String convertToDatabaseColumn(List<Long> attribute) {
            if (attribute == null || attribute.isEmpty()) {
                return null;
            }
            try {
                return objectMapper.writeValueAsString(attribute);
            } catch (IOException e) {
                throw new IllegalArgumentException("Error converting List to JSON", e);
            }
        }

        /**
         * Converts the JSON string from the database back into a List<Long>.
         * @param dbData The JSON string from the database column.
         * @return A List of Longs.
         */
        @Override
        public List<Long> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) {
                return Collections.emptyList();
            }
            try {
                return objectMapper.readValue(dbData, new TypeReference<>() {});
            } catch (IOException e) {
                throw new IllegalArgumentException("Error converting JSON to List", e);
            }
        }
    }

}
