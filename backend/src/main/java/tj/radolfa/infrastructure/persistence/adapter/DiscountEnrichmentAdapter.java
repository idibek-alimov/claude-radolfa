package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.infrastructure.persistence.entity.DiscountEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductVariantEntity;
import tj.radolfa.infrastructure.persistence.repository.DiscountRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductVariantRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates discount resolution logic for listing/home adapters.
 *
 * <p>Resolves active discounts from the {@code discounts} table
 * (populated by ERP Pricing Rule sync) and computes effective
 * discounted prices per variant.
 */
@Component
public class DiscountEnrichmentAdapter {

    private final ProductVariantRepository variantRepo;
    private final DiscountRepository discountRepo;

    public DiscountEnrichmentAdapter(ProductVariantRepository variantRepo,
                                     DiscountRepository discountRepo) {
        this.variantRepo = variantRepo;
        this.discountRepo = discountRepo;
    }

    /**
     * For grid/card views: resolves the best discount for each variant.
     *
     * @return map of variantId → DiscountInfo (only variants with active discounts)
     */
    public Map<Long, DiscountInfo> resolveForVariants(List<Long> variantIds) {
        if (variantIds.isEmpty()) return Map.of();

        List<ProductVariantEntity> variants = variantRepo.findAllById(variantIds);
        if (variants.isEmpty()) return Map.of();

        List<String> itemCodes = variants.stream()
                .map(ProductVariantEntity::getErpVariantCode)
                .distinct()
                .toList();

        Map<String, DiscountEntity> bestByItemCode = bestDiscountByItemCode(itemCodes);
        if (bestByItemCode.isEmpty()) return Map.of();

        Map<Long, DiscountInfo> result = new HashMap<>();
        for (ProductVariantEntity variant : variants) {
            DiscountEntity discount = bestByItemCode.get(variant.getErpVariantCode());
            if (discount == null || variant.getPrice() == null) continue;

            BigDecimal discountedPrice = computeDiscountedPrice(
                    variant.getPrice(), discount.getDiscountValue());

            result.put(variant.getId(), new DiscountInfo(
                    variant.getPrice(),
                    discountedPrice,
                    discount.getDiscountValue(),
                    discount.getValidUpto(),
                    discount.getTitle(),
                    discount.getColorHex()));
        }

        return result;
    }

    /**
     * For detail views: resolves the best discount per item code.
     *
     * @return map of itemCode → best DiscountEntity (only items with active discounts)
     */
    public Map<String, DiscountEntity> resolveForItemCodes(List<String> itemCodes) {
        if (itemCodes.isEmpty()) return Map.of();
        return bestDiscountByItemCode(itemCodes);
    }

    /**
     * Returns variant IDs that have at least one active discount.
     */
    public List<Long> findVariantIdsWithActiveDiscounts() {
        List<String> activeItemCodes = discountRepo.findActiveItemCodes();
        if (activeItemCodes.isEmpty()) return List.of();

        return variantRepo.findIdsByErpVariantCodeIn(activeItemCodes);
    }

    // ---- Internal ----

    private Map<String, DiscountEntity> bestDiscountByItemCode(Collection<String> itemCodes) {
        List<Object[]> pairs = discountRepo.findActiveDiscountsByItemCodes(itemCodes);

        // Each row: [0]=DiscountEntity, [1]=matchedItemCode
        // Already ordered by discountValue DESC — first per itemCode wins
        Map<String, DiscountEntity> best = new HashMap<>();
        for (Object[] row : pairs) {
            String itemCode = (String) row[1];
            best.putIfAbsent(itemCode, (DiscountEntity) row[0]);
        }
        return best;
    }

    private BigDecimal computeDiscountedPrice(BigDecimal originalPrice, BigDecimal discountPct) {
        BigDecimal multiplier = BigDecimal.ONE.subtract(
                discountPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return originalPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    public record DiscountInfo(
            BigDecimal originalPrice,
            BigDecimal discountedPrice,
            BigDecimal discountPercentage,
            Instant validUpto,
            String saleTitle,
            String saleColorHex
    ) {}
}
