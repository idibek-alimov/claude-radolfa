package tj.radolfa.domain.model;

/**
 * A flexible marketing label that can be assigned to listing variants.
 * Replaces the hard-coded {@code topSelling} and {@code featured} booleans —
 * new labels can be introduced without schema migrations.
 */
public record ProductTag(Long id, String name, String colorHex) {}
