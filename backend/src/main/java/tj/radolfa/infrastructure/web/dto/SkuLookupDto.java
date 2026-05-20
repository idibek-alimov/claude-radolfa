package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.Sku;

public record SkuLookupDto(
        Long   skuId,
        String skuCode,
        String barcode,
        String productName,
        String sizeLabel,
        int    stockQuantity,
        String binLocation
) {
    public static SkuLookupDto from(Sku sku, String productName, String binLocation) {
        return new SkuLookupDto(
                sku.getId(),
                sku.getSkuCode(),
                sku.getBarcode(),
                productName,
                sku.getSizeLabel(),
                sku.getStockQuantity() != null ? sku.getStockQuantity() : 0,
                binLocation);
    }
}
