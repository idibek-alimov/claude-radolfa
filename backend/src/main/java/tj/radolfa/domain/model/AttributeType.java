package tj.radolfa.domain.model;

/**
 * Defines how an attribute's value should be interpreted and validated.
 *
 * <ul>
 *   <li>{@code TEXT}   — freeform single-line string (e.g., "Season: Summer")</li>
 *   <li>{@code NUMBER} — numeric value; unit displayed via {@code unitName} (e.g., "Weight: 200 gr")</li>
 *   <li>{@code ENUM}   — single selection from a fixed list (e.g., "Fit: Oversized")</li>
 *   <li>{@code MULTI}  — multiple selections from a fixed list (e.g., "Material: Cotton, Acrylic")</li>
 * </ul>
 */
public enum AttributeType {
    TEXT, NUMBER, ENUM, MULTI
}
