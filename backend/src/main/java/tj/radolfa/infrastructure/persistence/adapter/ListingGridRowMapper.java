package tj.radolfa.infrastructure.persistence.adapter;

import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;

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
 * <p>Column layout (12 columns):
 * [0]=id, [1]=slug, [2]=name, [3]=categoryName, [4]=colorKey,
 * [5]=webDescription, [6]=topSelling, [7]=MIN(originalPrice),
 * [8]=totalStock, [9]=colorHexCode, [10]=featured, [11]=productCode
 */
final class ListingGridRowMapper {

    private ListingGridRowMapper() {}

    static ListingVariantDto toGridDto(Object[] row, Map<Long, List<String>> imageMap,
                                       Map<Long, DiscountInfo> discountMap) {
        Long id = (Long) row[0];
        DiscountInfo discount = discountMap.get(id);

        BigDecimal originalPrice = discount != null
                ? discount.originalPrice()
                : toBigDecimal(row[7]);

        return new ListingVariantDto(
                id,
                (String) row[1],   // slug
                (String) row[2],   // name
                (String) row[3],   // category
                (String) row[4],   // colorKey
                (String) row[9],   // colorHexCode
                (String) row[5],   // webDescription
                imageMap.getOrDefault(id, List.of()),
                originalPrice,
                discount != null ? discount.discountedPrice() : null,
                null,                                              // loyaltyPrice (enriched by controller)
                discount != null ? discount.discountPercentage() : null,
                null,                                              // loyaltyDiscountPercentage (enriched by controller)
                discount != null ? discount.saleTitle() : null,
                discount != null ? discount.saleColorHex() : null,
                toInteger(row[8]),     // totalStock
                (Boolean) row[6],      // topSelling
                (Boolean) row[10],     // featured
                (String) row[11]);     // productCode
    }

    static Map<Long, List<String>> loadImageMap(List<Long> variantIds,
                                                 ListingVariantRepository variantRepo) {
        if (variantIds.isEmpty()) return Map.of();
        return variantRepo.findImagesByVariantIds(variantIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())));
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
