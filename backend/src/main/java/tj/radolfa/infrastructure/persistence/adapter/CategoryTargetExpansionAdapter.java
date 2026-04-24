package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.ExpandCategoryTargetPort;
import tj.radolfa.infrastructure.persistence.repository.CategoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CategoryTargetExpansionAdapter implements ExpandCategoryTargetPort {

    private final CategoryRepository categoryRepository;
    private final EntityManager em;

    public CategoryTargetExpansionAdapter(CategoryRepository categoryRepository, EntityManager em) {
        this.categoryRepository = categoryRepository;
        this.em = em;
    }

    @Override
    public List<String> resolveSkuCodes(Map<Long, Boolean> categoryToIncludeDescendants) {
        if (categoryToIncludeDescendants.isEmpty()) return List.of();

        List<Long> allCategoryIds = new ArrayList<>();
        for (Map.Entry<Long, Boolean> entry : categoryToIncludeDescendants.entrySet()) {
            Long catId = entry.getKey();
            boolean descendants = Boolean.TRUE.equals(entry.getValue());
            if (descendants) {
                allCategoryIds.addAll(categoryRepository.findAllDescendantIds(catId));
            } else {
                allCategoryIds.add(catId);
            }
        }

        if (allCategoryIds.isEmpty()) return List.of();

        @SuppressWarnings("unchecked")
        List<String> codes = em.createQuery("""
                SELECT DISTINCT s.skuCode
                FROM SkuEntity s
                JOIN s.listingVariant lv
                JOIN lv.productBase pb
                WHERE pb.category.id IN :categoryIds
                """, String.class)
                .setParameter("categoryIds", allCategoryIds)
                .getResultList();

        return codes;
    }
}
