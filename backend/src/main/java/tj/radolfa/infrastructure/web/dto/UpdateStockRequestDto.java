package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateStockRequestDto(

        /** Absolute stock value to set. Mutually exclusive with {@code delta}. */
        Integer quantity,

        /**
         * Signed delta to apply (positive = add, negative = subtract).
         * Mutually exclusive with {@code quantity}.
         */
        Integer delta

) {
    public UpdateStockRequestDto {
        if (quantity == null && delta == null) {
            throw new IllegalArgumentException("Either 'quantity' or 'delta' must be provided");
        }
        if (quantity != null && delta != null) {
            throw new IllegalArgumentException("Provide either 'quantity' or 'delta', not both");
        }
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("quantity must be ≥ 0");
        }
    }
}
