package tj.radolfa.domain.model;

import java.time.Instant;

/**
 * Immutable domain representation of a marketplace seller.
 *
 * <p>{@code id} is {@code null} before the first persist. {@code logoUrl} and {@code bio}
 * are optional. {@code createdAt} is set by the persistence layer and may be {@code null}
 * on an unsaved instance.
 */
public record Seller(
        Long id,
        Long userId,      // FK to the owning user account (1:1)
        String shopName,
        String logoUrl,   // nullable
        String bio,       // nullable
        Instant createdAt // nullable before first save
) {
    public Seller {
        if (shopName == null || shopName.isBlank()) {
            throw new IllegalArgumentException("shopName must not be blank");
        }
    }
}
