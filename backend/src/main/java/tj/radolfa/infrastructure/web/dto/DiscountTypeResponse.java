package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.DiscountType;

public record DiscountTypeResponse(Long id, String name, int rank) {

    public static DiscountTypeResponse fromDomain(DiscountType type) {
        return new DiscountTypeResponse(type.id(), type.name(), type.rank());
    }
}
