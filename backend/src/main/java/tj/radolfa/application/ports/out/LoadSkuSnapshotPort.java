package tj.radolfa.application.ports.out;

import java.math.BigDecimal;

public interface LoadSkuSnapshotPort {

    /**
     * Snapshot of a SKU's display data at the moment it is added to a cart.
     * Price is sourced from ERPNext (the sole source of truth) and must not be
     * overridden by any Radolfa-owned enrichment layer.
     */
    record SkuSnapshot(
            Long skuId,
            String listingSlug,
            String productName,
            String sizeLabel,
            String imageUrl,
            BigDecimal price) {
    }

    /**
     * Loads the snapshot data needed to create a cart line item for the given SKU.
     *
     * @throws IllegalArgumentException if no SKU exists for the given id
     */
    SkuSnapshot load(Long skuId);
}
