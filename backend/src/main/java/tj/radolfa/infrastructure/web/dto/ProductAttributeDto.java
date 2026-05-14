package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Shared DTO for product attribute key + multi-value pairs.
 * Used in both product creation and listing update requests.
 */
public record ProductAttributeDto(
        @NotBlank(message = "Attribute key must not be blank")
        @Size(max = 100, message = "Attribute key must not exceed 100 characters")
        String key,
        @NotEmpty(message = "Attribute values must not be empty")
        List<@NotBlank(message = "Attribute value must not be blank")
             @Size(max = 512, message = "Attribute value must not exceed 512 characters")
             String> values,
        int sortOrder) {
}
