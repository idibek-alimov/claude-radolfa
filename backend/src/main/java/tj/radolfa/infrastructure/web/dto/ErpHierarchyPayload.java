package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO for the ERPNext product sync webhook.
 *
 * <p>Flat 2-layer structure:
 * Template → Variants (each with concrete attribute values).
 */
public record ErpHierarchyPayload(

        @NotBlank String templateCode,
        @NotBlank String templateName,
        String category,
        boolean disabled,
        @Valid List<VariantPayload> variants

) {
    public record VariantPayload(
            @NotBlank String erpVariantCode,
            Map<String, String> attributes,
            @NotNull BigDecimal price,
            @NotNull Integer stockQty,
            boolean disabled
    ) {}
}
