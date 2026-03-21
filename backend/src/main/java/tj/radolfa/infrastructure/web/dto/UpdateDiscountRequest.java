package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record UpdateDiscountRequest(
        @NotNull Long typeId,
        @NotEmpty List<String> itemCodes,
        @NotNull @DecimalMin("0.01") @DecimalMax("99.99") BigDecimal discountValue,
        @NotNull Instant validFrom,
        @NotNull Instant validUpto,
        String title,
        String colorHex
) {}
