package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.PlacementView;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.SkuSearchRow;

import java.util.List;

public record SkuLookupDto(
        Long   skuId,
        String skuCode,
        String barcode,
        String productName,
        String sizeLabel,
        int    stockQuantity,
        List<PlacementDto> placements
) {
    public static SkuLookupDto from(SkuSearchRow row) {
        return new SkuLookupDto(
                row.skuId(), row.skuCode(), row.barcode(),
                row.productName(), row.sizeLabel(), row.stockQuantity(),
                row.placements().stream().map(PlacementDto::from).toList());
    }

    public static SkuLookupDto from(Sku sku, String productName, List<PlacementView> placements) {
        return new SkuLookupDto(
                sku.getId(),
                sku.getSkuCode(),
                sku.getBarcode(),
                productName,
                sku.getSizeLabel(),
                sku.getStockQuantity() != null ? sku.getStockQuantity() : 0,
                placements.stream().map(PlacementDto::from).toList());
    }
}
