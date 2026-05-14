package tj.radolfa.domain.model;

import java.util.Objects;

public record SkuTarget(String itemCode) implements DiscountTarget {
    public SkuTarget {
        Objects.requireNonNull(itemCode, "itemCode must not be null");
    }
}
