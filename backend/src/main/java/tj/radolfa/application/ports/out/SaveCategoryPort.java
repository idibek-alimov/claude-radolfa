package tj.radolfa.application.ports.out;

import tj.radolfa.application.readmodel.CategoryView;

/**
 * Out-Port: persist category data.
 */
public interface SaveCategoryPort {

    CategoryView save(String name, String slug, Long parentId);
}
