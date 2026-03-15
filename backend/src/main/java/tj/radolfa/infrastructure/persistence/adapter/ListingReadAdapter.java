package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadListingPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantImageEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;
import tj.radolfa.infrastructure.web.dto.ListingVariantDetailDto;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;
import tj.radolfa.infrastructure.web.dto.SkuDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
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
 * Images are batch-loaded in a second query for the page.
 */
@Component
public class ListingReadAdapter implements LoadListingPort {

        private final ListingVariantRepository variantRepo;
        private final SkuRepository skuRepo;

        public ListingReadAdapter(ListingVariantRepository variantRepo,
                        SkuRepository skuRepo) {
                this.variantRepo = variantRepo;
                this.skuRepo = skuRepo;
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

        // ---- Grid helpers ----

        private PageResult<ListingVariantDto> toGridResult(Page<Object[]> raw, int page, int limit) {
                List<Long> variantIds = raw.getContent().stream()
                                .map(row -> (Long) row[0])
                                .toList();

                Map<Long, List<String>> imageMap = loadImageMap(variantIds);

                List<ListingVariantDto> items = raw.getContent().stream()
                                .map(row -> toGridDto(row, imageMap))
                                .toList();

                return new PageResult<>(items, raw.getTotalElements(), page,
                                (long) page * limit < raw.getTotalElements());
        }

        // Column layout: [0]=id, [1]=slug, [2]=name, [3]=categoryName, [4]=colorKey,
        //                 [5]=webDescription, [6]=topSelling,
        //                 [7]=originalPrice, [8]=discountedPrice (expiry-filtered),
        //                 [9]=totalStock, [10]=colorHexCode, [11]=featured,
        //                 [12]=discountPercentage (expiry-filtered)
        private ListingVariantDto toGridDto(Object[] row, Map<Long, List<String>> imageMap) {
                Long id = (Long) row[0];
                return new ListingVariantDto(
                                id,
                                (String) row[1],   // slug
                                (String) row[2],   // name
                                (String) row[3],   // category
                                (String) row[4],   // colorKey
                                (String) row[10],  // colorHexCode
                                (String) row[5],   // webDescription
                                imageMap.getOrDefault(id, List.of()),
                                toBigDecimal(row[7]),  // originalPrice
                                toBigDecimal(row[8]),  // discountedPrice (already expiry-filtered by JPQL)
                                null,                  // loyaltyPrice (enriched by controller)
                                toBigDecimal(row[12]), // discountPercentage (expiry-filtered by JPQL)
                                null,                  // loyaltyDiscountPercentage (enriched by controller)
                                toInteger(row[9]),     // totalStock
                                (Boolean) row[6],      // topSelling
                                (Boolean) row[11]);    // featured
        }

        // ---- Detail helpers ----

        private ListingVariantDetailDto toDetailDto(ListingVariantEntity entity) {
                List<String> images = entity.getImages().stream()
                                .map(ListingVariantImageEntity::getImageUrl)
                                .toList();

                List<SkuEntity> skuEntities = skuRepo.findByListingVariantId(entity.getId());
                Instant now = Instant.now();

                List<SkuDto> skus = skuEntities.stream()
                                .map(sku -> toSkuDto(sku, now))
                                .toList();

                BigDecimal originalPrice = skuEntities.stream()
                                .map(SkuEntity::getOriginalPrice)
                                .filter(Objects::nonNull)
                                .min(BigDecimal::compareTo)
                                .orElse(null);

                // Derive both values from the SAME SKU to keep price and badge% consistent
                SkuEntity cheapestDiscounted = skuEntities.stream()
                                .filter(s -> isDiscountActive(s, now))
                                .min(Comparator.comparing(SkuEntity::getDiscountedPrice))
                                .orElse(null);

                BigDecimal discountedPrice = cheapestDiscounted != null
                                ? cheapestDiscounted.getDiscountedPrice() : null;
                BigDecimal discountPercentage = cheapestDiscounted != null
                                ? cheapestDiscounted.getDiscountPercentage() : null;

                int totalStock = skuEntities.stream()
                                .mapToInt(s -> s.getStockQuantity() != null ? s.getStockQuantity() : 0)
                                .sum();

                // Siblings: load IDs, slugs, colorKeys, hexCodes — then batch-load thumbnails
                Long baseId = entity.getProductBase().getId();
                List<Object[]> siblingRows = variantRepo.findSiblings(baseId, entity.getId());

                List<Long> siblingIds = siblingRows.stream()
                                .map(row -> (Long) row[0])
                                .toList();
                Map<Long, List<String>> siblingImageMap = loadImageMap(siblingIds);

                List<ListingVariantDetailDto.SiblingVariant> siblings = siblingRows.stream()
                                .map(row -> {
                                        Long sibId = (Long) row[0];
                                        List<String> sibImages = siblingImageMap.getOrDefault(sibId, List.of());
                                        String thumbnail = sibImages.isEmpty() ? null : sibImages.get(0);
                                        return new ListingVariantDetailDto.SiblingVariant(
                                                        (String) row[1],   // slug
                                                        (String) row[2],   // colorKey
                                                        (String) row[3],   // colorHexCode
                                                        thumbnail);
                                })
                                .toList();

                String categoryName = entity.getProductBase().getCategory() != null
                                ? entity.getProductBase().getCategory().getName()
                                : null;
                String colorKey = entity.getColor() != null
                                ? entity.getColor().getColorKey()
                                : null;
                String colorHexCode = entity.getColor() != null
                                ? entity.getColor().getHexCode()
                                : null;

                return new ListingVariantDetailDto(
                                entity.getId(),
                                entity.getSlug(),
                                entity.getProductBase().getName(),
                                categoryName,
                                colorKey,
                                colorHexCode,
                                entity.getWebDescription(),
                                images,
                                originalPrice,
                                discountedPrice,
                                null,              // loyaltyPrice (enriched by controller)
                                discountPercentage,
                                null,              // loyaltyDiscountPercentage (enriched by controller)
                                totalStock,
                                entity.isTopSelling(),
                                entity.isFeatured(),
                                skus,
                                siblings);
        }

        // ---- Shared helpers ----

        private boolean isDiscountActive(SkuEntity sku, Instant now) {
                return sku.getDiscountedPrice() != null
                        && sku.getOriginalPrice() != null
                        && sku.getDiscountedPrice().compareTo(sku.getOriginalPrice()) < 0
                        && (sku.getDiscountedEndsAt() == null || now.isBefore(sku.getDiscountedEndsAt()));
        }

        private Map<Long, List<String>> loadImageMap(List<Long> variantIds) {
                if (variantIds.isEmpty())
                        return Map.of();
                return variantRepo.findImagesByVariantIds(variantIds).stream()
                                .collect(Collectors.groupingBy(
                                                row -> (Long) row[0],
                                                Collectors.mapping(row -> (String) row[1], Collectors.toList())));
        }

        private SkuDto toSkuDto(SkuEntity entity, Instant now) {
                boolean onSale = isDiscountActive(entity, now);

                // Null out discountedPrice and discountPercentage if expired
                BigDecimal effectiveDiscounted = onSale ? entity.getDiscountedPrice() : null;
                BigDecimal effectiveDiscountPct = onSale ? entity.getDiscountPercentage() : null;

                return new SkuDto(
                                entity.getId(),
                                entity.getErpItemCode(),
                                entity.getSizeLabel(),
                                entity.getStockQuantity(),
                                entity.getOriginalPrice(),
                                effectiveDiscounted,
                                null,               // loyaltyPrice (enriched by controller)
                                effectiveDiscountPct,
                                null,               // loyaltyDiscountPercentage (enriched by controller)
                                onSale,
                                onSale ? entity.getDiscountedEndsAt() : null);  // don't expose expired timestamp
        }

        private BigDecimal toBigDecimal(Object value) {
                if (value == null)
                        return null;
                if (value instanceof BigDecimal bd)
                        return bd;
                return new BigDecimal(value.toString());
        }

        private Integer toInteger(Object value) {
                if (value == null)
                        return 0;
                if (value instanceof Long l)
                        return l.intValue();
                if (value instanceof Integer i)
                        return i;
                return ((Number) value).intValue();
        }
}
