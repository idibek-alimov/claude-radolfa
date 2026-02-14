package tj.radolfa.application.ports.in;

import tj.radolfa.application.ports.out.LoadCategoryPort.CategoryView;

import java.util.List;

/**
 * In-Port: synchronise the category hierarchy from ERPNext.
 *
 * <p>Categories are synced BEFORE products. ERPNext pushes the full category tree.
 */
public interface SyncCategoriesUseCase {

    List<CategoryView> execute(SyncCategoriesCommand command);

    record SyncCategoriesCommand(List<CategoryPayload> categories) {
        public record CategoryPayload(
                String name,
                String parentName  // null = root
        ) {}
    }
}
