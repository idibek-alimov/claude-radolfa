package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.DeleteCategoryPort;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.ports.out.SaveCategoryPort;
import tj.radolfa.application.readmodel.CategoryView;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;
import tj.radolfa.infrastructure.persistence.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

@Component
public class CategoryAdapter implements LoadCategoryPort, SaveCategoryPort, DeleteCategoryPort {

    private final CategoryRepository categoryRepo;

    public CategoryAdapter(CategoryRepository categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    @Override
    public Optional<CategoryView> findById(Long id) {
        return categoryRepo.findById(id).map(this::toView);
    }

    @Override
    public Optional<CategoryView> findByName(String name) {
        return categoryRepo.findByName(name).map(this::toView);
    }

    @Override
    public Optional<CategoryView> findBySlug(String slug) {
        return categoryRepo.findBySlug(slug).map(this::toView);
    }

    @Override
    public List<CategoryView> findRoots() {
        return categoryRepo.findByParentIsNull().stream().map(this::toView).toList();
    }

    @Override
    public List<CategoryView> findByParentId(Long parentId) {
        return categoryRepo.findByParentId(parentId).stream().map(this::toView).toList();
    }

    @Override
    public List<CategoryView> findAll() {
        return categoryRepo.findAll().stream().map(this::toView).toList();
    }

    @Override
    public List<Long> getAllDescendantIds(Long categoryId) {
        return categoryRepo.findAllDescendantIds(categoryId);
    }

    @Override
    public CategoryView save(String name, String slug, Long parentId) {
        CategoryEntity entity = new CategoryEntity();
        entity.setName(name);
        entity.setSlug(slug);
        if (parentId != null) {
            CategoryEntity parent = categoryRepo.getReferenceById(parentId);
            entity.setParent(parent);
        }
        return toView(categoryRepo.save(entity));
    }

    @Override
    public CategoryView update(Long id, String name, Long parentId) {
        CategoryEntity entity = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: id=" + id));
        entity.setName(name);
        entity.setParent(parentId != null ? categoryRepo.getReferenceById(parentId) : null);
        return toView(categoryRepo.save(entity));
    }

    @Override
    public void deleteById(Long categoryId) {
        boolean inUse = categoryRepo.existsProductBasesByCategoryId(categoryId);
        if (inUse) {
            throw new IllegalStateException(
                    "Category id=" + categoryId + " is still assigned to products and cannot be deleted.");
        }
        categoryRepo.deleteById(categoryId);
    }

    private CategoryView toView(CategoryEntity entity) {
        return new CategoryView(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getParent() != null ? entity.getParent().getId() : null);
    }
}
