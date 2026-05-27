package tj.radolfa.domain.model;

import java.util.List;

public record SkuSearchRow(
        Long   skuId,
        String skuCode,
        String barcode,
        String sizeLabel,
        int    stockQuantity,
        String productName,
        List<PlacementView> placements
) {}
