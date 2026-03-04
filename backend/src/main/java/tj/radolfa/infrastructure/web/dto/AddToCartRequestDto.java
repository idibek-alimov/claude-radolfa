package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddToCartRequestDto(
        @NotNull Long skuId,
        @Min(1) int quantity) {
}
