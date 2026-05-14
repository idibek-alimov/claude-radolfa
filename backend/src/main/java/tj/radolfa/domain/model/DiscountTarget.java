package tj.radolfa.domain.model;

public sealed interface DiscountTarget permits SkuTarget, CategoryTarget, SegmentTarget {}
