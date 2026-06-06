package tj.radolfa.infrastructure.persistence.adapter;

import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;

import java.math.BigDecimal;
import java.util.*;

/**
 * Pure (no Spring/JPA) helper that rolls up direct per-category aggregates
 * to include every descendant subtree. Called once from CategoryAdapter.findAll().
 */
class CategoryAggregateRollup {

    record Aggregate(long count, BigDecimal minPrice) {}

    static Map<Long, Aggregate> rollUp(
            List<CategoryEntity> categories,
            Map<Long, Long> directCount,
            Map<Long, BigDecimal> directMinPrice) {

        Map<Long, List<Long>> childrenByParent = new HashMap<>();
        Set<Long> allIds = new HashSet<>();
        for (CategoryEntity cat : categories) {
            allIds.add(cat.getId());
            if (cat.getParent() != null) {
                childrenByParent
                        .computeIfAbsent(cat.getParent().getId(), k -> new ArrayList<>())
                        .add(cat.getId());
            }
        }

        Map<Long, Aggregate> memo = new HashMap<>();
        for (Long id : allIds) {
            compute(id, childrenByParent, directCount, directMinPrice, memo);
        }
        return memo;
    }

    private static Aggregate compute(
            Long id,
            Map<Long, List<Long>> childrenByParent,
            Map<Long, Long> directCount,
            Map<Long, BigDecimal> directMinPrice,
            Map<Long, Aggregate> memo) {

        if (memo.containsKey(id)) return memo.get(id);

        long count = directCount.getOrDefault(id, 0L);
        BigDecimal minPrice = directMinPrice.get(id);

        for (Long childId : childrenByParent.getOrDefault(id, List.of())) {
            Aggregate child = compute(childId, childrenByParent, directCount, directMinPrice, memo);
            count += child.count();
            if (child.minPrice() != null) {
                minPrice = minPrice == null ? child.minPrice() : minPrice.min(child.minPrice());
            }
        }

        Aggregate agg = new Aggregate(count, minPrice);
        memo.put(id, agg);
        return agg;
    }
}
