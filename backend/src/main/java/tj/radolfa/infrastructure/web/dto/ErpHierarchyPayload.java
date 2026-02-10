package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Rich DTO for the ERPNext hierarchy webhook.
 *
 * <p>Mirrors the ERPNext structure:
 * Template (ProductBase) → Variant (Colour) → Item (Size/SKU).
 */
public record ErpHierarchyPayload(

        @NotBlank String templateCode,
        @NotBlank String templateName,
        @NotEmpty @Valid List<VariantPayload> variants

) {
    public record VariantPayload(
            @NotBlank String colorKey,
            @NotEmpty @Valid List<ItemPayload> items
    ) {}

    public record ItemPayload(
            @NotBlank String erpItemCode,
            String sizeLabel,
            @NotNull Integer stockQuantity,
            @NotNull @Valid PricePayload price
    ) {}

    public record PricePayload(
            @NotNull BigDecimal list,
            @NotNull BigDecimal effective,
            Instant saleEndsAt
    ) {}
}
