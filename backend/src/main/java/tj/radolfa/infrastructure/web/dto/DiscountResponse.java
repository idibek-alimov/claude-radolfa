package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.Discount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DiscountResponse(
        Long id,
        DiscountTypeResponse type,
        List<String> itemCodes,
        BigDecimal discountValue,
        Instant validFrom,
        Instant validUpto,
        boolean disabled,
        String title,
        String colorHex
) {

    public static DiscountResponse fromDomain(Discount d) {
        return new DiscountResponse(
                d.id(),
                DiscountTypeResponse.fromDomain(d.type()),
                d.itemCodes(),
                d.discountValue(),
                d.validFrom(),
                d.validUpto(),
                d.disabled(),
                d.title(),
                d.colorHex()
        );
    }
}
