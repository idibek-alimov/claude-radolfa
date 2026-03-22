package tj.radolfa.application.ports.in;

import tj.radolfa.application.readmodel.CategoryView;

import java.util.List;
import java.util.Optional;

/**
 * In-Port: storefront read operations for product categories.
 */
public interface GetCategoryUseCase {

    /** All categories (flat list — callers build the tree themselves). */
    List<CategoryView> findAll();

    /** Single category by URL slug. Empty if not found. */
    Optional<CategoryView> findBySlug(String slug);

    /** Returns the given category ID plus all transitive descendant IDs. */
    List<Long> getDescendantIds(Long categoryId);
}
