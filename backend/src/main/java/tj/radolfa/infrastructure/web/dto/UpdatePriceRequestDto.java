package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdatePriceRequestDto(

        @NotNull(message = "price is required")
        @PositiveOrZero(message = "price must be ≥ 0")
        BigDecimal price

) {}
