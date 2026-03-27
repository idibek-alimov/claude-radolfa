package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequestDto(

        @NotBlank(message = "Category name is required")
        String name,

        /** Optional parent category ID for nested categories. */
        Long parentId

) {}
