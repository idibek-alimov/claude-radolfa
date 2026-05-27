package tj.radolfa.domain.model;

/** Resolved display view of a single inventory placement row. binLabel == null → inbound pool. */
public record PlacementView(String binLabel, int quantity) {}
