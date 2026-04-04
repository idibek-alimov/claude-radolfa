package tj.radolfa.domain.model;

import java.util.List;

/**
 * An enrichment attribute for a {@link ListingVariant}, carrying one or more values.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code ("Fit", ["Oversized"], 0)} — ENUM: single selection</li>
 *   <li>{@code ("Material", ["Cotton", "Acrylic"], 1)} — MULTI: multiple selections</li>
 *   <li>{@code ("Weight", ["200"], 2)} — NUMBER: numeric value</li>
 * </ul>
 *
 * <p>Owned entirely by the Radolfa content team.
 * Pure Java — zero Spring / JPA / Jackson dependencies.
 */
public record ProductAttribute(String key, List<String> values, int sortOrder) {

    public ProductAttribute {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Attribute key must not be blank");
        if (values == null || values.isEmpty()) throw new IllegalArgumentException("Attribute values must not be empty");
        for (String v : values) {
            if (v == null || v.isBlank()) throw new IllegalArgumentException("Attribute value must not be blank");
        }
    }
}
