package tj.radolfa.application.readmodel;

import java.util.List;

/**
 * Read model for product categories — shared by both the in-port (GetCategoryUseCase)
 * and out-port (LoadCategoryPort) so that neither port imports the other.
 *
 * <p>{@code traitIds} — IDs of review traits linked to this category.
 * Empty list when the category has no linked traits.
 */
public record CategoryView(Long id, String name, String slug, Long parentId, List<Long> traitIds) {}
