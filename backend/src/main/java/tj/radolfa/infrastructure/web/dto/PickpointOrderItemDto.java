package tj.radolfa.infrastructure.web.dto;

public record PickpointOrderItemDto(
        String productName,
        String skuCode,
        String sizeLabel,
        String imageUrl,
        int quantity) {}
