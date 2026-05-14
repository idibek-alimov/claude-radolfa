package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.*;

public record DiscountTargetResponse(
        String targetType,
        String referenceId,
        Boolean includeDescendants
) {
    public static DiscountTargetResponse fromDomain(DiscountTarget t) {
        if (t instanceof SkuTarget sku)
            return new DiscountTargetResponse("SKU", sku.itemCode(), null);
        if (t instanceof CategoryTarget cat)
            return new DiscountTargetResponse("CATEGORY", String.valueOf(cat.categoryId()), cat.includeDescendants());
        if (t instanceof SegmentTarget seg) {
            String ref = seg.referenceId() != null
                    ? seg.segment().name() + ":" + seg.referenceId()
                    : seg.segment().name();
            return new DiscountTargetResponse("SEGMENT", ref, null);
        }
        throw new IllegalArgumentException("Unknown target: " + t);
    }
}
