package tj.radolfa.infrastructure.persistence.adapter;

import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.application.readmodel.SkuDto;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared utility for mapping JPQL grid query rows ({@code Object[]})
 * to {@link ListingVariantDto} records.
 *
 * <p>Used by both {@link ListingReadAdapter} and {@link HomeCollectionsAdapter}
 * to eliminate duplicated column-index logic.
 *
 * <p>Column layout (13 columns):
 * [0]=id, [1]=slug, [2]=name, [3]=categoryName, [4]=colorKey,
 * [5]=webDescription, [6]=topSelling, [7]=MIN(originalPrice),
 * [8]=totalStock, [9]=colorHexCode, [10]=featured, [11]=productCode,
 * [12]=MAX(originalPrice)
 */
final class ListingGridRowMapper {

    private ListingGridRowMapper() {}

    static ListingVariantDto toGridDto(Object[] row, Map<Long, List<String>> imageMap,
                                       Map<Long, DiscountInfo> discountMap,
                                       Map<Long, List<SkuDto>> skuMap) {
        Long variantId = (Long) row[0];
        DiscountInfo discount = discountMap.get(variantId);

        BigDecimal originalPrice = discount != null ? discount.originalPrice() : toBigDecimal(row[7]);
        BigDecimal discountPrice = discount != null ? discount.discountedPrice() : null;
        Integer discountPercentage = discount != null ? discount.discountPercentage().intValue() : null;
        String discountName = discount != null ? discount.saleTitle() : null;
        String discountColorHex = discount != null ? discount.saleColorHex() : null;
        boolean isPartialDiscount = discount != null && discount.isPartialDiscount();

        return new ListingVariantDto(
                variantId,
                (String) row[1],   // slug
                (String) row[2],   // colorDisplayName (product name)
                (String) row[3],   // categoryName
                (String) row[4],   // colorKey
                (String) row[9],   // colorHex (hexCode)
                (String) row[5],   // webDescription
                imageMap.getOrDefault(variantId, List.of()),
                originalPrice,
                discountPrice,
                discountPercentage,
                discountName,
                discountColorHex,
                null,              // loyaltyPrice — stamped by TierPricingEnricher
                null,              // loyaltyPercentage — stamped by TierPricingEnricher
                isPartialDiscount,
                (Boolean) row[6],  // topSelling
                (Boolean) row[10], // featured
                (String) row[11],  // productCode
                skuMap.getOrDefault(variantId, List.of()));
    }

    static Map<Long, List<String>> loadImageMap(List<Long> variantIds,
                                                 ListingVariantRepository variantRepo) {
        if (variantIds.isEmpty()) return Map.of();
        return variantRepo.findImagesByVariantIds(variantIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())));
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
                                null   // loyaltyPrice — stamped by TierPricingEnricher
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
