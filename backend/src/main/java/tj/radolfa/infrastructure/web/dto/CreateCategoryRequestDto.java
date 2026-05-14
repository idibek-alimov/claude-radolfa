package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record CreateCategoryRequestDto(

        @NotBlank(message = "Category name is required")
        String name,

        /** Optional parent category ID for nested categories. */
        Long parentId,

        /** Optional IDs of review traits to link to this category. Null means no trait associations. */
        Set<@Positive Long> traitIds

) {}
