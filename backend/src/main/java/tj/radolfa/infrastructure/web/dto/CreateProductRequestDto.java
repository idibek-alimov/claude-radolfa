package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequestDto(

        @NotBlank(message = "Product name is required")
        String name,

        @NotNull(message = "categoryId is required")
        Long categoryId,

        @NotNull(message = "colorId is required")
        Long colorId,

        String webDescription,

        @NotEmpty(message = "At least one SKU definition is required")
        @Valid
        List<SkuDefinitionDto> skus,

        @Valid
        List<AttributeDto> attributes

) {
    public record SkuDefinitionDto(

            @NotBlank(message = "sizeLabel is required")
            String sizeLabel,

            @NotNull(message = "price is required")
            @PositiveOrZero(message = "price must be ≥ 0")
            BigDecimal price,

            @PositiveOrZero(message = "stockQuantity must be ≥ 0")
            int stockQuantity
    ) {}

    public record AttributeDto(

            @NotBlank(message = "attribute key is required")
            String key,

            @NotBlank(message = "attribute value is required")
            String value
    ) {}
}
