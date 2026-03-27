package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Shared DTO for product attribute key-value pairs.
 * Used in both product creation and listing update requests.
 */
public record ProductAttributeDto(
        @NotBlank(message = "Attribute key must not be blank")
        @Size(max = 100, message = "Attribute key must not exceed 100 characters")
        String key,
        @NotBlank(message = "Attribute value must not be blank")
        @Size(max = 1000, message = "Attribute value must not exceed 1000 characters")
        String value,
        int sortOrder) {
}
