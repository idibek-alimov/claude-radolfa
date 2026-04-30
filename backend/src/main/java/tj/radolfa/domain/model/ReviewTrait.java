package tj.radolfa.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A globally-defined review question that can be linked to product categories.
 *
 * <p>Mutable — label and input type may be updated by an admin after creation.
 * The {@code key} is immutable once created (it is the API-stable identifier).
 *
 * <p>Pure Java — zero framework dependencies.
 */
public class ReviewTrait {

    private final Long                 id;
    private final String               key;
    private       String               labelI18n;
    private       ReviewTraitInputType inputType;
    private final Instant              createdAt;
    private       Instant              updatedAt;

    public ReviewTrait(Long id,
                       String key,
                       String labelI18n,
                       ReviewTraitInputType inputType,
                       Instant createdAt,
                       Instant updatedAt) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (!key.matches("^[a-z][a-z0-9_]*$")) {
            throw new IllegalArgumentException(
                "key must start with a lowercase letter and contain only lowercase letters, digits, and underscores, got: " + key);
        }
        if (key.length() > 64) {
            throw new IllegalArgumentException("key must not exceed 64 characters");
        }
        if (labelI18n == null || labelI18n.isBlank()) {
            throw new IllegalArgumentException("labelI18n must not be blank");
        }
        Objects.requireNonNull(inputType, "inputType must not be null");

        this.id        = id;
        this.key       = key;
        this.labelI18n = labelI18n;
        this.inputType = inputType;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public void updateLabel(String newLabelI18n) {
        if (newLabelI18n == null || newLabelI18n.isBlank()) {
            throw new IllegalArgumentException("labelI18n must not be blank");
        }
        this.labelI18n = newLabelI18n;
        this.updatedAt = Instant.now();
    }

    public void updateInputType(ReviewTraitInputType newInputType) {
        Objects.requireNonNull(newInputType, "inputType must not be null");
        this.inputType = newInputType;
        this.updatedAt = Instant.now();
    }

    public Long                 getId()        { return id; }
    public String               getKey()       { return key; }
    public String               getLabelI18n() { return labelI18n; }
    public ReviewTraitInputType getInputType() { return inputType; }
    public Instant              getCreatedAt() { return createdAt; }
    public Instant              getUpdatedAt() { return updatedAt; }
}
