package tj.radolfa.infrastructure.persistence.mappers;

import org.springframework.stereotype.Component;
import tj.radolfa.domain.model.*;
import tj.radolfa.infrastructure.persistence.entity.DiscountEntity;
import tj.radolfa.infrastructure.persistence.entity.DiscountTargetEntity;

@Component
public class DiscountTargetMapper {

    public DiscountTarget toDomain(DiscountTargetEntity entity) {
        return switch (entity.getTargetType()) {
            case "SKU" -> new SkuTarget(entity.getReferenceId());
            case "CATEGORY" -> new CategoryTarget(Long.parseLong(entity.getReferenceId()), entity.isIncludeDescendants());
            case "SEGMENT" -> {
                String[] parts = entity.getReferenceId().split(":", 2);
                Segment seg = Segment.valueOf(parts[0]);
                String ref = parts.length > 1 ? parts[1] : null;
                yield new SegmentTarget(seg, ref);
            }
            default -> throw new IllegalStateException("Unknown target_type: " + entity.getTargetType());
        };
    }

    public DiscountTargetEntity toEntity(DiscountTarget target, DiscountEntity parent) {
        DiscountTargetEntity e = new DiscountTargetEntity();
        e.setDiscount(parent);
        if (target instanceof SkuTarget sku) {
            e.setTargetType("SKU");
            e.setReferenceId(sku.itemCode());
            e.setIncludeDescendants(false);
        } else if (target instanceof CategoryTarget cat) {
            e.setTargetType("CATEGORY");
            e.setReferenceId(String.valueOf(cat.categoryId()));
            e.setIncludeDescendants(cat.includeDescendants());
        } else if (target instanceof SegmentTarget seg) {
            e.setTargetType("SEGMENT");
            String ref = seg.referenceId() != null
                    ? seg.segment().name() + ":" + seg.referenceId()
                    : seg.segment().name();
            e.setReferenceId(ref);
            e.setIncludeDescendants(false);
        }
        return e;
    }
}
