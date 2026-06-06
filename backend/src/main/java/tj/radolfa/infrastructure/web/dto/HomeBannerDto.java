package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.BannerSlot;
import tj.radolfa.domain.model.HomeBanner;

import java.time.Instant;

public record HomeBannerDto(
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
    public static HomeBannerDto from(HomeBanner banner) {
        return new HomeBannerDto(
                banner.id(),
                banner.slot(),
                banner.title(),
                banner.subtitle(),
                banner.badgeText(),
                banner.ctaLabel(),
                banner.ctaUrl(),
                banner.bgColorHex(),
                banner.expiresAt(),
                banner.active());
    }
}
