package tj.radolfa.domain.model;

import java.time.Instant;

public record HomeBanner(
        Long id,
        BannerSlot slot,
        String title,
        String subtitle,
        String badgeText,
        String ctaLabel,
        String ctaUrl,
        String bgColorHex,
        Instant expiresAt,
        boolean active
) {
    public HomeBanner {
        if (slot == null) throw new IllegalArgumentException("slot must not be null");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
    }
}
