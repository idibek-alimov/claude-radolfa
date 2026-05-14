package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.DeleteCategoryUseCase;
import tj.radolfa.application.ports.out.DeleteCategoryPort;
import tj.radolfa.application.ports.out.LoadCategoryPort;

/**
 * Deletes a product category. Throws if still in use.
 */
@Service
public class DeleteCategoryService implements DeleteCategoryUseCase {

    private final LoadCategoryPort   loadCategoryPort;
    private final DeleteCategoryPort deleteCategoryPort;

    public DeleteCategoryService(LoadCategoryPort loadCategoryPort,
                                 DeleteCategoryPort deleteCategoryPort) {
        this.loadCategoryPort   = loadCategoryPort;
        this.deleteCategoryPort = deleteCategoryPort;
    }

    @Override
    @Transactional
    public void execute(Long categoryId) {
        loadCategoryPort.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found: id=" + categoryId));
        deleteCategoryPort.deleteById(categoryId);
    }
}
