package tj.radolfa.domain.model;

public record SkuSearchRow(
        Long   skuId,
        String skuCode,
        String barcode,
        String sizeLabel,
        int    stockQuantity,
        String productName,
        String binLocation
) {}
