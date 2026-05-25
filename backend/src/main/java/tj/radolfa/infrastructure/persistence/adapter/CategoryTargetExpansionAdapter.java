package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.ExpandCategoryTargetPort;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CategoryTargetExpansionAdapter implements ExpandCategoryTargetPort {

    private final EntityManager em;

    public CategoryTargetExpansionAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Map<Long, Set<String>> resolveSkuCodes(Map<Long, Boolean> categoryToIncludeDescendants) {
        if (categoryToIncludeDescendants.isEmpty()) return Map.of();

        List<Long> rootIds = List.copyOf(categoryToIncludeDescendants.keySet());

        List<Long> expandableRootIds = categoryToIncludeDescendants.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();

        // Sentinel prevents Postgres error on empty IN (); recursive step matches nothing.
        List<Long> expandableOrSentinel = expandableRootIds.isEmpty()
                ? List.of(-1L) : expandableRootIds;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                WITH RECURSIVE descendants(root_id, id) AS (
                    SELECT id, id FROM categories WHERE id IN (:rootIds)
                    UNION ALL
                    SELECT d.root_id, c.id
                    FROM categories c
                    JOIN descendants d ON c.parent_id = d.id
                    WHERE d.root_id IN (:expandableRootIds)
                )
                SELECT DISTINCT d.root_id, s.sku_code
                FROM descendants d
                JOIN product_bases pb ON pb.category_id = d.id
                JOIN listing_variants lv ON lv.product_base_id = pb.id
                JOIN skus s ON s.listing_variant_id = lv.id
                """)
                .setParameter("rootIds", rootIds)
                .setParameter("expandableRootIds", expandableOrSentinel)
                .getResultList();

        Map<Long, Set<String>> result = new HashMap<>();
        for (Object[] row : rows) {
            Long rootId  = ((Number) row[0]).longValue();
            String skuCode = (String) row[1];
            result.computeIfAbsent(rootId, k -> new HashSet<>()).add(skuCode);
        }
        return result;
    }
}
