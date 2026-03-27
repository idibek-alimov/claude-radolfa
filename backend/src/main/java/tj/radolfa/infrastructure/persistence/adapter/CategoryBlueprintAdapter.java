package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadCategoryBlueprintPort;
import tj.radolfa.application.ports.out.SaveCategoryBlueprintPort;
import tj.radolfa.domain.model.AttributeType;
import tj.radolfa.infrastructure.persistence.entity.CategoryAttributeBlueprintEntity;
import tj.radolfa.infrastructure.persistence.entity.CategoryAttributeBlueprintValueEntity;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;
import tj.radolfa.infrastructure.persistence.repository.CategoryAttributeBlueprintRepository;
import tj.radolfa.infrastructure.persistence.repository.CategoryRepository;

import java.util.ArrayList;
import java.util.List;

@Component
public class CategoryBlueprintAdapter implements LoadCategoryBlueprintPort, SaveCategoryBlueprintPort {

    private final CategoryAttributeBlueprintRepository blueprintRepo;
    private final CategoryRepository categoryRepo;

    public CategoryBlueprintAdapter(CategoryAttributeBlueprintRepository blueprintRepo,
                                    CategoryRepository categoryRepo) {
        this.blueprintRepo = blueprintRepo;
        this.categoryRepo = categoryRepo;
    }

    @Override
    public List<BlueprintEntry> findByCategoryId(Long categoryId) {
        return blueprintRepo.findByCategoryIdOrderBySortOrderAsc(categoryId).stream()
                .map(e -> new BlueprintEntry(
                        e.getId(),
                        categoryId,
                        e.getAttributeKey(),
                        e.getType(),
                        e.getUnitName(),
                        e.getAllowedValues().stream()
                                .map(CategoryAttributeBlueprintValueEntity::getAllowedValue)
                                .toList(),
                        e.isRequired(),
                        e.getSortOrder()))
                .toList();
    }

    @Override
    public Long save(Long categoryId, String attributeKey, AttributeType type, String unitName,
                     List<String> allowedValues, boolean required, int sortOrder) {
        CategoryEntity category = categoryRepo.getReferenceById(categoryId);

        CategoryAttributeBlueprintEntity entity = new CategoryAttributeBlueprintEntity();
        entity.setCategory(category);
        entity.setAttributeKey(attributeKey);
        entity.setType(type);
        entity.setUnitName(unitName);
        entity.setRequired(required);
        entity.setSortOrder(sortOrder);

        List<CategoryAttributeBlueprintValueEntity> valueEntities = new ArrayList<>();
        for (int i = 0; i < allowedValues.size(); i++) {
            CategoryAttributeBlueprintValueEntity v = new CategoryAttributeBlueprintValueEntity();
            v.setBlueprint(entity);
            v.setAllowedValue(allowedValues.get(i));
            v.setSortOrder(i);
            valueEntities.add(v);
        }
        entity.setAllowedValues(valueEntities);

        return blueprintRepo.save(entity).getId();
    }

    @Override
    public void deleteById(Long blueprintId) {
        blueprintRepo.deleteById(blueprintId);
    }
}
