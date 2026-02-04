package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hibernate {@link AttributeConverter} that serialises a {@code List<String>}
 * to and from a PostgreSQL {@code TEXT[]} array literal
 * (e.g. {@code {"url1","url2"}}).
 *
 * This avoids the need for a separate join table for the images column.
 */
@Converter
public class StringArrayConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        return attribute.stream()
                .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.equals("{}")) {
            return List.of();
        }
        // Strip outer braces, then split on commas that are outside quotes.
        String inner = dbData.substring(1, dbData.length() - 1);
        return Arrays.stream(inner.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                .collect(Collectors.toList());
    }
}
