package tj.radolfa.domain.model;

/** Resolved display view of a single inventory placement row. binLabel/binId == null → inbound pool. */
public record PlacementView(Long binId, String binLabel, int quantity) {}
