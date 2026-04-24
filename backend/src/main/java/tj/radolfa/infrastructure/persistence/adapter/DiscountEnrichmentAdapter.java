package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tj.radolfa.domain.model.AppliedDiscount;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.infrastructure.persistence.entity.DiscountEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.repository.DiscountRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;
import tj.radolfa.infrastructure.web.DiscountResolutionContext;

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
 * <p>{@link #resolveForVariants} delegates to {@link DiscountResolutionContext} so that
 * category targets, segment targets, stacking, and usage caps all apply on product grids.
 * {@link #resolveForItemCodes} remains on the legacy direct-DB path for detail views.
 */
@Component
public class DiscountEnrichmentAdapter {

    private final SkuRepository skuRepo;
    private final DiscountRepository discountRepo;
    private final ObjectProvider<DiscountResolutionContext> resolutionCtx;

    public DiscountEnrichmentAdapter(SkuRepository skuRepo,
                                     DiscountRepository discountRepo,
                                     ObjectProvider<DiscountResolutionContext> resolutionCtx) {
        this.skuRepo       = skuRepo;
        this.discountRepo  = discountRepo;
        this.resolutionCtx = resolutionCtx;
    }

    /**
     * For grid/card views: resolves the best (stacked) discount for each variant.
     * Picks the SKU with the cheapest final price per variant.
     *
     * @return map of variantId → DiscountInfo (only variants with active discounts)
     */
    public Map<Long, DiscountInfo> resolveForVariants(List<Long> variantIds) {
        if (variantIds.isEmpty()) return Map.of();

        List<SkuEntity> allSkus = skuRepo.findByListingVariantIdIn(variantIds);
        if (allSkus.isEmpty()) return Map.of();

        List<String> itemCodes = allSkus.stream()
                .map(SkuEntity::getSkuCode)
                .distinct()
                .toList();

        Map<String, List<AppliedDiscount>> resolved =
                resolutionCtx.getObject().resolveForListing(itemCodes);
        if (resolved.isEmpty()) return Map.of();

        Map<Long, List<SkuEntity>> skusByVariant = allSkus.stream()
                .collect(Collectors.groupingBy(s -> s.getListingVariant().getId()));

        Map<Long, DiscountInfo> result = new HashMap<>();
        for (var entry : skusByVariant.entrySet()) {
            Long variantId = entry.getKey();
            List<SkuEntity> variantSkus = entry.getValue();

            List<SkuEntity> pricedSkus = variantSkus.stream()
                    .filter(s -> s.getOriginalPrice() != null)
                    .toList();
            long discountedCount = pricedSkus.stream()
                    .filter(s -> resolved.containsKey(s.getSkuCode()))
                    .count();

            DiscountInfo best = null;
            for (SkuEntity sku : variantSkus) {
                List<AppliedDiscount> applied = resolved.get(sku.getSkuCode());
                BigDecimal original = sku.getOriginalPrice();
                if (applied == null || original == null) continue;

                BigDecimal finalPrice = applied.get(applied.size() - 1).reducedUnitPrice();
                Discount winner = applied.get(0).discount();

                if (best == null || finalPrice.compareTo(best.discountedPrice()) < 0) {
                    BigDecimal stackedPct = BigDecimal.ONE
                            .subtract(finalPrice.divide(original, 4, RoundingMode.HALF_UP))
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP);
                    best = new DiscountInfo(
                            original, finalPrice, stackedPct,
                            winner.validUpto(), winner.title(), winner.colorHex(),
                            winner.type().name(), false);
                }
            }

            if (best != null) {
                boolean isPartial = discountedCount < pricedSkus.size();
                result.put(variantId, new DiscountInfo(
                        best.originalPrice(), best.discountedPrice(), best.discountPercentage(),
                        best.validUpto(), best.saleTitle(), best.saleColorHex(), best.typeName(),
                        isPartial));
            }
        }

        return result;
    }

    /**
     * For detail views: resolves the best discount per item code (legacy direct-DB path).
     * Does not apply category/segment expansion or stacking — winner by type rank only.
     *
     * @return map of itemCode → best DiscountEntity (only items with active discounts)
     */
    public Map<String, DiscountEntity> resolveForItemCodes(List<String> itemCodes) {
        if (itemCodes.isEmpty()) return Map.of();
        return bestDiscountByItemCode(itemCodes);
    }

    /**
     * Returns variant IDs that have at least one SKU with an active discount.
     */
    public List<Long> findVariantIdsWithActiveDiscounts() {
        List<String> activeItemCodes = discountRepo.findActiveItemCodes();
        if (activeItemCodes.isEmpty()) return List.of();
        return skuRepo.findVariantIdsByItemCodes(activeItemCodes);
    }

    // ---- Internal (legacy path) ----

    private Map<String, DiscountEntity> bestDiscountByItemCode(Collection<String> itemCodes) {
        List<Object[]> pairs = discountRepo.findActiveDiscountsByItemCodes(itemCodes);
        Map<String, DiscountEntity> best = new HashMap<>();
        for (Object[] row : pairs) {
            String itemCode = (String) row[1];
            best.putIfAbsent(itemCode, (DiscountEntity) row[0]);
        }
        return best;
    }

    public record DiscountInfo(
            BigDecimal originalPrice,
            BigDecimal discountedPrice,
            BigDecimal discountPercentage,
            Instant validUpto,
            String saleTitle,
            String saleColorHex,
            String typeName,
            boolean isPartialDiscount
    ) {}
}
