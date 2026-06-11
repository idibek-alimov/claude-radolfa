package tj.radolfa.application.event;

import tj.radolfa.domain.model.DiscountTarget;

import java.util.List;

/**
 * Published after a discount campaign is created, updated, enabled, disabled,
 * or deleted, carrying the set of {@link DiscountTarget}s affected by the change.
 *
 * <p>Listeners resolve these targets to {@code ListingVariant} ids and refresh
 * the corresponding Elasticsearch documents (specifically {@code discountPercentage},
 * which is a point-in-time snapshot used by the "Any discount" / minDiscount filter).
 */
public record DiscountTargetsChangedEvent(List<DiscountTarget> targets) {
}
