package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.StackingPolicy;

public record DiscountTypeResponse(Long id, String name, int rank, StackingPolicy stackingPolicy) {

    public static DiscountTypeResponse fromDomain(DiscountType type) {
        return new DiscountTypeResponse(type.id(), type.name(), type.rank(), type.stackingPolicy());
    }
}
