package tj.radolfa.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Maps native grid query rows ({@code Object[]}) to {@link ListingVariantDto}.
 *
 * <p>Column layout [0..11]:
 *   [0]=variant_id, [1]=seo_slug, [2]=template name, [3]=category_name,
 *   [4]=min_price, [5]=total_stock, [6]=description, [7]=top_selling,
 *   [8]=featured, [9]=template_id, [10]=color_key, [11]=images (jsonb from product_color_images)
 */
final class ListingGridRowMapper {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private ListingGridRowMapper() {}

    static ListingVariantDto toGridDto(Object[] row,
                                       Map<Long, DiscountInfo> discountMap) {
        Long id = toLong(row[0]);
        DiscountInfo discount = discountMap.get(id);

        BigDecimal originalPrice = discount != null
                ? discount.originalPrice()
                : toBigDecimal(row[4]);

        List<String> images = parseImages(row[11]);

        return new ListingVariantDto(
                id,
                (String) row[1],           // slug
                (String) row[2],           // name
                (String) row[3],           // category
                (String) row[10],          // colorKey
                null,                      // colorHexCode (not stored in new model)
                (String) row[6],           // description
                images,
                originalPrice,
                discount != null ? discount.discountedPrice() : null,
                null,                      // loyaltyPrice (enriched by controller)
                discount != null ? discount.discountPercentage() : null,
                null,                      // loyaltyDiscountPercentage
                discount != null ? discount.saleTitle() : null,
                discount != null ? discount.saleColorHex() : null,
                toInteger(row[5]),         // totalStock
                toBoolean(row[7]),         // topSelling
                toBoolean(row[8]));        // featured
    }

    static List<String> parseImages(Object jsonValue) {
        if (jsonValue == null) return List.of();
        try {
            if (jsonValue instanceof String s) {
                return JSON.readValue(s, STRING_LIST);
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
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

    static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        return ((Number) value).longValue();
    }

    static boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }
}
