package tj.radolfa.domain.model;

import java.util.Objects;

public record CategoryTarget(Long categoryId, boolean includeDescendants) implements DiscountTarget {
    public CategoryTarget {
        Objects.requireNonNull(categoryId, "categoryId must not be null");
    }
}
