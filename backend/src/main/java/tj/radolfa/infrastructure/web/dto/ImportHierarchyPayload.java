package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rich DTO for the product hierarchy import webhook.
 *
 * <p>Mirrors the external catalogue structure:
 * Template (ProductBase) → Variant (Colour) → Item (Size/SKU).
 */
public record ImportHierarchyPayload(

        @NotBlank String templateCode,
        @NotBlank String templateName,
        String category,
        @NotEmpty @Valid List<VariantPayload> variants

) {
    public record VariantPayload(
            @NotBlank String colorKey,
            @NotEmpty @Valid List<ItemPayload> items
    ) {}

    public record ItemPayload(
            @NotBlank String skuCode,
            String sizeLabel,
            @NotNull Integer stockQuantity,
            @NotNull BigDecimal listPrice
    ) {}
}
