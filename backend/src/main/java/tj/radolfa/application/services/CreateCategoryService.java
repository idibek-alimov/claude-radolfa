package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.CreateCategoryUseCase;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.ports.out.SaveCategoryPort;

/**
 * Creates a product category natively.
 */
@Service
public class CreateCategoryService implements CreateCategoryUseCase {

    private final LoadCategoryPort loadCategoryPort;
    private final SaveCategoryPort saveCategoryPort;

    public CreateCategoryService(LoadCategoryPort loadCategoryPort,
                                 SaveCategoryPort saveCategoryPort) {
        this.loadCategoryPort = loadCategoryPort;
        this.saveCategoryPort = saveCategoryPort;
    }

    @Override
    @Transactional
    public Long execute(String name, Long parentId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        // Validate name uniqueness
        if (loadCategoryPort.findByName(name.trim()).isPresent()) {
            throw new IllegalArgumentException("Category with name '" + name + "' already exists");
        }
        // Validate parent exists if provided
        if (parentId != null) {
            loadCategoryPort.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent category not found: id=" + parentId));
        }

        String slug = name.trim().toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        LoadCategoryPort.CategoryView saved = saveCategoryPort.save(name.trim(), slug, parentId);
        return saved.id();
    }
}
