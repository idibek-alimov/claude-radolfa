package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.ports.out.SaveCategoryPort;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;
import tj.radolfa.infrastructure.persistence.repository.CategoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CategoryAdapter implements LoadCategoryPort, SaveCategoryPort {

    private final CategoryRepository categoryRepo;

    public CategoryAdapter(CategoryRepository categoryRepo) {
        this.categoryRepo = categoryRepo;
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
        List<Long> result = new ArrayList<>();
        result.add(categoryId);
        List<CategoryEntity> children = categoryRepo.findByParentId(categoryId);
        for (CategoryEntity child : children) {
            result.addAll(getAllDescendantIds(child.getId()));
        }
        return result;
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

    private CategoryView toView(CategoryEntity entity) {
        return new CategoryView(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getParent() != null ? entity.getParent().getId() : null
        );
    }
}
