package tj.radolfa.domain.model;

public record InventoryPlacement(
        Long id,
        Long skuId,
        Long warehouseId,
        Long binId,       // null = inbound pool (received but not yet assigned to a bin)
        int quantity) {}
