package tj.radolfa.application.ports.out;

import tj.radolfa.application.readmodel.CategoryView;

import java.util.Set;

/**
 * Out-Port: persist category data.
 *
 * <p>A {@code null} value for {@code traitIds} means "do not modify existing
 * trait associations" — useful for callers that don't manage traits.
 */
public interface SaveCategoryPort {

    CategoryView save(String name, String slug, Long parentId, Set<Long> traitIds);

    CategoryView update(Long id, String name, Long parentId, Set<Long> traitIds);
}
