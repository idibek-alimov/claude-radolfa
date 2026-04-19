package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tj.radolfa.domain.model.*;

public record DiscountTargetInput(
        @NotNull String targetType,
        @NotBlank String referenceId,
        Boolean includeDescendants
) {
    public DiscountTarget toDomain() {
        return switch (targetType) {
            case "SKU" -> new SkuTarget(referenceId);
            case "CATEGORY" -> new CategoryTarget(
                    Long.parseLong(referenceId),
                    Boolean.TRUE.equals(includeDescendants));
            case "SEGMENT" -> {
                String[] parts = referenceId.split(":", 2);
                yield new SegmentTarget(
                        Segment.valueOf(parts[0]),
                        parts.length > 1 ? parts[1] : null);
            }
            default -> throw new IllegalArgumentException("Unknown targetType: " + targetType);
        };
    }
}
