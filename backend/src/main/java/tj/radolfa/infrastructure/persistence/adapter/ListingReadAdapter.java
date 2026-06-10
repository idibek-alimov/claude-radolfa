package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadListingPort;
import tj.radolfa.application.readmodel.CatalogFacets;
import tj.radolfa.application.readmodel.CatalogResult;
import tj.radolfa.application.readmodel.ListingQueryCriteria;
import tj.radolfa.application.readmodel.ReviewTraitView;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.ReviewTrait;
import tj.radolfa.infrastructure.persistence.entity.BrandEntity;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;
import tj.radolfa.infrastructure.persistence.entity.ColorEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductBaseEntity;
import tj.radolfa.infrastructure.persistence.entity.ReviewTraitEntity;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantImageEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductRatingSummaryEntity;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductBaseRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductRatingSummaryRepository;
import tj.radolfa.infrastructure.persistence.repository.SellerRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;
import tj.radolfa.infrastructure.persistence.spec.ListingSpecifications;
import tj.radolfa.application.readmodel.ListingVariantDetailDto;
import tj.radolfa.application.readmodel.ListingVariantDetailDto.AttributeDto;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.application.readmodel.ListingVariantDto.TagView;
import tj.radolfa.application.readmodel.SkuDto;
import tj.radolfa.domain.model.AppliedDiscount;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantAttributeEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Hexagonal adapter implementing the SQL-backed read queries for listings.
 *
 * <p>
 * Grid queries use JPQL aggregates (single query, no N+1).
 * Images and SKUs are batch-loaded in separate queries for the page.
 * Discounts are resolved from the discounts table post-query.
 */
@Component
public class ListingReadAdapter implements LoadListingPort {

        private final ListingVariantRepository variantRepo;
        private final SkuRepository skuRepo;
        private final DiscountEnrichmentAdapter discountEnrichment;
        private final SellerRepository sellerRepo;
        private final ProductBaseRepository productBaseRepo;
        private final ProductRatingSummaryRepository ratingRepo;
        private final EntityManager em;

        public ListingReadAdapter(ListingVariantRepository variantRepo,
                        SkuRepository skuRepo,
                        DiscountEnrichmentAdapter discountEnrichment,
                        SellerRepository sellerRepo,
                        ProductBaseRepository productBaseRepo,
                        ProductRatingSummaryRepository ratingRepo,
                        EntityManager em) {
                this.variantRepo = variantRepo;
                this.skuRepo = skuRepo;
                this.discountEnrichment = discountEnrichment;
                this.sellerRepo = sellerRepo;
                this.productBaseRepo = productBaseRepo;
                this.ratingRepo = ratingRepo;
                this.em = em;
        }

        @Override
        public PageResult<ListingVariantDto> loadPage(int page, int limit) {
                Page<Object[]> raw = variantRepo.findGridPage(PageRequest.of(page - 1, limit));
                return toGridResult(raw, page, limit);
        }

        @Override
        public Optional<ListingVariantDetailDto> loadBySlug(String slug) {
                return variantRepo.findDetailBySlug(slug).map(this::toDetailDto);
        }

        @Override
        public PageResult<ListingVariantDto> search(String query, int page, int limit) {
                Page<Object[]> raw = variantRepo.searchGrid(query, PageRequest.of(page - 1, limit));
                return toGridResult(raw, page, limit);
        }

        @Override
        public List<String> autocomplete(String prefix, int limit) {
                return variantRepo.autocompleteNames(prefix, PageRequest.of(0, limit));
        }

        @Override
        public PageResult<ListingVariantDto> loadByCategoryIds(List<Long> categoryIds, int page, int limit) {
                Page<Object[]> raw = variantRepo.findGridByCategoryIds(categoryIds, PageRequest.of(page - 1, limit));
                return toGridResult(raw, page, limit);
        }

        @Override
        public PageResult<ListingVariantDto> findByProductCode(String code, int page, int limit) {
                Page<Object[]> raw = variantRepo.findGridByProductCode(code, PageRequest.of(page - 1, limit));
                return toGridResult(raw, page, limit);
        }

        @Override
        public CatalogResult searchCatalog(ListingQueryCriteria criteria, int page, int limit) {
                // Reduced fidelity for discount filtering — see ListingSpecifications Javadoc.
                List<Long> discountedVariantIds = (criteria.minDiscountPercent() != null && criteria.minDiscountPercent() > 0)
                                ? discountEnrichment.findVariantIdsWithActiveDiscounts()
                                : List.of();

                Specification<ListingVariantEntity> spec = ListingSpecifications.catalogFilter(criteria, discountedVariantIds);

                // Sort.unsorted(): ordering is set inside the Specification (query.orderBy),
                // which Spring Data preserves when the Pageable carries no Sort of its own.
                Page<ListingVariantEntity> idPage = variantRepo.findAll(spec, PageRequest.of(page - 1, limit, Sort.unsorted()));

                List<Long> orderedIds = idPage.getContent().stream().map(ListingVariantEntity::getId).toList();
                List<ListingVariantDto> content = buildContent(loadGridRowsInOrder(orderedIds));

                PageResult<ListingVariantDto> pageResult = new PageResult<>(content, idPage.getTotalElements(), page, limit,
                                (long) page * limit >= idPage.getTotalElements());

                CatalogFacets facets = buildFacets(criteria, discountedVariantIds);

                return new CatalogResult(pageResult, facets);
        }

        // ---- Grid helpers ----

        private PageResult<ListingVariantDto> toGridResult(Page<Object[]> raw, int page, int limit) {
                List<ListingVariantDto> content = buildContent(raw.getContent());
                return new PageResult<>(content, raw.getTotalElements(), page, limit,
                                (long) page * limit >= raw.getTotalElements());
        }

        /** Shared row-to-DTO mapping (batch enrichment) for every grid query, including {@link #searchCatalog}. */
        private List<ListingVariantDto> buildContent(List<Object[]> rows) {
                List<Long> variantIds = rows.stream()
                                .map(row -> (Long) row[0])
                                .toList();
                List<Long> productBaseIds = rows.stream()
                                .map(row -> (Long) row[11])
                                .distinct()
                                .toList();

                Map<Long, List<String>> imageMap = ListingGridRowMapper.loadImageMap(variantIds, variantRepo);
                Map<Long, DiscountInfo> discountMap = discountEnrichment.resolveForVariants(variantIds);
                Map<Long, List<SkuDto>> skuMap = ListingGridRowMapper.loadSkuMap(variantIds, skuRepo);
                Map<Long, List<TagView>> tagMap = ListingGridRowMapper.loadTagMap(variantIds, variantRepo);
                Map<Long, ProductRatingSummaryEntity> ratingMap = ListingGridRowMapper.loadRatingMap(variantIds, ratingRepo);
                Map<Long, String> sellerMap = ListingGridRowMapper.loadSellerNameMap(productBaseIds, productBaseRepo, sellerRepo);

                return rows.stream()
                                .map(row -> ListingGridRowMapper.toGridDto(row, imageMap, discountMap, skuMap, tagMap, ratingMap, sellerMap))
                                .toList();
        }

        /**
         * Loads the 12-column grid projection for exactly the given variant IDs and
         * reorders the rows to match {@code orderedIds} — the filter/sort/pagination
         * already happened in {@link #searchCatalog}'s Specification query; this is
         * page assembly, not client-side trimming.
         */
        private List<Object[]> loadGridRowsInOrder(List<Long> orderedIds) {
                if (orderedIds.isEmpty()) return List.of();
                Page<Object[]> raw = variantRepo.findGridByVariantIds(orderedIds, PageRequest.of(0, orderedIds.size()));
                Map<Long, Object[]> byId = raw.getContent().stream()
                                .collect(Collectors.toMap(row -> (Long) row[0], row -> row));
                return orderedIds.stream()
                                .map(byId::get)
                                .filter(Objects::nonNull)
                                .toList();
        }

        // ---- Catalog facets (brand/colour/price — discount buckets are Java-only, see ListingSpecifications) ----

        private CatalogFacets buildFacets(ListingQueryCriteria criteria, List<Long> discountedVariantIds) {
                List<CatalogFacets.BrandFacet> brands = brandFacets(criteria, discountedVariantIds);
                List<CatalogFacets.ColorFacet> colors = colorFacets(criteria, discountedVariantIds);
                CatalogFacets.PriceFacet price = priceFacet(criteria, discountedVariantIds);
                return new CatalogFacets(brands, colors, price, new CatalogFacets.DiscountFacets(0, 0, 0));
        }

        private List<CatalogFacets.BrandFacet> brandFacets(ListingQueryCriteria criteria, List<Long> discountedVariantIds) {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
                Root<ListingVariantEntity> root = cq.from(ListingVariantEntity.class);
                Join<ListingVariantEntity, ProductBaseEntity> productBase = root.join("productBase", JoinType.INNER);
                Join<ProductBaseEntity, BrandEntity> brand = productBase.join("brand", JoinType.INNER);

                List<Predicate> predicates = ListingSpecifications.filterPredicates(root, cq, cb, criteria, discountedVariantIds);

                cq.multiselect(brand.get("id"), brand.get("name"), cb.countDistinct(root.get("id")))
                                .where(predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new)))
                                .groupBy(brand.get("id"), brand.get("name"));

                return em.createQuery(cq).getResultList().stream()
                                .map(row -> new CatalogFacets.BrandFacet((Long) row[0], (String) row[1], (Long) row[2]))
                                .toList();
        }

        private List<CatalogFacets.ColorFacet> colorFacets(ListingQueryCriteria criteria, List<Long> discountedVariantIds) {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
                Root<ListingVariantEntity> root = cq.from(ListingVariantEntity.class);
                Join<ListingVariantEntity, ColorEntity> color = root.join("color", JoinType.INNER);

                List<Predicate> predicates = ListingSpecifications.filterPredicates(root, cq, cb, criteria, discountedVariantIds);

                cq.multiselect(color.get("colorKey"), color.get("displayName"), color.get("hexCode"), cb.countDistinct(root.get("id")))
                                .where(predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new)))
                                .groupBy(color.get("colorKey"), color.get("displayName"), color.get("hexCode"));

                return em.createQuery(cq).getResultList().stream()
                                .map(row -> new CatalogFacets.ColorFacet((String) row[0], (String) row[1], (String) row[2], (Long) row[3]))
                                .toList();
        }

        private CatalogFacets.PriceFacet priceFacet(ListingQueryCriteria criteria, List<Long> discountedVariantIds) {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
                Root<ListingVariantEntity> root = cq.from(ListingVariantEntity.class);
                Join<ListingVariantEntity, SkuEntity> sku = root.join("skus", JoinType.INNER);

                List<Predicate> predicates = ListingSpecifications.filterPredicates(root, cq, cb, criteria, discountedVariantIds);

                cq.multiselect(cb.min(sku.get("originalPrice")), cb.max(sku.get("originalPrice")))
                                .where(predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new)));

                Object[] result = em.createQuery(cq).getSingleResult();
                BigDecimal min = (BigDecimal) result[0];
                BigDecimal max = (BigDecimal) result[1];
                return new CatalogFacets.PriceFacet(
                                min != null ? min.setScale(2, RoundingMode.HALF_UP) : null,
                                max != null ? max.setScale(2, RoundingMode.HALF_UP) : null);
        }

        // ---- Detail helpers ----

        private ListingVariantDetailDto toDetailDto(ListingVariantEntity entity) {
                List<String> images = entity.getImages().stream()
                                .map(ListingVariantImageEntity::getImageUrl)
                                .toList();

                List<AttributeDto> attributes = entity.getAttributes().stream()
                                .map(a -> new AttributeDto(
                                        a.getAttrKey(),
                                        a.getValues().stream()
                                                .map(tj.radolfa.infrastructure.persistence.entity.ListingVariantAttributeValueEntity::getValue)
                                                .toList()))
                                .toList();

                List<SkuEntity> skuEntities = skuRepo.findByListingVariantId(entity.getId());

                // Resolve discounts for all SKU codes — full pipeline (category, stacking, etc.)
                List<String> skuCodes = skuEntities.stream()
                                .map(SkuEntity::getSkuCode)
                                .toList();
                Map<String, List<AppliedDiscount>> discountsBySkuCode =
                                discountEnrichment.resolveAppliedForItemCodes(skuCodes);

                List<SkuDto> skus = skuEntities.stream()
                                .map(sku -> toSkuDto(sku, discountsBySkuCode.get(sku.getSkuCode())))
                                .toList();

                // Find the winning SKU: cheapest effective (discounted) price
                SkuEntity winningSku = null;
                BigDecimal winningEffectivePrice = null;
                for (SkuEntity sku : skuEntities) {
                        if (sku.getOriginalPrice() == null) continue;
                        List<AppliedDiscount> applied = discountsBySkuCode.get(sku.getSkuCode());
                        BigDecimal effective = applied != null
                                        ? applied.get(applied.size() - 1).reducedUnitPrice()
                                        : sku.getOriginalPrice();
                        if (winningEffectivePrice == null || effective.compareTo(winningEffectivePrice) < 0) {
                                winningEffectivePrice = effective;
                                winningSku = sku;
                        }
                }

                BigDecimal originalPrice = winningSku != null ? winningSku.getOriginalPrice() : null;
                List<AppliedDiscount> winningApplied = winningSku != null
                                ? discountsBySkuCode.get(winningSku.getSkuCode()) : null;
                BigDecimal discountPrice = null;
                Integer discountPercentage = null;
                String discountName = null;
                String discountColorHex = null;
                if (winningApplied != null && originalPrice != null) {
                        BigDecimal finalPrice = winningApplied.get(winningApplied.size() - 1).reducedUnitPrice();
                        discountPrice = finalPrice;
                        Discount winner = winningApplied.get(0).discount();
                        discountName = winner.title();
                        discountColorHex = winner.colorHex();
                        discountPercentage = BigDecimal.ONE
                                        .subtract(finalPrice.divide(originalPrice, 4, RoundingMode.HALF_UP))
                                        .multiply(BigDecimal.valueOf(100))
                                        .setScale(0, RoundingMode.HALF_UP)
                                        .intValue();
                }

                // Loyalty isn't known yet (stamped later by TierPricingEnricher); the only
                // active mechanism visible here is the campaign, if any.
                String winningSource = discountPrice != null ? "CAMPAIGN" : null;

                long discountedCount = skuEntities.stream()
                                .filter(s -> discountsBySkuCode.containsKey(s.getSkuCode()))
                                .count();
                boolean isPartialDiscount = discountedCount > 0 && discountedCount < skuEntities.size();

                // Siblings: load slugs, colorKeys, hexCodes — then batch-load thumbnails
                Long baseId = entity.getProductBase().getId();
                List<Object[]> siblingRows = variantRepo.findSiblings(baseId, entity.getId());

                List<Long> siblingIds = siblingRows.stream()
                                .map(row -> (Long) row[0])
                                .toList();
                Map<Long, List<String>> siblingImageMap = ListingGridRowMapper.loadImageMap(siblingIds, variantRepo);

                List<ListingVariantDetailDto.SiblingVariant> siblings = siblingRows.stream()
                                .map(row -> {
                                        Long sibId = (Long) row[0];
                                        List<String> sibImages = siblingImageMap.getOrDefault(sibId, List.of());
                                        String thumbnail = sibImages.isEmpty() ? null : sibImages.get(0);
                                        return new ListingVariantDetailDto.SiblingVariant(
                                                        (String) row[1],   // slug
                                                        (String) row[2],   // colorKey
                                                        (String) row[3],   // colorHex (hexCode)
                                                        thumbnail);
                                })
                                .toList();

                String categoryName = entity.getProductBase().getCategory() != null
                                ? entity.getProductBase().getCategory().getName()
                                : null;
                String colorKey = entity.getColor() != null
                                ? entity.getColor().getColorKey()
                                : null;
                String colorHex = entity.getColor() != null
                                ? entity.getColor().getHexCode()
                                : null;

                List<TagView> tags = entity.getTags().stream()
                                .map(t -> new TagView(t.getId(), t.getName(), t.getColorHex()))
                                .toList();

                List<ReviewTraitView> reviewTraits = resolveTraits(
                                entity.getProductBase() != null
                                        ? entity.getProductBase().getCategory()
                                        : null);

                // Seller attribution — snapshot for "Sold by" display on the storefront.
                // NULL sellerId = Radolfa-owned; frontend renders "Sold by Radolfa" in that case.
                Long sellerId = entity.getProductBase().getSellerId();
                String sellerShopName = sellerId != null
                        ? sellerRepo.findById(sellerId).map(s -> s.getShopName()).orElse(null)
                        : null;

                return new ListingVariantDetailDto(
                                baseId,
                                entity.getId(),
                                entity.getSlug(),
                                entity.getProductBase().getName(),
                                categoryName,
                                colorKey,
                                colorHex,
                                entity.getWebDescription(),
                                images,
                                attributes,
                                originalPrice,
                                discountPrice,
                                discountPercentage,
                                discountName,
                                discountColorHex,
                                null,              // loyaltyPrice — stamped by TierPricingEnricher
                                null,              // loyaltyPercentage — stamped by TierPricingEnricher
                                winningSource,
                                isPartialDiscount,
                                tags,
                                skus,
                                siblings,
                                entity.getProductCode(),
                                entity.getWeightKg(),
                                entity.getWidthCm(),
                                entity.getHeightCm(),
                                entity.getDepthCm(),
                                reviewTraits,
                                sellerId,
                                sellerShopName);
        }

        // ---- Trait helpers (detail-page only) ----

        /**
         * Walks the category parent chain from the leaf up and collects the union
         * of review traits from every ancestor, deduplicated by trait id.
         */
        private List<ReviewTraitView> resolveTraits(CategoryEntity leafCategory) {
                if (leafCategory == null) return List.of();
                Map<Long, ReviewTraitEntity> byId = new LinkedHashMap<>();
                CategoryEntity current = leafCategory;
                while (current != null) {
                        for (ReviewTraitEntity trait : current.getReviewTraits()) {
                                byId.putIfAbsent(trait.getId(), trait);
                        }
                        current = current.getParent();
                }
                return new ArrayList<>(byId.values()).stream()
                        .map(t -> new ReviewTraitView(t.getTraitKey(), t.getLabelI18n(), t.getInputType()))
                        .toList();
        }

        // ---- SKU helpers (detail-page only) ----

        private SkuDto toSkuDto(SkuEntity entity, List<AppliedDiscount> applied) {
                BigDecimal originalPrice = entity.getOriginalPrice();
                BigDecimal discountPrice = null;
                Integer discountPercentage = null;
                String discountName = null;
                String discountColorHex = null;

                if (applied != null && originalPrice != null) {
                        BigDecimal finalPrice = applied.get(applied.size() - 1).reducedUnitPrice();
                        discountPrice = finalPrice;
                        Discount winner = applied.get(0).discount();
                        discountName = winner.title();
                        discountColorHex = winner.colorHex();
                        discountPercentage = BigDecimal.ONE
                                        .subtract(finalPrice.divide(originalPrice, 4, RoundingMode.HALF_UP))
                                        .multiply(BigDecimal.valueOf(100))
                                        .setScale(0, RoundingMode.HALF_UP)
                                        .intValue();
                }

                return new SkuDto(
                                entity.getId(),
                                entity.getSkuCode(),
                                entity.getSizeLabel(),
                                entity.getStockQuantity(),
                                originalPrice,
                                discountPrice,
                                discountPercentage,
                                discountName,
                                discountColorHex,
                                null,  // loyaltyPrice — stamped by TierPricingEnricher via withLoyalty cascade
                                discountPrice != null ? "CAMPAIGN" : null); // winningSource — refined by withLoyalty if a tier applies
        }
}
