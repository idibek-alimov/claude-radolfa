package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.infrastructure.persistence.entity.DiscountEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.repository.DiscountRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Encapsulates discount resolution logic for listing/home adapters.
 *
 * <p>Resolves active discounts from the {@code discounts} table
 * (populated by ERP Pricing Rule sync) and computes effective
 * discounted prices per variant or per item code.
 */
@Component
public class DiscountEnrichmentAdapter {

    private final SkuRepository skuRepo;
    private final DiscountRepository discountRepo;

    public DiscountEnrichmentAdapter(SkuRepository skuRepo, DiscountRepository discountRepo) {
        this.skuRepo = skuRepo;
        this.discountRepo = discountRepo;
    }

    /**
     * For grid/card views: resolves the best discount for each variant.
     * Picks the SKU with the cheapest effective discounted price per variant.
     *
     * @return map of variantId → DiscountInfo (only variants with active discounts)
     */
    public Map<Long, DiscountInfo> resolveForVariants(List<Long> variantIds) {
        if (variantIds.isEmpty()) return Map.of();

        List<SkuEntity> allSkus = skuRepo.findByListingVariantIdIn(variantIds);
        if (allSkus.isEmpty()) return Map.of();

        List<String> itemCodes = allSkus.stream()
                .map(SkuEntity::getErpItemCode)
                .distinct()
                .toList();

        Map<String, DiscountEntity> bestByItemCode = bestDiscountByItemCode(itemCodes);
        if (bestByItemCode.isEmpty()) return Map.of();

        // Group SKUs by variant
        Map<Long, List<SkuEntity>> skusByVariant = allSkus.stream()
                .collect(Collectors.groupingBy(s -> s.getListingVariant().getId()));

        Map<Long, DiscountInfo> result = new HashMap<>();
        for (var entry : skusByVariant.entrySet()) {
            Long variantId = entry.getKey();
            DiscountInfo best = null;

            for (SkuEntity sku : entry.getValue()) {
                DiscountEntity discount = bestByItemCode.get(sku.getErpItemCode());
                if (discount == null || sku.getOriginalPrice() == null) continue;

                BigDecimal discountedPrice = computeDiscountedPrice(
                        sku.getOriginalPrice(), discount.getDiscountValue());

                if (best == null || discountedPrice.compareTo(best.discountedPrice()) < 0) {
                    best = new DiscountInfo(
                            sku.getOriginalPrice(),
                            discountedPrice,
                            discount.getDiscountValue(),
                            discount.getValidUpto(),
                            discount.getTitle(),
                            discount.getColorHex());
                }
            }

            if (best != null) {
                result.put(variantId, best);
            }
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
     * Returns variant IDs that have at least one SKU with an active discount.
     * Uses indexed DB queries — no full table scans.
     */
    public List<Long> findVariantIdsWithActiveDiscounts() {
        List<String> activeItemCodes = discountRepo.findActiveItemCodes();
        if (activeItemCodes.isEmpty()) return List.of();

        return skuRepo.findVariantIdsByItemCodes(activeItemCodes);
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
