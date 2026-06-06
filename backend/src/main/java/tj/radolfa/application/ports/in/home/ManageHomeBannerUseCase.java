package tj.radolfa.application.ports.in.home;

import tj.radolfa.domain.model.BannerSlot;
import tj.radolfa.domain.model.HomeBanner;

import java.time.Instant;

public interface ManageHomeBannerUseCase {

    record Command(
            BannerSlot slot,
            String title,
            String subtitle,
            String badgeText,
            String ctaLabel,
            String ctaUrl,
            String bgColorHex,
            Instant expiresAt,
            boolean active
    ) {}

    HomeBanner create(Command command);
    HomeBanner update(Long id, Command command);
    void delete(Long id);
}
