package tj.radolfa.application.ports.out;

/**
 * Out-Port: persist category data.
 */
public interface SaveCategoryPort {

    LoadCategoryPort.CategoryView save(String name, String slug, Long parentId);
}
