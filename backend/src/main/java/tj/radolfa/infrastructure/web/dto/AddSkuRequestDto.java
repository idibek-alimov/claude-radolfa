package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AddSkuRequestDto(

        @NotBlank(message = "sizeLabel must not be blank")
        String sizeLabel,

        @NotNull(message = "price is required")
        @PositiveOrZero(message = "price must be ≥ 0")
        BigDecimal price,

        @NotNull(message = "stockQuantity is required")
        @PositiveOrZero(message = "stockQuantity must be ≥ 0")
        Integer stockQuantity

) {}
