package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateProductCategoryRequestDto(
        @NotNull(message = "categoryId must not be null")
        Long categoryId
) {}
