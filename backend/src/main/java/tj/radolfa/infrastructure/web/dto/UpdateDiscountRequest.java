package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import tj.radolfa.domain.model.AmountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record UpdateDiscountRequest(
        @NotNull Long typeId,
        @NotEmpty @Valid List<DiscountTargetInput> targets,
        @NotNull AmountType amountType,
        @NotNull @DecimalMin("0.01") BigDecimal amountValue,
        @NotNull Instant validFrom,
        @NotNull Instant validUpto,
        String title,
        String colorHex,
        BigDecimal minBasketAmount,
        Integer usageCapTotal,
        Integer usageCapPerCustomer,
        String couponCode
) {}
