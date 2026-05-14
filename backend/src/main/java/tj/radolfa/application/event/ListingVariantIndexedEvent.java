package tj.radolfa.application.event;

import java.time.Instant;
import java.util.List;

public record ListingVariantIndexedEvent(
        Long variantId,
        Long productBaseId,
        String slug,
        String name,
        String category,
        String colorKey,
        String colorHexCode,
        String description,
        List<String> images,
        Double price,
        Integer totalStock,
        Instant lastSyncAt,
        String productCode,
        List<String> skuCodes
) {}
