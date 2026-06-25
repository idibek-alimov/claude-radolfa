package tj.radolfa.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.DiscountSnapshotPort;
import tj.radolfa.domain.model.CategoryTarget;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountTarget;
import tj.radolfa.domain.model.SegmentTarget;
import tj.radolfa.domain.model.SkuTarget;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes the price-relevant slice of a {@link Discount} (not the full record) into a
 * compact JSON snapshot for the {@code discount_changes} audit ledger. Keeping the snapshot
 * narrow means the ledger's shape doesn't shift every time {@code Discount} gains an unrelated
 * field (title, coupon code, etc).
 */
@Component
public class DiscountSnapshotJpaAdapter implements DiscountSnapshotPort {

    private final ObjectMapper objectMapper;

    public DiscountSnapshotJpaAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String toJson(Discount discount) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("amountType", discount.amountType().name());
        snapshot.put("amountValue", discount.amountValue());
        snapshot.put("validFrom", discount.validFrom().toString());
        snapshot.put("validUpto", discount.validUpto().toString());
        snapshot.put("disabled", discount.disabled());
        snapshot.put("targets", targetsToMaps(discount.targets()));

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize discount snapshot for discountId=" + discount.id(), e);
        }
    }

    private List<Map<String, Object>> targetsToMaps(List<DiscountTarget> targets) {
        return targets.stream().map(this::targetToMap).toList();
    }

    private Map<String, Object> targetToMap(DiscountTarget target) {
        // LinkedHashMap, not Map.of(...) — SegmentTarget.referenceId() may be null,
        // and Map.of(...) rejects null values.
        Map<String, Object> map = new LinkedHashMap<>();
        switch (target) {
            case SkuTarget t -> {
                map.put("type", "SKU");
                map.put("itemCode", t.itemCode());
            }
            case CategoryTarget t -> {
                map.put("type", "CATEGORY");
                map.put("categoryId", t.categoryId());
                map.put("includeDescendants", t.includeDescendants());
            }
            case SegmentTarget t -> {
                map.put("type", "SEGMENT");
                map.put("segment", t.segment().name());
                map.put("referenceId", t.referenceId());
            }
        }
        return map;
    }
}
