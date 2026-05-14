package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.ExpandCategoryTargetPort;
import tj.radolfa.domain.model.AppliedDiscount;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.repository.DiscountRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;
import tj.radolfa.infrastructure.web.DiscountResolutionContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encapsulates discount resolution logic for listing/home adapters.
 *
 * <p>Both grid and detail paths now delegate to {@link DiscountResolutionContext} so that
 * category targets, segment targets, stacking, and usage caps apply uniformly.
 */
@Component
public class DiscountEnrichmentAdapter {

    private final SkuRepository skuRepo;
    private final DiscountRepository discountRepo;
    private final ExpandCategoryTargetPort expandCategoryTargetPort;
    private final ObjectProvider<DiscountResolutionContext> resolutionCtx;

    public DiscountEnrichmentAdapter(SkuRepository skuRepo,
                                     DiscountRepository discountRepo,
                                     ExpandCategoryTargetPort expandCategoryTargetPort,
                                     ObjectProvider<DiscountResolutionContext> resolutionCtx) {
        this.skuRepo                  = skuRepo;
        this.discountRepo             = discountRepo;
        this.expandCategoryTargetPort = expandCategoryTargetPort;
        this.resolutionCtx            = resolutionCtx;
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
     * For detail views: resolves the full stacked discount list per item code via the
     * complete resolution pipeline (category expansion, segment gates, stacking).
     *
     * @return map of itemCode → ordered AppliedDiscount list (only items with active discounts)
     */
    public Map<String, List<AppliedDiscount>> resolveAppliedForItemCodes(List<String> itemCodes) {
        if (itemCodes.isEmpty()) return Map.of();
        return resolutionCtx.getObject().resolveForListing(itemCodes);
    }

    /**
     * Returns variant IDs that have at least one SKU covered by an active discount,
     * including products covered via category targets (not only explicit SKU targets).
     */
    public List<Long> findVariantIdsWithActiveDiscounts() {
        Set<String> allItemCodes = new HashSet<>(discountRepo.findActiveItemCodes());

        // Also expand active category targets so category-sale products get the Sale badge.
        List<Object[]> categoryRefs = discountRepo.findActiveCategoryTargetRefs();
        if (!categoryRefs.isEmpty()) {
            Map<Long, Boolean> categoryMap = new HashMap<>();
            for (Object[] row : categoryRefs) {
                Long categoryId = Long.parseLong((String) row[0]);
                boolean includeDescendants = (Boolean) row[1];
                categoryMap.merge(categoryId, includeDescendants, (a, b) -> a || b);
            }
            allItemCodes.addAll(expandCategoryTargetPort.resolveSkuCodes(categoryMap));
        }

        if (allItemCodes.isEmpty()) return List.of();
        return new ArrayList<>(skuRepo.findVariantIdsByItemCodes(allItemCodes));
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
