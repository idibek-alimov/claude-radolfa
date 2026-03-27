package tj.radolfa.application.readmodel;

/**
 * Read model for product categories — shared by both the in-port (GetCategoryUseCase)
 * and out-port (LoadCategoryPort) so that neither port imports the other.
 */
public record CategoryView(Long id, String name, String slug, Long parentId) {}
