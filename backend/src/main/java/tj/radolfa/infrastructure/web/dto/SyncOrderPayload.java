package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record SyncOrderPayload(
        @NotBlank String erpOrderId,
        @NotBlank String customerPhone,
        @NotBlank String status,
        @NotNull BigDecimal totalAmount,
        @NotNull List<ItemPayload> items) {

    public record ItemPayload(
            @NotBlank String erpItemCode,
            String productName,
            int quantity,
            @NotNull BigDecimal price) {
    }
}
