package tj.radolfa.application.ports.out;

import java.util.List;
import java.util.Optional;

/**
 * Out-Port: read operations for product categories.
 */
public interface LoadCategoryPort {

    record CategoryView(Long id, String name, String slug, Long parentId) {}

    Optional<CategoryView> findByName(String name);

    Optional<CategoryView> findBySlug(String slug);

    List<CategoryView> findRoots();

    List<CategoryView> findByParentId(Long parentId);

    List<CategoryView> findAll();

    List<Long> getAllDescendantIds(Long categoryId);
}
