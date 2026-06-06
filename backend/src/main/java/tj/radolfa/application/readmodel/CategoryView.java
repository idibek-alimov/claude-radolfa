package tj.radolfa.application.readmodel;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read model for product categories — shared by both the in-port (GetCategoryUseCase)
 * and out-port (LoadCategoryPort) so that neither port imports the other.
 *
 * <p>{@code traitIds} — IDs of review traits linked to this category.
 * Empty list when the category has no linked traits.
 *
 * <p>{@code productCount} and {@code minPrice} are populated only by the tree endpoint
 * ({@code findAll()}); single-category fetches leave them null.
 */
public record CategoryView(Long id, String name, String slug, Long parentId,
                           List<Long> traitIds, Long productCount, BigDecimal minPrice) {

    public CategoryView(Long id, String name, String slug, Long parentId, List<Long> traitIds) {
        this(id, name, slug, parentId, traitIds, null, null);
    }
}
