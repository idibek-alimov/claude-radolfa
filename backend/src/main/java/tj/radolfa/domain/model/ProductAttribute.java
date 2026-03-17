package tj.radolfa.domain.model;

/**
 * An enrichment key-value attribute for a {@link ListingVariant}.
 *
 * <p>Examples: {@code ("Material", "Organic Wool")}, {@code ("Fit", "Oversized")},
 * {@code ("Care", "Machine wash cold")}, {@code ("Origin", "Tajikistan")}.
 *
 * <p>Owned entirely by the Radolfa content team — never overwritten by ERP sync.
 * Pure Java — zero Spring / JPA / Jackson dependencies.
 */
public record ProductAttribute(String key, String value, int sortOrder) {

    public ProductAttribute {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Attribute key must not be blank");
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Attribute value must not be blank");
    }
}
