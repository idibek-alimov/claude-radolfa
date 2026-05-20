package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateStockReceiptRequestDto(
        String supplierReference,
        @Size(max = 1000) String notes,
        @NotEmpty List<ReceiptItemRequest> items
) {
    public record ReceiptItemRequest(
            @NotNull Long skuId,
            @Min(1) int quantity,
            String notes
    ) {}
}
