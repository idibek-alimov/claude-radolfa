package tj.radolfa.infrastructure.persistence.adapter;

import org.junit.jupiter.api.Test;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryAggregateRollupTest {

    @Test
    void leafWithProductsAndPrice() {
        CategoryEntity leaf = cat(10L, null);

        var result = CategoryAggregateRollup.rollUp(
                List.of(leaf),
                Map.of(10L, 5L),
                Map.of(10L, new BigDecimal("99.00")));

        assertThat(result.get(10L).count()).isEqualTo(5L);
        assertThat(result.get(10L).minPrice()).isEqualByComparingTo("99.00");
    }

    @Test
    void emptyCategoryHasZeroCountAndNullPrice() {
        CategoryEntity leaf = cat(10L, null);

        var result = CategoryAggregateRollup.rollUp(List.of(leaf), Map.of(), Map.of());

        assertThat(result.get(10L).count()).isZero();
        assertThat(result.get(10L).minPrice()).isNull();
    }

    @Test
    void rootRollsUpDescendants() {
        // root (1) → childA (2) → grandchild (4)
        //          → childB (3, no products)
        CategoryEntity root = cat(1L, null);
        CategoryEntity childA = cat(2L, root);
        CategoryEntity childB = cat(3L, root);
        CategoryEntity grandchild = cat(4L, childA);

        var result = CategoryAggregateRollup.rollUp(
                List.of(root, childA, childB, grandchild),
                Map.of(1L, 2L, 2L, 3L, 4L, 7L),
                Map.of(1L, new BigDecimal("500.00"), 2L, new BigDecimal("200.00"), 4L, new BigDecimal("50.00")));

        // root = 2 + 3 + 0 + 7 = 12; minPrice = min(500, 200, 50) = 50
        assertThat(result.get(1L).count()).isEqualTo(12L);
        assertThat(result.get(1L).minPrice()).isEqualByComparingTo("50.00");

        // childA = 3 + 7 = 10; minPrice = min(200, 50) = 50
        assertThat(result.get(2L).count()).isEqualTo(10L);
        assertThat(result.get(2L).minPrice()).isEqualByComparingTo("50.00");

        // childB has no products, no descendants
        assertThat(result.get(3L).count()).isZero();
        assertThat(result.get(3L).minPrice()).isNull();

        // grandchild direct only
        assertThat(result.get(4L).count()).isEqualTo(7L);
        assertThat(result.get(4L).minPrice()).isEqualByComparingTo("50.00");
    }

    @Test
    void rootWithNoProductsButDescendantHasProducts() {
        CategoryEntity root = cat(1L, null);
        CategoryEntity child = cat(2L, root);

        var result = CategoryAggregateRollup.rollUp(
                List.of(root, child),
                Map.of(2L, 4L),
                Map.of(2L, new BigDecimal("120.00")));

        assertThat(result.get(1L).count()).isEqualTo(4L);
        assertThat(result.get(1L).minPrice()).isEqualByComparingTo("120.00");
    }

    private CategoryEntity cat(Long id, CategoryEntity parent) {
        CategoryEntity e = new CategoryEntity();
        e.setId(id);
        e.setParent(parent);
        return e;
    }
}
