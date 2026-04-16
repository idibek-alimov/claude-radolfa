package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.UpdateCategoryUseCase;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.ports.out.SaveCategoryPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;

import java.util.List;

@Service
public class UpdateCategoryService implements UpdateCategoryUseCase {

    private final LoadCategoryPort loadCategoryPort;
    private final SaveCategoryPort saveCategoryPort;

    public UpdateCategoryService(LoadCategoryPort loadCategoryPort,
                                 SaveCategoryPort saveCategoryPort) {
        this.loadCategoryPort = loadCategoryPort;
        this.saveCategoryPort = saveCategoryPort;
    }

    @Override
    @Transactional
    public void execute(Long id, String name, Long parentId) {
        loadCategoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: id=" + id));

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        if (name.trim().length() > 128) {
            throw new IllegalArgumentException("Category name must not exceed 128 characters");
        }

        // Name uniqueness — allow same name when it belongs to this category itself
        loadCategoryPort.findByName(name.trim()).ifPresent(existing -> {
            if (!existing.id().equals(id)) {
                throw new IllegalArgumentException("Category with name '" + name.trim() + "' already exists");
            }
        });

        if (parentId != null) {
            loadCategoryPort.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found: id=" + parentId));

            if (parentId.equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }

            // Prevent making a category a child of one of its own descendants
            List<Long> descendantIds = loadCategoryPort.getAllDescendantIds(id);
            if (descendantIds.contains(parentId)) {
                throw new IllegalArgumentException(
                        "Circular parentage: category id=" + parentId + " is a descendant of id=" + id);
            }
        }

        saveCategoryPort.update(id, name.trim(), parentId);
    }
}
