package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequestDto(

        @NotBlank(message = "Product name is required")
        String name,

        @NotNull(message = "categoryId is required")
        Long categoryId,

        Long brandId,

        @NotEmpty(message = "At least one variant is required")
        @Valid
        List<ListingVariantCreationDto> variants

) {
    public record ListingVariantCreationDto(

            @NotNull(message = "colorId is required")
            Long colorId,

            String webDescription,

            @Valid List<ProductAttributeDto> attributes,

            List<@NotBlank(message = "Image URL must not be blank")
                 @Pattern(regexp = "^https://\\S+$", message = "Image URL must use HTTPS and contain no spaces")
                 @Size(max = 2048, message = "Image URL must not exceed 2048 characters")
                 String> images,

            @NotEmpty(message = "At least one SKU definition is required")
            @Valid
            List<SkuDefinitionDto> skus

    ) {}

    public record SkuDefinitionDto(

            @NotBlank(message = "sizeLabel is required")
            String sizeLabel,

            @NotNull(message = "price is required")
            @PositiveOrZero(message = "price must be ≥ 0")
            BigDecimal price,

            @PositiveOrZero(message = "stockQuantity must be ≥ 0")
            int stockQuantity,

            @NotBlank(message = "barcode is required")
            String barcode,

            Double weightKg,

            Integer widthCm,

            Integer heightCm,

            Integer depthCm

    ) {}
}
