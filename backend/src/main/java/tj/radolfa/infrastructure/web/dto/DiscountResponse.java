package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.Discount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DiscountResponse(
        Long id,
        DiscountTypeResponse type,
        List<DiscountTargetResponse> targets,
        AmountType amountType,
        BigDecimal amountValue,
        Instant validFrom,
        Instant validUpto,
        boolean disabled,
        String title,
        String colorHex,
        BigDecimal minBasketAmount,
        Integer usageCapTotal,
        Integer usageCapPerCustomer,
        String couponCode
) {

    public static DiscountResponse fromDomain(Discount d) {
        return new DiscountResponse(
                d.id(),
                DiscountTypeResponse.fromDomain(d.type()),
                d.targets().stream().map(DiscountTargetResponse::fromDomain).toList(),
                d.amountType(),
                d.amountValue(),
                d.validFrom(),
                d.validUpto(),
                d.disabled(),
                d.title(),
                d.colorHex(),
                d.minBasketAmount(),
                d.usageCapTotal(),
                d.usageCapPerCustomer(),
                d.couponCode()
        );
    }
}
