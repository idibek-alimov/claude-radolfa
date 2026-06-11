package tj.radolfa.infrastructure.persistence.adapter;

import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.application.readmodel.ListingVariantDto.TagView;
import tj.radolfa.application.readmodel.SkuDto;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.entity.ProductRatingSummaryEntity;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductBaseRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductRatingSummaryRepository;
import tj.radolfa.infrastructure.persistence.repository.SellerRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * Shared utility for mapping JPQL grid query rows ({@code Object[]})
 * to {@link ListingVariantDto} records.
 *
 * <p>Used by both {@link ListingReadAdapter} and {@link HomeCollectionsAdapter}
 * to eliminate duplicated column-index logic.
 *
 * <p>Column layout (12 columns):
 * [0]=id, [1]=slug, [2]=name, [3]=categoryName, [4]=colorKey,
 * [5]=webDescription, [6]=MIN(originalPrice), [7]=totalStock,
 * [8]=colorHexCode, [9]=productCode, [10]=MAX(originalPrice), [11]=productBaseId
 */
final class ListingGridRowMapper {

    private ListingGridRowMapper() {}

    /** Brand reference for a product base, batch-loaded by {@link #loadBrandMap}. */
    record BrandRef(Long id, String name) {}

    static ListingVariantDto toGridDto(Object[] row, Map<Long, List<String>> imageMap,
                                       Map<Long, DiscountInfo> discountMap,
                                       Map<Long, List<SkuDto>> skuMap,
                                       Map<Long, List<TagView>> tagMap,
                                       Map<Long, ProductRatingSummaryEntity> ratingMap,
                                       Map<Long, String> sellerMap,
                                       Map<Long, BrandRef> brandMap) {
        Long variantId = (Long) row[0];
        Long productBaseId = (Long) row[11];
        DiscountInfo discount = discountMap.get(variantId);
        ProductRatingSummaryEntity rating = ratingMap.get(variantId);
        BrandRef brand = brandMap.get(productBaseId);

        BigDecimal originalPrice = discount != null ? discount.originalPrice() : toBigDecimal(row[6]);
        BigDecimal discountPrice = discount != null ? discount.discountedPrice() : null;
        Integer discountPercentage = discount != null ? discount.discountPercentage().intValue() : null;
        String discountName = discount != null ? discount.saleTitle() : null;
        String discountColorHex = discount != null ? discount.saleColorHex() : null;
        boolean isPartialDiscount = discount != null && discount.isPartialDiscount();
        // Loyalty isn't known yet (stamped later by TierPricingEnricher); the only
        // active mechanism visible here is the campaign, if any.
        String winningSource = discountPrice != null ? "CAMPAIGN" : null;

        return new ListingVariantDto(
                productBaseId,
                variantId,
                (String) row[1],   // slug
                (String) row[2],   // colorDisplayName (product name)
                (String) row[3],   // categoryName
                (String) row[4],   // colorKey
                (String) row[8],   // colorHex (hexCode)
                (String) row[5],   // webDescription
                imageMap.getOrDefault(variantId, List.of()),
                originalPrice,
                discountPrice,
                discountPercentage,
                discountName,
                discountColorHex,
                null,              // loyaltyPrice — stamped by TierPricingEnricher
                null,              // loyaltyPercentage — stamped by TierPricingEnricher
                winningSource,
                isPartialDiscount,
                tagMap.getOrDefault(variantId, List.of()),
                (String) row[9],   // productCode
                skuMap.getOrDefault(variantId, List.of()),
                rating != null ? rating.getAverageRating() : null,
                rating != null ? rating.getReviewCount() : 0,
                sellerMap.get(productBaseId), // null = Radolfa-owned
                brand != null ? brand.id() : null,
                brand != null ? brand.name() : null);
    }

    /** Batch-loads a productBaseId → shopName map for the given product base IDs. */
    static Map<Long, String> loadSellerNameMap(List<Long> productBaseIds,
                                               ProductBaseRepository productBaseRepo,
                                               SellerRepository sellerRepo) {
        if (productBaseIds.isEmpty()) return Map.of();
        List<Object[]> pairs = productBaseRepo.findSellerIdsByIds(productBaseIds);
        if (pairs.isEmpty()) return Map.of();

        Map<Long, Long> baseToSeller = pairs.stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        List<Long> sellerIds = new ArrayList<>(new HashSet<>(baseToSeller.values()));
        Map<Long, String> sellerNameById = sellerRepo.findAllById(sellerIds).stream()
                .collect(Collectors.toMap(s -> s.getId(), s -> s.getShopName()));

        return baseToSeller.entrySet().stream()
                .filter(e -> sellerNameById.containsKey(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> sellerNameById.get(e.getValue())));
    }

    /** Batch-loads a productBaseId → BrandRef map for the given product base IDs. */
    static Map<Long, BrandRef> loadBrandMap(List<Long> productBaseIds,
                                             ProductBaseRepository productBaseRepo) {
        if (productBaseIds.isEmpty()) return Map.of();
        return productBaseRepo.findBrandsByIds(productBaseIds).stream()
                .collect(Collectors.toMap(r -> (Long) r[0],
                        r -> new BrandRef((Long) r[1], (String) r[2])));
    }

    static Map<Long, ProductRatingSummaryEntity> loadRatingMap(List<Long> variantIds,
                                                                ProductRatingSummaryRepository ratingRepo) {
        if (variantIds.isEmpty()) return Map.of();
        return ratingRepo.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductRatingSummaryEntity::getListingVariantId, Function.identity()));
    }

    static Map<Long, List<String>> loadImageMap(List<Long> variantIds,
                                                 ListingVariantRepository variantRepo) {
        if (variantIds.isEmpty()) return Map.of();
        return variantRepo.findImagesByVariantIds(variantIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())));
    }

    static Map<Long, List<TagView>> loadTagMap(List<Long> variantIds,
                                                ListingVariantRepository variantRepo) {
        if (variantIds.isEmpty()) return Map.of();
        return variantRepo.findTagsByVariantIds(variantIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(
                                row -> new TagView((Long) row[1], (String) row[2], (String) row[3]),
                                Collectors.toList())));
    }

    static Map<Long, List<SkuDto>> loadSkuMap(List<Long> variantIds,
                                               SkuRepository skuRepo) {
        if (variantIds.isEmpty()) return Map.of();
        return skuRepo.findGridSkusByVariantIds(variantIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> new SkuDto(
                                (Long) row[1],
                                (String) row[2],
                                (String) row[3],
                                toInteger(row[4]),
                                toBigDecimal(row[5]), // originalPrice
                                null,  // discountPrice — not resolved for grid-path SKUs
                                null,  // discountPercentage
                                null,  // discountName
                                null,  // discountColorHex
                                null,  // loyaltyPrice — stamped by TierPricingEnricher
                                null   // winningSource — stamped by TierPricingEnricher
                        ), Collectors.toList())));
    }

    static BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    static Integer toInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Long l) return l.intValue();
        if (value instanceof Integer i) return i;
        return ((Number) value).intValue();
    }
}
