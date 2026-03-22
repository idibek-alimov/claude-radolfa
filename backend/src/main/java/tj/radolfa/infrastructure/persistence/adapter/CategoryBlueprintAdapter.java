package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadCategoryBlueprintPort;
import tj.radolfa.infrastructure.persistence.repository.CategoryAttributeBlueprintRepository;

import java.util.List;

@Component
public class CategoryBlueprintAdapter implements LoadCategoryBlueprintPort {

    private final CategoryAttributeBlueprintRepository repo;

    public CategoryBlueprintAdapter(CategoryAttributeBlueprintRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<BlueprintEntry> findByCategoryId(Long categoryId) {
        return repo.findByCategoryIdOrderBySortOrderAsc(categoryId).stream()
                .map(e -> new BlueprintEntry(
                        e.getId(),
                        e.getCategory().getId(),
                        e.getAttributeKey(),
                        e.isRequired(),
                        e.getSortOrder()))
                .toList();
    }
}
