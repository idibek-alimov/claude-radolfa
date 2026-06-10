package tj.radolfa.infrastructure.persistence.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import tj.radolfa.application.readmodel.ListingQueryCriteria;
import tj.radolfa.application.readmodel.ListingSort;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.infrastructure.persistence.entity.BrandEntity;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;
import tj.radolfa.infrastructure.persistence.entity.ColorEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductBaseEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductRatingSummaryEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL fallback predicates/sort for {@code ListingReadAdapter#searchCatalog}, mirroring
 * the filter/sort semantics of {@code ListingSearchAdapter#searchCatalog} (Elasticsearch)
 * as closely as the relational schema allows.
 *
 * <p><b>Known reduced-fidelity case (documented, by design):</b> {@code discountPercentage}
 * is computed in Java by {@code DiscountEnrichmentAdapter} (stacking + category-target
 * resolution), not a SQL column. {@link ListingQueryCriteria#minDiscountPercent()} is
 * therefore enforced here as "has at least one active discount" via the precomputed
 * {@code discountedVariantIds} set (ignoring the exact threshold), and
 * {@link ListingSort#BIGGEST_DISCOUNT} degrades to the default (variant id ascending)
 * order. Exact discount-threshold filtering and true discount-percentage sort are
 * Elasticsearch-only.
 */
public final class ListingSpecifications {

    private ListingSpecifications() {}

    /**
     * Builds the catalog {@link Specification}: filter predicates from {@code criteria}
     * plus (for the content query, not the count query) the whitelisted sort order.
     *
     * @param discountedVariantIds variant IDs with at least one active discount, as
     *                              resolved by {@code DiscountEnrichmentAdapter#findVariantIdsWithActiveDiscounts()}.
     *                              Only consulted when {@code criteria.minDiscountPercent() > 0}.
     */
    public static Specification<ListingVariantEntity> catalogFilter(ListingQueryCriteria criteria,
            List<Long> discountedVariantIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = filterPredicates(root, query, cb, criteria, discountedVariantIds);

            if (query.getResultType() != Long.class) {
                query.orderBy(buildOrder(root, query, cb, criteria.sort()));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * The filter predicates alone (no ordering) — reused by the SQL facet
     * (brand/colour/price) group-by queries in {@code ListingReadAdapter} so facet
     * counts are computed over the exact same filtered set as the page.
     */
    public static List<Predicate> filterPredicates(Root<ListingVariantEntity> root, CriteriaQuery<?> query,
            CriteriaBuilder cb, ListingQueryCriteria criteria, List<Long> discountedVariantIds) {
        List<Predicate> predicates = new ArrayList<>();

        Join<ListingVariantEntity, ProductBaseEntity> productBase = root.join("productBase", JoinType.INNER);
        Join<ListingVariantEntity, ColorEntity> color = root.join("color", JoinType.INNER);

        predicates.add(cb.equal(productBase.get("status"), ProductStatus.ACTIVE));

        if (criteria.hasCategoryFilter()) {
            Join<ProductBaseEntity, CategoryEntity> category = productBase.join("category", JoinType.INNER);
            predicates.add(category.get("id").in(criteria.categoryIds()));
        }

        if (!criteria.colorKeys().isEmpty()) {
            predicates.add(color.get("colorKey").in(criteria.colorKeys()));
        }

        if (!criteria.brandIds().isEmpty()) {
            Join<ProductBaseEntity, BrandEntity> brand = productBase.join("brand", JoinType.INNER);
            predicates.add(brand.get("id").in(criteria.brandIds()));
        }

        if (criteria.hasQuery()) {
            String pattern = "%" + criteria.query().toLowerCase() + "%";
            Predicate nameLike = cb.like(cb.lower(productBase.get("name")), pattern);
            Predicate colorLike = cb.like(cb.lower(color.get("colorKey")), pattern);
            Predicate descLike = cb.like(cb.lower(root.get("webDescription")), pattern);
            Predicate codeLike = cb.like(cb.lower(root.get("productCode")), pattern);

            Subquery<Long> skuSub = query.subquery(Long.class);
            Root<SkuEntity> skuRoot = skuSub.from(SkuEntity.class);
            skuSub.select(skuRoot.get("id"))
                    .where(cb.equal(skuRoot.get("listingVariant"), root),
                            cb.like(cb.lower(skuRoot.get("skuCode")), pattern));

            predicates.add(cb.or(nameLike, colorLike, descLike, codeLike, cb.exists(skuSub)));
        }

        if (criteria.priceMin() != null) {
            predicates.add(cb.ge(minPriceSubquery(root, query, cb), criteria.priceMin()));
        }
        if (criteria.priceMax() != null) {
            predicates.add(cb.le(minPriceSubquery(root, query, cb), criteria.priceMax()));
        }

        if (Boolean.TRUE.equals(criteria.inStockOnly())) {
            Subquery<Integer> stockSub = query.subquery(Integer.class);
            Root<SkuEntity> skuRoot = stockSub.from(SkuEntity.class);
            stockSub.select(cb.sum(skuRoot.get("stockQuantity")))
                    .where(cb.equal(skuRoot.get("listingVariant"), root));
            predicates.add(cb.gt(stockSub, 0));
        }

        if (criteria.minDiscountPercent() != null && criteria.minDiscountPercent() > 0) {
            // Reduced fidelity: "has any active discount", not the exact threshold (ES-only).
            predicates.add(discountedVariantIds.isEmpty()
                    ? cb.disjunction()
                    : root.get("id").in(discountedVariantIds));
        }

        return predicates;
    }

    /** Correlated {@code MIN(sku.originalPrice)} for the variant in {@code root}. */
    private static Subquery<BigDecimal> minPriceSubquery(Root<ListingVariantEntity> root, CriteriaQuery<?> query,
            CriteriaBuilder cb) {
        Subquery<BigDecimal> sub = query.subquery(BigDecimal.class);
        Root<SkuEntity> skuRoot = sub.from(SkuEntity.class);
        sub.select(cb.min(skuRoot.get("originalPrice")))
                .where(cb.equal(skuRoot.get("listingVariant"), root));
        return sub;
    }

    /**
     * Maps the whitelisted {@link ListingSort} to a concrete SQL order. Raw client
     * input never reaches this method — only enum constants.
     */
    private static List<Order> buildOrder(Root<ListingVariantEntity> root, CriteriaQuery<?> query,
            CriteriaBuilder cb, ListingSort sort) {
        return switch (sort) {
            case CHEAPEST -> List.of(cb.asc(minPriceSubquery(root, query, cb)), cb.asc(root.get("id")));
            case DEAREST -> List.of(cb.desc(minPriceSubquery(root, query, cb)), cb.asc(root.get("id")));
            case NEWEST -> List.of(cb.desc(root.get("createdAt")), cb.asc(root.get("id")));
            case RATING -> {
                Subquery<BigDecimal> ratingSub = query.subquery(BigDecimal.class);
                Root<ProductRatingSummaryEntity> ratingRoot = ratingSub.from(ProductRatingSummaryEntity.class);
                ratingSub.select(ratingRoot.get("averageRating"))
                        .where(cb.equal(ratingRoot.get("listingVariantId"), root.get("id")));
                yield List.of(cb.desc(cb.coalesce(ratingSub, BigDecimal.ZERO)), cb.asc(root.get("id")));
            }
            // POPULAR (browse default) and BIGGEST_DISCOUNT (reduced fidelity, see class
            // Javadoc) both degrade to the existing default ordering.
            case POPULAR, BIGGEST_DISCOUNT -> List.of(cb.asc(root.get("id")));
        };
    }
}
