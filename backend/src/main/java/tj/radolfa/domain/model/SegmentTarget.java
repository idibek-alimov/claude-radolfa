package tj.radolfa.domain.model;

import java.util.Objects;

public record SegmentTarget(Segment segment, String referenceId) implements DiscountTarget {
    public SegmentTarget {
        Objects.requireNonNull(segment, "segment must not be null");
    }
}
